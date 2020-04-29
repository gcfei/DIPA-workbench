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
 */
package org.knime.workbench.editor2.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.knime.core.node.NodeLogger;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.NodePortUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;

/**
 * Action to open a port view on a specific out-port.
 *
 * @author Florian Georg, University of Konstanz
 */
public class OpenPortViewAction extends Action {
    private final NodeContainerUI m_nodeContainer;

    private final int m_index;

    private final int m_userIndex;

    private final int m_totalPortCount;

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(OpenPortViewAction.class);

    /**
     * New action to open view on a port.
     *
     * @param nodeContainer The node
     * @param portIndex The index of the out-port
     * @param totalPortCount The total number of ports
     */
    public OpenPortViewAction(final NodeContainerUI nodeContainer,
            final int portIndex, final int totalPortCount) {
        m_nodeContainer = nodeContainer;
        m_index = portIndex;
        /* in normal nodes (not metanodes) we have an additional port (the
         * implicit flow variable port). The index for the user is still the
         * old index though (w/o implicit port)
         */
        if (!(nodeContainer instanceof WorkflowManagerUI)) {
            m_userIndex = m_index - 1;
            m_totalPortCount = totalPortCount - 1;
        } else {
            m_userIndex = m_index;
            m_totalPortCount = totalPortCount;
        }

    }

    protected int getPortIndex() {
        return m_index;
    }

    protected NodeContainerUI getNodeContainer() {
        return m_nodeContainer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/openPortView.png"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return Messages.OpenPortViewAction_1 + m_index;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        StringBuilder b = new StringBuilder();
        if (m_totalPortCount >= 4) {
            b.append(m_userIndex).append(": "); //$NON-NLS-1$
        }
        b.append(m_nodeContainer.getOutPort(m_index).getPortName());
        return b.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        LOGGER.debug(Messages.OpenPortViewAction_3 + m_nodeContainer.getName() + " (#" //$NON-NLS-2$
                + m_index + ")"); //$NON-NLS-1$
        NodePortUI port = m_nodeContainer.getOutPort(m_index);
        m_nodeContainer.getOutPort(m_index).openPortView(port.getPortName(), OpenViewAction.getAppBoundsAsAWTRec());
    }
}
