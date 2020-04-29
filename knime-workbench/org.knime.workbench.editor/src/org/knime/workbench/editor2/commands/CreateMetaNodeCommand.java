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
package org.knime.workbench.editor2.commands;

import java.util.Arrays;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.WorkflowAnnotation;
import org.knime.core.node.workflow.WorkflowCopyContent;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.ui.util.SWTUtilities;

/**
 * GEF command for adding a metanode from the repository to the workflow.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class CreateMetaNodeCommand extends AbstractKNIMECommand {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(CreateMetaNodeCommand.class);

    private final WorkflowPersistor m_persistor;

    /**
     * Location of the new metanode.
     * @since 2.12
     */
    protected final Point m_location;

    /**
     * Snap metanode to grid.
     * @since 2.12
     */
    protected final boolean m_snapToGrid;

    // for undo
    private WorkflowCopyContent m_copyContent;

    /**
     * Container of the new metanode.
     * @since 2.12
     */
    protected NodeContainer m_container;

    /**
     * Creates a new command.
     *
     * @param manager The workflow manager that should host the new node
     * @param persistor the paste content
     * @param location Initial visual location in the
     * @param snapToGrid if node location should be rounded to closest grid location.
     */
    public CreateMetaNodeCommand(final WorkflowManager manager,
            final WorkflowPersistor persistor, final Point location, final boolean snapToGrid) {
        super(manager);
        m_persistor = persistor;
        m_location = location;
        m_snapToGrid = snapToGrid;
    }

    /** We can execute, if all components were 'non-null' in the constructor.
     * {@inheritDoc} */
    @Override
    public boolean canExecute() {
        return super.canExecute() && m_persistor != null && m_location != null;
    }

    /** {@inheritDoc} */
    @Override
    public void execute() {
        // Add node to workflow and get the container
        try {
            WorkflowManager wfm = getHostWFM();
            m_copyContent = wfm.paste(m_persistor);
            NodeID[] nodeIDs = m_copyContent.getNodeIDs();
            if (nodeIDs.length > 0) {
                NodeID first = nodeIDs[0];
                m_container = wfm.getNodeContainer(first);
                // create extra info and set it
                NodeUIInformation info = NodeUIInformation.builder()
                		.setNodeLocation(m_location.x, m_location.y, -1, -1)
                		.setHasAbsoluteCoordinates(false)
                		.setSnapToGrid(m_snapToGrid).setIsDropLocation(true).build();
                m_container.setUIInformation(info);
            }
        } catch (Throwable t) {
            // if fails notify the user
            String error = Messages.CreateMetaNodeCommand_0;
            LOGGER.debug(error + ": " + t.getMessage(), t); //$NON-NLS-1$
            MessageBox mb = new MessageBox(SWTUtilities.getActiveShell(), SWT.ICON_WARNING | SWT.OK);
            mb.setText(error);
            mb.setMessage(Messages.CreateMetaNodeCommand_2
                    + Messages.CreateMetaNodeCommand_3 + t.getMessage());
            mb.open();
            return;
        }

    }

    /** {@inheritDoc} */
    @Override
    public boolean canUndo() {
        if (m_copyContent == null) {
            return false;
        }
        NodeID[] ids = m_copyContent.getNodeIDs();
        for (NodeID id : ids) {
            if (!getHostWFM().canRemoveNode(id)) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        NodeID[] ids = m_copyContent.getNodeIDs();
        if (LOGGER.isDebugEnabled()) {
            String debug = Messages.CreateMetaNodeCommand_4;
            if (ids.length == 1) {
                debug = debug + " " + ids[0]; //$NON-NLS-1$
            } else {
                debug = debug + " " + Arrays.asList(ids);
            }
            LOGGER.debug(debug);
        }
        WorkflowManager wm = getHostWFM();
        if (canUndo()) {
            for (NodeID id : ids) {
                wm.removeNode(id);
            }
            for (WorkflowAnnotation anno : wm.getWorkflowAnnotations(m_copyContent.getAnnotationIDs())) {
                wm.removeAnnotation(anno);
            }
        } else {
            MessageDialog.openInformation(SWTUtilities.getActiveShell(),
                    Messages.CreateMetaNodeCommand_7, Messages.CreateMetaNodeCommand_8
                    + Arrays.asList(ids) + Messages.CreateMetaNodeCommand_9);
        }
    }
}
