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
 *   25.03.2015 (tibuch): created
 */
package org.knime.workbench.editor2.commands;

import java.net.URI;
import java.util.Collections;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.RootEditPart;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;

/**
 *
 * @author Tim-Oliver Buchholz, KNIME AG, Zurich, Switzerland
 */
public class InsertMetaNodeTempalteCommand extends CreateMetaNodeTemplateCommand {

    private ConnectionContainer m_edge;

    private final RootEditPart m_root;

    private DeleteCommand m_delete;

    private InsertHelper m_ih;

    /**
     * @param manager the workflow manager
     * @param templateFolder the folder of the metanode template
     * @param location the insert location of the new metanode template
     * @param snapToGrid should metanode snap to grid
     * @param edge on which the metanode should be inserted
     * @throws IllegalArgumentException if the passed file store doesn't represent a workflow template
     */
    public InsertMetaNodeTempalteCommand(final WorkflowManager manager, final AbstractExplorerFileStore templateFolder,
        final Point location, final boolean snapToGrid, final ConnectionContainerEditPart edge) {
        super(manager, templateFolder, location, snapToGrid);
        m_edge = Wrapper.unwrapCC(edge.getModel());
        m_root = edge.getRoot();
        m_ih = new InsertHelper(getHostWFM(), m_edge);

        // delete command handles undo and restores all connections and node correctly
        m_delete = new DeleteCommand(Collections.singleton(edge), manager);
    }

    /**
     * @param manager the workflow manager
     * @param templateURI the URI to the folder or file of the metanode template
     * @param location the insert location of the new metanode template
     * @param snapToGrid should metanode snap to grid
     * @param edge on which the metanode should be inserted
     * @param isRemoteLocation if the workflow template needs to be downloaded first (determines whether to show a busy
     *            cursor on command execution)
     */
    public InsertMetaNodeTempalteCommand(final WorkflowManager manager, final URI templateURI, final Point location,
        final boolean snapToGrid, final ConnectionContainerEditPart edge, final boolean isRemoteLocation) {
        super(manager, templateURI, location, snapToGrid, isRemoteLocation);
        m_edge = Wrapper.unwrapCC(edge.getModel());
        m_root = edge.getRoot();
        m_ih = new InsertHelper(getHostWFM(), m_edge);

        // delete command handles undo and restores all connections and node correctly
        m_delete = new DeleteCommand(Collections.singleton(edge), manager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canExecute() {
        return super.canExecute() && m_delete.canExecute() && m_ih.canInsertNode(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canRedo() {
        return super.canExecute() && m_delete.canExecute() && m_ih.canInsertNode(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        m_delete.execute();
        super.execute();
        m_ih.reconnect(m_container, m_snapToGrid, m_location.x, m_location.y);
        // the connections are not always properly re-drawn after "unmark". (Eclipse bug.) Repaint here.
        m_root.refresh();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        super.undo();
        m_delete.undo();
    }
}
