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
package org.knime.workbench.explorer.view.preferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.INodeChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.NodeChangeEvent;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.ui.preferences.PreferenceConstants;

public class ExplorerPrefsSyncer implements IPropertyChangeListener, IPreferenceChangeListener, INodeChangeListener {

    private List<MountSettings> m_previousValues;

    /**
     * Creates a new preference syncer.
     */
    public ExplorerPrefsSyncer() {
        m_previousValues = getUserOrDefaultValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void propertyChange(final PropertyChangeEvent event) {
        if (PreferenceConstants.P_EXPLORER_MOUNT_POINT_XML.equals(
            event.getProperty())) {
            List<MountSettings> newValue = getUserOrDefaultValue();
            updateSettings(m_previousValues, newValue);
            m_previousValues = newValue;
        }
    }

    /**
     * {@inheritDoc}
     * @since 6.3
     */
    @Override
    public void preferenceChange(final PreferenceChangeEvent event) {
        if (InstanceScope.INSTANCE.getNode(MountSettings.getMountpointPreferenceLocation()).equals(event.getNode().parent())) {
            Object eventValue = event.getNewValue();
            if (!(eventValue == null)) {
                // if the eventValue is null, then the preference was removed
                List<MountSettings> newValue = getUserOrDefaultValue();
                updateSettings(m_previousValues, newValue);
                m_previousValues = newValue;
            }
        }
    }

    private synchronized void updateSettings(final List<MountSettings> oldValues, final List<MountSettings> newValues) {
        if (Objects.equals(oldValues, newValues)) {
            return;
        }

        Set<MountSettings> oldSettings = new LinkedHashSet<MountSettings>(oldValues);
        oldSettings.removeAll(new LinkedHashSet<MountSettings>(newValues));

        Set<MountSettings> newSettings = new LinkedHashSet<MountSettings>(newValues);
        // leave unchanged values untouched
        newSettings.removeAll(oldSettings);

        // remove deleted mount points
        for (MountSettings ms : oldSettings) {
            boolean successful = ExplorerMountTable.unmount(
                    ms.getMountID());
            if (!successful) {
                // most likely mount point was not present to begin with
                NodeLogger.getLogger(this.getClass()).debug(Messages.getString("ExplorerPrefsSyncer.0") + ms.getDisplayName() //$NON-NLS-1$
                        + Messages.getString("ExplorerPrefsSyncer.1")); //$NON-NLS-1$
            }
        }

        // add all new mount points
        for (MountSettings ms : newSettings) {
            if (!ms.isActive()) {
                continue;
            }
            try {
                ExplorerMountTable.mount(ms.getMountID(),
                        ms.getFactoryID(), ms.getContent());
            } catch (IOException e) {
                NodeLogger.getLogger(this.getClass()).error(Messages.getString("ExplorerPrefsSyncer.2") + ms.getDisplayName() //$NON-NLS-1$
                        + Messages.getString("ExplorerPrefsSyncer.3"), e); //$NON-NLS-1$
            }
        }

        // sync the ordering of the mount points
        List<String> newMountIds = new ArrayList<String>();
        for (MountSettings ms : newValues) {
            newMountIds.add(ms.getMountID());
        }
        ExplorerMountTable.setMountOrder(newMountIds);
    }

    private static List<MountSettings> getUserOrDefaultValue() {
        return MountSettings.loadSortedMountSettingsFromPreferenceNode();
    }


    /**
     * {@inheritDoc}
     * @since 8.2
     */
    @Override
    public void added(final NodeChangeEvent event) {
        // AP-8989 switching to IEclipsePreferences
        if (InstanceScope.INSTANCE.getNode(MountSettings.getMountpointPreferenceLocation()).equals(event.getParent())) {
            IEclipsePreferences childNode = InstanceScope.INSTANCE
                .getNode(MountSettings.getMountpointPreferenceLocation() + "/" + event.getChild().name()); //$NON-NLS-1$
            childNode.addPreferenceChangeListener(this);
        }
    }

    /**
     * {@inheritDoc}
     * @since 8.2
     */
    @Override
    public void removed(final NodeChangeEvent event) {
        // AP-8989 switching to IEclipsePreferences
        if (InstanceScope.INSTANCE.getNode(MountSettings.getMountpointPreferenceLocation()).equals(event.getParent())) {
            List<MountSettings> newValue = getUserOrDefaultValue();
            updateSettings(m_previousValues, newValue);
        }
    }
}
