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
 *   21.06.2012 (Peter Ohl): created
 */
package org.knime.workbench.editor2.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.knime.core.node.port.MetaPortInfo;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * GEF command for reconfiguring (rename, change port number and/or types). Command executes only if at least one of the
 * three "new" things is set. Either a new set of input ports, a new set of output ports, or a new name. (Or a
 * combination of which.)
 *
 * @author Peter Ohl, KNIME AG, Zurich, Switzerland
 */
public class ReconfigureMetaNodeCommand extends AbstractKNIMECommand {

    private final NodeID m_metanodeID;

    private List<MetaPortInfo> m_inPorts;

    private List<MetaPortInfo> m_outPorts;

    private String m_name;

    // for undo
    private String m_oldName;

    private List<MetaPortInfo> m_reverseInports;

    private List<MetaPortInfo> m_reverseOutports;

    /**
     * Creates a new command.
     *
     * @param workflowManager hostWFM
     * @param metaNode to reconfigure (must be contained in hostWFM as direct child)
     */
    public ReconfigureMetaNodeCommand(final WorkflowManager workflowManager, final NodeID metaNode) {
        super(workflowManager);
        m_metanodeID = metaNode;
    }

    /**
     * @param inPorts the inPorts to set
     */
    public void setNewInPorts(final List<MetaPortInfo> inPorts) {
        m_inPorts = inPorts;
    }

    /**
     * @param outPorts the outPorst to set
     */
    public void setNewOutPorts(final List<MetaPortInfo> outPorts) {
        m_outPorts = outPorts;
    }

