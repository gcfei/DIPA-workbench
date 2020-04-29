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
 * ------------------------------------------------------------------------
 */
package org.knime.workbench.explorer.view.actions;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeTimer;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.RemoteWorkflowInput;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileStore;
import org.knime.workbench.explorer.view.ExplorerJob;
import org.osgi.framework.FrameworkUtil;

/**
 * Action that downloads remote items to a temp location and opens them in an editor.
 *
 * @author Peter Ohl, KNIME AG, Zurich, Switzerland
 * @since 6.4
 */
public class DownloadAndOpenWorkflowAction extends Action {

    private static final String PLUGIN_ID = FrameworkUtil.getBundle(DownloadAndOpenWorkflowAction.class)
        .getSymbolicName();

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DownloadAndOpenWorkflowAction.class);

    /*--------- inner job class -------------------------------------------------------------------------*/
    private static class DownloadAndOpenJob extends ExplorerJob {

        private final IWorkbenchPage m_page;

        private final RemoteExplorerFileStore m_source;

        DownloadAndOpenJob(final IWorkbenchPage page, final RemoteExplorerFileStore filestore) {
            super("Download and open remote items"); //$NON-NLS-1$
            m_page = page;
            m_source = filestore;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected IStatus run(final IProgressMonitor monitor) {
            SubMonitor progress = SubMonitor.convert(monitor, 1);
            progress.beginTask(Messages.getString("DownloadAndOpenWorkflowAction.1") + m_source.getName(), 1); //$NON-NLS-1$
            LocalExplorerFileStore tmpDestDir;
            try {
                tmpDestDir = ExplorerMountTable.createExplorerTempDir(m_source.getName());
                tmpDestDir = tmpDestDir.getChild(m_source.getName());
                tmpDestDir.mkdir(EFS.NONE, progress);
            } catch (CoreException e1) {
                return new Status(e1.getStatus().getSeverity(), PLUGIN_ID, e1.getMessage(), e1);
            }
            final WorkflowDownload downloadAction =
                    new WorkflowDownload(m_source, tmpDestDir, /*deleteSource=*/false, null, progress);
            try {
                downloadAction.runSync(monitor);
            } catch (CoreException e) {
                LOGGER.info(Messages.getString("DownloadAndOpenWorkflowAction.2") //$NON-NLS-1$
                    + tmpDestDir);
                return e.getStatus();
            }

            String[] content;
            try {
                content = tmpDestDir.childNames(EFS.NONE, monitor);
            } catch (CoreException e) {
                return new Status(e.getStatus().getSeverity(), PLUGIN_ID, e.getMessage(), e);

            }
            if (content == null || content.length == 0) {
                try {
                    tmpDestDir.delete(EFS.NONE, monitor);
                } catch (CoreException e) {
                    LOGGER.error(Messages.getString("DownloadAndOpenWorkflowAction.3") + e.getMessage(), e); //$NON-NLS-1$
                    // ignore the deletion error
                }
                return new Status(IStatus.ERROR, PLUGIN_ID, 1, Messages.getString("DownloadAndOpenWorkflowAction.4"), null); //$NON-NLS-1$
            }
            if (content.length == 1) {
                // it is weird if the length is not 1 (because we downloaded one item)
                tmpDestDir = tmpDestDir.getChild(content[0]);
            }

            if (tmpDestDir.fetchInfo().isDirectory()) {
                LocalExplorerFileStore wf = tmpDestDir.getChild(WorkflowPersistor.WORKFLOW_FILE);
                if (wf.fetchInfo().exists()) {
                    tmpDestDir = wf;
                } else {
                    // directories that are not workflows cannot be opened
                    LOGGER.info(Messages.getString("DownloadAndOpenWorkflowAction.5") + tmpDestDir); //$NON-NLS-1$
                    return new Status(IStatus.ERROR, PLUGIN_ID, 1,
                        Messages.getString("DownloadAndOpenWorkflowAction.6"), null); //$NON-NLS-1$
                }
            }

            final LocalExplorerFileStore editorInput = tmpDestDir;
            final AtomicReference<IStatus> returnStatus = new AtomicReference<IStatus>(Status.OK_STATUS);
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    try {
                        IEditorDescriptor editorDescriptor = IDE.getEditorDescriptor(editorInput.getName());
                        m_page.openEditor(new RemoteWorkflowInput(editorInput, m_source.toURI()),
                            editorDescriptor.getId());
                        NodeTimer.GLOBAL_TIMER.incRemoteWorkflowOpening();
                    } catch (PartInitException ex) {
                        LOGGER.info(Messages.getString("DownloadAndOpenWorkflowAction.7") //$NON-NLS-1$
                            + Messages.getString("DownloadAndOpenWorkflowAction.8") + downloadAction.getTargetDir()); //$NON-NLS-1$
                        returnStatus.set(new Status(IStatus.ERROR, PLUGIN_ID, 1,
                            Messages.getString("DownloadAndOpenWorkflowAction.9") + editorInput.getName(), null)); //$NON-NLS-1$
                    }
                }
            });

            return returnStatus.get();
        }
    }

    /* --- end of inner job class -----------------------------------------------------------------------------------*/

    private final List<RemoteExplorerFileStore> m_sources;

    private final IWorkbenchPage m_page;

    /**
     * Downloads a remote item to a temp location and opens it in an editor.
     *
     * @param page the current workbench page
     * @param sources things to open
     */
    public DownloadAndOpenWorkflowAction(final IWorkbenchPage page, final List<RemoteExplorerFileStore> sources) {
        setText(Messages.getString("DownloadAndOpenWorkflowAction.10")); //$NON-NLS-1$
        setDescription(Messages.getString("DownloadAndOpenWorkflowAction.11")); //$NON-NLS-1$
        setToolTipText(getDescription());
        m_page = page;
        m_sources = new LinkedList<RemoteExplorerFileStore>(sources);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        for (RemoteExplorerFileStore s : m_sources) {
            LOGGER.info(Messages.getString("DownloadAndOpenWorkflowAction.12") + s.getFullName() + Messages.getString("DownloadAndOpenWorkflowAction.13")); //$NON-NLS-1$ //$NON-NLS-2$
            DownloadAndOpenJob job = new DownloadAndOpenJob(m_page, s);
            job.schedule();
        }
    }

    @Override
    public boolean isEnabled() {
        boolean enabled = true;
        for (RemoteExplorerFileStore s : m_sources) {
            if (s.fetchInfo().isReservedSystemItem()) {
                enabled = false;
            }
        }
        return enabled;
    }
}
