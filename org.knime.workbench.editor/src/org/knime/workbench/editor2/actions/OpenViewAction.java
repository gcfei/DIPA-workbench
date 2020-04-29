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
 *   25.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.actions;

import javax.swing.SwingUtilities;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.Node;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;

/**
 * Action to open a view of a node.
 *
 * TODO: Embedd view in an eclipse view (preference setting)
 *
 * @author Florian Georg, University of Konstanz
 */
public class OpenViewAction extends Action {
    private final NodeContainer m_nodeContainer;

    private final int m_index;

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(OpenViewAction.class);

    /**
     * New action to opne a node view.
     *
     * @param nodeContainer The node
     * @param viewIndex The index of the node view
     */
    public OpenViewAction(final NodeContainer nodeContainer,
            final int viewIndex) {
        m_nodeContainer = nodeContainer;
        m_index = viewIndex;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/openView.gif"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return Messages.OpenViewAction_1 + m_index + ": " //$NON-NLS-2$
                + m_nodeContainer.getViewName(m_index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return Messages.OpenViewAction_3 + m_nodeContainer.getViewName(m_index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        LOGGER.debug(Messages.OpenViewAction_4 + m_nodeContainer.getName() + " (#" + m_index + ")"); //$NON-NLS-2$ //$NON-NLS-3$
        try {
            final String title = m_nodeContainer.getViewName(m_index) + " - " + m_nodeContainer.getDisplayLabel(); //$NON-NLS-1$
            final java.awt.Rectangle knimeWindowBounds = OpenViewAction.getAppBoundsAsAWTRec();
            SwingUtilities.invokeLater(
                () -> Node.invokeOpenView(m_nodeContainer.getView(m_index), title, knimeWindowBounds));
        } catch (Throwable t) {
            MessageBox mb = new MessageBox(SWTUtilities.getActiveShell(), SWT.ICON_ERROR | SWT.OK);
            mb.setText(Messages.OpenViewAction_8);
            mb.setMessage(Messages.OpenViewAction_9 + t.getMessage());
            mb.open();
            LOGGER.error(Messages.OpenViewAction_10 + m_nodeContainer.getNameWithID() + Messages.OpenViewAction_11
                + t.getClass().getSimpleName() + Messages.OpenViewAction_12 + Messages.OpenViewAction_13, t);
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return "knime.open.view.action"; //$NON-NLS-1$
    }

    /** Get the workbench window as a Swing Rectangle -- used in various actions to center a new swing view on
     * top of the application.
     * @return non-null rectangle. */
    static java.awt.Rectangle getAppBoundsAsAWTRec() {
        final Rectangle knimeWindowBounds = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().getBounds();
        return new java.awt.Rectangle(knimeWindowBounds.x, knimeWindowBounds.y, knimeWindowBounds.width, knimeWindowBounds.height);
    }
}