    /**
     * @param name the name to set
     */
    public void setNewName(final String name) {
        m_name = name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canExecute() {
        if (!super.canExecute()) {
            return false;
        }
        if (m_metanodeID == null) {
            return false;
        }
        NodeContainer nc = getHostWFM().getNodeContainer(m_metanodeID);
        boolean isWriteProtected;
        if (nc instanceof WorkflowManager) {
            isWriteProtected = ((WorkflowManager)nc).isWriteProtected();
        } else if (nc instanceof SubNodeContainer) {
            isWriteProtected = ((SubNodeContainer)nc).isWriteProtected();
        } else {
            return false;
        }
        if (!(nc instanceof WorkflowManager) && !(nc instanceof SubNodeContainer)) {
            return false;
        }
        if (isWriteProtected) {
            return false;
        }
        // at least one thing to change should be set
        return (m_inPorts != null || m_outPorts != null || m_name != null);
    }

    /** {@inheritDoc} */
    @Override
    public void execute() {
        NodeContainer nc = getHostWFM().getNodeContainer(m_metanodeID);
        if (nc instanceof WorkflowManager) {
            if (m_name != null) {
                WorkflowManager metaNode = (WorkflowManager) nc;
                m_oldName = metaNode.getName();
                metaNode.setName(m_name);
            }
            if (m_inPorts != null) {
                m_reverseInports =
                    createReverseOperationList(getHostWFM().getMetanodeInputPortInfo(m_metanodeID), m_inPorts);
                getHostWFM().changeMetaNodeInputPorts(m_metanodeID,
                    m_inPorts.toArray(new MetaPortInfo[m_inPorts.size()]));
            }
            if (m_outPorts != null) {
                m_reverseOutports =
                        createReverseOperationList(getHostWFM().getMetanodeOutputPortInfo(m_metanodeID), m_outPorts);
                getHostWFM().changeMetaNodeOutputPorts(m_metanodeID,
                        m_outPorts.toArray(new MetaPortInfo[m_outPorts.size()]));
            }
        } else if (nc instanceof SubNodeContainer) {
            SubNodeContainer snc = (SubNodeContainer) nc;
            if (m_name != null) {
                m_oldName = snc.getName();
                snc.setName(m_name);
            }
            if (m_inPorts != null) {
                m_reverseInports =
                    createReverseOperationList(getHostWFM().getSubnodeInputPortInfo(m_metanodeID), m_inPorts);
                getHostWFM().changeSubNodeInputPorts(m_metanodeID,
                    m_inPorts.toArray(new MetaPortInfo[m_inPorts.size()]));
            }
            if (m_outPorts != null) {
                m_reverseOutports =
                        createReverseOperationList(getHostWFM().getSubnodeOutputPortInfo(m_metanodeID), m_outPorts);
                getHostWFM().changeSubNodeOutputPorts(m_metanodeID,
                    m_outPorts.toArray(new MetaPortInfo[m_outPorts.size()]));
            }
        }
    }

    /**
     * Creates a port list that can be applied - after the newPortList was applied to a metanode with the
     * currentPortList - to get the metanode back to a port list like the curentPortList.
     *
     * @param currentPortList
     * @param newPortList
     * @return
     */
    private List<MetaPortInfo> createReverseOperationList(final MetaPortInfo[] currentPortList,
            final List<MetaPortInfo> newPortList) {
        MetaPortInfo[] reverse = new MetaPortInfo[currentPortList.length];
        for (MetaPortInfo newInfo : newPortList) {
            if (newInfo.getOldIndex() >= 0) {
                int revOldIdx = newInfo.getNewIndex();
                int revNewIdx = newInfo.getOldIndex();
                PortType revType = currentPortList[newInfo.getOldIndex()].getType();
                boolean revConn = currentPortList[newInfo.getOldIndex()].isConnected();
                MetaPortInfo revInfo = MetaPortInfo.builder()
                        .setPortType(revType)
                        .setIsConnected(revConn)
                        .setOldIndex(revOldIdx)
                        .setNewIndex(revNewIdx).build();
                reverse[revNewIdx] = revInfo;
            }
        }
        // all array positions still null got deleted and must be filled from the current list
        for (int i = 0; i < reverse.length; i++) {
            if (reverse[i] != null) {
                // that is a port that got moved back above
                continue;
            }
            MetaPortInfo currentInfo = currentPortList[i];
            MetaPortInfo revInfo = MetaPortInfo.builder(currentInfo)
                    .setIsConnected(false)
                    .setMessage(null)
                    .setOldIndex(-1)
                    .setNewIndex(i).build();
            reverse[i] = revInfo;
        }
        return new ArrayList<MetaPortInfo>(Arrays.asList(reverse));
    }

    /** {@inheritDoc} */
    @Override
    public boolean canUndo() {
        if (m_name != null && m_oldName == null) {
            return false;
        }
        if (m_inPorts != null && m_reverseInports == null) {
            return false;
        }
        if (m_outPorts != null && m_reverseOutports == null) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        NodeContainer nc = getHostWFM().getNodeContainer(m_metanodeID);
        if (nc instanceof WorkflowManager) {
            if (m_oldName != null) {
                ((WorkflowManager)nc).setName(m_oldName);

            }
            if (m_reverseInports != null) {
                getHostWFM().changeMetaNodeInputPorts(m_metanodeID,
                        m_reverseInports.toArray(new MetaPortInfo[m_reverseInports.size()]));
            }
            if (m_reverseOutports != null) {
                getHostWFM().changeMetaNodeOutputPorts(m_metanodeID,
                        m_reverseOutports.toArray(new MetaPortInfo[m_reverseOutports.size()]));
            }
        } else if (nc instanceof SubNodeContainer) {
            SubNodeContainer snc = (SubNodeContainer) nc;
            if (m_oldName != null) {
                snc.setName(m_oldName);
            }
            if (m_reverseInports != null) {
                getHostWFM().changeSubNodeInputPorts(m_metanodeID,
                        m_reverseInports.toArray(new MetaPortInfo[m_reverseInports.size()]));
            }
            if (m_reverseOutports != null) {
                getHostWFM().changeSubNodeOutputPorts(m_metanodeID,
                        m_reverseOutports.toArray(new MetaPortInfo[m_reverseOutports.size()]));
            }
        }
    }

}
