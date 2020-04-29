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


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileInfo;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.filesystem.RemoteDownloadStream;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileInfo;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.AbstractContentProvider.AfterRunCallback;
import org.knime.workbench.explorer.view.ExplorerJob;

/**
 *
 * @author ohl, KNIME AG, Zurich, Switzerland
 * @since 7.0
 */
public class WorkflowDownload extends TempExtractArchive {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(WorkflowDownload.class);

    private final RemoteExplorerFileStore m_source;

    /**
     * Creates a action with the source and parent directory.
     *
     * @param source the source file store containing the workflow
     * @param target the target directory to download the workflow to
     * @param deleteSource if true the source is deleted after a successful download
     * @param afterRunCallback see
     *            {@link AbstractContentProvider#performDownloadAsync(RemoteExplorerFileStore, LocalExplorerFileStore, boolean, AfterRunCallback)}
     *            - may be null.
     */
    public WorkflowDownload(final RemoteExplorerFileStore source,
            final LocalExplorerFileStore target, final boolean deleteSource, final AfterRunCallback afterRunCallback) {
        this(source, target, deleteSource, afterRunCallback, null);
    }

    /**
     * Creates a action with the source and parent directory.
     *
     * @param source the source file store containing the workflow
     * @param target the target directory to download the workflow to
     * @param deleteSource if true the source is deleted after a successful download
     * @param afterRunCallback see {@link AbstractContentProvider#performDownloadAsync(RemoteExplorerFileStore,
     * LocalExplorerFileStore, boolean, AfterRunCallback)} - may be null.
     * @param monitor the progress monitor to use
     */
    public WorkflowDownload(final RemoteExplorerFileStore source, final LocalExplorerFileStore target,
        final boolean deleteSource, final AfterRunCallback afterRunCallback, final IProgressMonitor monitor) {
        super(null, target, deleteSource, afterRunCallback, monitor);
        m_source = source;
    }

    /**
     * @return the file store to download
     */
    protected RemoteExplorerFileStore getSourceFile() {
        return m_source;
    }

    /**
     * @return true if the download source provided by {@link #getSourceFile()}
     *      represents a workflow
     * @since 6.4
     */
    protected boolean isSourceSupported() {
        RemoteExplorerFileInfo sourceInfo = getSourceFile().fetchInfo();
        AbstractContentProvider targetContentProvider = getTargetDir().getContentProvider();

        return sourceInfo.isWorkflow() || sourceInfo.isWorkflowGroup() || sourceInfo.isSnapshot()
                || (sourceInfo.isFile() && targetContentProvider.canHostDataFiles())
                || targetContentProvider.canHostWorkflowTemplate(getSourceFile());
// copying of workflow jobs is disabled until implemented on server
//        RemoteExplorerFileInfo info = getSourceFile().fetchInfo();
//        return info.isWorkflow()
//            || (info.isWorkflowJob() && !info.isExecuting());
    }

    /**
     *
     */
    protected void extractDownloadToTarget(final File downloadedFile)
            throws Exception {
        AbstractExplorerFileStore source = getSourceFile();

        AbstractExplorerFileInfo info = source.fetchInfo();
        if (info.isSnapshot()) {
            source = source.getParent();
            info = source.fetchInfo();
        }

        if (info.isFile()) {
            FileUtils.copyFile(downloadedFile, getTargetDir().toLocalFile());
        } else if (info.isWorkflow() || info.isWorkflowTemplate() || info.isWorkflowGroup()) {
            setSourceArchiveFile(downloadedFile);
            unpackWorkflowIntoLocalDir();
        } else {
            throw new IllegalArgumentException(Messages.getString("WorkflowDownload.0") + getSourceFile().getMountIDWithFullPath() + "'" //$NON-NLS-1$ //$NON-NLS-2$
                    + Messages.getString("WorkflowDownload.2")); //$NON-NLS-1$
        }
    }

