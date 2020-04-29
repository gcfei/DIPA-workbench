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
 * -------------------------------------------------------------------
 *
 */
package org.knime.workbench.editor2.actions;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.node.workflow.SubNodeContainerUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.commands.ExpandSubNodeCommand;
import org.knime.workbench.editor2.editparts.GUIWorkflowCipherPrompt;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Action to expand the selected sub node.
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
public class ExpandSubNodeAction extends AbstractNodeAction {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ExpandSubNodeAction.class);

    /**
     * unique ID for this action.
     */
    public static final String ID = "knime.action.expandsubnode"; //$NON-NLS-1$

    /**
     * @param editor The workflow editor
     */
    public ExpandSubNodeAction(final WorkflowEditor editor) {
        super(editor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return Messages.ExpandSubNodeAction_1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/meta/metanode_expand.png"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return Messages.ExpandSubNodeAction_3;
    }

    /**
     * @return <code>true</code>, if exactly one sub node is selected.
     *
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    @Override
    protected boolean internalCalculateEnabled() {
        if (getManager().isWriteProtected()) {
            return false;
        }
        NodeContainerEditPart[] parts = getSelectedParts(NodeContainerEditPart.class);
        if (parts.length != 1) {
            return false;
        }
        if (parts[0].getNodeContainer() instanceof SubNodeContainerUI) {
            WorkflowManagerUI wm = ((SubNodeContainerUI)parts[0].getNodeContainer()).getWorkflowManager();
            return !wm.isWriteProtected();
        }
        return false;
    }

    /**
     * Expand sub node!
     *
     * {@inheritDoc}
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        if (nodeParts.length < 1) {
            return;
        }

        LOGGER.debug(Messages.ExpandSubNodeAction_4 + nodeParts.length + Messages.ExpandSubNodeAction_5);
        try {
            WorkflowManager manager = getManager();
            SubNodeContainer subNode = Wrapper.unwrap(nodeParts[0].getNodeContainer(), SubNodeContainer.class);
            if (!subNode.getWorkflowManager().unlock(new GUIWorkflowCipherPrompt())) {
                return;
            }
            // before we do anything, let's see if an expand will
            // reset the metanode
            if (manager.canResetNode(subNode.getID())) {
                // yes: ask if we can reset, otherwise bail
                MessageBox mb = new MessageBox(SWTUtilities.getActiveShell(), SWT.OK | SWT.CANCEL);
                mb.setMessage(Messages.ExpandSubNodeAction_6 + Messages.ExpandSubNodeAction_7);
                mb.setText(Messages.ExpandSubNodeAction_8);
                int dialogreturn = mb.open();
                if (dialogreturn == SWT.CANCEL) {
                    return;
                }
                // perform reset
                if (manager.canResetNode(subNode.getID())) {
                    manager.resetAndConfigureNode(subNode.getID());
                }
            }
            String res = manager.canExpandSubNode(subNode.getID());
            if (res != null) {
                throw new IllegalArgumentException(res);
            }
            ExpandSubNodeCommand emnc = new ExpandSubNodeCommand(manager, subNode.getID(), getEditor());
            execute(emnc);
        } catch (IllegalArgumentException e) {
            MessageBox mb = new MessageBox(SWTUtilities.getActiveShell(), SWT.ERROR);
            mb.setMessage(Messages.ExpandSubNodeAction_9 + e.getMessage());
            mb.setText(Messages.ExpandSubNodeAction_10);
            mb.open();
        }
        try {
            // Give focus to the editor again. Otherwise the actions (selection)
            // is not updated correctly.
            getWorkbenchPart().getSite().getPage().activate(getWorkbenchPart());
        } catch (Exception e) {
            // ignore
        }
    }

}
