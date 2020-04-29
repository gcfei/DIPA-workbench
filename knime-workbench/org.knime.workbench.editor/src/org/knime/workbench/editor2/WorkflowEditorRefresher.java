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
package org.knime.workbench.editor2;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.ui.node.workflow.async.AsyncWorkflowManagerUI;
import org.knime.core.ui.node.workflow.async.SnapshotNotFoundException;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * Encapsulates all the (automatic) refresh logic for the workflow editor.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
class WorkflowEditorRefresher {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(WorkflowEditorRefresher.class);

    /** A timer thread the workflow refresh timer tasks are submitted to. */
    private static Timer REFRESH_TIMER = null;

    /** A timer thread the connected timer tasks are submitted to. */
    private static Timer CONNECTED_TIMER = null;

    /** If non-null, a currently scheduled task that periodically refreshes the WorkflowManagerUI **/
    private TimerTask m_refreshTimerTask = null;

    /**
     * If non-null, it periodically checks whether the workflow has been refreshed within a specified time interval.
     * Otherwise the workflow (and workflow editor) is considered as disconnected.
     */
    private TimerTask m_connectedTimerTask = null;

    /** Flag whether the auto-workflow-refresh (for refreshable workflows only) is enabled */
    private boolean m_isAutoRefreshEnabled;

    /** The auto-refresh interval */
    private long m_autoRefreshInterval;

    /** Flag whether edit operations are disabled */
    private boolean m_isEditDisabled;

    /** Whether the workflow has been refreshed recently. */
    private AtomicBoolean m_hasBeenRefreshed = new AtomicBoolean(true);

    /** If the workflow manager is the client to a server this flag indicates whether it has a connection or not. */
    private boolean m_isConnected = true;

    /** To get notified on events when the workflow editor is hidden or gets visible again */
    private IPartListener2 m_partListener;

    /** A flag indicating whether the workflow editor is visible to the user. */
    private boolean m_isVisible;

    private WorkflowEditor m_editor;

    /** Called whenever the connection status changes */
    private Runnable m_connectedCallback;

    private String m_disconnectedMessage = null;

