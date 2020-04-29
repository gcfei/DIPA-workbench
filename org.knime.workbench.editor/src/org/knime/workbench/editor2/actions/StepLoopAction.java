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
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.LoopEndNode;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NativeNodeContainer.LoopStatus;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Action to step execution of a loop.
 *
 * @author M. Berthold, University of Konstanz
 */
public class StepLoopAction extends AbstractNodeAction {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(StepLoopAction.class);

    /**
     * unique ID for this action.
     */
    public static final String ID = "knime.action.steploop"; //$NON-NLS-1$

    /**
     * @param editor The workflow editor
     */
    public StepLoopAction(final WorkflowEditor editor) {
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
        return Messages.StepLoopAction_1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/step.png"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getDisabledImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/step_disabled.png"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return Messages.StepLoopAction_4;
    }

    /**
     * @return <code>true</code>, if just one loop end node part is selected
     *         which is executable and a loop is in progress.
     *
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    @Override
    protected boolean internalCalculateEnabled() {
        NodeContainerEditPart[] parts =
            getSelectedParts(NodeContainerEditPart.class);
        if (parts.length != 1) {
            return false;
        }
        // enabled if the one selected node is a configured and "in progress"
        // LoopEndNode
        NodeContainerUI nc = parts[0].getNodeContainer();
        if (Wrapper.wraps(nc, NativeNodeContainer.class)) {
            NativeNodeContainer nnc = Wrapper.unwrap(nc, NativeNodeContainer.class);
            if (nnc.isModelCompatibleTo(LoopEndNode.class) && nnc.getLoopStatus().equals(LoopStatus.PAUSED)) {
                // either the node is paused...
                return true;
            }
            WorkflowManager wm = getEditor().getWorkflowManager().get();
            if (wm.canExecuteNodeDirectly(nc.getID())) {
                // ...or we can execute it (then this will be the first step)
                return true;
            }
        }
        return false;
    }

    /**
     * Perform one step through the entire loop and pause again.
     *
     * {@inheritDoc}
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        LOGGER.debug(Messages.StepLoopAction_5
                + nodeParts.length + Messages.StepLoopAction_6);
        WorkflowManager manager = getManager();
        for (NodeContainerEditPart p : nodeParts) {
            NodeContainerUI nc = p.getNodeContainer();
            if (Wrapper.wraps(nc, NativeNodeContainer.class)) {
                NativeNodeContainer nnc = Wrapper.unwrap(nc, NativeNodeContainer.class);
                if (nnc.isModelCompatibleTo(LoopEndNode.class) && nnc.getLoopStatus().equals(LoopStatus.PAUSED)) {
                    manager.resumeLoopExecution(nnc, /*oneStep=*/true);
                } else if (manager.canExecuteNodeDirectly(nc.getID())) {
                    manager.executeUpToHere(nc.getID());
                    manager.pauseLoopExecution(nnc);
                }
            }
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
