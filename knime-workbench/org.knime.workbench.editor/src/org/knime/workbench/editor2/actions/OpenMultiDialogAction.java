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
import org.knime.core.node.util.NodeExecutionJobManagerPool;
import org.knime.core.node.workflow.NodeContainer.NodeContainerSettings.SplitType;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.ui.wrapper.WrappedMultipleNodeDialog;

/**
 * Action to open the dialog of multiple selected nodes.
 *
 * @author Peter Ohl, KNIME.com AG, Switzerland
 */
public class OpenMultiDialogAction extends AbstractNodeAction {

    /** unique ID for this action. * */
    public static final String ID = "knime.action.openMultipleDialog"; //$NON-NLS-1$

    /**
     *
     * @param editor The workflow editor
     */
    public OpenMultiDialogAction(final WorkflowEditor editor) {
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
        return Messages.OpenMultiDialogAction_1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/openDialog.gif"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return Messages.OpenMultiDialogAction_3;
    }

    /**
     * @return <code>true</code> if at we have a single node which has a dialog
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    @Override
    protected boolean internalCalculateEnabled() {
        NodeContainerEditPart[] selected = getSelectedParts(NodeContainerEditPart.class);
        if (selected.length < 1) {
            return false;
        }

        if (NodeExecutionJobManagerPool.getNumberOfJobManagersFactories() <= 1) {
            return false;
        }
        for (NodeContainerEditPart ep : selected) {
            NodeContainerUI nc = ep.getNodeContainer();
            if (nc.getParent().isWriteProtected()) {
                // don't do it in write protected metanodes.
                return false;
            }
            if (nc.getNodeContainerState().isExecutionInProgress()) {
                // can't do it with executing nodes.
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        NodeID[] nodes = new NodeID[nodeParts.length];
        SplitType splitType = SplitType.USER;
        for (int i = 0; i < nodeParts.length; i++) {
            NodeContainerUI nc = nodeParts[i].getNodeContainer();
            nodes[i] = nc.getID();
            if (nc instanceof WorkflowManagerUI) {
                // one metanode disables splitting
                splitType = SplitType.DISALLOWED;
            }
        }
        WrappedMultipleNodeDialog dlg =
            new WrappedMultipleNodeDialog(SWTUtilities.getActiveShell(), getManager(), splitType, nodes);
        dlg.open(); // the dialog applies new settings on OK
    }
}
