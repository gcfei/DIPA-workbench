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
 * ---------------------------------------------------------------------
 *
 * History
 *   20.02.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.model;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class WorkflowPortBar {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            WorkflowPortBar.class);

    private final boolean m_in;
    private final WorkflowManagerUI m_manager;

    public WorkflowPortBar(final WorkflowManagerUI manager, final boolean in) {
        m_in = in;
        m_manager = manager;
    }

    public boolean isInPortBar() {
        return m_in;
    }

    public WorkflowManagerUI getWorkflowManager() {
        return m_manager;
    }

    public NodeUIInformation getUIInfo() {
        // retrieve the ui info directly from manager
        if (m_in) {
            LOGGER.debug("getting ui info: " + m_manager.getInPortsBarUIInfo());
            return m_manager.getInPortsBarUIInfo();
        } else {
            LOGGER.debug("getting ui info: " + m_manager.getOutPortsBarUIInfo());
            return m_manager.getOutPortsBarUIInfo();
        }
    }

    public void setUIInfo(final NodeUIInformation uiInfo) {
        LOGGER.debug("setting ui info to: " + uiInfo);
        // set here the ui info directly into the manager
        if (m_in) {
            m_manager.setInPortsBarUIInfo(uiInfo);
        } else {
            m_manager.setOutPortsBarUIInfo(uiInfo);
        }
        LOGGER.debug("in port bar: " + m_manager.getInPortsBarUIInfo());
        LOGGER.debug("out port bar: " + m_manager.getOutPortsBarUIInfo());
    }

}
