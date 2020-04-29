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
 *   ${date} (${user}): created
 */
package org.knime.workbench.editor2;

import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.ui.actions.ActionBarContributor;
import org.eclipse.gef.ui.actions.DeleteRetargetAction;
import org.eclipse.gef.ui.actions.RedoRetargetAction;
import org.eclipse.gef.ui.actions.UndoRetargetAction;
import org.eclipse.gef.ui.actions.ZoomComboContributionItem;
import org.eclipse.gef.ui.actions.ZoomInRetargetAction;
import org.eclipse.gef.ui.actions.ZoomOutRetargetAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.RetargetAction;
import org.knime.core.node.util.NodeExecutionJobManagerPool;

/**
 * Contributes action to the toolbar / menu bar.
 *
 * @author Florian Georg, University of Konstanz
 */
public class WorkflowEditorActionBarContributor extends ActionBarContributor {
    private WorkflowEditor m_editor;
    private ZoomComboContributionItem m_zoomComboBox;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void buildActions() {
        addRetargetAction(new UndoRetargetAction());
        addRetargetAction(new RedoRetargetAction());
        addRetargetAction(new DeleteRetargetAction());
        addRetargetAction(new ZoomInRetargetAction());
        addRetargetAction(new ZoomOutRetargetAction());
        addRetargetAction(new RetargetAction(ActionFactory.SAVE_AS.getId(), "Save As"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void declareGlobalActionKeys() {
        addGlobalActionKey(ActionFactory.PRINT.getId());
        addGlobalActionKey(ActionFactory.SELECT_ALL.getId());
        addGlobalActionKey(ActionFactory.PASTE.getId());
        addGlobalActionKey(ActionFactory.COPY.getId());
        addGlobalActionKey(ActionFactory.CUT.getId());
        addGlobalActionKey(ActionFactory.SAVE_AS.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void contributeToToolBar(final IToolBarManager tbm) {
        tbm.add(new Separator());
        final String[] zoomStrings = new String[]{ZoomManager.FIT_ALL, ZoomManager.FIT_HEIGHT, ZoomManager.FIT_WIDTH};
        m_zoomComboBox = new ZoomComboContributionItem(getPage(), zoomStrings);
        tbm.add(m_zoomComboBox);

        // In 4.7 RCPs, this is called before setActiveEditor, but we check in both places as future-proofing
        if (m_editor != null) {
            m_editor.setZoomComboBox(m_zoomComboBox);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setActiveEditor(final IEditorPart editor) {
        if (NodeExecutionJobManagerPool.getNumberOfJobManagersFactories() <= 1) {
            editor.getEditorSite().getActionBars().getToolBarManager()
                    .remove("org.knime.workbench.editor.actions.openMultiDialog");
        }
        if (editor instanceof WorkflowEditor) {
            m_editor = (WorkflowEditor)editor;

            // In 4.7 RCPs, this is called after contributeToToolBar, but we check in both places as future-proofing
            if (m_zoomComboBox != null) {
                m_editor.setZoomComboBox(m_zoomComboBox);
            }
        }
        super.setActiveEditor(editor);
    }
}
