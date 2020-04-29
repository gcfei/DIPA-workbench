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
 */
package org.knime.workbench.ui.database;

import static org.knime.workbench.core.WorkflowMigrationSettings.P_WORKFLOW_MIGRATION_NOTIFICATION_ENABLED;
import static org.knime.workbench.core.preferences.HeadlessPreferencesConstants.P_DATABASE_DRIVERS;
import static org.knime.workbench.core.preferences.HeadlessPreferencesConstants.P_DATABASE_TIMEOUT;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.workbench.core.KNIMECorePlugin;

/**
 * Preference page used to load additional database drivers.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public class DatabasePreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    /**
     * Constructs a {@link DatabasePreferencePage}.
     */
    public DatabasePreferencePage() {
        super("Database preferences (legacy)", null, GRID); //$NON-NLS-1$
        setDescription(Messages.DatabasePreferencePage_1
            + Messages.DatabasePreferencePage_2
            + Messages.DatabasePreferencePage_3);
    }

    @Override
    protected void createFieldEditors() {
        final Composite fieldEditorParent = getFieldEditorParent();
        addField(new DatabaseDriverListEditor(P_DATABASE_DRIVERS,
            Messages.DatabasePreferencePage_4, fieldEditorParent, this));
        addField(new IntegerFieldEditor(P_DATABASE_TIMEOUT, Messages.DatabasePreferencePage_5,
            fieldEditorParent, 5));
        if (DatabaseConnectionSettings.getSystemPropertyDatabaseTimeout() >= 0) {
            setMessage(Messages.DatabasePreferencePage_6, IMessageProvider.WARNING);
        }
        addField(new BooleanFieldEditor(P_WORKFLOW_MIGRATION_NOTIFICATION_ENABLED,
            Messages.DatabasePreferencePage_7, fieldEditorParent));
    }

    @Override
    public void init(final IWorkbench workbench) {
        IPreferenceStore corePrefStore = KNIMECorePlugin.getDefault().getPreferenceStore();
        setPreferenceStore(corePrefStore);
    }

}
