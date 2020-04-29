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

import java.util.OptionalInt;

import javax.swing.SwingUtilities;

import org.eclipse.jface.resource.ImageDescriptor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeOutPort;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Action that opens the first out-port view of all selected nodes.
 *
 * @author Thomas Gabriel, KNIME.com AG, Zurich
 */
public class OpenFirstOutPortViewAction extends AbstractNodeAction {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            OpenFirstOutPortViewAction.class);

    /** Unique id for this action. */
    public static final String ID = "knime.action.openFirstOutPortView"; //$NON-NLS-1$

    /**
     *
     * @param editor current editor
     */
    public OpenFirstOutPortViewAction(final WorkflowEditor editor) {
        super(editor);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return Messages.OpenFirstOutPortViewAction_1 + getHotkey("knime.commands.openFirstOutPortView"); //$NON-NLS-2$
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
    public ImageDescriptor getDisabledImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/openPortView_disabled.png"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return Messages.OpenFirstOutPortViewAction_5;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean internalCalculateEnabled() {
        NodeContainerEditPart[] parts =
            getSelectedParts(NodeContainerEditPart.class);
        for (NodeContainerEditPart p : parts) {
            final NodeContainerUI cont = p.getNodeContainer();
            return getPortIndex(cont).isPresent();
        }
        return false;
    }

    /** "first" port index -- for ordinary nodes that's port index 1 as the first port is the flow variable
     * port; for workflow managers it's 0
     * @param nc the node in question.
     * @return that index or an empty optional if the node has no such port.
     */
    private static OptionalInt getPortIndex(final NodeContainerUI nc) {
        int portOfInterest = nc instanceof WorkflowManagerUI ? 0 : 1;
        return portOfInterest < nc.getNrOutPorts() ? OptionalInt.of(portOfInterest) : OptionalInt.empty();
    }

    /**
     * This opens the first view of all the selected nodes.
     *
     * {@inheritDoc}
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        LOGGER.debug(Messages.OpenFirstOutPortViewAction_6
                + nodeParts.length + Messages.OpenFirstOutPortViewAction_7);
        for (NodeContainerEditPart p : nodeParts) {
            final NodeContainerUI cont = p.getNodeContainer();
            final java.awt.Rectangle knimeWindowBounds = OpenViewAction.getAppBoundsAsAWTRec();
            // first port is flow var port
            final OptionalInt portIndex = getPortIndex(cont);
            if (portIndex.isPresent()) {
                SwingUtilities.invokeLater(new Runnable() {
                    /** {inheritDoc} */
                    @Override
                    public void run() {
                        NodeOutPort port = Wrapper.unwrapNC(cont).getOutPort(portIndex.getAsInt());
                        LOGGER.debug(Messages.OpenFirstOutPortViewAction_8 + cont.getName() + Messages.OpenFirstOutPortViewAction_9 + port.getPortName());
                        port.openPortView(port.getPortName(), knimeWindowBounds);
                    }
                });
            }
        }

        try {
            // Give focus to the editor again. Otherwise the actions (selection)
            // is not updated correctly.
            getWorkbenchPart().getSite().getPage().activate(getWorkbenchPart());
        } catch (Exception e) {
            // ignore
        }
    }

}
