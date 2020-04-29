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
 * Created: Mar 17, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.FileUtil;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.localworkspace.LocalWorkspaceContentProviderFactory;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.AbstractContentProviderFactory;
import org.knime.workbench.explorer.view.preferences.ExplorerPreferenceInitializer;
import org.knime.workbench.explorer.view.preferences.MountSettings;
import org.knime.workbench.ui.preferences.PreferenceConstants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.prefs.BackingStoreException;

/**
 *
 * @author ohl, University of Konstanz
 */
public final class ExplorerMountTable {
    /**
     * The property for changes on mount points IPropertyChangeListener can
     * register for.
     */
    public static final String MOUNT_POINT_PROPERTY = "MOUNT_POINTS"; //$NON-NLS-1$

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(ExplorerMountTable.class);

    private static final String PLUGIN_ID = FrameworkUtil.getBundle(ExplorerMountTable.class).getSymbolicName();

    private static final List<IPropertyChangeListener> CHANGE_LISTENER =
            new CopyOnWriteArrayList<IPropertyChangeListener>();

    /**
     * Valid mount IDs must comply with the hostname restrictions. That is, they
     * must only contain a-z, A-Z, 0-9 and '.' or '-'. They must not start with
     * a number, dot or a hyphen and must not end with a dot or hyphen.
     */
    private static final Pattern MOUNTID_PATTERN = Pattern
            .compile("^[a-zA-Z](?:[.a-zA-Z0-9-]*[a-zA-Z0-9])?$"); //$NON-NLS-1$

    private ExplorerMountTable() {
        // hiding constructor of utility class
    }

    /**
     * Keeps all currently mounted content with the mountID (provided by the
     * user).
     */
    private static final HashMap<String, MountPoint> MOUNTED =
            new LinkedHashMap<String, MountPoint>();

    /**
     * Creates a new instance of the specified content provider. May open a user
     * dialog to get parameters needed by the provider factory. Returns null, if
     * the user canceled.
     *
     * @param mountID name under which the content is mounted
     * @param providerID the provider factory id
     * @return a new content provider instance - or null if user canceled.
     * @throws IOException if the mounting fails
     */
    public static AbstractContentProvider mount(final String mountID,
            final String providerID) throws IOException {
        return mountOrRestore(mountID, providerID, (String)null);
    }

