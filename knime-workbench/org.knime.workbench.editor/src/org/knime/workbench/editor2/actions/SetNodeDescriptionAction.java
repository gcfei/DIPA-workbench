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
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.knime.core.node.NodeLogger;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Action to set the custom description for a node.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class SetNodeDescriptionAction extends AbstractNodeAction {
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(SetNodeDescriptionAction.class);

    /**
     * unique ID for this action.
     */
    public static final String ID = "knime.action.setnameanddescription"; //$NON-NLS-1$

    /**
     * @param editor The workflow editor
     */
    public SetNodeDescriptionAction(final WorkflowEditor editor) {
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
        return Messages.SetNodeDescriptionAction_1 + getHotkey("knime.commands.setNameAndDescription"); //$NON-NLS-2$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/setNameDescription.PNG"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return Messages.SetNodeDescriptionAction_4;
    }

    /**
     * @return <code>true</code>, if just one node part is selected.
     *
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    @Override
    protected boolean internalCalculateEnabled() {
        if (getManager().isWriteProtected()) {
            return false;
        }
        NodeContainerEditPart[] parts =
            getSelectedParts(NodeContainerEditPart.class);

        // only if just one node part is selected
        if (parts.length != 1) {
            return false;
        }

        return true;
    }

    /**
     * Opens a dialog and collects the user name and description. After the
     * dialog is closed the new name and description are set to the node
     * container if applicable.
     *
     * {@inheritDoc}
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        // if more than one node part is selected
        if (nodeParts.length != 1) {
            LOGGER.debug(
                Messages.SetNodeDescriptionAction_5 + Messages.SetNodeDescriptionAction_6);
            return;
        }

        final NodeContainerUI container = nodeParts[0].getNodeContainer();

        try {
            final Shell parent = SWTUtilities.getActiveShell();

            String initialDescr = ""; //$NON-NLS-1$
            if (container.getCustomDescription() != null) {
                initialDescr = container.getCustomDescription();
            }
            final String dialogTitle = container.getDisplayLabel();

            final NodeDescriptionDialog dialog = new NodeDescriptionDialog(parent, dialogTitle, initialDescr,
                // (bugfix 1402)
                container.getID());

            final int result = dialog.open();

            // check if ok was pressed
            if (result == Window.OK) {
                container.setCustomDescription(dialog.getDescription());
            }
        } catch (final Exception ex) {
            LOGGER.error(Messages.SetNodeDescriptionAction_8 + ex.getMessage(), ex);
        }
    }
}
