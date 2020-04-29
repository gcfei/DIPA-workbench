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
 *   27.04.2011 (meinl): created
 */
package org.knime.workbench.helpview;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.knime.core.node.NodeLogger;
import org.osgi.framework.Bundle;

/**
 * Handler for Jetty that resolves node-relative and bundle-relative links.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
final class FileHandler extends AbstractHandler {
    /** Singleton instance of this handler. */
    static final FileHandler instance = new FileHandler();

    private static final MimeTypes MIME_MAP = new MimeTypes();

    private static final NodeLogger logger = NodeLogger.getLogger(FileHandler.class);

    private FileHandler() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handle(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response)
        throws IOException, ServletException {
        String bundleName = request.getParameter("bundle");
        String file = request.getParameter("file");
        Bundle bundle = Platform.getBundle(bundleName);
        if (bundle == null) {
            logger.error("Bundle '" + bundleName + "' does not exist?!");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        URL url = null;
        if ("/bundle/".equals(target)) {
            url = bundle.getEntry(file);
        } else if ("/node/".equals(target)) {
            String packageLoc =
                    request.getParameter("package").replace('.', '/');
            url = bundle.getResource(packageLoc + "/" + file);
        }
        if (url == null) {
            logger.warn("File for '" + request.getRequestURI() + "' not found");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        InputStream in = url.openStream();
        if (in == null) {
            logger.warn("Could not open stream for '" + url + "'");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MIME_MAP.getMimeByExtension(file));

        byte[] buf = new byte[16384];
        OutputStream out = response.getOutputStream();
        int read = 0;
        while ((read = in.read(buf)) > -1) {
            out.write(buf, 0, read);
        }
        in.close();
        out.close();
    }
}