    /**
     * Valid mount IDs must comply with the hostname restrictions. That is, they
     * must only contain a-z, A-Z, 0-9 and '.' or '-'. They must not start with
     * a number, dot or a hyphen and must not end with a dot or hyphen.
     *
     * @param id the id to test
     * @return true if the id is valid (in terms of contained characters)
     *         independent of it may already be in use.
     */
    public static boolean isValidMountID(final String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        if (id.startsWith("knime.") || !MOUNTID_PATTERN.matcher(id).find()) { //$NON-NLS-1$
            return false;
        }
        try {
            // this is the way we build URIs to reference server items - this must not choke.
            new URI(ExplorerFileSystem.SCHEME, id, "/test/path", null); //$NON-NLS-1$
            return true;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * Throws an exception, if the specified id is not a valid mount id (in
     * terms of contained characters). (@see {@link #isValidMountID(String)})
     *
     * @param id to test
     * @throws IllegalArgumentException if the specified id is not a valid mount
     *             id (in terms of contained characters). (@see
     *             {@link #isValidMountID(String)})
     */
    public static void checkMountID(final String id)
            throws IllegalArgumentException {
        if (!isValidMountID(id)) {
            throw new IllegalArgumentException(id);
        }
    }

    /**
     * Sorts the mount points in the same way as in the passed list. It makes
     * sure that if two entries A and B of the list are mounted and A comes
     * before B, the mount point A will appear before mount point B in the mount
     * table.
     *
     * @param mountIDs a list of mount ids
     */
    public static void setMountOrder(final List<String> mountIDs) {
        if (!compareSortOrder(mountIDs)) {
            synchronized (MOUNTED) {
                for (String mountID : mountIDs) {
                    MountPoint mountPoint = MOUNTED.get(mountID);
                    if (mountPoint != null) {
                        /*
                         * Remove the mount point and insert it again immediately to
                         * get the same order as in the mount id list.
                         */
                        MOUNTED.remove(mountID);
                        notifyListeners(new PropertyChangeEvent(mountPoint, MOUNT_POINT_PROPERTY,
                            mountID, null));
                        MOUNTED.put(mountID, mountPoint);
                        notifyListeners(new PropertyChangeEvent(mountPoint,
                            MOUNT_POINT_PROPERTY, null, mountID));
                    }
                }
            }
        }
    }

    /**
     * @param mountIDs a list of mount IDs in the expected order
     * @return if the sort order of the passed list of mount IDs is the same as in the mount table
     */
    private static boolean compareSortOrder(final List<String> mountIDs) {
        synchronized (MOUNTED) {
            // sanity check
            if (mountIDs.size() != MOUNTED.size()) {
                return false;
            }
            // compare entry set with mount ID list
            Iterator<Entry<String, MountPoint>> iterator = MOUNTED.entrySet().iterator();
            for (int i = 0; i < mountIDs.size(); i++) {
                if (!mountIDs.get(i).equals(iterator.next().getKey())) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Creates a new instance of the specified content provider.
     *
     * @param mountID name under which the content is mounted
     * @param providerID the provider factory id
     * @param storage the stored data of the content provider
     * @return a new content provider instance - or null if user canceled.
     * @throws IOException if the mounting fails
     */
    public static AbstractContentProvider mount(final String mountID,
            final String providerID, final String storage) throws IOException {
        return mountOrRestore(mountID, providerID, storage);
    }

    /**
     * Mounts a new content with the specified mountID and from the specified
     * provider factory - and initializes it from the specified storage, if that
     * is not null.
     *
     * @param mountID
     * @param providerID
     * @param storage
     * @param active
     * @return
     */
    private static AbstractContentProvider mountOrRestore(final String mountID,
            final String providerID, final String storage) throws IOException {
        checkMountID(mountID);
        synchronized (MOUNTED) {
            // can't mount different providers with the same ID
            MountPoint existMp = MOUNTED.get(mountID);
            if (existMp != null) {
                if (existMp.getProviderFactory().getID().equals(providerID)) {
                    // re-use the provider
                    LOGGER.debug(Messages.getString("ExplorerMountTable.4") //$NON-NLS-1$
                            + Messages.getString("ExplorerMountTable.5") + providerID //$NON-NLS-1$
                            + Messages.getString("ExplorerMountTable.6") //$NON-NLS-1$
                            + mountID + Messages.getString("ExplorerMountTable.7")); //$NON-NLS-1$
                    existMp.incrRefCount();
                    return existMp.getProvider();
                }
                throw new IOException(
                        Messages.getString("ExplorerMountTable.8") //$NON-NLS-1$
                                + Messages.getString("ExplorerMountTable.9") //$NON-NLS-1$
                                + existMp.getProvider().toString() + Messages.getString("ExplorerMountTable.10")); //$NON-NLS-1$
            }

            AbstractContentProviderFactory fac =
                    CONTENT_FACTORIES.get(providerID);
            if (fac == null) {
                LOGGER.coding(Messages.getString("ExplorerMountTable.11") //$NON-NLS-1$
                        + Messages.getString("ExplorerMountTable.12") + providerID //$NON-NLS-1$
                        + Messages.getString("ExplorerMountTable.13")); //$NON-NLS-1$
                throw new IOException(
                        Messages.getString("ExplorerMountTable.14") //$NON-NLS-1$
                                + Messages.getString("ExplorerMountTable.15") + providerID //$NON-NLS-1$
                                + Messages.getString("ExplorerMountTable.16")); //$NON-NLS-1$
            }
            if (!fac.multipleInstances() && isMounted(providerID)) {
                throw new IllegalStateException(Messages.getString("ExplorerMountTable.17") //$NON-NLS-1$
                        + fac.toString() + Messages.getString("ExplorerMountTable.18")); //$NON-NLS-1$
            }
            AbstractContentProvider newProvider = null;
            if (storage == null) {
                // may open a dialog for the user to provide parameters
                newProvider = fac.createContentProvider(mountID);
                if (newProvider == null) {
                    // user probably canceled.
                    return null;
                }
            } else {
                newProvider = fac.createContentProvider(mountID, storage);
                if (newProvider == null) {
                    // something went wrong
                    return null;
                }
            }

            MountPoint mp = new MountPoint(mountID, newProvider, fac);
            synchronized (MOUNTED) {
                MOUNTED.put(mountID, mp);
                notifyListeners(new PropertyChangeEvent(mp, MOUNT_POINT_PROPERTY, null, mp.getMountID()));
            }
            return newProvider;
        }
    }

    /**
     * @param mountID the id to unmount
     * @return true if unmounting was successful, false otherwise
     */
    public static synchronized boolean unmount(final String mountID) {
        synchronized (MOUNTED) {
            MountPoint mp = MOUNTED.remove(mountID);
            if (mp == null) {
                return false;
            }
            mp.dispose();
            notifyListeners(new PropertyChangeEvent(mp, MOUNT_POINT_PROPERTY,
                    mp.getMountID(), null));
            return true;
        }
    }

    /**
     * Unmounts all MountPoints.
     */
    public static void unmountAll() {
        synchronized (MOUNTED) {
            List<String> ids = getAllMountedIDs();
            for (String id : ids) {
                unmount(id);
            }
        }
    }

    /**
     * Returns a list of content providers that could be added (that is that
     * allow multiple instances or are not yet mounted). The list contains the
     * factory objects. Their toString method should return a useful name.
     *
     * @return a map of available content providers (key = name, value = ID).
     */
    public static List<AbstractContentProviderFactory>
            getAddableContentProviders() {
        LinkedList<AbstractContentProviderFactory> result =
                new LinkedList<AbstractContentProviderFactory>();
        // nobody should add a new provider while we are working
        synchronized (MOUNTED) {
            for (Map.Entry<String, AbstractContentProviderFactory> e
                    : CONTENT_FACTORIES.entrySet()) {
                String facID = e.getKey();
                AbstractContentProviderFactory fac = e.getValue();
                if (fac.multipleInstances()
                        || !(isMounted(facID))) {
                    result.add(fac);
                }
            }
        }
        return result;
    }

    /**
     * Returns a list of content providers that could be added (that is that
     * allow multiple instances or are not contained in a provided list). The list contains the
     * factory objects. Their toString method should return a useful name.
     * Use this method for intermediate determination of addable content providers (e.g. in preferences)
     * @param existingProviderIDs a list with given content provider IDs
     * @return a list of available content providers
     * @since 6.0
     */
    public static List<AbstractContentProviderFactory>
            getAddableContentProviders(final List<String> existingProviderIDs) {
        LinkedList<AbstractContentProviderFactory> result =
                new LinkedList<AbstractContentProviderFactory>();
        for (Map.Entry<String, AbstractContentProviderFactory> e
                : CONTENT_FACTORIES.entrySet()) {
            String facID = e.getKey();
            AbstractContentProviderFactory fac = e.getValue();
            if (!fac.isTempSpace() && (fac.multipleInstances() || !existingProviderIDs.contains(facID))) {
                result.add(fac);
            }
        }
        return result;
    }

    /**
     * Returns the content provider factory for the provided id.
     *
     * @param factoryID the id of the factory
     *
     * @return The content provider factory for the provided id, null if id does not exist.
     */
    public static AbstractContentProviderFactory getContentProviderFactory(
            final String factoryID) {
        return CONTENT_FACTORIES.get(factoryID);
    }

    /**
     * @return a list of all mount IDs currently in use and not hidden.
     * @since 6.4
     */
    public static List<String> getAllVisibleMountIDs() {
        ArrayList<String> result = new ArrayList<String>();
        synchronized (MOUNTED) {
            for (Map.Entry<String, MountPoint> e : MOUNTED.entrySet()) {
                if (!e.getValue().getProviderFactory().isTempSpace()) {
                    result.add(e.getKey());
                }
            }
        }
        return result;
    }

    /**
     * @return a list of all local mount IDs currently in use and not hidden
     * @since 8.4
     */
    public static List<String> getAllVisibleLocalMountIDs() {
        final ArrayList<String> result = new ArrayList<String>();
        synchronized (MOUNTED) {
            for (Map.Entry<String, MountPoint> e : MOUNTED.entrySet()) {
                final MountPoint mountPoint = e.getValue();
                if (!mountPoint.getProviderFactory().isTempSpace()
                        && !mountPoint.getProvider().isRemote()) {
                    result.add(e.getKey());
                }
            }
        }
        return result;
    }

    /**
     * @return a list of all mount IDs currently in use.
     * @since 6.4
     */
    public static List<String> getAllMountedIDs() {
        synchronized (MOUNTED) {
            return new ArrayList<String>(MOUNTED.keySet());
        }
    }

    /**
     * @return a map with all currently mounted content providers with their mount ID (including the temp space that
     * should never be shown to the user).
     * @since 6.4
     */
    public static Map<String, AbstractContentProvider> getMountedContentInclTempSpace() {
        HashMap<String, AbstractContentProvider> result =
                new LinkedHashMap<String, AbstractContentProvider>();
        synchronized (MOUNTED) {
            for (Map.Entry<String, MountPoint> e : MOUNTED.entrySet()) {
                result.put(e.getKey(), e.getValue().getProvider());
            }
        }
        return result;
    }

    /**
     * @return a map with the currently mounted content providers with their mount ID (temp space is not included).
     */
    public static Map<String, AbstractContentProvider> getMountedContent() {
        HashMap<String, AbstractContentProvider> result =
                new LinkedHashMap<String, AbstractContentProvider>();
        synchronized (MOUNTED) {
            for (Map.Entry<String, MountPoint> e : MOUNTED.entrySet()) {
                if (!e.getValue().getProviderFactory().isTempSpace()) {
                    result.put(e.getKey(), e.getValue().getProvider());
                }
            }
        }
        return result;
    }

    /**
     * The MountPoint (containing the content provider) currently mounted with
     * the specified ID - or null if the ID is not used as mount point.
     *
     * @param mountID the mount point
     * @return null, if no content is mounted with the specified ID
     */
    public static MountPoint getMountPoint(final String mountID) {
        synchronized (MOUNTED) {
            return MOUNTED.get(mountID);
        }
    }

    /**
     * Checks whether an instance created by the specified factory is already
     * mounted.
     *
     *
     * @param providerID the id of the provider factory
     * @return true, if an instance created by the specified factory exists
     *         already, false, if not.
     */
    public static boolean isMounted(final String providerID) {
        return !getMountIDs(providerID).isEmpty();
    }

    /**
     * Checks whether an instance created by the specified factory is already
     * mounted and returns the mount IDs for it.
     *
     *
     * @param providerID the id of the provider factory
     * @return a list with mount IDs (or an empty list if provider is not
     *         mounted).
     */
    public static List<String> getMountIDs(final String providerID) {
        if (providerID == null || providerID.isEmpty()) {
            throw new IllegalArgumentException(
                    Messages.getString("ExplorerMountTable.19")); //$NON-NLS-1$
        }
        LinkedList<String> mountIDs = new LinkedList<String>();

        synchronized (MOUNTED) {
            for (Map.Entry<String, MountPoint> e : MOUNTED.entrySet()) {
                MountPoint mp = e.getValue();
                if (providerID.equals(mp.getProviderFactory().getID())) {
                    mountIDs.add(e.getKey());
                }
            }
        }
        return mountIDs;
    }

    /**
     *
     * @return the file system representing the content of the mount table.
     */
    public static ExplorerFileSystem getFileSystem() {
        return ExplorerFileSystem.INSTANCE;
    }

    /**
     * Creates a new dir in the explorer temp provider. The dir is deleted on exit. The returned file store has a valid
     * mount id - which is not shown in the explorer view.
     *
     * @param prefix to the name of the returned file store
     * @return a newly created temporary directory. Deleted on VM exit.
     * @throws CoreException if it couldn't create the temp dir
     * @since 6.4
     */
    public static LocalExplorerFileStore createExplorerTempDir(final String prefix) throws CoreException {
        Map<String, AbstractContentProvider> allMounts = getMountedContentInclTempSpace();
        AbstractContentProvider tempProvider = null;
        for (Entry<String, AbstractContentProvider> e : allMounts.entrySet()) {
            if (e.getValue().getFactory().isTempSpace()) {
                tempProvider = e.getValue();
                break;
            }
        }
        if (tempProvider == null) {
            throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID,
                Messages.getString("ExplorerMountTable.20"))); //$NON-NLS-1$
        }
        AbstractExplorerFileStore tmpRoot = tempProvider.getRootStore();
        File localTmp;
        try {
            localTmp = tmpRoot.toLocalFile();
        } catch (CoreException e1) {
            throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID,
                Messages.getString("ExplorerMountTable.21") + tmpRoot)); //$NON-NLS-1$
        }
        try {
            File tmpDir = FileUtil.createTempDir(prefix, localTmp,
                false /* entire temp mount point is deleted on exit */);
            AbstractExplorerFileStore tmpFileStore = tmpRoot.getChild(tmpDir.getName());
            if (!(tmpFileStore instanceof LocalExplorerFileStore)) {
                throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID,
                    Messages.getString("ExplorerMountTable.22") //$NON-NLS-1$
                            + tmpFileStore.getClass().getCanonicalName()));
            }
            return (LocalExplorerFileStore)tmpFileStore;
        } catch (IOException e1) {
            throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID,
                Messages.getString("ExplorerMountTable.23") + tmpRoot, e1)); //$NON-NLS-1$
        }
    }

    /* ------------- read the extension point ------------------------------ */

    /**
     * Stores all content provider factories (registered with the extension
     * point) mapped to their ID.
     */
    private static final TreeMap<String, AbstractContentProviderFactory>
            CONTENT_FACTORIES =
                new TreeMap<String, AbstractContentProviderFactory>();

    private static final TreeMap<String, String> FACTORY_NAMES =
            new TreeMap<String, String>();

    static {
        // read out the extension point now
        collectContentProviderFactories();
        init();
    }

    /**
     * Some extension point parameters.
     */
    private static final String EXT_POINT_ID =
            "org.knime.workbench.explorer.contentprovider"; //$NON-NLS-1$

    private static final String ATTR_CONT_PROV_FACT = "ContentProviderFactory"; //$NON-NLS-1$

    private static void collectContentProviderFactories() {

        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
        if (point == null) {
            LOGGER.error(Messages.getString("ExplorerMountTable.26") //$NON-NLS-1$
                    + Messages.getString("ExplorerMountTable.27") //$NON-NLS-1$
                    + Messages.getString("ExplorerMountTable.28")); //$NON-NLS-1$
            // let's throw in the local workspace content provider
            LocalWorkspaceContentProviderFactory lwcpf =
                    new LocalWorkspaceContentProviderFactory();
            CONTENT_FACTORIES.put(lwcpf.getID(), lwcpf);
            FACTORY_NAMES.put(lwcpf.toString(), lwcpf.getID());
            return;
        }

        for (IConfigurationElement elem : point.getConfigurationElements()) {
            String contProvFact = elem.getAttribute(ATTR_CONT_PROV_FACT);
            String decl = elem.getDeclaringExtension().getUniqueIdentifier();

            if (contProvFact == null || contProvFact.isEmpty()) {
                LOGGER.error(Messages.getString("ExplorerMountTable.29") + decl //$NON-NLS-1$
                        + Messages.getString("ExplorerMountTable.30") //$NON-NLS-1$
                        + ATTR_CONT_PROV_FACT + "'"); //$NON-NLS-1$
                LOGGER.error(Messages.getString("ExplorerMountTable.32") + decl + Messages.getString("ExplorerMountTable.33")); //$NON-NLS-1$ //$NON-NLS-2$
                continue;
            }

            // try instantiating the content provider factory.
            AbstractContentProviderFactory instance = null;
            try {
                instance =
                        (AbstractContentProviderFactory)elem
                                .createExecutableExtension(ATTR_CONT_PROV_FACT);
            } catch (Throwable t) {
                LOGGER.error(Messages.getString("ExplorerMountTable.34") //$NON-NLS-1$
                        + Messages.getString("ExplorerMountTable.35") + contProvFact //$NON-NLS-1$
                        + Messages.getString("ExplorerMountTable.36"), t); //$NON-NLS-1$
                if (decl != null) {
                    LOGGER.error(Messages.getString("ExplorerMountTable.37") + decl + Messages.getString("ExplorerMountTable.38")); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }

            if (instance != null) {
                CONTENT_FACTORIES.put(instance.getID(), instance);
                FACTORY_NAMES.put(instance.toString(), instance.getID());
            }
        }

    }

    /*---------------------------------------------------------------*/

    /**
     * Initializes the explorer mount table based on the preferences of the
     * plugin's preference store.
     */
    public static void init() {
        unmountAll();
        mountTempSpace();
        synchronized (MOUNTED) {
            for (MountSettings ms : getMountSettings()) {
                // ignore inactive
                if (!ms.isActive()) {
                    continue;
                }
                String mountID = ms.getMountID();
                String storage = ms.getContent();
                if (storage == null) {
                    LOGGER.error(Messages.getString("ExplorerMountTable.39") //$NON-NLS-1$
                            + Messages.getString("ExplorerMountTable.40") + mountID + Messages.getString("ExplorerMountTable.41")); //$NON-NLS-1$ //$NON-NLS-2$
                    continue;
                }
                String factID = ms.getFactoryID();
                if (factID == null) {
                    LOGGER.error(Messages.getString("ExplorerMountTable.42") //$NON-NLS-1$
                            + Messages.getString("ExplorerMountTable.43") + mountID + Messages.getString("ExplorerMountTable.44")); //$NON-NLS-1$ //$NON-NLS-2$
                    continue;
                }

                try {
                    if (mountOrRestore(mountID, factID, storage)
                            == null) {
                        LOGGER.error(Messages.getString("ExplorerMountTable.45") //$NON-NLS-1$
                                + mountID + Messages.getString("ExplorerMountTable.46") + factID //$NON-NLS-1$
                                + Messages.getString("ExplorerMountTable.47")); //$NON-NLS-1$
                    }
                } catch (Throwable t) {
                    String msg = t.getMessage();
                    if (msg == null || msg.isEmpty()) {
                        msg = Messages.getString("ExplorerMountTable.48"); //$NON-NLS-1$
                    }
                    LOGGER.error(Messages.getString("ExplorerMountTable.49") + mountID //$NON-NLS-1$
                            + Messages.getString("ExplorerMountTable.50") + factID + "): " + msg, t); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
        }

    }

    private static List<MountSettings> getMountSettings() {
        // AP-8989 switching to IEclipsePreferences
        List<MountSettings> mountSettings = new ArrayList<>();

        IEclipsePreferences mountPointNode = InstanceScope.INSTANCE.getNode(MountSettings.getMountpointPreferenceLocation());
        String[] childrenNames = null;
        try {
            childrenNames = mountPointNode.childrenNames();
        } catch (BackingStoreException e) {
            LOGGER.error(Messages.getString("ExplorerMountTable.52") + e.getMessage(), e); //$NON-NLS-1$
        }

        if (!ArrayUtils.isEmpty(childrenNames)) {
            mountSettings = MountSettings.loadSortedMountSettingsFromPreferenceNode();
        } else {
            IPreferenceStore pStore = ExplorerActivator.getDefault().getPreferenceStore();
            String mpSettings;
            if (ExplorerPreferenceInitializer.existsMountPreferencesXML()) {
                mpSettings = pStore.getString(PreferenceConstants.P_EXPLORER_MOUNT_POINT_XML);
            } else {
                mpSettings = pStore.getString(PreferenceConstants.P_EXPLORER_MOUNT_POINT);
            }
            if (StringUtils.isEmpty(mpSettings)) {
                ExplorerPreferenceInitializer.loadDefaultMountPoints();
                mpSettings = pStore.getDefaultString(PreferenceConstants.P_EXPLORER_MOUNT_POINT_XML);
            }
             mountSettings = MountSettings.parseSettings(mpSettings, true);
             mountSettings.addAll(MountSettings.loadSortedMountSettingsFromDefaultPreferenceNode());
        }

        return mountSettings;
    }

    /* Mounts all hidden spaces that provide a default mount id. */
    private static void mountTempSpace() {
        List<AbstractContentProviderFactory> contentProviders = getAddableContentProviders();
        for (AbstractContentProviderFactory fac : contentProviders) {
            String mountID = fac.getDefaultMountID();
            if (fac.isTempSpace() && mountID != null) {
                try {
                    mountOrRestore(mountID, fac.getID(), null);
                    LOGGER.info(Messages.getString("ExplorerMountTable.53") + mountID + "' - " + fac.getID()); //$NON-NLS-1$ //$NON-NLS-2$
                    return; // mounting only one temp space
                } catch (IOException e) {
                    LOGGER.error(Messages.getString("ExplorerMountTable.55") + mountID + "' - " + fac.getID(), e); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
        }
        LOGGER.debug(Messages.getString("ExplorerMountTable.57")); //$NON-NLS-1$
    }

    /*---------------------------------------------------------------*/
    /**
     * Adds a property change listener for mount changes.
     *
     * @param listener the property change listener to add
     */
    public static void addPropertyChangeListener(
            final IPropertyChangeListener listener) {
        CHANGE_LISTENER.add(listener);
    }

    /**
     * Removes the given listener. Calling this method has no affect if the
     * listener is not registered.
     *
     * @param listener a property change listener
     */
    public static void removePropertyChangeListener(
            final IPropertyChangeListener listener) {
        CHANGE_LISTENER.remove(listener);
    }

    private static void notifyListeners(final PropertyChangeEvent event) {
        for (IPropertyChangeListener listener : CHANGE_LISTENER) {
            listener.propertyChange(event);
        }
    }

    /**
     * Updates the settings of all providers in the preferences. Some providers may get additional attributes once they
     * are used (e.g. the REST address for server mount points) which should be persisted.
     * @since 7.3
     */
    public static void updateProviderSettings() {
        // AP-8989 switching to IEclipsePreferences
        Map<String, AbstractContentProvider> mountedContent = getMountedContent();
        List<MountSettings> mountSettingsToSave = new ArrayList<>();

        for (MountSettings ms : getMountSettings()) {
            if (mountedContent.containsKey(ms.getMountID())) {
                mountSettingsToSave.add(new MountSettings(mountedContent.get(ms.getMountID())));
            } else {
                mountSettingsToSave.add(ms);
            }
        }

        MountSettings.saveMountSettings(mountSettingsToSave);

    }

    /**
     * Returns the collected ContentProviderFactories.
     *
     * @return the collected ContentProviderFactories
     * @since 8.2
     */
    public static TreeMap<String, AbstractContentProviderFactory> getContentProviderFactories() {
        return CONTENT_FACTORIES;
    }
}
