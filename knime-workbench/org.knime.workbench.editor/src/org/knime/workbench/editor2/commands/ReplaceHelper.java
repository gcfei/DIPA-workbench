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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.ConnectionContainer.ConnectionType;
import org.knime.core.node.workflow.ConnectionUIInformation;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.node.workflow.ConnectionContainerUI;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * @author Tim-Oliver Buchholz, KNIME AG, Zurich, Switzerland
 */
public class ReplaceHelper {
    /**
     * This checks for an executed state of downstream nodes and, if the user's preference is such, dialog's the user to
     * determine whether they really want to perform the action which will move those nodes out of that state.
     *
     * @param wm the <code>WorkflowManager</code> instance containining the connection(s) in question
     * @param node if non-null, <code>wm</code> will have its <code>canRemoveNode(NodeID)</node> consulted and if false
     *            is returned, this will trigger a potential dialog
     * @param connections one or more connections whose destinations will be checked for an executed state
     * @return true if the replacement can occur
     */
    public static boolean executedStateAllowsReplace(final WorkflowManager wm, final NodeContainer node,
        final ConnectionContainer... connections) {
        boolean aWarnableStateExists =
                (node != null) ? (((connections != null) && (connections.length > 0))
                    && (node.getNodeContainerState().isExecuted() || (!wm.canRemoveNode(node.getID())))) : false;

        if ((!aWarnableStateExists) && (connections != null)) {
            for (final ConnectionContainer connectionContainer : connections) {
                WorkflowManager wmToFindDestNode = wm;
                if (doesConnectionLeaveWorkflow(connectionContainer)) {
                    wmToFindDestNode = wm.getParent();
                }
                if (wmToFindDestNode.findNodeContainer(connectionContainer.getDest()).getNodeContainerState()
                    .isExecuted()) {
                    aWarnableStateExists = true;

                    break;
                }
            }
        }

        if (aWarnableStateExists) {
            final IPreferenceStore store = KNIMEUIPlugin.getDefault().getPreferenceStore();
            if (!store.contains(PreferenceConstants.P_CONFIRM_RESET)
                || store.getBoolean(PreferenceConstants.P_CONFIRM_RESET)) {
                final MessageDialogWithToggle dialog =
                    MessageDialogWithToggle.openOkCancelConfirm(SWTUtilities.getActiveShell(), Messages.ReplaceHelper_0,
                        Messages.ReplaceHelper_1, Messages.ReplaceHelper_2, false, null, null);
                if (dialog.getReturnCode() != IDialogConstants.OK_ID) {
                    return false;
                }
                if (dialog.getToggleState()) {
                    store.setValue(PreferenceConstants.P_CONFIRM_RESET, false);
                    KNIMEUIPlugin.getDefault().savePluginPreferences();
                }
            }
        }

        return true;
    }

    private static final boolean doesConnectionLeaveWorkflow(final ConnectionContainer connection) {
        return connection.getType() == ConnectionType.WFMOUT;
    }

    private static final Comparator<ConnectionContainer> DEST_PORT_SORTER = (o1, o2) -> {
        return Integer.compare(o1.getDestPort(), o2.getDestPort());
    };

    private static final Comparator<ConnectionContainer> SOURCE_PORT_SORTER = (o1, o2) -> {
        return Integer.compare(o1.getSourcePort(), o2.getSourcePort());
    };

    private NodeContainer m_oldNode;

    /** The connection ui infos. */
    protected Map<ConnectionContainerUI, ConnectionUIInformation> m_connectionUIInfoMap;

    /** The workflow manager. */
    protected final WorkflowManager m_wfm;

    /** The incoming node connections. */
    protected final ArrayList<ConnectionContainer> m_incomingConnections;

    /** The outgoing node connections. */
    protected final ArrayList<ConnectionContainer> m_outgoingConnections;

    /**
     * @param wfm the workflow manager
     * @param oldNode the node which was replaced
     */
    public ReplaceHelper(final WorkflowManager wfm, final NodeContainer oldNode) {
        m_wfm = wfm;
        m_oldNode = oldNode;

        final NodeID oldNodeId = m_oldNode.getID();
        m_incomingConnections = new ArrayList<>(wfm.getIncomingConnectionsFor(oldNodeId));
        m_outgoingConnections = new ArrayList<>(wfm.getOutgoingConnectionsFor(oldNodeId));

        // sort according to ports
        Collections.sort(m_incomingConnections, DEST_PORT_SORTER);
        Collections.sort(m_outgoingConnections, SOURCE_PORT_SORTER);
    }

