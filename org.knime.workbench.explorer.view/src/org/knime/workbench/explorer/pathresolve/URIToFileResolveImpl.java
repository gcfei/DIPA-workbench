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
 *
 */
package org.knime.workbench.explorer.pathresolve;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.knime.core.util.FileUtil;
import org.knime.core.util.pathresolve.URIToFileResolve;
import org.knime.workbench.explorer.ExplorerURLStreamHandler;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileStore;

/**
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class URIToFileResolveImpl implements URIToFileResolve {
    /** {@inheritDoc} */
    @Override
    public File resolveToFile(final URI uri) throws IOException {
        return resolveToFile(uri, new NullProgressMonitor());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File resolveToLocalOrTempFile(final URI uri) throws IOException {
        return resolveToLocalOrTempFile(uri, new NullProgressMonitor());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File resolveToFile(final URI uri, final IProgressMonitor monitor) throws IOException {
        if (uri == null) {
            throw new IllegalArgumentException(Messages.URIToFileResolveImpl_0);
        }
        String scheme = uri.getScheme();
        if ("file".equalsIgnoreCase(scheme)) { //$NON-NLS-1$
            try {
                return new File(uri);
            } catch (IllegalArgumentException e) {
                throw new IOException(Messages.URIToFileResolveImpl_2 + uri + Messages.URIToFileResolveImpl_3, e);
            }
        } else if (ExplorerFileSystem.SCHEME.equalsIgnoreCase(scheme)) {
            URL url = ExplorerURLStreamHandler.resolveKNIMEURL(uri.toURL());
            if ("file".equals(url.getProtocol())) { //$NON-NLS-1$
                return FileUtil.getFileFromURL(url);
            } else if (ExplorerFileSystem.SCHEME.equals(url.getProtocol())) {
                return resolveStandardUri(uri, monitor);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private File resolveStandardUri(final URI uri, final IProgressMonitor monitor) throws IOException {
        try {
            AbstractExplorerFileStore s = ExplorerFileSystem.INSTANCE.getStore(uri);
            if (s == null) {
                throw new IOException(
                    Messages.URIToFileResolveImpl_5 + uri + Messages.URIToFileResolveImpl_6
                        + Messages.URIToFileResolveImpl_7);
            }
            return s.toLocalFile(EFS.NONE, monitor);
        } catch (Exception e) {
            throw new IOException(Messages.URIToFileResolveImpl_8 + uri + Messages.URIToFileResolveImpl_9, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File resolveToLocalOrTempFile(final URI uri, final IProgressMonitor monitor) throws IOException {
        if (uri == null) {
            throw new IllegalArgumentException(Messages.URIToFileResolveImpl_10);
        }
        String scheme = uri.getScheme();
        if ("file".equalsIgnoreCase(scheme)) { //$NON-NLS-1$
            try {
                return new File(uri);
            } catch (IllegalArgumentException e) {
                throw new IOException(Messages.URIToFileResolveImpl_12 + uri + Messages.URIToFileResolveImpl_13, e);
            }
        } else if (ExplorerFileSystem.SCHEME.equalsIgnoreCase(scheme)) {
            URL url = ExplorerURLStreamHandler.resolveKNIMEURL(uri.toURL());
            if ("file".equals(url.getProtocol())) { //$NON-NLS-1$
                return FileUtil.getFileFromURL(url);
            } else if (ExplorerFileSystem.SCHEME.equals(url.getProtocol())) {
                AbstractExplorerFileStore fs = ExplorerFileSystem.INSTANCE.getStore(uri);
                if (fs instanceof LocalExplorerFileStore) {
                    return resolveStandardUri(uri, monitor);
                } else if (fs instanceof RemoteExplorerFileStore) {
                    return fetchRemoteFileStore((RemoteExplorerFileStore)fs, monitor);
                } else {
                    throw new IOException(Messages.URIToFileResolveImpl_15 + fs.getClass());
                }
            } else {
                // use the original URL because otherwise the handler may not be invoked correctly
                return fetchRemoteFile(uri.toURL());
            }
        } else if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) { //$NON-NLS-1$ //$NON-NLS-2$
            return fetchRemoteFile(uri.toURL());
        } else {
            throw new IOException(Messages.URIToFileResolveImpl_18 + uri + Messages.URIToFileResolveImpl_19);
        }
    }

    private File fetchRemoteFileStore(final RemoteExplorerFileStore source, final IProgressMonitor monitor)
        throws IOException {
        try {
            return source.resolveToLocalFile(monitor);
        } catch (CoreException e) {
            throw new IOException(e);
        }
    }

    private static File fetchRemoteFile(final URL url) throws IOException {
        File f = FileUtil.createTempFile("download", ".bin"); //$NON-NLS-1$ //$NON-NLS-2$
        try (InputStream is = addAuthHeaderAndOpenStream(url); OutputStream os = new FileOutputStream(f)) {
            IOUtils.copy(is, os);
        }
        return f;
    }

    private static InputStream addAuthHeaderAndOpenStream(final URL url) throws IOException {
        String userInfo = url.getUserInfo();
        if (userInfo != null) {
            URLConnection uc = url.openConnection();
            String urlDecodedUserInfo = URLDecoder.decode(userInfo, StandardCharsets.UTF_8.name());
            String basicAuth = "Basic " + new String(Base64.getEncoder().encode(urlDecodedUserInfo.getBytes())); //$NON-NLS-1$
            uc.setRequestProperty("Authorization", basicAuth); //$NON-NLS-1$
            return uc.getInputStream();
        } else {
            return url.openStream();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @since 5.0
     */
    @Override
    public boolean isMountpointRelative(final URI uri) {
        return ExplorerFileSystem.SCHEME.equalsIgnoreCase(uri.getScheme())
            && ExplorerURLStreamHandler.MOUNTPOINT_RELATIVE.equalsIgnoreCase(uri.getHost());
    }

    /**
     * {@inheritDoc}
     *
     * @since 5.0
     */
    @Override
    public boolean isWorkflowRelative(final URI uri) {
        return ExplorerFileSystem.SCHEME.equalsIgnoreCase(uri.getScheme())
            && ExplorerURLStreamHandler.WORKFLOW_RELATIVE.equalsIgnoreCase(uri.getHost());
    }

    /**
     * {@inheritDoc}
     *
     * @since 6.4
     */
    @Override
    public boolean isNodeRelative(final URI uri) {
        return ExplorerFileSystem.SCHEME.equalsIgnoreCase(uri.getScheme())
            && ExplorerURLStreamHandler.NODE_RELATIVE.equalsIgnoreCase(uri.getHost());
    }
}
