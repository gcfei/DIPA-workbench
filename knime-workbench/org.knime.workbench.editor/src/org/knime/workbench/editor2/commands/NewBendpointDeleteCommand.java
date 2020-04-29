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
package org.knime.workbench.editor2.commands;

import org.eclipse.gef.commands.Command;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.ConnectionUIInformation;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;

/**
 * Command for deletion of connection bendpoints.
 *
 * @author Florian Georg, University of Konstanz
 */
public class NewBendpointDeleteCommand extends Command {

    private final WorkflowManager m_workflowManager;
    private final NodeID m_destNodeID;
    private final int m_destPort;

    private int m_index;
    private int[] m_point;

    /**
     * New Bendpoint deletion command.
     *
     * @param connection The connecton container
     * @param workflowManager The workflow manager that contains the connection.
     * @param index bendpoint index
     */
    public NewBendpointDeleteCommand(
            final ConnectionContainerEditPart connection,
            final WorkflowManager workflowManager,
            final int index) {
        m_workflowManager = workflowManager;
        m_index = index;
        m_destNodeID = connection.getModel().getDest();
        m_destPort = connection.getModel().getDestPort();
    }

    private ConnectionContainer getConnectionContainer() {
        return m_workflowManager.getIncomingConnectionFor(
                m_destNodeID, m_destPort);
    }

    private ConnectionUIInformation getUIInfo(final ConnectionContainer conn) {
        ConnectionUIInformation uiInfo = conn.getUIInfo();
        if (uiInfo == null) {
            uiInfo = ConnectionUIInformation.builder().build();
        }
        return uiInfo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        ConnectionContainer connection = getConnectionContainer();
        ConnectionUIInformation uiInfo = getUIInfo(connection);
        m_point = uiInfo.getBendpoint(m_index);
        uiInfo = ConnectionUIInformation.builder(uiInfo).removeBendpoint(m_index).build();

        // issue notification
        connection.setUIInfo(uiInfo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void redo() {
        ConnectionContainer connection = getConnectionContainer();
        ConnectionUIInformation uiInfo = getUIInfo(connection);
        uiInfo = ConnectionUIInformation.builder(uiInfo).removeBendpoint(m_index).build();
        // issue notification
        connection.setUIInfo(uiInfo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        ConnectionContainer connection = getConnectionContainer();
        ConnectionUIInformation uiInfo = getUIInfo(connection);
        uiInfo = ConnectionUIInformation.builder(uiInfo).addBendpoint(m_point[0], m_point[1], m_index).build();
        // issue notification
        connection.setUIInfo(uiInfo);
    }
}
