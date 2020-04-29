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
 *   11.01.2007 (sieb): created
 */
package org.knime.workbench.editor2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.swing.UIManager;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.UnsupportedWorkflowVersionException;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.node.workflow.WorkflowCreationHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryType;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.core.util.LockFailedException;
import org.knime.workbench.editor2.WorkflowEditorEventListener.ActiveWorkflowEditorEvent;
import org.knime.workbench.editor2.actions.CheckUpdateMetaNodeLinkAllAction;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * A runnable which is used by the {@link WorkflowEditor} to load a workflow with a progress bar. NOTE: As the
 * {@link UIManager} holds a reference to this runnable an own class file is necessary such that all references to the
 * created workflow manager can be deleted, otherwise the manager cannot be deleted later and the memory cannot be
 * freed.
 *
 * @author Christoph Sieb, University of Konstanz
 * @author Fabian Dill, University of Konstanz
 */
class LoadWorkflowRunnable extends PersistWorkflowRunnable {

    /**
     * Message returned by {@link #getLoadingCanceledMessage()} in case the loading has been canceled due to a (future)
     * version conflict. (See also AP-7982)
     */
    static final String INCOMPATIBLE_VERSION_MSG = Messages.LoadWorkflowRunnable_0;

    private static final NodeLogger LOGGER = NodeLogger.getLogger(LoadWorkflowRunnable.class);

    private WorkflowEditor m_editor;

    private File m_workflowFile;

    private File m_mountpointRoot;

    private URI m_mountpointURI;

    private boolean m_isTemporaryCopy;

    private Throwable m_throwable = null;

    /** Message, which is non-null if the user canceled to the load. */
    private String m_loadingCanceledMessage;

    /**
     * Creates a new runnable that load a workflow.
     *
     * @param editor the {@link WorkflowEditor} for which the workflow should be loaded
     * @param uri the URI from the explorer
     * @param workflowFile the workflow file from which the workflow should be loaded (or created = empty workflow file)
     * @param mountpointRoot the root directory of the mountpoint in which the workflow is contained
     * @param isTemporaryCopy <code>true</code> if the workflow is a temporary copy of a workflow that lives somewhere
     *            else, e.g. on a server, <code>false</code> if the workflow is in its original location
     */
    public LoadWorkflowRunnable(final WorkflowEditor editor, final URI uri, final File workflowFile,
        final File mountpointRoot, final boolean isTemporaryCopy) {
        m_editor = editor;
        m_mountpointURI = uri;
        m_workflowFile = workflowFile;
        m_mountpointRoot = mountpointRoot;
        m_isTemporaryCopy = isTemporaryCopy;
    }

