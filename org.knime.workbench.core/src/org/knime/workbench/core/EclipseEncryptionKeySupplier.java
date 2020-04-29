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
package org.knime.workbench.core;

import javax.crypto.SecretKey;

import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.EncryptionKeySupplier;
import org.knime.core.util.KnimeEncryption;
import org.knime.workbench.core.preferences.HeadlessPreferencesConstants;

/**
 * Encryption key supplier used to en-/decrypt password (using a master key) in
 * an eclipse headless mode.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public class EclipseEncryptionKeySupplier implements EncryptionKeySupplier {

    /** Last master entered with the dialog/preference page. */
    private String m_lastMasterKey = null;

    /** If encryption with master key is enabled. */
    private boolean m_isEnabled = true;

    /** Master key has been set before, but was not saved. */
    private boolean m_wasSet = false;

    /**
     * Creates a new encryption key supplier.
     */
    public EclipseEncryptionKeySupplier() {
        init();
    }

    /**
     * Read preference store.
     *
     * @return current master key or null, if not set
     */
    private synchronized String init() {
        IPreferenceStore coreStore =
            KNIMECorePlugin.getDefault().getPreferenceStore();
        coreStore.setDefault(
                HeadlessPreferencesConstants.P_MASTER_KEY_ENABLED, false);
        // if an master key has been provided before, the pref page contains
        // an entry that this key was saved or not
        m_wasSet =
            coreStore.contains(HeadlessPreferencesConstants.P_MASTER_KEY_SAVED);

        if (m_isEnabled
                && (m_lastMasterKey == null || m_lastMasterKey.isEmpty())) {
            // the masterkey preference page was opened at least once.
            m_isEnabled = coreStore.getBoolean(
                    HeadlessPreferencesConstants.P_MASTER_KEY_ENABLED);
            if (m_isEnabled) {
                // master key is enabled and was (not) saved
                boolean wasSaved = coreStore.getBoolean(
                        HeadlessPreferencesConstants.P_MASTER_KEY_SAVED);
                if (wasSaved) {
                    try {
                        String mk = coreStore.getString(
                                     HeadlessPreferencesConstants.P_MASTER_KEY);
                        // preference store returns empty string if not set
                        if (mk.isEmpty()) {
                            return m_lastMasterKey;
                        }
                        SecretKey sk = KnimeEncryption.createSecretKey(
                                HeadlessPreferencesConstants.P_MASTER_KEY);
                        m_lastMasterKey = KnimeEncryption.decrypt(sk, mk);
                    } catch (Exception e) {
                        NodeLogger.getLogger(EclipseEncryptionKeySupplier.class)
                            .warn(Messages.EclipseEncryptionKeySupplier_0
                                    + e.getMessage(), e);
                    }
                }
            }
        }
        return m_lastMasterKey;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized String getEncryptionKey() {
        return init();
    }

    /**
     * @return the lastMasterKey
     */
    public String getLastMasterKey() {
        return m_lastMasterKey;
    }

    /**
     * @param lastMasterKey the lastMasterKey to set
     */
    public void setLastMasterKey(final String lastMasterKey) {
        m_lastMasterKey = lastMasterKey;
    }

    /**
     * @return the isEnabled
     */
    public boolean isEnabled() {
        return m_isEnabled;
    }

    /**
     * @param isEnabled the isEnabled to set
     */
    public void setEnabled(final boolean isEnabled) {
        m_isEnabled = isEnabled;
    }

    /**
     * @return the wasSet
     */
    public boolean isWasSet() {
        return m_wasSet;
    }

    /**
     * @param wasSet the wasSet to set
     */
    public void setWasSet(final boolean wasSet) {
        m_wasSet = wasSet;
    }


}
