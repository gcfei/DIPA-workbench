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
 *   17.11.2016 (thor): created
 */
package org.knime.workbench.editor2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.util.LockFailedException;

/**
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
abstract class AbstractSaveRunnable extends PersistWorkflowRunnable {
    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());

    private WorkflowEditor m_editor;

    private StringBuilder m_exceptionMessage;

    private IProgressMonitor m_monitor;

    /**
     * Creates a runnable that saves the worfklow.
     *
     * @param editor the editor holding the workflow to save
     * @param exceptionMessage holding an exception message
     * @param monitor the progress monitor to report the progress to
     */
    public AbstractSaveRunnable(final WorkflowEditor editor, final StringBuilder exceptionMessage,
        final IProgressMonitor monitor) {
        m_editor = editor;
        m_exceptionMessage = exceptionMessage;
        m_monitor = monitor;
    }

    @Override
    public final void run(final IProgressMonitor pm) throws InterruptedException {
        final File workflowDir = getSaveLocation();
        try {
            final WorkflowManager wfm = m_editor.getWorkflowManager().get();
            final ProgressHandler progressHandler =
                new ProgressHandler(pm, wfm.getNodeContainers().size(), Messages.AbstractSaveRunnable_0);
            final CheckCancelNodeProgressMonitor progressMonitor = new CheckCancelNodeProgressMonitor(pm);

            progressMonitor.addProgressListener(progressHandler);
            final ExecutionMonitor exec = new ExecutionMonitor(progressMonitor);

            save(wfm, exec);

            m_monitor = null;
        } catch (final FileNotFoundException fnfe) {
            m_logger.fatal(Messages.AbstractSaveRunnable_1, fnfe);
            m_exceptionMessage.append(Messages.AbstractSaveRunnable_2 + fnfe.getMessage());
            handleRunExceptionCleanUp(fnfe);
        } catch (final IOException ioe) {
            if (new File(workflowDir, WorkflowPersistor.WORKFLOW_FILE).length() == 0) {
                m_logger.info(Messages.AbstractSaveRunnable_3);
                m_monitor = null;
            } else {
                m_logger.error(Messages.AbstractSaveRunnable_4 + workflowDir.getName()
                                    + Messages.AbstractSaveRunnable_5, ioe);
                m_exceptionMessage.append(Messages.AbstractSaveRunnable_6 + ioe.getMessage());
                handleRunExceptionCleanUp(ioe);
            }
        } catch (final CanceledExecutionException cee) {
            m_logger.info(Messages.AbstractSaveRunnable_7 + workflowDir.getName());
            m_exceptionMessage.append(Messages.AbstractSaveRunnable_8 + Messages.AbstractSaveRunnable_9);
            handleRunExceptionCleanUp(null);
        } catch (final Exception e) {
            m_logger.error(Messages.AbstractSaveRunnable_10, e);
            m_exceptionMessage.append(Messages.AbstractSaveRunnable_11 + e.getMessage());
            handleRunExceptionCleanUp(e);
        } finally {
            pm.subTask("Finished."); //$NON-NLS-1$
            pm.done();
            m_editor = null;
            m_exceptionMessage = null;
        }
    }

    protected abstract File getSaveLocation();

    protected abstract void save(WorkflowManager wfm, ExecutionMonitor exec) throws IOException, CanceledExecutionException, LockFailedException;

    // Here's the funny thing - WorkflowEditor.saveTo is written to show a dialog to the user on a save failure,
    //      but only if it gets an exception; we swallowed all the exceptions in run(IProgressMonitor) above,
    //      but then (prior to the fix for AP-11179) executed code which caused SWT to throw an exception, which
    //      then resulted in the desired behavior of showing a dialog.
    // Now that we have fixed the SWT problem with AP-11179, we must throw our own exception so that the dialog
    //      continues to be shown.
    private void handleRunExceptionCleanUp(final Exception rootCause) throws InterruptedException {
        PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
            m_monitor.setCanceled(true);
            m_monitor = null;
        });

        if (rootCause != null) {
            throw new InterruptedException(rootCause.getMessage());
        }
    }
}
