/*
 * ------------------------------------------------------------------------
 *
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
 *   Oct 9, 2019 (gabriel): created
 */
package org.knime.workbench.explorer;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.knime.filehandling.core.connections.attributes.FSBasicAttributes;
import org.knime.filehandling.core.connections.attributes.FSFileAttributes;
import org.knime.filehandling.core.util.MountPointIDProvider;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileInfo;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;

/**
 * Implementation Of {@link MountPointIDProvider} backed by {@link ExplorerMountTable}.
 *
 * @author Gabriel Einsdorf
 * @since 8.7
 */
public class ExplorerMountPointIDProvider implements MountPointIDProvider {

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getMountedIDs() {
        return ExplorerMountTable.getAllMountedIDs();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URL resolveKNIMEURL(final URL url) throws IOException {
        return ExplorerURLStreamHandler.resolveKNIMEURL(url);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if mountpoint does not exist
     */
    @Override
    public List<URI> listFiles(final URI uri) throws IOException {
        try {
            final AbstractExplorerFileStore store = getStore(uri);
            final List<URI> children = new ArrayList<>();
            for (final String childName : store.childNames(0, null)) {
                children.add(store.getChild(childName).toURI());
            }
            return children;
        } catch (final CoreException e) {
            throw new IOException(e);
        }

    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if mountpoint does not exist
     */
    @Override
    public FSFileAttributes getFileAttributes(final URI uri) throws IOException {

        final AbstractExplorerFileStore store = getStore(uri);
        AbstractExplorerFileInfo info;
        try {
            info = store.fetchInfo(0, null);
        } catch (final CoreException e) {
            throw new IOException(e);
        }

        if (!info.exists()) {
            throw new IOException(Messages.getString("ExplorerMountPointIDProvider.0") + uri.toString() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        final FileTime lastMod = FileTime.fromMillis(info.getLastModified());

        return new FSFileAttributes(!info.isDirectory() || AbstractExplorerFileStore.isWorkflow(store), null,
            p -> new FSBasicAttributes(lastMod, lastMod, lastMod, info.getLength(), false, false));
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if mountpoint does not exist
     */
    @Override
    public boolean copyFile(final URI source, final URI target) throws IOException {

        final AbstractExplorerFileStore sourceStore = getStore(source);
        final AbstractExplorerFileStore targetStore = getStore(target);
        try {
            sourceStore.copy(targetStore, 0, null);
        } catch (final CoreException e) {
            throw new IOException(e);
        }

        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if mountpoint does not exist
     */
    @Override
    public boolean moveFile(final URI source, final URI target) throws IOException {

        final AbstractExplorerFileStore sourceStore = getStore(source);
        final AbstractExplorerFileStore targetStore = getStore(target);
        try {
            sourceStore.move(targetStore, 0, null);
        } catch (final CoreException e) {
            throw new IOException(e);
        }
        sourceStore.getContentProvider().refresh();
        targetStore.getContentProvider().refresh();

        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if mountpoint does not exist
     */
    @Override
    public boolean deleteFile(final URI uri) throws IOException {
        try {
            getStore(uri).delete(0, null);
        } catch (final CoreException e) {
            throw new IOException(e);
        }

        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if mountpoint does not exist
     */
    @Override
    public void createDirectory(final URI uri) throws IOException {
        try {
            final AbstractExplorerFileStore store = getStore(uri);
            store.mkdir(0, null);
            store.getParent().refresh();
        } catch (final CoreException e) {
            throw new IOException(e);
        }

    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if mountpoint does not exist
     */
    @Override
    public boolean isReadable(final URI uri) throws IOException {
        //FIXME hacky way to see if we are connected
        try {
            return getStore(uri).getContentProvider().getRootStore().childNames(0, null).length != 0;
        } catch (final CoreException e) {
            throw new IOException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if mountpoint does not exist
     */
    @Override
    public boolean isWorkflow(final URI uri) {
        return AbstractExplorerFileStore.isWorkflow(getStore(uri));
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if mountpoint does not exist
     */
    @Override
    public URI getDefaultDirectory(final URI uri) {
        return getStore(uri).getContentProvider().getRootStore().toURI();
    }

    private static AbstractExplorerFileStore getStore(final URI uri) {
        final AbstractExplorerFileStore store = ExplorerMountTable.getFileSystem().getStore(uri);
        if (store == null) {
            throw new IllegalArgumentException(String.format(Messages.getString("ExplorerMountPointIDProvider.2"), uri.getHost())); //$NON-NLS-1$
        } else {
            return store;
        }
    }

}
