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
 *   Oct 14, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.workbench.editor2.actions.ports;

import java.util.Arrays;

import org.knime.core.node.context.ModifiableNodeCreationConfiguration;
import org.knime.core.node.context.ports.ExchangeablePortGroup;
import org.knime.core.node.port.PortType;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * An port action for native node container nodes that allows to replace ports.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class NativeNodeReplacePortAction extends AbstractNativeNodePortAction {

    /**
     * Constructor.
     *
     * @param editPart the node container edit part
     * @param creationConfig the node creation configuration
     * @param grpName the group name
     * @param text the name of the action
     */
    NativeNodeReplacePortAction(final NodeContainerEditPart editPart,
        final ModifiableNodeCreationConfiguration creationConfig, final String grpName, final String text) {
        super(editPart, creationConfig, grpName, text);
    }

    @Override
    protected boolean modifyPorts() {
        final ExchangeablePortGroup excPortGrp = getPortGroup(ExchangeablePortGroup.class);
        // reduce the supported port types to those that match not the currently selected type
        final PortType[] supportedTypes = Arrays.stream(excPortGrp.getSupportedPortTypes())//
            .filter(pType -> !pType.equals(excPortGrp.getSelectedPortType()))//
            .toArray(PortType[]::new);

        final PortType toAdd;
        if (supportedTypes.length > 1) {
            toAdd = createDialog(Messages.NativeNodeReplacePortAction_0 + m_grpName + Messages.NativeNodeReplacePortAction_1, supportedTypes);
        } else {
            toAdd = supportedTypes[0];
        }
        if (toAdd != null) {
            excPortGrp.setSelectedPortType(toAdd);
            return true;
        }
        return false;
    }

}
