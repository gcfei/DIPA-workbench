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
 *   2010 03 28 (ohl): created
 */
package org.knime.workbench.ui.layout.commands;

import java.util.Collection;
import java.util.Map;
import java.util.Random;

import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.ConnectionID;
import org.knime.core.node.workflow.ConnectionUIInformation;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.wrapper.WorkflowManagerWrapper;
import org.knime.workbench.editor2.commands.AbstractKNIMECommand;
import org.knime.workbench.ui.layout.LayoutManager;

/**
 *
 * @author ohl, KNIME AG, Zurich, Switzerland
 */
public class AutoLayoutCommand extends AbstractKNIMECommand {

    private final WorkflowManager m_wfm;

    private final Collection<NodeContainerUI> m_nodes;

    private LayoutManager m_layoutMgr;

    private long m_seed;

    private final Random m_random = new Random();

    /**
     * @param wfm
     * @param nodes if null, all nodes are laid out
     */
    public AutoLayoutCommand(final WorkflowManager wfm,
            final Collection<NodeContainerUI> nodes) {
        super(wfm);
        m_wfm = wfm;
        m_nodes = nodes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        m_seed = m_random.nextLong();
        doLayout(m_seed);
    }

    private void doLayout(final long seed) {
        m_layoutMgr = new LayoutManager(WorkflowManagerWrapper.wrap(m_wfm), seed);
        m_layoutMgr.doLayout(m_nodes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void redo() {
        doLayout(m_seed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canUndo() {
        return m_layoutMgr != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        Map<NodeID, NodeUIInformation> oldPositions =
                m_layoutMgr.getOldNodeCoordinates();
        Map<ConnectionID, ConnectionUIInformation> oldBendpoints =
                m_layoutMgr.getOldBendpoints();
        // re-position nodes
        for (Map.Entry<NodeID, NodeUIInformation> e : oldPositions.entrySet()) {
            NodeContainer nc = m_wfm.getNodeContainer(e.getKey());
            if (nc == null) {
                continue;
            }
            nc.setUIInformation(e.getValue());
        }
        // re-create bendpoints
        for (Map.Entry<ConnectionID, ConnectionUIInformation> e : oldBendpoints
                .entrySet()) {
            ConnectionContainer cc = m_wfm.getConnection(e.getKey());
            if (cc == null) {
                continue;
            }
            cc.setUIInfo(e.getValue());
        }

    }
}
