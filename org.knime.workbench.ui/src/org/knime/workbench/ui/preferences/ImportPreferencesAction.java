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
 *   26.08.2009 (ohl): created
 */
package org.knime.workbench.ui.preferences;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IExportedPreferences;
import org.eclipse.core.runtime.preferences.IPreferenceFilter;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 * Action to import current preferences to a file.
 *
 * @author Peter Ohl, KNIME.com, Switzerland
 */
public class ImportPreferencesAction extends Action {

    private static final ImageDescriptor ICON =
            KNIMEUIPlugin.imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID,
                    "icons/prefs_import.png"); //$NON-NLS-1$

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(ImportPreferencesAction.class);

    /**
     * The id for this action.
     */
    public static final String ID = "KNIMEPreferencesImport"; //$NON-NLS-1$

    /**
     * The workbench window; or <code>null</code> if this action has been
     * <code>dispose</code>d.
     */

    /**
     * Create a new instance of this class.
     *
     * @param window the window
     */
    public ImportPreferencesAction(final IWorkbenchWindow window) {
        super(Messages.ImportPreferencesAction_2);
        if (window == null) {
            throw new IllegalArgumentException();
        }
        setToolTipText(Messages.ImportPreferencesAction_3);
        setId(ID); //$NON-NLS-1$
    }

    /**
     * Create a new instance of this class.
     *
     * @param workbench the workbench
     * @deprecated use the constructor
     *             <code>ExportPreferencesAction(IWorkbenchWindow)</code>
     */
    @Deprecated
    public ImportPreferencesAction(final IWorkbench workbench) {
        this(workbench.getActiveWorkbenchWindow());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ICON;
    }

    /**
     * Invoke the Import wizards selection Wizard.
     */
    @Override
    public void run() {
        IWorkbenchWindow workbenchWindow =
                PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (workbenchWindow == null) {
            // action has been disposed
            return;
        }

        FileDialog fileDialog =
                new FileDialog(workbenchWindow.getShell(), SWT.OPEN);
        fileDialog.setFilterExtensions(new String[]{"*.epf", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
        fileDialog.setText(Messages.ImportPreferencesAction_6);
        String filePath = fileDialog.open();
        if (filePath == null || filePath.trim().length() == 0) {
            return;
        }

        File inFile = new File(filePath);
        if (!inFile.isFile() || !inFile.canRead()) {
            MessageDialog.openError(workbenchWindow.getShell(),
                    Messages.ImportPreferencesAction_7,
                    Messages.ImportPreferencesAction_8);
            return;
        }

        try (InputStream in = new BufferedInputStream(new FileInputStream(inFile))) {
            IPreferencesService prefService = Platform.getPreferencesService();
            LOGGER.info(Messages.ImportPreferencesAction_9 + inFile.getAbsolutePath() + Messages.ImportPreferencesAction_10);
            IExportedPreferences prefs = prefService.readPreferences(in);
            IPreferenceFilter filter = new IPreferenceFilter() {
                @Override
                public String[] getScopes() {
                    return new String[] { InstanceScope.SCOPE,
                            ConfigurationScope.SCOPE,
                            "profile" }; //$NON-NLS-1$
                }

                @Override
                @SuppressWarnings("rawtypes")
                public Map getMapping(final String scope) {
                    return null; // this filter is applicable for all nodes
                }
            };
            /* Calling this method with filters and not the applyPreferences
             * without filters is very important! The other method does not
             * merge the preferences but deletes all default values. */
            prefService.applyPreferences(prefs,
                    new IPreferenceFilter[] {filter});
            MessageDialog.openInformation(workbenchWindow.getShell(), Messages.ImportPreferencesAction_12,
                Messages.ImportPreferencesAction_13);
            LOGGER.info(Messages.ImportPreferencesAction_14);
        } catch (Throwable t) {
            String msg = Messages.ImportPreferencesAction_15;
            if (t.getMessage() != null && !t.getMessage().isEmpty()) {
                msg = t.getMessage();
            }
            MessageDialog.openError(workbenchWindow.getShell(),
                    Messages.ImportPreferencesAction_16, msg);
            return;
        }
    }
}
