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
 *   12.05.2010 (Bernd Wiswedel): created
 */
package org.knime.workbench.editor2.actions;

import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.Role;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContainerTemplate;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowLoadHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.commands.UpdateMetaNodeLinkCommand;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Action to check for updates of metanode templates.
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class CheckUpdateMetaNodeLinkAction extends AbstractNodeAction {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(CheckUpdateMetaNodeLinkAction.class);

    private final boolean m_showInfoMsgIfNoUpdateAvail;

    /** Action ID. */
    public static final String ID = "knime.action.meta_node_check_update_link"; //$NON-NLS-1$

    /** Create new action based on given editor.
     * @param editor The associated editor. */
    public CheckUpdateMetaNodeLinkAction(final WorkflowEditor editor) {
        this(editor, true);
    }

    /** Create new action based on given editor.
     * @param editor The associated editor.
     * @param showInfoMsgIfNoUpdateAvail If to show an info box if no
     * updates are available, true if this is a manually triggered command,
     * false if is run as automatic procedure after load (no user interaction)
     */
    public CheckUpdateMetaNodeLinkAction(final WorkflowEditor editor,
            final boolean showInfoMsgIfNoUpdateAvail) {
        super(editor);
        m_showInfoMsgIfNoUpdateAvail = showInfoMsgIfNoUpdateAvail;
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
        return Messages.CheckUpdateMetaNodeLinkAction_1 + getHotkey("knime.commands.updateMetaNodeLink"); //$NON-NLS-2$
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return Messages.CheckUpdateMetaNodeLinkAction_3
            + Messages.CheckUpdateMetaNodeLinkAction_4;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/meta/metanode_link_update.png"); //$NON-NLS-1$
    }

    /**
     * @return true, if underlying model instance of
     *         <code>WorkflowManager</code>, otherwise false
     */
    @Override
    protected boolean internalCalculateEnabled() {
        if (getManager().isWriteProtected()) {
            return false;
        }
        return !getMetaNodesToCheck().isEmpty();
    }

    protected List<NodeID> getMetaNodesToCheck() {
        List<NodeID> list = new ArrayList<NodeID>();
        for (NodeContainerEditPart p : getSelectedParts(NodeContainerEditPart.class)) {
            NodeContainerUI model = p.getNodeContainer();
            if (Wrapper.wraps(model, NodeContainerTemplate.class)) {
                NodeContainerTemplate tnc = Wrapper.unwrap(model, NodeContainerTemplate.class);
                if (tnc.getTemplateInformation().getRole().equals(Role.Link)) {
                    if (!getManager().canUpdateMetaNodeLink(tnc.getID())) {
                        return Collections.emptyList();
                    }
                    list.add(tnc.getID());
                }
                list.addAll(getNCTemplatesToCheck(tnc));
            }
        }
        return list;
    }

    private List<NodeID> getNCTemplatesToCheck(final NodeContainerTemplate template) {
        List<NodeID> list = new ArrayList<NodeID>();
        for (NodeContainer nc : template.getNodeContainers()) {
            if (nc instanceof NodeContainerTemplate) {
                NodeContainerTemplate tnc = (NodeContainerTemplate)nc;
                if (tnc.getTemplateInformation().getRole().equals(Role.Link)) {
                    if (!getManager().canUpdateMetaNodeLink(tnc.getID())) {
                        return Collections.emptyList();
                    }
                    list.add(tnc.getID());
                }
                list.addAll(getNCTemplatesToCheck(tnc));
            }
        }
        return list;
    }

    /** {@inheritDoc} */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodes) {
        throw new IllegalStateException(Messages.CheckUpdateMetaNodeLinkAction_6);
    }

    /** {@inheritDoc} */
    @Override
    public void runInSWT() {
        List<NodeID> candidateList = getMetaNodesToCheck();
        final Shell shell = SWTUtilities.getActiveShell();
        IWorkbench wb = PlatformUI.getWorkbench();
        IProgressService ps = wb.getProgressService();
        LOGGER.debug(Messages.CheckUpdateMetaNodeLinkAction_7 + candidateList.size() + Messages.CheckUpdateMetaNodeLinkAction_8);
        CheckUpdateRunnableWithProgress runner =
            new CheckUpdateRunnableWithProgress(getManager(), candidateList);
        try {
            ps.busyCursorWhile(runner);
        } catch (InvocationTargetException e) {
            LOGGER.warn(Messages.CheckUpdateMetaNodeLinkAction_9 + e.getMessage(), e);
            return;
        } catch (InterruptedException e) {
            return;
        }
        List<NodeID> updateList = runner.getUpdateList();
        Status status = runner.getStatus();
        if (status.getSeverity() == IStatus.ERROR
                || status.getSeverity() == IStatus.WARNING) {
            ErrorDialog.openError(SWTUtilities.getActiveShell(), null,
                Messages.CheckUpdateMetaNodeLinkAction_10, status);
            if (candidateList.size() == 1) {
                /* As only one node is selected and its update failed,
                 * there is nothing else to do. */
                return;
            }
        }

        // find nodes that will be reset as part of the update
        int nodesToResetCount = 0;
        boolean hasOnlySelectedSubnodes = true;
        for (NodeID id : updateList) {
            NodeContainerTemplate templateNode =
                (NodeContainerTemplate)getManager().findNodeContainer(id);
            // TODO problematic with through-connections
            if (templateNode.containsExecutedNode()) {
                nodesToResetCount += 1;
            }
            if (!(templateNode instanceof SubNodeContainer)) {
                hasOnlySelectedSubnodes = false;
            }
        }

        String nodeSLow = hasOnlySelectedSubnodes ? Messages.CheckUpdateMetaNodeLinkAction_11 : Messages.CheckUpdateMetaNodeLinkAction_12;
        String nodeSUp = hasOnlySelectedSubnodes ? Messages.CheckUpdateMetaNodeLinkAction_13 : Messages.CheckUpdateMetaNodeLinkAction_14;

        if (updateList.isEmpty()) {
            if (m_showInfoMsgIfNoUpdateAvail) {
                MessageDialog.openInformation(shell, Messages.CheckUpdateMetaNodeLinkAction_15, Messages.CheckUpdateMetaNodeLinkAction_16);
            } else {
                //LOGGER.infoWithFormat(Messages.CheckUpdateMetaNodeLinkAction_17, candidateList.size(), nodeSLow);
                LOGGER.infoWithFormat("No updates available (%d %s link(s))", candidateList.size(), nodeSLow);
            }
        } else {
            boolean isSingle = updateList.size() == 1;
            String title = Messages.CheckUpdateMetaNodeLinkAction_18 + nodeSUp + (isSingle ? "" : "s"); //$NON-NLS-2$ //$NON-NLS-3$
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append(Messages.CheckUpdateMetaNodeLinkAction_21);
            if (isSingle && candidateList.size() == 1) {
                messageBuilder.append(nodeSLow);
                messageBuilder.append(" \""); //$NON-NLS-1$
                messageBuilder.append(getManager().findNodeContainer(
                        candidateList.get(0)).getNameWithID());
                messageBuilder.append(Messages.CheckUpdateMetaNodeLinkAction_23);
            } else if (isSingle) {
                messageBuilder.append(Messages.CheckUpdateMetaNodeLinkAction_24 + nodeSLow + Messages.CheckUpdateMetaNodeLinkAction_25);
            } else {
                messageBuilder.append(updateList.size());
                messageBuilder.append(Messages.CheckUpdateMetaNodeLinkAction_26).append(nodeSLow).append(Messages.CheckUpdateMetaNodeLinkAction_27);
            }
            messageBuilder.append("\n\n"); //$NON-NLS-1$
            if (nodesToResetCount > 0) {
                messageBuilder.append(Messages.CheckUpdateMetaNodeLinkAction_29 + nodeSLow + Messages.CheckUpdateMetaNodeLinkAction_30);
            } else {
                messageBuilder.append(Messages.CheckUpdateMetaNodeLinkAction_31);
            }
            String message = messageBuilder.toString();
            if (MessageDialog.openQuestion(shell, title, message)) {
                LOGGER.debug(Messages.CheckUpdateMetaNodeLinkAction_32 + updateList.size() + Messages.CheckUpdateMetaNodeLinkAction_33 + updateList);
                execute(new UpdateMetaNodeLinkCommand(getManager(),
                        updateList.toArray(new NodeID[updateList.size()])));
            }
        }
    }

    private static final class CheckUpdateRunnableWithProgress
        implements IRunnableWithProgress {

        private final WorkflowManager m_hostWFM;
        private final List<NodeID> m_candidateList;
        private final List<NodeID> m_updateList;
        private Status m_status;

        /**
         * @param hostWFM
         * @param candidateList */
        public CheckUpdateRunnableWithProgress(final WorkflowManager hostWFM,
                final List<NodeID> candidateList) {
            m_hostWFM = hostWFM;
            m_candidateList = candidateList;
            m_updateList = new ArrayList<NodeID>();
        }

        /** {@inheritDoc} */
        @Override
        public void run(final IProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException {
            monitor.beginTask(Messages.CheckUpdateMetaNodeLinkAction_34, m_candidateList.size());
            WorkflowLoadHelper lH = new WorkflowLoadHelper(true, m_hostWFM.getContext());
            final String idName = KNIMEEditorPlugin.PLUGIN_ID;
            Status[] stats = new Status[m_candidateList.size()];
            int overallStatus = IStatus.OK;
            for (int i = 0; i < m_candidateList.size(); i++) {
                NodeID id = m_candidateList.get(i);
                NodeContainerTemplate tnc = (NodeContainerTemplate)m_hostWFM.findNodeContainer(id);
                WorkflowManager parent = tnc.getParent();
                monitor.subTask(tnc.getNameWithID());
                Status stat;
                try {
                    String msg;
                    if (parent.checkUpdateMetaNodeLink(id, lH)) {
                        m_updateList.add(id);
                        msg = Messages.CheckUpdateMetaNodeLinkAction_35 + tnc.getNameWithID();
                    } else {
                        msg = Messages.CheckUpdateMetaNodeLinkAction_36 + tnc.getNameWithID();
                    }
                    stat = new Status(IStatus.OK, idName, msg);
                } catch (Exception ex) {
                    Throwable cause = ex;
                    while ((cause.getCause() != null) && (cause.getCause() != cause)) {
                        cause = cause.getCause();
                    }
                    String causeMsg = cause.getMessage();

                    if (cause instanceof FileNotFoundException) {
                        causeMsg = Messages.CheckUpdateMetaNodeLinkAction_37 + causeMsg;
                    }

                    String msg = Messages.CheckUpdateMetaNodeLinkAction_38
                        + Messages.CheckUpdateMetaNodeLinkAction_39 + tnc.getNameWithID() + "\": " //$NON-NLS-2$
                        + cause.getMessage();
                    LOGGER.warn(msg, cause);
                    stat = new Status(IStatus.WARNING , idName, msg, null);
                    overallStatus = IStatus.WARNING;
                }
                if (monitor.isCanceled()) {
                    throw new InterruptedException(Messages.CheckUpdateMetaNodeLinkAction_41);
                }
                stats[i] = stat;
                monitor.worked(1);
            }
            m_status = new MultiStatus(
                    idName, overallStatus, stats, Messages.CheckUpdateMetaNodeLinkAction_42, null);
            monitor.done();
        }

        /** @return the updateList */
        public List<NodeID> getUpdateList() {
            return m_updateList;
        }

        /** @return the status */
        public Status getStatus() {
            return m_status;
        }
    }

}