    /**
     * @since 7.3
     */
    @Override
    protected void runSyncInternal(final IProgressMonitor monitor) throws CoreException {
        String srcIdentifier = getSourceFile().getMountIDWithFullPath();
        if (!isSourceSupported()) {
            throw new IllegalArgumentException(Messages.getString("WorkflowDownload.3") //$NON-NLS-1$
                    + srcIdentifier
                    + Messages.getString("WorkflowDownload.4")); //$NON-NLS-1$
        }
        LOGGER.debug(Messages.getString("WorkflowDownload.5") + srcIdentifier //$NON-NLS-1$
                + Messages.getString("WorkflowDownload.6") + getTargetIdentifier() + "'"); //$NON-NLS-1$ //$NON-NLS-2$

        final DownloadRunnable dwnLoader = new DownloadRunnable(
                getSourceFile());
        dwnLoader.run(monitor);

        // now wait for the download to finish
        boolean success = false;
        LOGGER.info(Messages.getString("WorkflowDownload.8") + srcIdentifier + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        success = dwnLoader.waitUntilDone();

        // error handling if download failed
        File tmpLoc = dwnLoader.getTempFile();
        if (tmpLoc == null || !success) {
            int status;
            String msg = Messages.getString("WorkflowDownload.10"); //$NON-NLS-1$
            if (success) {
                msg += dwnLoader.getErrorMessage();
                status = IStatus.ERROR;
                LOGGER.error(msg);
            } else {
                status = IStatus.WARNING;
                msg += Messages.getString("WorkflowDownload.11"); //$NON-NLS-1$
                LOGGER.warn(msg);
            }
            if (getTargetDir().fetchInfo().exists()) {
                LOGGER.info(Messages.getString("WorkflowDownload.12") //$NON-NLS-1$
                        + getTargetIdentifier() + ") "); //$NON-NLS-1$
            }
            throw new CoreException(new Status(status, ExplorerActivator.PLUGIN_ID, msg));
        }

        prepareTarget();

        try {
            extractDownloadToTarget(tmpLoc);
        } catch (Exception e) {
            LOGGER.error(Messages.getString("WorkflowDownload.14"), e); //$NON-NLS-1$
            success = false;
        } finally {
            tmpLoc.delete();
        }
        refreshTarget();
        Status status = dwnLoader.getStatus();
        if (status != null) {
            throw new CoreException(status);
        }
        if (success && getDeleteSource()) {
            m_source.delete(EFS.NONE, monitor);
        }
    }

    /**
     */
    public void schedule() {
        ExplorerJob j = new ExplorerJob(Messages.getString("WorkflowDownload.15") + getSourceFile().getName() + Messages.getString("WorkflowDownload.16") //$NON-NLS-1$ //$NON-NLS-2$
                + getTargetDir().getMountIDWithFullPath()) {
            @Override
            protected IStatus run(final IProgressMonitor monitor2) {
                monitor2.beginTask(Messages.getString("WorkflowDownload.17") + getSourceFile().getName() + Messages.getString("WorkflowDownload.18") //$NON-NLS-1$ //$NON-NLS-2$
                        + getTargetDir().getMountIDWithFullPath() , 1);
                try {
                    runSync(monitor2);
                } catch (CoreException e) {
                    LOGGER.info(Messages.getString("WorkflowDownload.19") + getSourceFile().getMountIDWithFullPath() + Messages.getString("WorkflowDownload.20") //$NON-NLS-1$ //$NON-NLS-2$
                        + getTargetDir().getMountIDWithFullPath() + ": " + e.getMessage(), e); //$NON-NLS-1$
                    return e.getStatus();
                }
                return Status.OK_STATUS;
            }
        };
        j.schedule();
    }

    //=========================================================================

    /**
     * Downloads a remote file store to a local temp dir.
     *
     * @author Peter Ohl, KNIME AG, Zurich, Switzerland
     */
    protected static class DownloadRunnable implements Runnable {

        private final RemoteExplorerFileStore m_source;

        private final AtomicBoolean m_cancel = new AtomicBoolean(false);

        // this is also used as object to wait for the runnable to finish
        private final AtomicBoolean m_done = new AtomicBoolean(false);

        private File m_tmpFile;

        private String m_errorMsg;

        private MultiStatus m_status = null;

        /**
         * Returns the collected status of the download operation. If some
         * items could not be downloaded, e.g. due to missing permissions,
         * they are collected as a MultiStatus.
         *
         * @return the status of the download or null if no status messages
         *      are available
         */
        public Status getStatus() {
            return m_status;
        }

        /**
         * @param source the file store to download
         *
         */
        public DownloadRunnable(final RemoteExplorerFileStore source) {
            if (source == null) {
                throw new NullPointerException(Messages.getString("WorkflowDownload.22")); //$NON-NLS-1$
            }
            m_source = source;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            run(null);
        }

        /**
         * @param monitor the progress monitor
         */
        public void run(final IProgressMonitor monitor) {
            try {
                if (monitor != null) {
                    StringBuilder progMsg = null;
                    progMsg = new StringBuilder(Messages.getString("WorkflowDownload.23")); //$NON-NLS-1$
                    progMsg.append(m_source.getFullName());
                    progMsg.append(Messages.getString("WorkflowDownload.24")); //$NON-NLS-1$
                    monitor.setTaskName(progMsg.toString());
                    // we progress over kilobytes in case people download
                    // flows larger than 4GB. Have fun.
                    monitor.beginTask(progMsg.toString(),
                            IProgressMonitor.UNKNOWN);
                }
                RemoteDownloadStream in =
                        m_source.openDownloadStream();
                // wait for the server to finish zipping
                while (!in.readyForDownload()) {
                    if (monitor != null && monitor.isCanceled()) {
                        m_cancel.set(true);
                    }
                    if (m_cancel.get()) {
                        m_tmpFile = null;
                        m_errorMsg = Messages.getString("WorkflowDownload.25"); //$NON-NLS-1$
                        // cancel server
                        in.close();
                        return;
                    }
                   Thread.sleep(1000);
                }

                String size = " / <unknown>"; //$NON-NLS-1$
                final String taskMessage = Messages.getString("WorkflowDownload.27") + m_source.getFullName()+ ": "; //$NON-NLS-1$ //$NON-NLS-2$
                long downloaded = 0;
                boolean useKB = false;
                boolean useGB = false;

                if (monitor != null) {
                    int kbyte = IProgressMonitor.UNKNOWN;
                    long l = in.length();
                    // we progress over kilobytes in case people download
                    // flows larger than 4GB. Have fun.
                    if (l >= 0) {
                        kbyte = (int)(l >> 10);

                        if (kbyte >> 10 == 0) {
                            useKB = true;
                            size = " / " + kbyte + " kB."; //$NON-NLS-1$ //$NON-NLS-2$
                        } else {
                            useGB = kbyte >> 20 > 0;
                            size = " / " + getDecimalSize(kbyte, useGB) + "."; //$NON-NLS-1$ //$NON-NLS-2$
                        }
                    }
                    monitor.beginTask(taskMessage + (useKB ? "0 kB" : useGB ? "0 GB" : "0 MB") + size, kbyte); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                }
                m_tmpFile = File.createTempFile("KNIMEServerDownload", ".tmp"); //$NON-NLS-1$ //$NON-NLS-2$
                LOGGER.debug(Messages.getString("WorkflowDownload.38") + m_source //$NON-NLS-1$
                        + Messages.getString("WorkflowDownload.39") //$NON-NLS-1$
                        + m_tmpFile.getAbsolutePath() + "'"); //$NON-NLS-1$

                try (BufferedInputStream inStream = new BufferedInputStream(in, 1024 * 1024);
                        FileOutputStream outStream = new FileOutputStream(m_tmpFile)) {
                    int b;
                    byte[] buffer = new byte[1024 * 1024];
                    while ((b = inStream.read(buffer)) >= 0) {
                        outStream.write(buffer, 0, b);
                        if (monitor != null) {
                            monitor.worked(b >> 10);
                            downloaded += b;
                            monitor.setTaskName(taskMessage
                                + (useKB ? (downloaded >> 10) + " kB" : getDecimalSize(downloaded >> 10, useGB)) //$NON-NLS-1$
                                + size);
                            if (monitor.isCanceled()) {
                                m_cancel.set(true);
                            }
                        }
                        if (m_cancel.get()) {
                            m_tmpFile.delete();
                            m_tmpFile = null;
                            m_errorMsg = Messages.getString("WorkflowDownload.42"); //$NON-NLS-1$
                            return;
                        }
                    }
                }
                m_errorMsg = null;
                List<String> messages = null;
                try {
                    messages = in.getMessages();
                } catch (Exception e) {
                    messages = Collections.emptyList();
                    LOGGER.error(Messages.getString("WorkflowDownload.43"), e); //$NON-NLS-1$
                }

                if (messages.size() > 0) {
                    final List<IStatus> result = new LinkedList<IStatus>();
                    for (String msg : messages) {
                        result.add(new Status(IStatus.WARNING,
                                ExplorerActivator.PLUGIN_ID, msg));
                    }
                    m_status = new MultiStatus(
                            ExplorerActivator.PLUGIN_ID,
                            IStatus.WARNING, result.toArray(new IStatus[0]),
                            Messages.getString("WorkflowDownload.44") //$NON-NLS-1$
                            + Messages.getString("WorkflowDownload.45"), null); //$NON-NLS-1$
                }
            } catch (Throwable e) {
                m_tmpFile = null;
                m_errorMsg = e.getMessage();
            } finally {
                done();
            }
        }

        /**
         * Gets the size of the file with the first decimal digit.
         *
         * @param size The size in kB.
         * @param useGB <code>true</code> if the size should be displayed in GB, <code>false</code> if the size should
         *            be displayed in MB.
         * @return The formatted string.
         */
        private static final String getDecimalSize(final long size, final boolean useGB) {
            if (useGB) {
                double gb = size / (1024.0 * 1024.0);
                return String.format("%.1f GB", gb); //$NON-NLS-1$
            } else {
                double mb = size / 1024.0;
                return String.format("%.1f MB", mb); //$NON-NLS-1$
            }
        }

        /**
         * Doesn't cancel server side activities and doesn't interrupt until
         * server response is received.
         */
        public void cancel() {
            m_cancel.set(true);
        }

        private void done() {
            synchronized (m_done) {
                m_done.set(true);
                m_done.notifyAll();
            }
        }

        /**
         * Waits until the download has finished.
         *
         * @return true if the download has successfully finished, false
         *      if it has been canceled
         */
        public boolean waitUntilDone() {
            synchronized (m_done) {
                if (m_done.get()) {
                    return !m_cancel.get();
                }
                try {
                    m_done.wait();
                } catch (InterruptedException e) {
                    return !m_cancel.get();
                }
                return !m_cancel.get();
            }
        }

        /**
         * @return true if the download has successfully finished, false
         *      otherwise
         */
        public boolean finished() {
            synchronized (m_done) {
                return m_done.get();
            }
        }

        /**
         *
         * @return null until the jobs finishes. If successful a file with the
         *         archive. If unsuccessfully zipped, also null, and the error
         *         message is set.
         */
        public File getTempFile() {
            synchronized (m_done) {
                if (m_done.get()) {
                    return m_tmpFile;
                }
                return null;
            }
        }

        /**
         * Should contain something if {@link #getTempFile()} returns null.
         *
         * @return ...
         */
        public String getErrorMessage() {
            return m_errorMsg;
        }
    }

}
