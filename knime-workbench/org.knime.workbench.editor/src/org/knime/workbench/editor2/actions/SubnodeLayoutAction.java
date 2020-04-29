/*
 * ------------------------------------------------------------------------
 *
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
import org.eclipse.jface.wizard.WizardDialog;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.wizard.WizardNode;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowManager.NodeModelFilter;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.subnode.SubnodeLayoutWizard;

/**
 * Action to define layout of wizard nodes in subnode.
 *
 * @author Christian Albrecht, KNIME AG, Zurich, Switzerland
 */
public class SubnodeLayoutAction extends AbstractNodeAction {

    /** unique ID for this action. * */
    public static final String ID = "knime.action.sub_node_layout"; //$NON-NLS-1$

    /** filter for wizard node class */
    @SuppressWarnings("rawtypes")
    public static final NodeModelFilter<WizardNode> WIZARD_NODE_FILTER = new NodeModelFilter<WizardNode>();

    /** filter for dialog node class */
    @SuppressWarnings("rawtypes")
    public static final NodeModelFilter<DialogNode> DIALOG_NODE_FILTER = new NodeModelFilter<DialogNode>();

    /**
     *
     * @param editor The workflow editor
     */
    public SubnodeLayoutAction(final WorkflowEditor editor) {
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
        return Messages.SubnodeLayoutAction_1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/layout_16.png"); //$NON-NLS-1$
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getDisabledImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/layout_16_bw.png"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return Messages.SubnodeLayoutAction_4;
    }

    /**
     * @return <code>true</code>, if subnode
     *         as possible
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    @Override
    protected boolean internalCalculateEnabled() {
        final WorkflowManager manager = getManager();
        if(manager == null) {
            return false;
        }
        if (manager.isWriteProtected()) {
            return false;
        }
        if (manager.getDirectNCParent() instanceof SubNodeContainer) {
            boolean containsViews = !manager.findNodes(WizardNode.class, WIZARD_NODE_FILTER, false, true).isEmpty();
            boolean containsDialog = !manager.findNodes(DialogNode.class, DIALOG_NODE_FILTER, false, false).isEmpty();
            return containsViews || containsDialog;
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        WorkflowManager manager = getManager();
        SubNodeContainer subnode = (SubNodeContainer)manager.getDirectNCParent();
        SubnodeLayoutWizard wizard = new SubnodeLayoutWizard(subnode);
        WizardDialog dlg = new WizardDialog(SWTUtilities.getActiveShell(), wizard);
        dlg.setMinimumPageSize(1000, 600);
        dlg.create();
        dlg.open();
    }
}
