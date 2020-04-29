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
package org.knime.workbench.ui.preferences;

import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.knime.core.node.NodeLogger.LEVEL;
import org.knime.workbench.core.KNIMECorePlugin;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 * This class represents a preference page that is contributed to the
 * Preferences dialog. By subclassing <samp>FieldEditorPreferencePage</samp>,
 * we can use the field support built into JFace that allows us to create a page
 * that is small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the
 * preference store that belongs to the main plug-in class. That way,
 * preferences can be accessed directly via the preference store.
 *
 * @author Florian Georg, University of Konstanz
 */
public class MainPreferencePage extends FieldEditorPreferencePage
        implements IWorkbenchPreferencePage {

    private RadioGroupFieldEditor m_consoleLogEditor;

    private IntegerFieldEditor m_autoSaveIntervalEditor;
    private BooleanFieldEditor m_autoSaveWithDataEditor;

    /**
     * Constructor.
     */
    public MainPreferencePage() {
        super(GRID);
    }

    /**
     * Creates the field editors. Field editors are abstractions of the common
     * GUI blocks needed to manipulate various types of preferences. Each field
     * editor knows how to save and restore itself.
     */
    @Override
    public void createFieldEditors() {
        final Composite parent = getFieldEditorParent();

        // Specify the minimum log level for the console
        m_consoleLogEditor = new RadioGroupFieldEditor(
                KNIMECorePlugin.P_LOGLEVEL_CONSOLE,
                Messages.MainPreferencePage_0, 4,
                new String[][] {
                        {Messages.MainPreferencePage_1, LEVEL.DEBUG.name()},
                        {Messages.MainPreferencePage_2,  LEVEL.INFO.name()},
                        {Messages.MainPreferencePage_3,  LEVEL.WARN.name()},
                        {Messages.MainPreferencePage_4, LEVEL.ERROR.name()}
                }, parent);
        addField(m_consoleLogEditor);

        addField(new HorizontalLineField(parent));
        addField(new BooleanFieldEditor(PreferenceConstants.P_CONFIRM_RESET,
                Messages.MainPreferencePage_5, parent));
        addField(new BooleanFieldEditor(PreferenceConstants.P_CONFIRM_DELETE,
                Messages.MainPreferencePage_6, parent));
        addField(new BooleanFieldEditor(PreferenceConstants.P_CONFIRM_REPLACE,
                Messages.MainPreferencePage_7, parent));
        addField(new BooleanFieldEditor(PreferenceConstants.P_CONFIRM_RECONNECT,
                Messages.MainPreferencePage_8, parent));
        addField(new BooleanFieldEditor(
                PreferenceConstants.P_CONFIRM_EXEC_NODES_NOT_SAVED,
                Messages.MainPreferencePage_9, parent));
        addField(new BooleanFieldEditor(
            PreferenceConstants.P_CONFIRM_LOAD_NIGHTLY_BUILD_WORKFLOW,
            Messages.MainPreferencePage_10, parent));

        ComboFieldEditor dataAwareExecutePromptEditor = new ComboFieldEditor(
            PreferenceConstants.P_EXEC_NODES_DATA_AWARE_DIALOGS,
            Messages.MainPreferencePage_11,
            new String[][] {
                    {"Always", MessageDialogWithToggle.ALWAYS}, //$NON-NLS-1$
                    {"Never", MessageDialogWithToggle.NEVER}, //$NON-NLS-1$
                    {"Prompt", MessageDialogWithToggle.PROMPT}, //$NON-NLS-1$
            }, getFieldEditorParent());
        addField(dataAwareExecutePromptEditor);

        addField(new HorizontalLineField(parent));
        final BooleanFieldEditor enableAutoSaveBooleanField = new BooleanFieldEditor(
            PreferenceConstants.P_AUTO_SAVE_ENABLE, Messages.MainPreferencePage_15, parent) {
            @Override
            protected void valueChanged(final boolean old, final boolean neu) {
                m_autoSaveIntervalEditor.setEnabled(neu, parent);
                m_autoSaveWithDataEditor.setEnabled(neu, parent);
            }
        };
        m_autoSaveIntervalEditor = new IntegerFieldEditor(PreferenceConstants.P_AUTO_SAVE_INTERVAL,
            Messages.MainPreferencePage_16, parent);
        m_autoSaveWithDataEditor = new BooleanFieldEditor(PreferenceConstants.P_AUTO_SAVE_DATA,
            Messages.MainPreferencePage_17, parent);
        addField(enableAutoSaveBooleanField);
        addField(m_autoSaveIntervalEditor);
        addField(m_autoSaveWithDataEditor);

        addField(new HorizontalLineField(parent));
        addField(new BooleanFieldEditor(PreferenceConstants.P_WRAP_TABLE_HEADER,
                                        Messages.MainPreferencePage_18, parent));
        addField(new IntegerFieldEditor(PreferenceConstants.P_ANNOTATION_BORDER_SIZE,
            Messages.MainPreferencePage_19, parent));
        addField(new HorizontalLineField(parent));

        final ComboFieldEditor updateMetaNodeLinkOnLoadEditor = new ComboFieldEditor(
                PreferenceConstants.P_META_NODE_LINK_UPDATE_ON_LOAD,
                Messages.MainPreferencePage_20,
                new String[][] {
                        {"Always", MessageDialogWithToggle.ALWAYS}, //$NON-NLS-1$
                        {"Never", MessageDialogWithToggle.NEVER}, //$NON-NLS-1$
                        {"Prompt", MessageDialogWithToggle.PROMPT}, //$NON-NLS-1$
                }, getFieldEditorParent());
        addField(updateMetaNodeLinkOnLoadEditor);

        addField(new HorizontalLineField(parent));
        addField(new BooleanFieldEditor(PreferenceConstants.P_OMIT_MISSING_BROWSER_WARNING,
            Messages.MainPreferencePage_24, parent));

        addField(new HorizontalLineField(parent));
        addField(new LabelField(parent, Messages.MainPreferencePage_25));
        final IntegerFieldEditor freqHistorySizeEditor = new IntegerFieldEditor(
                PreferenceConstants.P_FAV_FREQUENCY_HISTORY_SIZE,
                Messages.MainPreferencePage_26, parent, 3);
        freqHistorySizeEditor.setValidRange(1, 50);
        freqHistorySizeEditor.setTextLimit(3);
        freqHistorySizeEditor.load();
        final IntegerFieldEditor usedHistorySizeEditor = new IntegerFieldEditor(
                PreferenceConstants.P_FAV_LAST_USED_SIZE,
                Messages.MainPreferencePage_27, parent, 3);
        usedHistorySizeEditor.setValidRange(1, 50);
        usedHistorySizeEditor.setTextLimit(3);
        usedHistorySizeEditor.load();
        addField(usedHistorySizeEditor);
        addField(freqHistorySizeEditor);
    }

    /** {@inheritDoc} */
    @Override
    public void init(final IWorkbench workbench) {
        // we use the pref store of the UI plugin
        setPreferenceStore(KNIMEUIPlugin.getDefault().getPreferenceStore());
    }

    /** {@inheritDoc} */
    @Override
    protected void initialize() {
        super.initialize();
        m_consoleLogEditor.setPreferenceStore(KNIMECorePlugin.getDefault().getPreferenceStore());
        m_consoleLogEditor.load();
    }

    /** {@inheritDoc} */
    @Override
    protected void performDefaults() {
        super.performDefaults();
        m_consoleLogEditor.setPreferenceStore(KNIMECorePlugin.getDefault().getPreferenceStore());
        m_consoleLogEditor.loadDefault();
    }
}