    /**
     * Creates a new refresher.
     *
     * @param editor the workflow editor this refresh is associated with
     * @param connectedCallback callback called when connected-status is changed (i.e. server cannot be reached anymore)
     */
    WorkflowEditorRefresher(final WorkflowEditor editor, final Runnable connectedCallback) {
        m_editor = editor;
        m_connectedCallback = connectedCallback;
        // add part listener to be notified when the workflow editor is active
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            m_partListener = new IPartListener2() {

                @Override
                public void partVisible(final IWorkbenchPartReference partRef) {
                    // start refresh job for this workflow editor
                    if (m_editor == partRef.getPart(false)) {
                        m_isVisible = true;
                        tryStartingRefreshTimer();
                    }
                }

                @Override
                public void partOpened(final IWorkbenchPartReference partRef) {
                }

                @Override
                public void partInputChanged(final IWorkbenchPartReference partRef) {
                }

                @Override
                public void partHidden(final IWorkbenchPartReference partRef) {
                    // pause refresh job for this workflow editor
                    if (m_editor == partRef.getPart(false)) {
                        m_isVisible = false;
                        if (cancelTimers()) {
                            LOGGER
                                .debug(Messages.WorkflowEditorRefresher_0 + partRef.getPartName() + "'"); //$NON-NLS-2$
                        }
                    }
                }

                @Override
                public void partDeactivated(final IWorkbenchPartReference partRef) {
                }

                @Override
                public void partClosed(final IWorkbenchPartReference partRef) {
                }

                @Override
                public void partBroughtToTop(final IWorkbenchPartReference partRef) {
                }

                @Override
                public void partActivated(final IWorkbenchPartReference partRef) {
                }
            };
            window.getPartService().addPartListener(m_partListener);

            IPreferenceStore prefStore = KNIMEUIPlugin.getDefault().getPreferenceStore();
            prefStore.addPropertyChangeListener(e -> {
                switch (e.getProperty()) {
                    case PreferenceConstants.P_REMOTE_WORKFLOW_EDITOR_AUTO_REFRESH:
                    case PreferenceConstants.P_REMOTE_WORKFLOW_EDITOR_AUTO_REFRESH_INTERVAL_MS:
                    case PreferenceConstants.P_REMOTE_WORKFLOW_EDITOR_EDITS_DISABLED:
                        setup();
                        break;
                    default:
                }
            });
        }
    }

    /**
     * (Re-)Loads the preferences and sets up the workflow editor refresher. Depending on the preference settings also
     * might cancel or start a new refresh timer.
     */
    void setup() {
        IPreferenceStore prefStore = KNIMEUIPlugin.getDefault().getPreferenceStore();
        m_isAutoRefreshEnabled = prefStore.getBoolean(PreferenceConstants.P_REMOTE_WORKFLOW_EDITOR_AUTO_REFRESH);
        if (!m_isAutoRefreshEnabled && m_refreshTimerTask != null) {
            cancelTimers();
        }

        long autoRefreshInterval = prefStore.getLong(PreferenceConstants.P_REMOTE_WORKFLOW_EDITOR_AUTO_REFRESH_INTERVAL_MS);
        if (m_autoRefreshInterval != autoRefreshInterval) {
            m_autoRefreshInterval = autoRefreshInterval;
            //restart the timer if its running
            cancelTimers();
        }

        boolean isEditDisabled = prefStore.getBoolean(PreferenceConstants.P_REMOTE_WORKFLOW_EDITOR_EDITS_DISABLED);
        if (m_isEditDisabled != isEditDisabled) {
            m_isEditDisabled = isEditDisabled;
            cancelTimers();
        }

        if (m_isVisible) {
            tryStartingRefreshTimer();
        }
    }

    /**
     * Tries to start the refresh timer if enabled, not already running etc.
     */
    void tryStartingRefreshTimer() {
        if (m_editor.getWorkflowManagerUI() != null && m_editor.getWorkflowManagerUI() instanceof AsyncWorkflowManagerUI
            && m_refreshTimerTask == null && m_isAutoRefreshEnabled) {
            synchronized (WorkflowEditor.class) {
                if (REFRESH_TIMER == null) {
                    REFRESH_TIMER = new Timer("Workflow Refresh Timer", true); //$NON-NLS-1$
                }
            }
            m_refreshTimerTask = new TimerTask() {
                private boolean m_lastRefreshSuccessful = true;
                @Override
                public void run() {
                    try {
                        getAsyncWFM().refreshOrFail(false);
                        m_hasBeenRefreshed.set(true);
                        m_lastRefreshSuccessful = true;
                    } catch (SnapshotNotFoundException e) {
                        //refresh not possible because, e.g., underlying job has been swapped to disk
                        String message = Messages.WorkflowEditorRefresher_3
                            + Messages.WorkflowEditorRefresher_4;
                        cancelTimers();
                        disconnect(true, message);
                        Display.getDefault().syncExec(() -> MessageDialog.openWarning(SWTUtilities.getActiveShell(),
                            "Auto-refresh failed", message)); //$NON-NLS-1$
                    } catch (NoSuchElementException e) {
                        //job-workflow is not available anymore
                        //job has mostly likely been deleted on the server
                        String message = Messages.WorkflowEditorRefresher_6;
                        cancelTimers();
                        disconnect(true, message);
                        Display.getDefault().syncExec(() -> MessageDialog.openWarning(SWTUtilities.getActiveShell(),
                            "Auto-refresh failed", message)); //$NON-NLS-1$
                    } catch (Exception e) {
                        //if something went wrong refreshing the workflow (e.g. timeout)
                        //-> just log it, continue refreshing and hope for the best
                        if (m_lastRefreshSuccessful) {
                            //issue a log-warning once if the workflow has been refreshed in the last cycle
                            LOGGER.warn(Messages.WorkflowEditorRefresher_8 + e.getMessage(), e);
                        }
                        m_lastRefreshSuccessful = false;
                   }
                }
            };
            //delay timer start by 500 ms in order to give the editor time to load the workflow visuals before
            //the change-events (e.g. progress or state) arrive
            //(which otherwise leads, e.g., to a strange position of the node annotations, sometimes)
            REFRESH_TIMER.schedule(m_refreshTimerTask, 500, m_autoRefreshInterval);
            LOGGER.debug(Messages.WorkflowEditorRefresher_9 + m_editor.getTitle() + Messages.WorkflowEditorRefresher_10
                + m_autoRefreshInterval + " ms"); //$NON-NLS-1$

            //start timer that checks whether the workflow has been refreshed within a certain time interval
            //otherwise the workflow and workflow editor is regarded as disconnected
            if (!isWorkflowEditDisabled()) {
                synchronized (WorkflowEditor.class) {
                    if (CONNECTED_TIMER == null) {
                        CONNECTED_TIMER = new Timer("Workflow Connection-Test Timer", true); //$NON-NLS-1$
                    }
                }
                m_connectedTimerTask = new TimerTask() {
                    @Override
                    public void run() {
                        if (m_hasBeenRefreshed.getAndSet(false)) {
                            //everything fine
                            if (!isConnected()) {
                                connect(true);
                            }
                        } else {
                            if (isConnected()) {
                                disconnect(true,
                                    Messages.WorkflowEditorRefresher_13);
                            }
                        }
                    }
                };
                //delay timer start by 500 ms (see above)
                CONNECTED_TIMER.schedule(m_connectedTimerTask, 500,
                    KNIMEConstants.WORKFLOW_EDITOR_CONNECTION_TIMEOUT);
            } else {
                disconnect(false, "Edit operations are disabled."); //$NON-NLS-1$
            }
        } else {
            disconnect(false, "Auto-refresh turned off."); //$NON-NLS-1$
        }
    }

    /**
     * Whether the auto-refresh is enabled.
     *
     * @return <code>true</code> if enabled
     */
    boolean isAutoRefreshEnabled() {
        return m_isAutoRefreshEnabled;
    }

    /**
     * @return the auto-refresh interval in ms
     */
    long getAutoRefreshInterval() {
        return m_autoRefreshInterval;
    }

    /**
     * Indicates whether editing is allowed. Workflow edit operations are disabled if the auto-refresh is disabled, the
     * refresh rate is too low, or the workflow-edit option in the preferences is disabled.
     *
     * @return <code>true</code> if workflow edits are disabled
     */
    boolean isWorkflowEditDisabled() {
        return !isAutoRefreshEnabled() || m_autoRefreshInterval > KNIMEConstants.WORKFLOW_EDITOR_CONNECTION_TIMEOUT
            || m_isEditDisabled;
    }

    /**
     * Whether there is a valid connection or not for refreshes.
     *
     * @return <code>true</code> if there is a connection to the server
     */
    boolean isConnected() {
        return !m_editor.getWorkflowManager().isPresent() && m_isConnected;
    }

    /**
     * @return the reason why the remote workflow editor is disconnected or an empty optional if connected
     */
    Optional<String> getDisconnectedMessage() {
        if (isConnected()) {
            assert m_disconnectedMessage == null;
            return Optional.empty();
        } else {
            return Optional.of(m_disconnectedMessage);
        }
    }

    /**
     * Disposes the refresher, i.e. unregisters listeners, stops the refresh timer etc.
     */
    void dispose() {
        //unregister part listener
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        assert window != null;
        window.getPartService().removePartListener(m_partListener);

        cancelTimers();
    }

    private void disconnect(final boolean callback, final String message) {
        if (m_disconnectedMessage == null) {
            m_disconnectedMessage = message;
        }
        setConnected(false, callback);
    }

    private void connect(final boolean callback) {
        m_disconnectedMessage = null;
        setConnected(true, callback);
    }

    private void setConnected(final boolean isConnected, final boolean callback) {
        m_isConnected = isConnected;
        getAsyncWFM().setDisconnected(!isConnected);
        if (callback) {
            m_connectedCallback.run();
        }
    }

    private boolean cancelTimers() {
        if (m_refreshTimerTask != null) {
            m_refreshTimerTask.cancel();
            m_refreshTimerTask = null;
            if (m_connectedTimerTask != null) {
                m_connectedTimerTask.cancel();
                m_connectedTimerTask = null;
                m_hasBeenRefreshed.set(true);
            }
            return true;
        }
        return false;
    }

    private AsyncWorkflowManagerUI getAsyncWFM() {
        if (m_editor.getWorkflowManagerUI() instanceof AsyncWorkflowManagerUI) {
            return (AsyncWorkflowManagerUI)m_editor.getWorkflowManagerUI();
        } else {
            throw new IllegalStateException(
                Messages.WorkflowEditorRefresher_16);
        }
    }

}
