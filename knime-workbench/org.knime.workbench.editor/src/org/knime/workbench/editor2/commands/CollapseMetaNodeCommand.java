/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   07.05.2011 (mb): created
 */
package org.knime.workbench.editor2.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowAnnotation;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.action.CollapseIntoMetaNodeResult;
import org.knime.core.node.workflow.action.MetaNodeToSubNodeResult;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.workbench.editor2.AnnotationUtilities;
import org.knime.workbench.editor2.editparts.AnnotationEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 *
 * @author M. Berthold, University of Konstanz
 */
public class CollapseMetaNodeCommand extends AbstractKNIMECommand {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(CollapseMetaNodeCommand.class);

    private final NodeID[] m_nodes;
    private final WorkflowAnnotation[] m_annos;
    private final boolean m_encapsulateAsSubnode;
    private final String m_name;
    private CollapseIntoMetaNodeResult m_collapseResult;
    private MetaNodeToSubNodeResult m_metaNodeToSubNodeResult;


    /**
     * @param wfm the workflow manager holding the new metanode
     * @param nodes the ids of the nodes to collapse
     * @param annos the workflow annotations to collapse
     * @param name of new metanode
     */
    private CollapseMetaNodeCommand(final WorkflowManager wfm,
            final NodeID[] nodes, final WorkflowAnnotation[] annos,
            final String name, final boolean encapsulateAsSubnode) {
        super(wfm);
        m_encapsulateAsSubnode = encapsulateAsSubnode;
        m_nodes = nodes.clone();
        m_annos = annos.clone();
        m_name = name;
    }

    /** {@inheritDoc} */
    @Override
    public boolean canExecute() {
        if (!super.canExecute()) {
            return false;
        }
        return null == getHostWFM().canCollapseNodesIntoMetaNode(m_nodes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        try {
            m_collapseResult = getHostWFM().collapseIntoMetaNode(m_nodes, m_annos, m_name);
            if (m_encapsulateAsSubnode) {
                m_metaNodeToSubNodeResult = getHostWFM().convertMetaNodeToSubNode(
                    m_collapseResult.getCollapsedMetanodeID());
            }
        } catch (Exception e) {
            String error = Messages.CollapseMetaNodeCommand_0 + e.getMessage();
            LOGGER.error(error, e);
            MessageDialog.openError(SWTUtilities.getActiveShell(), Messages.CollapseMetaNodeCommand_1, error);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canUndo() {
        return m_collapseResult != null && m_collapseResult.canUndo();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        if (m_metaNodeToSubNodeResult != null) {
            m_metaNodeToSubNodeResult.undo();
        }
        m_collapseResult.undo();
        m_metaNodeToSubNodeResult = null;
        m_collapseResult = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void redo() {
        execute();
    }

    /**
     * @param manager
     * @param nodeParts
     * @param annoParts
     * @param encapsulateAsSubnode TODO
     * @return an Optional Command
     */
    public static Optional<CollapseMetaNodeCommand> create(final WorkflowManager manager,
        final NodeContainerEditPart[] nodeParts, final AnnotationEditPart[] annoParts,
        final boolean encapsulateAsSubnode) {

        final NodeID[] nodeIds = new NodeID[nodeParts.length];
        for (int i = 0; i < nodeParts.length; i++) {
            nodeIds[i] = nodeParts[i].getNodeContainer().getID();
        }
        final WorkflowAnnotation[] annos = AnnotationUtilities.extractWorkflowAnnotations(annoParts);
        try {
            // before testing anything, let's see if we should reset
            // the selected nodes:
            final List<NodeID> resetableIDs = new ArrayList<NodeID>();
            for (NodeID id : nodeIds) {
                if (manager.canResetNode(id)) {
                    resetableIDs.add(id);
                }
            }
            if (resetableIDs.size() > 0) {
                // found some: ask if we can reset, otherwise bail
                final MessageBox mb = new MessageBox(SWTUtilities.getActiveShell(), SWT.OK | SWT.CANCEL);
                mb.setMessage(Messages.CollapseMetaNodeCommand_2);
                mb.setText(Messages.CollapseMetaNodeCommand_3);

                final int dialogreturn = mb.open();
                if (dialogreturn == SWT.CANCEL) {
                    return Optional.empty();
                }
            } else {
                // if there are no resetable nodes we can check if
                // we can collapse - otherwise we need to first reset
                // those nodes (which we don't want to do before we
                // have not gathered all info - and allowed the user
                // to cancel the operation!)
                final String res = manager.canCollapseNodesIntoMetaNode(nodeIds);
                if (res != null) {
                    throw new IllegalArgumentException(res);
                }
            }
            // let the user enter a name
            String name = encapsulateAsSubnode ? "Component" : "Metanode"; //$NON-NLS-1$ //$NON-NLS-2$
            final InputDialog idia = new InputDialog(SWTUtilities.getActiveShell(), Messages.CollapseMetaNodeCommand_6 + name,
                Messages.CollapseMetaNodeCommand_7 + name + ":", name, null); //$NON-NLS-2$

            final int dialogreturn = idia.open();
            if (dialogreturn == Window.CANCEL) {
                return Optional.empty();
            }
            if (dialogreturn == Window.OK) {
                if (resetableIDs.size() > 0) {
                    // do quick&dirty reset: just reset them in random order
                    // and skip the ones that were already reset in passing.
                    for (final NodeID id : resetableIDs) {
                        if (manager.canResetNode(id)) {
                            manager.resetAndConfigureNode(id);
                        }
                    }
                }
                // check if there is another reason why we cannot collapse
                final String res = manager.canCollapseNodesIntoMetaNode(nodeIds);
                if (res != null) {
                    throw new IllegalArgumentException(res);
                }
                name = idia.getValue();
                return Optional.of(new CollapseMetaNodeCommand(manager, nodeIds, annos, name, encapsulateAsSubnode));
            }
        } catch (IllegalArgumentException e) {
            final MessageBox mb = new MessageBox(SWTUtilities.getActiveShell(), SWT.ERROR);
            final String error = Messages.CollapseMetaNodeCommand_9 + e.getMessage();
            LOGGER.error(error, e);
            mb.setMessage(error);
            mb.setText(Messages.CollapseMetaNodeCommand_10);
            mb.open();
        }
        return Optional.empty();
    }

}
