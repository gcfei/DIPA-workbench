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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.NodeLogger;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * Action to reset a node.
 *
 * @author Florian Georg, University of Konstanz
 */
public class ResetAction extends AbstractNodeAction {
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(ResetAction.class);

    /** unique ID for this action. * */
    public static final String ID = "knime.action.reset"; //$NON-NLS-1$

    /**
     *
     * @param editor The workflow editor
     */
    public ResetAction(final WorkflowEditor editor) {
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
        return Messages.ResetAction_1 + getHotkey("knime.commands.reset"); //$NON-NLS-2$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/resetNode.gif"); //$NON-NLS-1$
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getDisabledImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/resetNode_disabled.gif"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return Messages.ResetAction_5;
    }

    /**
     * Resets all nodes, this is lightweight and does not need to be executed
     * inside an async job.
     *
     * {@inheritDoc}
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        // the following code has mainly been copied from
        // IDEWorkbenchWindowAdvisor#preWindowShellClose
        IPreferenceStore store =
            KNIMEUIPlugin.getDefault().getPreferenceStore();
        if (!store.contains(PreferenceConstants.P_CONFIRM_RESET)
                || store.getBoolean(PreferenceConstants.P_CONFIRM_RESET)) {
            MessageDialogWithToggle dialog =
                MessageDialogWithToggle.openOkCancelConfirm(
                    SWTUtilities.getActiveShell(),
                    Messages.ResetAction_6,
                    Messages.ResetAction_7,
                    Messages.ResetAction_8, false, null, null);
            if (dialog.getReturnCode() != IDialogConstants.OK_ID) {
                return;
            }
            if (dialog.getToggleState()) {
                store.setValue(PreferenceConstants.P_CONFIRM_RESET, false);
                KNIMEUIPlugin.getDefault().savePluginPreferences();
            }
        }

        LOGGER.debug(Messages.ResetAction_9 + nodeParts.length + Messages.ResetAction_10);
        try {
            for (int i = 0; i < nodeParts.length; i++) {
                NodeContainerEditPart part = nodeParts[i];
                NodeContainerUI nc = part.getNodeContainer();
                if (getManagerUI().canResetNode(nc.getID())) {
                    // skip locked nodes
                    getManagerUI().resetAndConfigureNode(nodeParts[i].getNodeContainer().getID());
                }
            }

        } catch (Exception ex) {
            LOGGER.warn(Messages.ResetAction_11, ex);
            MessageBox mb = new MessageBox(SWTUtilities.getActiveShell(), SWT.ICON_INFORMATION | SWT.OK);
            mb.setText(Messages.ResetAction_12);
            mb.setMessage(Messages.ResetAction_13
                    + Messages.ResetAction_14 + ex.getMessage());
            mb.open();
        }

    }

    /**
     * @return <code>true</code> if at least one node is resetable
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    @Override
    protected boolean internalCalculateEnabled() {
        NodeContainerEditPart[] parts =
            getSelectedParts(NodeContainerEditPart.class);
        for (int i = 0; i < parts.length; i++) {
            NodeContainerEditPart part = parts[i];
            NodeContainerUI nc = part.getNodeContainer();
            if (getManagerUI().canResetNode(nc.getID())) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean canHandleWorkflowManagerUI() {
        return true;
    }

}
