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
 * ---------------------------------------------------------------------
 *
 * History
 *   24.03.2015 (tibuch): created
 */
package org.knime.workbench.editor2;

import static org.knime.core.ui.wrapper.Wrapper.wraps;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.repository.model.AbstractNodeTemplate;
import org.knime.workbench.repository.model.MetaNodeTemplate;

/**
 * A DropTargetListener for metanode drops from the node repository.
 *
 * @author Tim-Oliver Buchholz, KNIME AG, Zurich, Switzerland
 */
public class MetaNodeDropTargetListener extends
    WorkflowEditorDropTargetListener<MetaNodeCreationFactory> {

    /**
     * @param viewer the viewer
     */
    protected MetaNodeDropTargetListener(final EditPartViewer viewer) {
        super(viewer, new MetaNodeCreationFactory());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled(final DropTargetEvent event) {
        AbstractNodeTemplate snt = getSelectionNodeTemplate();
        //not yet supported by WorkflowManagerUI-implementations
        if (snt != null && wraps(getWorkflowManager(), WorkflowManager.class)) {
            event.feedback = DND.FEEDBACK_SELECT;
            event.operations = DND.DROP_COPY;
            event.detail = DND.DROP_COPY;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handleDrop() {
        MetaNodeTemplate template = getSelectionNodeTemplate();
        getFactory().setMetaNodeTemplate(template);
        super.handleDrop();
    }

    private MetaNodeTemplate getSelectionNodeTemplate() {
        if (LocalSelectionTransfer.getTransfer().getSelection() == null) {
            return null;
        }
        if (((IStructuredSelection)LocalSelectionTransfer.getTransfer().getSelection()).size() > 1) {
            // allow dropping a single node only
            return null;
        }

        Object template = ((IStructuredSelection)LocalSelectionTransfer.getTransfer().getSelection()).getFirstElement();
        if (template instanceof MetaNodeTemplate) {
            return (MetaNodeTemplate)template;
        }
        // Last change: Ask adaptables for an adapter object
        if (template instanceof IAdaptable) {
            return (MetaNodeTemplate)((IAdaptable)template).getAdapter(MetaNodeTemplate.class);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Transfer getTransfer() {
        return LocalSelectionTransfer.getTransfer();
    }

}
