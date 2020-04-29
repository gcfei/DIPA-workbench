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
 *   ${date} (${user}): created
 */
package org.knime.workbench.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeLogger.LEVEL;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.port.database.DatabaseDriverLoader;
import org.knime.core.util.KnimeEncryption;
import org.knime.workbench.core.preferences.HeadlessPreferencesConstants;
import org.knime.workbench.core.util.ThreadsafeImageRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;


/**
 * The core plugin, basically a holder for the framework's jar and some minor
 * workbench componentes that are needed everywhere (ErrorDialog,...).
 *
 * NOTE: Plugins need to depend upon this, as this plugin exports the underlying
 * framework API !!
 *
 * @author Florian Georg, University of Konstanz
 */
public class KNIMECorePlugin extends AbstractUIPlugin {
    /** Make sure that this *always* matches the ID in plugin.xml. */
    public static final String PLUGIN_ID = FrameworkUtil.getBundle(
            KNIMECorePlugin.class).getSymbolicName();

    // The shared instance.
    private static KNIMECorePlugin plugin;

    // Resource bundle.
    private ResourceBundle m_resourceBundle;

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            KNIMECorePlugin.class);

    /** Preference constant: log level for console appender. */
    public static final String P_LOGLEVEL_CONSOLE = "logging.loglevel.console"; //$NON-NLS-1$

    /**
     * Keeps list of <code>ConsoleViewAppender</code>. TODO FIXME remove
     * static if you want to have a console for each Workbench
     */
    private static final List<ConsoleViewAppender> APPENDERS =
            new ArrayList<ConsoleViewAppender>();

    /**
     * The constructor.
     */
    public KNIMECorePlugin() {
        super();
        plugin = this;
    }

    /**
     * This method is called upon plug-in activation.
     *
     * @param context The OSGI bundle context
     * @throws Exception If this plugin could not be started
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        if (!Boolean.getBoolean("java.awt.headless") && (Display.getCurrent() != null)) { //$NON-NLS-1$
            getImageRegistry();
        }

        try {
            // get the preference store
            // with the preferences for nr threads and tempDir
            IPreferenceStore pStore =
                KNIMECorePlugin.getDefault().getPreferenceStore();
            initMaxThreadCountProperty();
            initTmpDirProperty();
            // set log file level to stored
            String logLevelFile =
                pStore.getString(HeadlessPreferencesConstants
                        .P_LOGLEVEL_LOG_FILE);
            NodeLogger.setAppenderLevelRange(NodeLogger.LOGFILE_APPENDER, LEVEL.valueOf(logLevelFile), LEVEL.FATAL);
            final boolean enableWorkflowRelativeLogging =
                    pStore.getBoolean(HeadlessPreferencesConstants.P_LOG_FILE_LOCATION);
            NodeLogger.logInWorkflowDir(enableWorkflowRelativeLogging);
            final boolean enableGlobalInWfLogging =
                    pStore.getBoolean(HeadlessPreferencesConstants.P_LOG_GLOBAL_IN_WF_DIR);
            NodeLogger.logGlobalMsgsInWfDir(enableGlobalInWfLogging);
            pStore.addPropertyChangeListener(new IPropertyChangeListener() {

                @Override
                public void propertyChange(final PropertyChangeEvent event) {
                    final String propertyName = event.getProperty();
                    if (HeadlessPreferencesConstants.P_MAXIMUM_THREADS.equals(propertyName)) {
                        if (!(event.getNewValue() instanceof Integer)) {
                            // when preferences are imported and this value is
                            // not set, they send an empty string
                            return;
                        }
                        int count;
                        try {
                            count = (Integer)event.getNewValue();
                            KNIMEConstants.GLOBAL_THREAD_POOL.setMaxThreads(count);
                        } catch (Exception e) {
                            LOGGER.error(Messages.KNIMECorePlugin_2 + Messages.KNIMECorePlugin_3, e);
                        }
                    } else if (HeadlessPreferencesConstants.P_TEMP_DIR.equals(propertyName)) {
                        if (!(event.getNewValue() instanceof String)) {
                            // when preferences are imported and this value is
                            // not set, they send an empty string
                            return;
                        }
                        String dirName = (String)event.getNewValue();
                        if (dirName.isEmpty()) {
                            return;
                        }
                        File f = new File(dirName);
                        LOGGER.debug(Messages.KNIMECorePlugin_4 + f.getAbsolutePath());
                        try {
                            KNIMEConstants.setKNIMETempDir(f);
                        } catch (Exception e) {
                            LOGGER.error(Messages.KNIMECorePlugin_5 + e.getMessage(), e);
                        }
                    } else if (HeadlessPreferencesConstants.P_LOGLEVEL_LOG_FILE.equals(propertyName)) {
                        if (!(event.getNewValue() instanceof String)) {
                            // when preferences are imported and this value is
                            // not set, they send an empty string
                            return;
                        }
                        String newName = (String)event.getNewValue();
                        if (newName.isEmpty()) {
                            return;
                        }
                        LEVEL level = LEVEL.WARN;
                        try {
                            level = LEVEL.valueOf(newName);
                        } catch (IllegalArgumentException iae) {
                            LOGGER.error(Messages.KNIMECorePlugin_6 + newName + Messages.KNIMECorePlugin_7);
                        }
                        NodeLogger.setAppenderLevelRange(NodeLogger.LOGFILE_APPENDER, level, LEVEL.FATAL);
                    } else if (HeadlessPreferencesConstants.P_LOG_FILE_LOCATION.equals(propertyName)) {
                        if (!(event.getNewValue() instanceof Boolean)) {
                            // when preferences are imported and this value is not set, they send an empty string
                            return;
                        }
                        Boolean enable = (Boolean)event.getNewValue();
                        NodeLogger.logInWorkflowDir(enable);
                    } else if (HeadlessPreferencesConstants.P_LOG_GLOBAL_IN_WF_DIR.equals(propertyName)) {
                        if (!(event.getNewValue() instanceof Boolean)) {
                            // when preferences are imported and this value is not set, they send an empty string
                            return;
                        }
                        Boolean enable = (Boolean)event.getNewValue();
                        NodeLogger.logGlobalMsgsInWfDir(enable);
                    } else if (P_LOGLEVEL_CONSOLE.equals(propertyName)) {
                        if (!(event.getNewValue() instanceof String)) {
                            // when preferences are imported and this value is
                            // not set, they send an empty string
                            return;
                        }
                        String newName = (String)event.getNewValue();
                        if (newName.isEmpty()) {
                            return;
                        }
                        setLogLevel(newName);
                    } else if (HeadlessPreferencesConstants.P_DATABASE_DRIVERS.equals(propertyName)) {
                        String dbDrivers = (String)event.getNewValue();
                        initDatabaseDriver(dbDrivers);
                    } else if (HeadlessPreferencesConstants.P_DATABASE_TIMEOUT.equals(propertyName)) {
                        DatabaseConnectionSettings.setDatabaseTimeout(Integer.parseInt(event.getNewValue().toString()));
                    } else if (WorkflowMigrationSettings.P_WORKFLOW_MIGRATION_NOTIFICATION_ENABLED.contentEquals(propertyName)) {
                        final Object newValue = event.getNewValue();
                        if (newValue instanceof Boolean) {
                            WorkflowMigrationSettings.setNotificationEnabled((Boolean)newValue);
                        }
                    }
                }
            });
            // end property listener

            String logLevelConsole =
                pStore.getString(P_LOGLEVEL_CONSOLE);
            if (!Boolean.getBoolean("java.awt.headless") && PlatformUI.isWorkbenchRunning()) { //$NON-NLS-1$
                //async exec should fix AP-13234 (deadlock):
                Display.getDefault().asyncExec(() -> {
                    try {
                        ConsoleViewAppender.FORCED_APPENDER.write(
                                KNIMEConstants.WELCOME_MESSAGE);
                        ConsoleViewAppender.INFO_APPENDER.write(
                        Messages.KNIMECorePlugin_9
                        + KNIMEConstants.getKNIMEHomeDir() + File.separator
                        + NodeLogger.LOG_FILE + "\n"); //$NON-NLS-1$
                    } catch (IOException ioe) {
                        LOGGER.error(Messages.KNIMECorePlugin_11, ioe);
                    }
                    setLogLevel(logLevelConsole);
                });
            }
            // encryption key supplier registered with the eclipse framework
            // and serves as a master key provider
            KnimeEncryption.setEncryptionKeySupplier(
                    new EclipseEncryptionKeySupplier());

            // load database driver files from core preference page
            String dbDrivers = pStore.getString(
                    HeadlessPreferencesConstants.P_DATABASE_DRIVERS);
            initDatabaseDriver(dbDrivers);

            DatabaseConnectionSettings.setDatabaseTimeout(pStore
                .getInt(HeadlessPreferencesConstants.P_DATABASE_TIMEOUT));

            WorkflowMigrationSettings.setNotificationEnabled(pStore
                .getBoolean(WorkflowMigrationSettings.P_WORKFLOW_MIGRATION_NOTIFICATION_ENABLED));
        } catch (Throwable e) {
            LOGGER.error(
                Messages.KNIMECorePlugin_12 + e.getMessage(),
                e);
        }
    }

    private void initDatabaseDriver(final String dbDrivers) {
        if (dbDrivers != null && !dbDrivers.trim().isEmpty()) {
            for (String d : dbDrivers.split(";")) { //$NON-NLS-1$
                try {
                    DatabaseDriverLoader.loadDriver(new File(d));
                } catch (IOException ioe) {
                    LOGGER.warn(Messages.KNIMECorePlugin_14 + d + "\"" //$NON-NLS-2$
                        + (ioe.getMessage() != null
                            ? Messages.KNIMECorePlugin_16 + ioe.getMessage() : Messages.KNIMECorePlugin_17));
                }
            }
        }
    }

    private void initMaxThreadCountProperty() {
        IPreferenceStore pStore =
            KNIMECorePlugin.getDefault().getPreferenceStore();
        int maxThreads = pStore.getInt(
                HeadlessPreferencesConstants.P_MAXIMUM_THREADS);
        String maxTString =
            System.getProperty(KNIMEConstants.PROPERTY_MAX_THREAD_COUNT);
        if (maxTString == null) {
            if (maxThreads <= 0) {
                LOGGER.warn(Messages.KNIMECorePlugin_18 + maxThreads
                        + Messages.KNIMECorePlugin_19);
            } else {
                KNIMEConstants.GLOBAL_THREAD_POOL.setMaxThreads(maxThreads);
                LOGGER.debug(Messages.KNIMECorePlugin_20
                        + maxThreads);
            }
        } else {
            LOGGER.debug(Messages.KNIMECorePlugin_21
                    + maxThreads + Messages.KNIMECorePlugin_22
                    + "\"org.knime.core.maxThreads\" (" + maxTString + ")"); //$NON-NLS-2$
        }
    }

    private void initTmpDirProperty() {
        IPreferenceStore pStore =
            KNIMECorePlugin.getDefault().getPreferenceStore();
        String tmpDirPref = pStore.getString(
                HeadlessPreferencesConstants.P_TEMP_DIR);
        String tmpDirSystem = System.getProperty(
                KNIMEConstants.PROPERTY_TEMP_DIR);
        File tmpDir = null;
        if (tmpDirSystem == null) {
            if (tmpDirPref != null) {
                tmpDir = new File(tmpDirPref);
                if (!(tmpDir.isDirectory() && tmpDir.canWrite())) {
                    LOGGER.warn(Messages.KNIMECorePlugin_25 + tmpDirPref + Messages.KNIMECorePlugin_26);
                    tmpDir = null;
                }
            }
        } else {
            tmpDir = new File(tmpDirSystem);
            if (!(tmpDir.isDirectory() && tmpDir.canWrite())) {
                LOGGER.warn(Messages.KNIMECorePlugin_27 + tmpDirSystem + Messages.KNIMECorePlugin_28);
                // try to set path from preference page as fallback
                tmpDir = new File(tmpDirPref);
                if (!(tmpDir.isDirectory() && tmpDir.canWrite())) {
                    LOGGER.warn(Messages.KNIMECorePlugin_29 + tmpDirPref + Messages.KNIMECorePlugin_30);
                    tmpDir = null;
                }
            }
        }
        if (tmpDir != null) {
            LOGGER.debug(Messages.KNIMECorePlugin_31
                    + tmpDir.getAbsolutePath() + "\""); //$NON-NLS-1$
            KNIMEConstants.setKNIMETempDir(tmpDir);
        }
    }

    /**
     * This method is called when the plug-in is stopped.
     *
     * @param context The OSGI bundle context
     * @throws Exception If this plugin could not be stopped
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        // remove appender listener from "our" NodeLogger
        for (int i = 0; i < APPENDERS.size(); i++) {
            removeAppender(APPENDERS.get(i));
        }
        super.stop(context);
        plugin = null;
        m_resourceBundle = null;
    }


    /**
     * Register the appenders according to logLevel, i.e.
     * PreferenceConstants.P_LOGLEVEL_DEBUG,
     * PreferenceConstants.P_LOGLEVEL_INFO, etc.
     *
     * @param logLevel The new log level.
     */
    private static void setLogLevel(final String logLevel) {
        // check if can create a console view
        // only possible if we are not "headless"
        if (Boolean.valueOf(System.getProperty("java.awt.headless", "false"))) { //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }
        boolean changed = false;
        if (logLevel.equals(LEVEL.DEBUG.name())) {
            changed |= addAppender(ConsoleViewAppender.DEBUG_APPENDER);
            changed |= addAppender(ConsoleViewAppender.INFO_APPENDER);
            changed |= addAppender(ConsoleViewAppender.WARN_APPENDER);
            changed |= addAppender(ConsoleViewAppender.ERROR_APPENDER);
            changed |= addAppender(ConsoleViewAppender.FATAL_ERROR_APPENDER);
        } else if (logLevel.equals(LEVEL.INFO.name())) {
            changed |= removeAppender(ConsoleViewAppender.DEBUG_APPENDER);
            changed |= addAppender(ConsoleViewAppender.INFO_APPENDER);
            changed |= addAppender(ConsoleViewAppender.WARN_APPENDER);
            changed |= addAppender(ConsoleViewAppender.ERROR_APPENDER);
            changed |= addAppender(ConsoleViewAppender.FATAL_ERROR_APPENDER);
        } else if (logLevel.equals(LEVEL.WARN.name())) {
            changed |= removeAppender(ConsoleViewAppender.DEBUG_APPENDER);
            changed |= removeAppender(ConsoleViewAppender.INFO_APPENDER);
            changed |= addAppender(ConsoleViewAppender.WARN_APPENDER);
            changed |= addAppender(ConsoleViewAppender.ERROR_APPENDER);
            changed |= addAppender(ConsoleViewAppender.FATAL_ERROR_APPENDER);
        } else if (logLevel.equals(LEVEL.ERROR.name())) {
            changed |= removeAppender(ConsoleViewAppender.DEBUG_APPENDER);
            changed |= removeAppender(ConsoleViewAppender.INFO_APPENDER);
            changed |= removeAppender(ConsoleViewAppender.WARN_APPENDER);
            changed |= addAppender(ConsoleViewAppender.ERROR_APPENDER);
            changed |= addAppender(ConsoleViewAppender.FATAL_ERROR_APPENDER);
        } else {
            LOGGER.warn(Messages.KNIMECorePlugin_35 + logLevel + Messages.KNIMECorePlugin_36
                    + LEVEL.WARN.name());
            setLogLevel(LEVEL.WARN.name());
        }
        if (changed) {
            LOGGER.info(Messages.KNIMECorePlugin_37 + logLevel);
        }
    }


    /**
     * Add the given Appender to the NodeLogger.
     *
     * @param app Appender to add.
     * @return If the given appender was not previously registered.
     */
    static boolean addAppender(final ConsoleViewAppender app) {
        if (!APPENDERS.contains(app)) {
            NodeLogger.addKNIMEConsoleWriter(app, app.getLevel(), app.getLevel());
            APPENDERS.add(app);
            return true;
        }
        return false;
    }

    /**
     * Removes the given Appender from the NodeLogger.
     *
     * @param app Appender to remove.
     * @return If the given appended was previously registered.
     */
    static boolean removeAppender(final ConsoleViewAppender app) {
        if (APPENDERS.contains(app)) {
            NodeLogger.removeWriter(app);
            APPENDERS.remove(app);
            return true;
        }
        return false;
    }


    /**
     * Returns the shared instance.
     *
     * @return Singleton instance of the Core Plugin
     */
    public static KNIMECorePlugin getDefault() {
        return plugin;
    }

    /**
     * Returns the string from the plugin's resource bundle, or 'key' if not
     * found.
     *
     * @param key The resource key
     * @return The resource value, or the key if not found in the resource
     *         bundle
     */
    public static String getResourceString(final String key) {
        ResourceBundle bundle = KNIMECorePlugin.getDefault()
                .getResourceBundle();
        try {
            return (bundle != null) ? bundle.getString(key) : key;
        } catch (MissingResourceException e) {
            return key;
        }
    }

    /**
     * Returns the plugin's resource bundle.
     *
     * @return The resource bundle, or <code>null</code>
     */
    public ResourceBundle getResourceBundle() {
        try {
            if (m_resourceBundle == null) {
                m_resourceBundle = ResourceBundle.getBundle(plugin
                        .getClass().getName());
            }
        } catch (MissingResourceException x) {
            m_resourceBundle = null;
            WorkbenchErrorLogger
                    .warning(Messages.KNIMECorePlugin_38
                            + plugin.getClass().getName());
        }
        return m_resourceBundle;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ImageRegistry createImageRegistry() {
        //If we are in the UI Thread use that
        if (Display.getCurrent() != null) {
            return new ThreadsafeImageRegistry(Display.getCurrent());
        } else {
            Display display;
            if (PlatformUI.isWorkbenchRunning()) {
                display = PlatformUI.getWorkbench().getDisplay();
            } else {
                display = Display.getDefault();
            }
            final AtomicReference<ImageRegistry> ref = new AtomicReference<>();
            display.syncExec(new Runnable() {
                @Override
                public void run() {
                    ref.set(new ThreadsafeImageRegistry(Display.getCurrent()));
                }
            });
            return ref.get();
        }
    }
}
