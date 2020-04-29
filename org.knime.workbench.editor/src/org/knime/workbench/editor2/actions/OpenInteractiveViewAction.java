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
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.Node;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;

/**
 * Action to open an interactive view of a node.
 *
 * @author Thomas Gabriel, KNIME AG, Zurich, Switzerland
 * @since 2.8
 */
public class OpenInteractiveViewAction extends Action {
    private final NodeContainer m_nodeContainer;

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(OpenInteractiveViewAction.class);

    /**
     * New action to open an interactive node view.
     *
     * @param nodeContainer The node
     */
    public OpenInteractiveViewAction(final NodeContainer nodeContainer) {
        m_nodeContainer = CheckUtils.checkArgumentNotNull(nodeContainer);
        CheckUtils.checkArgument(m_nodeContainer.hasInteractiveView(), Messages.OpenInteractiveViewAction_0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return m_nodeContainer.getNodeContainerState().isExecuted();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/openInteractiveView.png"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return Messages.OpenInteractiveViewAction_2 + m_nodeContainer.getInteractiveViewName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return Messages.OpenInteractiveViewAction_3 + m_nodeContainer.getInteractiveViewName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        LOGGER.debug(Messages.OpenInteractiveViewAction_4 + m_nodeContainer.getName());
        try {
            AbstractNodeView<?> view = m_nodeContainer.getInteractiveView();
            final String title = m_nodeContainer.getInteractiveViewName();
            Node.invokeOpenView(view, title, OpenViewAction.getAppBoundsAsAWTRec());
        } catch (Throwable t) {
            final MessageBox mb = new MessageBox(SWTUtilities.getActiveShell(), SWT.ICON_ERROR | SWT.OK);
            mb.setText(Messages.OpenInteractiveViewAction_5);
            mb.setMessage(Messages.OpenInteractiveViewAction_6 + t.getMessage());
            mb.open();
            LOGGER.error(Messages.OpenInteractiveViewAction_7 + m_nodeContainer.getNameWithID() + Messages.OpenInteractiveViewAction_8
                    + t.getClass().getSimpleName() + Messages.OpenInteractiveViewAction_9, t);
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return "knime.open.interactive.view.action"; //$NON-NLS-1$
    }
}
