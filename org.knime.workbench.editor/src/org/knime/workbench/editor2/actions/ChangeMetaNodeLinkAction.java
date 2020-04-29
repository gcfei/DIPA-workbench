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
 *   17.07.2013 (Peter Ohl): created.
 */
package org.knime.workbench.editor2.actions;

import static org.knime.core.ui.wrapper.Wrapper.unwrapWFM;
import static org.knime.core.ui.wrapper.Wrapper.wraps;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.Role;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.core.util.pathresolve.ResolverUtil;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.commands.ChangeMetaNodeLinkCommand;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.AbstractContentProvider.LinkType;

/**
 * Allows changing the type of the template link of a metanode.
 * @author Peter Ohl, KNIME AG, Zurich, Switzerland
 */
public class ChangeMetaNodeLinkAction extends AbstractNodeAction {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ChangeMetaNodeLinkAction.class);

    /** id of this action. */
    public static final String ID = "knime.action.meta_node_relink"; //$NON-NLS-1$

    /**
     * @param editor the current workflow editor
     */
    public ChangeMetaNodeLinkAction(final WorkflowEditor editor) {
        super(editor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ID;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return Messages.ChangeMetaNodeLinkAction_1;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return Messages.ChangeMetaNodeLinkAction_2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/meta/metanode_setname.png"); //$NON-NLS-1$
    }

    /**
     * @return true, if underlying model instance of <code>WorkflowManager</code>, otherwise false
     */
    @Override
    protected boolean internalCalculateEnabled() {
        NodeContainerEditPart[] nodes = getSelectedParts(NodeContainerEditPart.class);
        if (nodes.length != 1) {
            return false;
        }
        NodeContainerUI nc = nodes[0].getNodeContainer();
        if (!(nc instanceof WorkflowManagerUI)) {
            return false;
        }
        if (!wraps(nc, WorkflowManager.class)) {
            //action not yet supported for the general UI workflow manager
            return false;
        }
        WorkflowManagerUI metaNode = (WorkflowManagerUI)nc;
        if (!Role.Link.equals(unwrapWFM(metaNode).getTemplateInformation().getRole()) || metaNode.getParent().isWriteProtected()) {
            // metanode must be linked and parent must not forbid the change
            return false;
        }
        // we can reconfigure the template link - but only if template and flow are in the same mountpoint
        URI targetURI = unwrapWFM(metaNode).getTemplateInformation().getSourceURI();
        try {
            if (ResolverUtil.isMountpointRelativeURL(targetURI)
                || ResolverUtil.isWorkflowRelativeURL(targetURI)) {
                return true;
            }
        } catch (IOException e) {
            return false;
        }
        // we can change absolute links if the mount points of flow and template are the same
        AbstractContentProvider workflowMountPoint = null;
        WorkflowContext wfc = unwrapWFM(metaNode).getProjectWFM().getContext();
        LocalExplorerFileStore fs =
            ExplorerFileSystem.INSTANCE.fromLocalFile(wfc.getMountpointRoot());
        if (fs != null) {
            workflowMountPoint = fs.getContentProvider();
        }
        if (workflowMountPoint == null) {
            return false;
        }
        AbstractExplorerFileStore targetfs = ExplorerFileSystem.INSTANCE.getStore(targetURI);
        if (targetfs == null) {
            return false;
        }
        return workflowMountPoint.equals(targetfs.getContentProvider());
    }

    /** {@inheritDoc} */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        if (nodeParts.length < 1) {
            return;
        }

        WorkflowManager metaNode = Wrapper.unwrapWFM(nodeParts[0].getNodeContainer());
        if (Role.Link.equals(metaNode.getTemplateInformation().getRole())) {
            final WorkflowManager wfm = metaNode.getParent();
            URI targetURI = metaNode.getTemplateInformation().getSourceURI();
            LinkType linkType = LinkType.None;
            try {
                if (ResolverUtil.isMountpointRelativeURL(targetURI)) {
                    linkType = LinkType.MountpointRelative;
                } else if (ResolverUtil.isWorkflowRelativeURL(targetURI)) {
                    linkType = LinkType.WorkflowRelative;
                } else {
                    linkType = LinkType.Absolute;
                }
            } catch (IOException e) {
                LOGGER.error(Messages.ChangeMetaNodeLinkAction_4 + targetURI + ": " + e.getMessage(), e); //$NON-NLS-2$
                return;
            }

            String msg = Messages.ChangeMetaNodeLinkAction_6;
            msg += Messages.ChangeMetaNodeLinkAction_7;
            msg += Messages.ChangeMetaNodeLinkAction_8 + linkType + Messages.ChangeMetaNodeLinkAction_9 + targetURI + ")\n"; //$NON-NLS-3$
            msg += Messages.ChangeMetaNodeLinkAction_11;
            LinkPrompt dlg = new LinkPrompt(getEditor().getSite().getShell(), msg, linkType);
            dlg.open();
            if (dlg.getReturnCode() == Window.CANCEL) {
                return;
            }
            LinkType newLinkType = dlg.getLinkType();
            if (linkType.equals(newLinkType)) {
                LOGGER.info(Messages.ChangeMetaNodeLinkAction_12 + targetURI);
                return;
            }
            // as the workflow is local and the template in the same mountID, it should resolve to a file
            URI newURI = null;
            NodeContext.pushContext(metaNode);
            try {
                File targetFile = ResolverUtil.resolveURItoLocalFile(targetURI);
                LocalExplorerFileStore targetfs = ExplorerFileSystem.INSTANCE.fromLocalFile(targetFile);
                newURI = AbstractContentProvider.createMetanodeLinkUri(metaNode, targetfs, newLinkType);
            } catch (IOException e) {
                LOGGER.error(Messages.ChangeMetaNodeLinkAction_13 + targetURI + ": " + e.getMessage(), e); //$NON-NLS-2$
                return;
            } catch (URISyntaxException e) {
                LOGGER.error(Messages.ChangeMetaNodeLinkAction_15 + targetURI + ": " + e.getMessage(), e); //$NON-NLS-2$
                return;
            } catch (CoreException e) {
                LOGGER.error(Messages.ChangeMetaNodeLinkAction_17 + targetURI + ": " + e.getMessage(), e); //$NON-NLS-2$
                return;
            } finally {
                NodeContext.removeLastContext();
            }
            ChangeMetaNodeLinkCommand cmd = new ChangeMetaNodeLinkCommand(wfm, metaNode, targetURI, newURI);
            getCommandStack().execute(cmd);
        } else {
            throw new IllegalStateException(
                Messages.ChangeMetaNodeLinkAction_19
                    + metaNode + Messages.ChangeMetaNodeLinkAction_20);
        }
    }

    private final class LinkPrompt extends MessageDialog {
        private Button m_absoluteLink;

        private Button m_mountpointRelativeLink;

        private Button m_workflowRelativeLink;

        private LinkType m_linkType;

        private LinkType m_preSelect;

        /**
         *
         */
        public LinkPrompt(final Shell parentShell, final String message, final LinkType preSelect) {
            super(parentShell, Messages.ChangeMetaNodeLinkAction_21, null, message,
                MessageDialog.QUESTION_WITH_CANCEL, new String[]{IDialogConstants.OK_LABEL,
                    IDialogConstants.CANCEL_LABEL}, 0);
            setShellStyle(getShellStyle() | SWT.SHEET);
            if (preSelect != null) {
                m_preSelect = preSelect;
                m_linkType = preSelect;
            } else {
                m_preSelect = LinkType.Absolute;
                m_linkType = LinkType.Absolute;
            }
        }

        /**
         * After the dialog closes get the selected link type.
         *
         * @return null, if no link should be created, otherwise the selected link type.
         */
        public LinkType getLinkType() {
            return m_linkType;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Control createCustomArea(final Composite parent) {
            Composite group = new Composite(parent, SWT.NONE);
            group.setLayout(new GridLayout(2, true));
            group.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));

            Label l1 = new Label(group, SWT.NONE);
            l1.setText(Messages.ChangeMetaNodeLinkAction_22);
            m_absoluteLink = new Button(group, SWT.RADIO);
            m_absoluteLink.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));
            m_absoluteLink.setText(Messages.ChangeMetaNodeLinkAction_23);
            m_absoluteLink.setToolTipText(Messages.ChangeMetaNodeLinkAction_24
                + Messages.ChangeMetaNodeLinkAction_25);
            m_absoluteLink.setSelection(LinkType.Absolute.equals(m_preSelect));
            m_absoluteLink.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    m_linkType = LinkType.Absolute;
                }
            });

            new Label(group, SWT.NONE);
            m_mountpointRelativeLink = new Button(group, SWT.RADIO);
            m_mountpointRelativeLink.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));
            m_mountpointRelativeLink.setText(Messages.ChangeMetaNodeLinkAction_26);
            m_mountpointRelativeLink.setToolTipText(Messages.ChangeMetaNodeLinkAction_27
                + Messages.ChangeMetaNodeLinkAction_28);
            m_mountpointRelativeLink.setSelection(LinkType.MountpointRelative.equals(m_preSelect));
            m_mountpointRelativeLink.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    m_linkType = LinkType.MountpointRelative;
                }
            });

            new Label(group, SWT.NONE);
            m_workflowRelativeLink = new Button(group, SWT.RADIO);
            m_workflowRelativeLink.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));
            m_workflowRelativeLink.setText(Messages.ChangeMetaNodeLinkAction_29);
            m_workflowRelativeLink.setToolTipText(Messages.ChangeMetaNodeLinkAction_30);
            m_workflowRelativeLink.setSelection(LinkType.WorkflowRelative.equals(m_preSelect));
            m_workflowRelativeLink.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    m_linkType = LinkType.WorkflowRelative;
                }
            });
            return group;
        }
    }

}
