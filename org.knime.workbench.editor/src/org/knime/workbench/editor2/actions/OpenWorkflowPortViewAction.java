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
 *   05.02.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.actions;


import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.NodeOutPortUI;
import org.knime.core.ui.node.workflow.WorkflowInPortUI;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class OpenWorkflowPortViewAction extends OpenPortViewAction {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            OpenWorkflowPortViewAction.class);

    private final NodeOutPortUI m_port;

    /**
     * @param nodeContainer
     * @param portIndex
     * @param totalPortCount
     */
    public OpenWorkflowPortViewAction(final NodeContainerUI nodeContainer,
            final int portIndex, final int totalPortCount) {
        super(nodeContainer, portIndex, totalPortCount);
        m_port = ((WorkflowInPortUI)getNodeContainer().getInPort(getPortIndex()))
            .getUnderlyingPort();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {

        // the name is constructed like "Model outport <specificIndex>"
        String name;

        if (m_port.getPortType().equals(
                BufferedDataTable.TYPE)) {
            name = Messages.OpenWorkflowPortViewAction_0 + getPortIndex();
        } else if (PMMLPortObject.TYPE.isSuperTypeOf(
                m_port.getPortType())) {
            name = Messages.OpenWorkflowPortViewAction_1 + getPortIndex();
        } else {
            name = Messages.OpenWorkflowPortViewAction_2 + getPortIndex();
        }

        // the text will be displayed in the context menu
        // it consists of the specific port type and index and its description
        String description = m_port.getPortName();

        return name + ": " + description; //$NON-NLS-1$
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        LOGGER.debug(Messages.OpenWorkflowPortViewAction_4 + getNodeContainer().getName() + " (#" + getPortIndex() + ")"); //$NON-NLS-2$ //$NON-NLS-3$
        m_port.openPortView(m_port.getPortName(), OpenViewAction.getAppBoundsAsAWTRec());
    }

}