    /**
     *
     * @return the throwable which was thrown during the loading of the workflow or null, if no throwable was thrown
     */
    Throwable getThrowable() {
        return m_throwable;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void run(final IProgressMonitor pm) {
        // indicates whether to create an empty workflow
        // this is done if the file is empty
        boolean createEmptyWorkflow = false;

        // name of workflow will be null (uses directory name then)
        String name = null;

        m_throwable = null;

        try {
            // create progress monitor
            ProgressHandler progressHandler = new ProgressHandler(pm, 101, "Loading workflow..."); //$NON-NLS-1$
            final CheckCancelNodeProgressMonitor progressMonitor = new CheckCancelNodeProgressMonitor(pm);
            progressMonitor.addProgressListener(progressHandler);

            File workflowDirectory = m_workflowFile.getParentFile();
            Display d = Display.getDefault();
            GUIWorkflowLoadHelper loadHelper = new GUIWorkflowLoadHelper(d, workflowDirectory.getName(),
                m_mountpointURI, workflowDirectory, m_mountpointRoot, m_isTemporaryCopy);
            final WorkflowLoadResult result =
                WorkflowManager.loadProject(workflowDirectory, new ExecutionMonitor(progressMonitor), loadHelper);
            final WorkflowManager wm = result.getWorkflowManager();
            m_editor.setWorkflowManager(wm);
            pm.subTask("Finished."); //$NON-NLS-1$
            pm.done();
            if (wm.isDirty()) {
                m_editor.markDirty();
            }

            final IStatus status = createStatus(result, !result.getGUIMustReportDataLoadErrors(), false);
            String message;
            switch (status.getSeverity()) {
                case IStatus.OK:
                    message = Messages.LoadWorkflowRunnable_3;
                    break;
                case IStatus.WARNING:
                    message = Messages.LoadWorkflowRunnable_4;
                    logPreseveLineBreaks(
                        Messages.LoadWorkflowRunnable_5 + result.getFilteredError("", LoadResultEntryType.Warning), false); //$NON-NLS-2$
                    break;
                default:
                    message = Messages.LoadWorkflowRunnable_7;
                    logPreseveLineBreaks(
                        Messages.LoadWorkflowRunnable_8 + result.getFilteredError("", LoadResultEntryType.Warning), true); //$NON-NLS-2$
            }
            if (!status.isOK()) {
                showLoadErrorDialog(result, status, message, true);
            }
            final List<NodeID> linkedMNs = wm.getLinkedMetaNodes(true);
            if (!linkedMNs.isEmpty()) {
                final WorkflowEditor editor = m_editor;
                m_editor.addAfterOpenRunnable(new Runnable() {
                    @Override
                    public void run() {
                        postLoadCheckForMetaNodeUpdates(editor, wm, linkedMNs);
                    }
                });
            }
            final Collection<WorkflowEditorEventListener> workflowEditorEventListeners =
                WorkflowEditorEventListeners.getListeners();
            if (!workflowEditorEventListeners.isEmpty()) {
                final WorkflowEditor editor = m_editor;
                editor.addAfterOpenRunnable(() -> {
                    final ActiveWorkflowEditorEvent event =
                        WorkflowEditorEventListeners.createActiveWorkflowEditorEvent(editor);
                    for (final WorkflowEditorEventListener listener : workflowEditorEventListeners) {
                        try {
                            listener.workflowLoaded(event);
                        } catch (final Throwable throwable) {
                            LOGGER.error(Messages.LoadWorkflowRunnable_10, throwable);
                        }
                    }
                });
            }
        } catch (FileNotFoundException fnfe) {
            m_throwable = fnfe;
            LOGGER.fatal(Messages.LoadWorkflowRunnable_11, fnfe);
        } catch (IOException ioe) {
            m_throwable = ioe;
            if (m_workflowFile.length() == 0) {
                LOGGER.info(Messages.LoadWorkflowRunnable_12);
                // this is the only place to set this flag to true: we have an
                // empty workflow file, i.e. a new project was created
                // bugfix 1555: if an exception is thrown DO NOT create empty
                // workflow
                createEmptyWorkflow = true;
            } else {
                LOGGER.error(Messages.LoadWorkflowRunnable_13 + m_workflowFile.getName(), ioe);
            }
        } catch (InvalidSettingsException ise) {
            LOGGER.error(Messages.LoadWorkflowRunnable_14 + m_workflowFile.getName(), ise);
            m_throwable = ise;
        } catch (UnsupportedWorkflowVersionException uve) {
            m_loadingCanceledMessage = INCOMPATIBLE_VERSION_MSG;
            LOGGER.info(m_loadingCanceledMessage, uve);
            m_editor.setWorkflowManager(null);
        } catch (CanceledExecutionException cee) {
            m_loadingCanceledMessage = Messages.LoadWorkflowRunnable_15 + m_workflowFile.getParentFile().getName();
            LOGGER.info(m_loadingCanceledMessage, cee);
            m_editor.setWorkflowManager(null);
        } catch (LockFailedException lfe) {
            StringBuilder error = new StringBuilder();
            error.append(Messages.LoadWorkflowRunnable_16);
            error.append(m_workflowFile.getParentFile().getName());
            if (m_workflowFile.getParentFile().exists()) {
                error.append(Messages.LoadWorkflowRunnable_17);
            } else {
                error.append(Messages.LoadWorkflowRunnable_18);
            }
            m_loadingCanceledMessage = error.toString();
            LOGGER.info(m_loadingCanceledMessage, lfe);
            m_editor.setWorkflowManager(null);
        } catch (Throwable e) {
            m_throwable = e;
            LOGGER.error(Messages.LoadWorkflowRunnable_19 + e.getMessage(), e);
            m_editor.setWorkflowManager(null);
        } finally {
            // create empty WFM if a new workflow is created
            // (empty workflow file)
            if (createEmptyWorkflow) {
                WorkflowCreationHelper creationHelper = new WorkflowCreationHelper();
                WorkflowContext.Factory fac = new WorkflowContext.Factory(m_workflowFile.getParentFile());
                fac.setMountpointRoot(m_mountpointRoot);
                fac.setMountpointURI(m_mountpointURI);
                creationHelper.setWorkflowContext(fac.createContext());

                m_editor.setWorkflowManager(WorkflowManager.ROOT.createAndAddProject(name, creationHelper));
                // save empty project immediately
                // bugfix 1341 -> see WorkflowEditor line 1294
                // (resource delta visitor movedTo)
                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        m_editor.doSave(new NullProgressMonitor());
                    }
                });
                m_editor.setIsDirty(false);

            }
            // IMPORTANT: Remove the reference to the file and the
            // editor!!! Otherwise the memory cannot be freed later
            m_editor = null;
            m_workflowFile = null;
            m_mountpointRoot = null;
        }
    }

    static void showLoadErrorDialog(final LoadResult result, final IStatus status, final String message,
        final boolean isWorkflow) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                Shell shell = SWTUtilities.getActiveShell();
                if (result.getMissingNodes().isEmpty() && result.getMissingTableFormats().isEmpty()) {
                    String title = isWorkflow ? "Workflow Load" : "Component Load"; //$NON-NLS-1$ //$NON-NLS-2$
                    // will not open if status is OK.
                    ErrorDialog.openError(shell, title, message, status);
                } else {

                    List<String> missingExtensionList = new ArrayList<>();

                    result.getMissingNodes().stream().map(i -> i.getComponentName()).distinct()
                        .forEach(missingExtensionList::add);

                    result.getMissingTableFormats().stream().map(i -> i.getComponentName()).distinct()
                        .forEach(missingExtensionList::add);

                    String missingExtensions = StringUtils.join(missingExtensionList, ", "); //$NON-NLS-1$

                    String[] dialogButtonLabels = {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL};
                    String title =
                        isWorkflow ? "Workflow requires missing extensions" : "Component requires missing extensions"; //$NON-NLS-1$ //$NON-NLS-2$
                    MessageDialog dialog = new MessageDialog(shell, title, null,
                        message + Messages.LoadWorkflowRunnable_25 + missingExtensions
                            + Messages.LoadWorkflowRunnable_26,
                        MessageDialog.WARNING, dialogButtonLabels, 0);
                    if (dialog.open() == 0) {
                        Job j = new InstallMissingNodesJob(result.getMissingNodes(), result.getMissingTableFormats());
                        j.setUser(true);
                        j.schedule();
                    }
                }
            }
        });
    }

    /** @return True if the load process has been interrupted. */
    public boolean hasLoadingBeenCanceled() {
        return m_loadingCanceledMessage != null;
    }

    /**
     * @return the loadingCanceledMessage, non-null if {@link #hasLoadingBeenCanceled()}.
     */
    public String getLoadingCanceledMessage() {
        return m_loadingCanceledMessage;
    }

    static void postLoadCheckForMetaNodeUpdates(final WorkflowEditor editor, final WorkflowManager parent,
        final List<NodeID> links) {

        final Map<Boolean, List<NodeID>> partitionedLinks = links.stream()
            .collect(Collectors.partitioningBy(i -> parent.findNodeContainer(i) instanceof SubNodeContainer));
        final List<NodeID> componentLinks = partitionedLinks.get(Boolean.TRUE);
        final List<NodeID> metanodeLinks = partitionedLinks.get(Boolean.FALSE);

        final StringBuilder m = new StringBuilder("The workflow contains "); //$NON-NLS-1$
        if (componentLinks.size() > 0) {
            if (componentLinks.size() == 1) {
                m.append("one component link (\""); //$NON-NLS-1$
                m.append(parent.findNodeContainer(componentLinks.get(0)).getNameWithID());
                m.append("\")"); //$NON-NLS-1$
            } else {
                m.append(componentLinks.size()).append(" component links"); //$NON-NLS-1$
            }
            if (metanodeLinks.size() > 0) {
                m.append(" and "); //$NON-NLS-1$
            } else {
                m.append("."); //$NON-NLS-1$
            }
        }
        if (metanodeLinks.size() == 1) {
            m.append("one metanode link (\""); //$NON-NLS-1$
            m.append(parent.findNodeContainer(metanodeLinks.get(0)).getNameWithID());
            m.append("\")."); //$NON-NLS-1$
        } else if (metanodeLinks.size() > 1) {
            m.append(metanodeLinks.size()).append(" metanode links."); //$NON-NLS-1$
        }
        m.append(Messages.LoadWorkflowRunnable_36);

        final String message = m.toString();
        final AtomicBoolean result = new AtomicBoolean(false);
        final IPreferenceStore corePrefStore = KNIMEUIPlugin.getDefault().getPreferenceStore();
        final String pKey = PreferenceConstants.P_META_NODE_LINK_UPDATE_ON_LOAD;
        String pref = corePrefStore.getString(pKey);
        boolean showInfoMsg = true;
        if (MessageDialogWithToggle.ALWAYS.equals(pref)) {
            result.set(true);
            showInfoMsg = false;
        } else if (MessageDialogWithToggle.NEVER.equals(pref)) {
            result.set(false);
        } else {
            final Display display = Display.getDefault();
            display.syncExec(new Runnable() {
                @Override
                public void run() {
                    Shell activeShell = SWTUtilities.getActiveShell(display);
                    MessageDialogWithToggle dlg = MessageDialogWithToggle.openYesNoCancelQuestion(activeShell,
                        Messages.LoadWorkflowRunnable_37, message, Messages.LoadWorkflowRunnable_38, false, corePrefStore, pKey);
                    switch (dlg.getReturnCode()) {
                        case IDialogConstants.YES_ID:
                            result.set(true);
                            break;
                        default:
                            result.set(false);
                    }
                }
            });
        }
        if (result.get()) {
            new CheckUpdateMetaNodeLinkAllAction(editor, showInfoMsg).run();
        }
    }

}
