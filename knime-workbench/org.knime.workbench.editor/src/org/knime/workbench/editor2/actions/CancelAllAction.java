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
 * History
 *   25.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.actions;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Action to cancel all nodes that are running.
 *
 * @author Christoph sieb, University of Konstanz
 */
public class CancelAllAction extends AbstractNodeAction {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(CancelAllAction.class);

    /** unique ID for this action. * */
    public static final String ID = "knime.action.cancelall"; //$NON-NLS-1$

    /**
     *
     * @param editor The workflow editor
     */
    public CancelAllAction(final WorkflowEditor editor) {
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
        return Messages.CancelAllAction_1 + getHotkey("knime.commands.cancelall"); //$NON-NLS-2$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/cancelAll.PNG"); //$NON-NLS-1$
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getDisabledImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/cancelAll_disabled.PNG"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return Messages.CancelAllAction_5;
    }

    /**
     * @return <code>true</code>, if at least one node is running
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    @Override
    protected boolean internalCalculateEnabled() {
        WorkflowManagerUI manager = getManagerUI();
        if (Wrapper.wraps(manager, WorkflowManager.class) && manager.getParent() == null) {
            //the default workflow manager implementation has a dedicate root workflow manager as parent
            //otherwise it's the root itself
            return false;
        }
        return manager.canCancelAll();
    }

    /**
     * This cancels all running jobs.
     *
     * {@inheritDoc}
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        MessageBox mb = new MessageBox(SWTUtilities.getActiveShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
        mb.setText(Messages.CancelAllAction_6);
        mb.setMessage(Messages.CancelAllAction_7);
        if (mb.open() != SWT.YES) {
            return;
        }
        LOGGER.debug(Messages.CancelAllAction_8);
        WorkflowManagerUI manager = getManagerUI();
        if (manager.getParent() == null) {
            //can't just cancel the parent -> try canceling every single node
            manager.getNodeContainers().stream().filter(nc -> nc.getNodeContainerState().isExecutionInProgress())
                .forEach(nc -> manager.cancelExecution(nc));
        } else {
            //just cancel the parent
            manager.getParent().cancelExecution(manager);
        }
        try {
            // Give focus to the editor again. Otherwise the actions (selection)
            // is not updated correctly.
            getWorkbenchPart().getSite().getPage().activate(getWorkbenchPart());
        } catch (Exception e) {
            // ignore
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean canHandleWorkflowManagerUI() {
        return true;
    }
}