    /**
     * Checks execution status of downstream nodes and pops up reset warning if enabled.
     *
     * @return if new node can be replaced
     */
    public boolean replaceNode() {
        final ConnectionContainer[] connections =
            m_outgoingConnections.toArray(new ConnectionContainer[m_outgoingConnections.size()]);

        return executedStateAllowsReplace(m_wfm, m_oldNode, connections);
    }

    /**
     * This should be called prior to invoking <code>reconnect(NodeContainer)</code>.
     *
     * @param map a map between the termini of a connection (node + port for source and destination) and the UI info of
     *            that connection; this map will be referenced directly and not cloned
     */
    public void setConnectionUIInfoMap(final Map<ConnectionContainerUI, ConnectionUIInformation> map) {
        m_connectionUIInfoMap = map;
    }

    /**
     * Connects new node with connection of the old node.
     *
     * @param container new node container
     */
    public void reconnect(final NodeContainer container) {
        // reset node location
        setUIInformation(container);

        int inShift;
        int outShift;

        if ((m_oldNode instanceof WorkflowManager) && !(container instanceof WorkflowManager)) {
            inShift = 0;
            // replacing a metanode (no opt. flow var ports) with a "normal" node (that has optional flow var ports)
            if ((m_oldNode.getNrInPorts() > 0) && (container.getNrInPorts() > 1)) {
                // shift ports one index - unless we need to use the invisible optional flow var port of new node
                if (!m_oldNode.getInPort(0).getPortType().equals(FlowVariablePortObject.TYPE)) {
                    inShift = 1;
                } else if (container.getInPort(1).getPortType().equals(FlowVariablePortObject.TYPE)) {
                    inShift = 1;
                }
            }

            outShift = 0;
            if ((m_oldNode.getNrOutPorts() > 0) && (container.getNrOutPorts() > 1)) {
                if (!m_oldNode.getOutPort(0).getPortType().equals(FlowVariablePortObject.TYPE)) {
                    outShift = 1;
                } else if (container.getOutPort(1).getPortType().equals(FlowVariablePortObject.TYPE)) {
                    outShift = 1;
                }
            }
        } else if (!(m_oldNode instanceof WorkflowManager) && (container instanceof WorkflowManager)) {
            // replacing a "normal" node with a metanode
            inShift = -1;
            for (final ConnectionContainer cc : m_incomingConnections) {
                if (cc.getDestPort() == 0) {
                    inShift = 0;
                    break;
                }
            }

            outShift = -1;
            for (final ConnectionContainer cc : m_outgoingConnections) {
                if (cc.getSourcePort() == 0) {
                    outShift = 0;
                    break;
                }
            }
        } else {
            inShift = 0;
            outShift = 0;
        }

        // set incoming connections
        final NodeID newId = container.getID();
        for (final ConnectionContainer c : m_incomingConnections) {
            if (m_wfm.canAddConnection(c.getSource(), c.getSourcePort(), newId, c.getDestPort() + inShift)) {
                final ConnectionContainer cc =
                    m_wfm.addConnection(c.getSource(), c.getSourcePort(), newId, c.getDestPort() + inShift);
                setConnectionUIInfo(c, cc);
            } else {
                break;
            }
        }

        // set outgoing connections
        for (final ConnectionContainer c : m_outgoingConnections) {
            if (m_wfm.canAddConnection(newId, c.getSourcePort() + outShift, c.getDest(), c.getDestPort())) {
                final ConnectionContainer cc =
                    m_wfm.addConnection(newId, c.getSourcePort() + outShift, c.getDest(), c.getDestPort());
                setConnectionUIInfo(c, cc);
            } else {
                break;
            }
        }
    }

    /**
     * Sets the connection ui info.
     * @param infoSrc the connection container carrying the information
     * @param infoTgt the connection container to add the information to
     */
    protected void setConnectionUIInfo(final ConnectionContainer infoSrc, final ConnectionContainer infoTgt) {
        if (m_connectionUIInfoMap != null) {
            @SuppressWarnings("unlikely-arg-type")
            final ConnectionUIInformation uiInfo = m_connectionUIInfoMap.get(infoSrc);
            if (uiInfo != null) {
                infoTgt.setUIInfo(uiInfo);
            }
        }
    }

    /**
     * Copies the UI information from the old to the new container.
     *
     * @param container the new node container
     */
    protected void setUIInformation(final NodeContainer container) {
        final NodeUIInformation uiInformation = m_oldNode.getUIInformation();
        final int[] bounds = uiInformation.getBounds();
        final NodeUIInformation info =
            NodeUIInformation.builder().setNodeLocation(bounds[0], bounds[1], -1, -1).setHasAbsoluteCoordinates(true)
                .setSnapToGrid(uiInformation.getSnapToGrid()).setIsDropLocation(false).build();
        container.setUIInformation(info);
    }
 }
