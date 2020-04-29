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
 *   04.02.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2;

import static org.knime.core.ui.wrapper.Wrapper.wraps;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.view.ContentObject;

/**
 * @author Bernd Wiswedel, KNIME.com, Zurich Switzerland
 * @author Peter Ohl, KNIME.com, Zurich Switzerland
 */
public class MetaNodeTemplateDropTargetListener extends WorkflowEditorDropTargetListener<IFileStoreFactory> {
    /**
     * @param v the edit part viewer this drop target listener is attached
     *            to
     */
    public MetaNodeTemplateDropTargetListener(final EditPartViewer v) {
        super(v, new IFileStoreFactory());
    }

    /** {@inheritDoc} */
    @Override
    protected void handleDrop() {
        final ContentObject obj = getDragResources(getCurrentEvent());
        final AbstractExplorerFileStore store =  obj.getObject();
        if (AbstractExplorerFileStore.isWorkflowTemplate(store)) {
            getFactory().setSourceFileStore(store);
            super.handleDrop();
        } else {
            clearTransferSelection();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled(final DropTargetEvent event) {
        if (!super.isEnabled(event)) {
            return false;
        }
        final AbstractExplorerFileStore fileStore = getDragResources(event).getObject();
        final boolean isMetaNodeTemplate = AbstractExplorerFileStore.isWorkflowTemplate(fileStore);
        //not yet supported by WorkflowManagerUI-implementations
        if (isMetaNodeTemplate && wraps(getWorkflowManager(), WorkflowManager.class)) {
            event.feedback = DND.FEEDBACK_SELECT;
            event.operations = DND.DROP_COPY | DND.DROP_LINK;
            event.detail = DND.DROP_COPY;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Transfer getTransfer() {
        return LocalSelectionTransfer.getTransfer();
    }
}
