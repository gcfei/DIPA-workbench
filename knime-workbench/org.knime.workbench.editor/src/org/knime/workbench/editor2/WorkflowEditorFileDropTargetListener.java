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
 */
package org.knime.workbench.editor2;

import static org.knime.core.ui.wrapper.Wrapper.wraps;

import java.io.File;
import java.net.URL;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowManager;

/**
 *
 * @author ohl, University of Konstanz
 * @author Dominik Morent, KNIME AG, Zurich, Switzerland
 */
public class WorkflowEditorFileDropTargetListener
        extends WorkflowEditorDropTargetListener<ReaderNodeCreationFactory> {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(WorkflowEditorFileDropTargetListener.class);

    /**
     * @param viewer the edit part viewer this drop target listener is attached
     *      to
     */
    public WorkflowEditorFileDropTargetListener(final EditPartViewer viewer) {
        super(viewer, new ReaderNodeCreationFactory());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void drop(final DropTargetEvent event) {
        String file = getFile(event);
        if(file == null) {
            return;
        }
        // Set the factory on the current request
        URL url;
        try {
            url = new File(file).toURI().toURL();
            getFactory().setReaderNodeSettings(new ReaderNodeSettings(getNodeFactory(url), url));
            super.handleDrop();
        } catch (Exception ex) {
            LOGGER.error(Messages.WorkflowEditorFileDropTargetListener_0 + file + "): " + ex.getMessage(), ex); //$NON-NLS-2$
        }
    }
    /**
     * @param event
     * @return
     */
    private String getFile(final DropTargetEvent event) {
        if(event.data == null) {
            return null;
        }
        String[] filePaths = (String[])event.data;
        if (filePaths.length > 1) {
            LOGGER.warn(Messages.WorkflowEditorFileDropTargetListener_2);
        }
        return filePaths[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Transfer getTransfer() {
        return FileTransfer.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled(final DropTargetEvent event) {
        //not yet supported by WorkflowManagerUI-implementations
        if (wraps(getWorkflowManager(), WorkflowManager.class)) {
            event.feedback = DND.FEEDBACK_SELECT;
            event.operations = DND.DROP_DEFAULT;
            event.detail = DND.DROP_DEFAULT;
            return true;
        } else {
            return false;
        }
    }


}
