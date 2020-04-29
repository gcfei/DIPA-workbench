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
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;

/**
 * GEF command for replacing a <code>Metanode Template</code> in the <code>WorkflowManager</code>.
 *
 * @author Tim-Oliver Buchholz, KNIME AG, Zurich, Switzerland
 */
public class ReplaceMetaNodeTemplateCommand extends CreateMetaNodeTemplateCommand {

    private final NodeContainerEditPart m_node;

    private final RootEditPart m_root;

    private final DeleteCommand m_delete;

    private final ReplaceHelper m_replaceHelper;

    /**
     * @param manager the workflow manager
     * @param templateFolder the folder of the metanode template
     * @param location the insert location of the new metanode template
     * @param snapToGrid should metanode snap to grid
     * @param node which will be replaced by this metanode template
     * @throws IllegalArgumentException if the passed file store doesn't represent a workflow template
     */
    public ReplaceMetaNodeTemplateCommand(final WorkflowManager manager, final AbstractExplorerFileStore templateFolder,
        final Point location, final boolean snapToGrid, final NodeContainerEditPart node) {
        super(manager, templateFolder, location, snapToGrid);
        m_node = node;
        m_root = node.getRoot();
        m_replaceHelper = new ReplaceHelper(manager, Wrapper.unwrapNC(m_node.getNodeContainer()));
        m_delete = new DeleteCommand(Collections.singleton(m_node), getHostWFM());
    }

    /**
     * @param manager the workflow manager
     * @param templateURI the uri of the metanode template directory or file
     * @param location the insert location of the new metanode template
     * @param snapToGrid should metanode snap to grid
     * @param node which will be replaced by this metanode template
     * @param isRemoteLocation if the workflow template needs to be downloaded first (determines whether to show a busy
     *            cursor on command execution)
     */
    public ReplaceMetaNodeTemplateCommand(final WorkflowManager manager, final URI templateURI, final Point location,
        final boolean snapToGrid, final NodeContainerEditPart node, final boolean isRemoteLocation) {
        super(manager, templateURI, location, snapToGrid, isRemoteLocation);
        m_node = node;
        m_root = node.getRoot();
        m_replaceHelper = new ReplaceHelper(manager, Wrapper.unwrapNC(m_node.getNodeContainer()));
        m_delete = new DeleteCommand(Collections.singleton(m_node), getHostWFM());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canExecute() {
        return super.canExecute() && m_delete.canExecute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        if (m_replaceHelper.replaceNode()) {
            m_delete.execute();
            super.execute();
            m_replaceHelper.reconnect(m_container);
            // the connections are not always properly re-drawn after "unmark". (Eclipse bug.) Repaint here.
            m_root.refresh();
        }
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
