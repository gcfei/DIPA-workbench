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
 *   2010 10 25 (ohl): created
 */
package org.knime.workbench.editor2.directannotationedit;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.tools.CellEditorLocator;
import org.eclipse.gef.tools.DirectEditManager;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.CellEditorActionHandler;
import org.knime.core.node.workflow.Annotation;
import org.knime.core.node.workflow.AnnotationData;
import org.knime.workbench.editor2.AnnotationUtilities;
import org.knime.workbench.editor2.editparts.AnnotationEditPart;
import org.knime.workbench.editor2.editparts.FontStore;
import org.knime.workbench.editor2.editparts.NodeAnnotationEditPart;

/**
 * @author ohl, KNIME AG, Zurich, Switzerland
 */
public class AnnotationEditManager extends DirectEditManager {
    /*
     * Since we can only ever have one annotation in edit across the application (edit ends on focus loss and on active
     * tab change in the workflow editor part,) the singleton nature of this is fine.
     */
    private static GraphicalEditPart PART_CURRENTLY_IN_EDIT = null;

    /**
     * @param aep the AnnotationEditPart for which we wish to know whether is should veto its selection (due to another
     *            AnnotationEditPart being in edit.) This request arose due to AP-11008.
     * @return false if the param value representing the AnnotationEditPart instance which is currently being edited, or
     *         nothing is being edited
     */
    public static boolean partShouldVetoSelection(final AnnotationEditPart aep) {
        return (PART_CURRENTLY_IN_EDIT != null) && (!aep.equals(PART_CURRENTLY_IN_EDIT));
    }


    private IActionBars m_actionBars;

    private CellEditorActionHandler m_actionHandler;

    private IAction m_copy;
    private IAction m_cut;
    private IAction m_paste;
    private IAction m_undo;
    private IAction m_redo;
    private IAction m_find;
    private IAction m_selectAll;
    private IAction m_delete;

    private Point m_mouseDownLocation;

    /**
     * Constructs an instace of the edit manager.
     *
     * @param editPart
     * @param locator
     */
    public AnnotationEditManager(final AnnotationEditPart editPart, final CellEditorLocator locator) {
        super(editPart, StyledTextEditor.class, locator);
    }

    /**
     * Initializes the cell editor.
     *
     * @see org.eclipse.gef.tools.DirectEditManager#initCellEditor()
     */
    @Override
    protected void initCellEditor() {
        // de-select the underlying annotation to remove the selection handles
        final GraphicalEditPart editPart = getEditPart();
        editPart.getRoot().getViewer().deselectAll();
        editPart.getFigure().setVisible(false);

        final StyledTextEditor editor = (StyledTextEditor)getCellEditor();
        final Annotation annotation = ((AnnotationEditPart)editPart).getModel();
        final Font defaultFont;
        if (editPart instanceof NodeAnnotationEditPart) {
            defaultFont = AnnotationUtilities.getNodeAnnotationDefaultFont();
            editor.pruneMenuAndToolbarForNodeAnnotation();
        } else if (annotation.getVersion() < AnnotationData.VERSION_20151012) {
            defaultFont = FontStore.INSTANCE.getSystemDefaultFont();
        } else {
            defaultFont = AnnotationUtilities.getWorkflowAnnotationDefaultFont();
        }
        editor.setDefaultFont(defaultFont);
        editor.setValue(annotation);
        editor.setMouseDownLocation(m_mouseDownLocation);

        // Hook the cell editor's copy/paste actions to the actionBars so that
        // they can
        // be invoked via keyboard shortcuts.
        m_actionBars = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getActivePage().getActiveEditor().getEditorSite()
                .getActionBars();
        saveCurrentActions(m_actionBars);
        m_actionHandler = new CellEditorActionHandler(m_actionBars);
        m_actionHandler.addCellEditor(getCellEditor());
        m_actionBars.updateActionBars();
    }

    /**
     * @param p the location of the mouse click which started the edit using this editor, if it is known. If this is
     *            non-null, it will be used to set the caret position on display of the editor.
     */
    public void setMouseDownLocation(final Point p) {
        m_mouseDownLocation = p;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void show() {
        PART_CURRENTLY_IN_EDIT = getEditPart();

        super.show();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void bringDown() {
        super.bringDown();

        if (m_actionHandler != null) {
            m_actionHandler.dispose();
            m_actionHandler = null;
        }

        if (m_actionBars != null) {
            restoreSavedActions(m_actionBars);
            m_actionBars.updateActionBars();
            m_actionBars = null;
        }

        final GraphicalEditPart editPart = getEditPart();
        // select later as the "commit" mouse-click maybe in the region of the
        // figure (which would trigger another edit). This problem would only
        // occur if zoom > 1 (as the edit area is smaller than the figure)
        Display.getCurrent().asyncExec(() -> {
            if (Display.getCurrent().isDisposed()) {
                PART_CURRENTLY_IN_EDIT = null;

                return;
            }

            final EditPartViewer v = editPart.getRoot().getViewer();
            editPart.getFigure().setVisible(true);
            v.deselectAll();
            v.appendSelection(editPart);

            PART_CURRENTLY_IN_EDIT = null;
        });
    }

    private void restoreSavedActions(final IActionBars actionBars) {
        actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), m_copy);
        actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(), m_paste);
        actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(), m_delete);
        actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), m_selectAll);
        actionBars.setGlobalActionHandler(ActionFactory.CUT.getId(), m_cut);
        actionBars.setGlobalActionHandler(ActionFactory.FIND.getId(), m_find);
        actionBars.setGlobalActionHandler(ActionFactory.UNDO.getId(), m_undo);
        actionBars.setGlobalActionHandler(ActionFactory.REDO.getId(), m_redo);
    }

    private void saveCurrentActions(final IActionBars actionBars) {
        m_copy = actionBars.getGlobalActionHandler(ActionFactory.COPY.getId());
        m_paste = actionBars.getGlobalActionHandler(ActionFactory.PASTE.getId());
        m_delete = actionBars.getGlobalActionHandler(ActionFactory.DELETE.getId());
        m_selectAll = actionBars.getGlobalActionHandler(ActionFactory.SELECT_ALL.getId());
        m_cut = actionBars.getGlobalActionHandler(ActionFactory.CUT.getId());
        m_find = actionBars.getGlobalActionHandler(ActionFactory.FIND.getId());
        m_undo = actionBars.getGlobalActionHandler(ActionFactory.UNDO.getId());
        m_redo = actionBars.getGlobalActionHandler(ActionFactory.REDO.getId());
    }
}
