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
 *   03.10.2010 (meinl): created
 */
package org.knime.workbench.ui.p2.actions;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.p2.ui.LoadMetadataRepositoryJob;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.action.Action;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.PathUtils;

/**
 * Abstract action for p2 related tasks (installing new extensions or updating, e.g.).
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public abstract class AbstractP2Action extends Action {
    /**
     * Creates a new action.
     *
     * @param text the actions name as shown in the menu
     * @param description a description for the action
     * @param id a unique id
     */
    protected AbstractP2Action(final String text, final String description, final String id) {
        super(text);
        setDescription(description);
        setId(id);
    }

    /**
     * Starts the repository load job. After the repositories have been loaded,
     * {@link #openWizard(LoadMetadataRepositoryJob, ProvisioningUI)} is called.
     */
    protected final void startLoadJob() {
        final ProvisioningUI provUI = ProvisioningUI.getDefaultUI();
        Job.getJobManager().cancel(LoadMetadataRepositoryJob.LOAD_FAMILY);
        final LoadMetadataRepositoryJob loadJob = new LoadMetadataRepositoryJob(provUI);
        loadJob.setProperty(LoadMetadataRepositoryJob.ACCUMULATE_LOAD_ERRORS, Boolean.toString(true));

        loadJob.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(final IJobChangeEvent event) {
                if (PlatformUI.isWorkbenchRunning() && event.getResult().isOK()) {
                    openWizard(loadJob, provUI);
                }
            }
        });
        loadJob.setUser(true);
        loadJob.schedule();
    }

    /**
     * Checks whether the current instance is run from an SDK and if the configuration area is writable.
     *
     * @return <code>true</code> if the action should continue, <code>false</code> if it should be aborted
     */
    protected final boolean checkSDKAndReadOnly() {
        final ProvisioningUI provUI = ProvisioningUI.getDefaultUI();
        if (provUI.getRepositoryTracker() == null) {
            MessageBox mbox = new MessageBox(ProvUI.getDefaultParentShell(), SWT.ICON_WARNING | SWT.OK);
            mbox.setText(Messages.AbstractP2Action_0);
            mbox.setMessage(Messages.AbstractP2Action_1
                + Messages.AbstractP2Action_2);
            mbox.open();
            return false;
        }

        String installLocation = Platform.getInstallLocation().getURL().toString();
        String configurationLocation = Platform.getConfigurationLocation().getURL().toString();

        if (!configurationLocation.contains(installLocation)) {
            MessageBox mbox = new MessageBox(ProvUI.getDefaultParentShell(), SWT.ICON_WARNING | SWT.YES | SWT.NO);
            mbox.setText(Messages.AbstractP2Action_3);
            mbox.setMessage(Messages.AbstractP2Action_4
                + Messages.AbstractP2Action_5
                + Messages.AbstractP2Action_6
                + Messages.AbstractP2Action_7);
            return (mbox.open() == SWT.YES);
        }
        return true;
    }

    private final AtomicBoolean m_shutdownHookAdded = new AtomicBoolean();

    /**
     * Clears the OSGi configuration area in a shutdown hook. This forces re-initialization of all bundles information
     * as if Eclipse would be started with "-clean".
     */
    protected final void clearOsgiAreaBeforeRestart() {
        if (m_shutdownHookAdded.getAndSet(true)) {
            return;
        }

        try {
            @SuppressWarnings("restriction")
            URL configUrl = Platform.getConfigurationLocation().getDataArea(EquinoxContainer.NAME);
            Path configPath = Paths.get(new URI(configUrl.toString().replace(" ", "%20"))); //$NON-NLS-1$ //$NON-NLS-2$

            // make sure PathUtils is initialized, because it registers a shutdown hook itself and this is not possible
            // while the JVM shuts down
            PathUtils.RWX_ALL_PERMISSIONS.isEmpty();

            Runtime.getRuntime().addShutdownHook(new Thread(Messages.AbstractP2Action_10) {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public void run() {
                    try {
                        if (Files.isWritable(configPath)) {
                            NodeLogger.getLogger(AbstractP2Action.this.getClass())
                                .debug(Messages.AbstractP2Action_11 + configPath);
                            PathUtils.deleteDirectoryIfExists(configPath);
                        }
                    } catch (IOException ex) {
                        NodeLogger.getLogger(AbstractP2Action.this.getClass())
                            .error(Messages.AbstractP2Action_12 + ex.getMessage());
                    }
                }
            });
        } catch (IOException | URISyntaxException ex) {
            NodeLogger.getLogger(AbstractP2Action.this.getClass())
                .error(Messages.AbstractP2Action_13 + ex.getMessage());
        }
    }

    /**
     * This is called when a wizard (install, update, ...) should be opened. Subclasses must override this method and
     * open the desired wizard.
     *
     * @param job the repository job
     * @param provUI the provisioning UI instance
     */
    protected abstract void openWizard(final LoadMetadataRepositoryJob job, ProvisioningUI provUI);
}
