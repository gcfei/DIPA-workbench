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
 *   04.05.2011 (meinl): created
 */
package org.knime.workbench.helpview;

import org.eclipse.jetty.util.DateCache;
import org.eclipse.jetty.util.log.Logger;
import org.knime.core.node.NodeLogger;

/**
 * This class is not intended to be used externally!
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class JettyLogger implements Logger {
    private static final DateCache DATE_CACHE = new DateCache("yyyy-MM-dd HH:mm:ss");

    private static final boolean DEBUG = System.getProperty("DEBUG", null) != null;

    private final String m_name;

    private StringBuffer m_buffer = new StringBuffer();

    private boolean m_debugEnabled;

    private final NodeLogger m_logger;

    /** Creates a new logger. */
    public JettyLogger() {
        this("org.knime.workbench.help.jetty");
    }

    private JettyLogger(final String name) {
        m_name = name;
        m_logger = NodeLogger.getLogger(m_name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDebugEnabled() {
        return m_debugEnabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDebugEnabled(final boolean enabled) {
        m_debugEnabled = enabled;
    }

    @Override
    public void debug(final String msg, final Throwable th) {
        if (DEBUG) {
            String d = DATE_CACHE.now();
            synchronized (m_buffer) {
                tag(d, ":DBUG:");
                format(msg);
                format(th);
                m_logger.debug(m_buffer.toString(), th);
            }
        }
    }

    @Override
    public void warn(final String msg, final Throwable th) {
        String d = DATE_CACHE.now();
        synchronized (m_buffer) {
            tag(d, ":WARN:");
            format(msg);
            format(th);
            m_logger.warn(m_buffer.toString(), th);
        }
    }

    private void tag(final String d, final String tag) {
        m_buffer.setLength(0);
        m_buffer.append(d).append(tag).append(m_name).append(':');
    }

    private String format(String msg, final Object... args) {
        msg = String.valueOf(msg); // Avoids NPE
        String braces = "{}";
        StringBuilder builder = new StringBuilder();
        int start = 0;
        for (Object arg : args) {
            int bracesIndex = msg.indexOf(braces, start);
            if (bracesIndex < 0) {
                builder.append(msg.substring(start));
                builder.append(" ");
                builder.append(arg);
                start = msg.length();
            } else {
                builder.append(msg.substring(start, bracesIndex));
                builder.append(String.valueOf(arg));
                start = bracesIndex + braces.length();
            }
        }
        builder.append(msg.substring(start));
        return builder.toString();
    }

    private void format(final Throwable th) {
        if (th == null) {
            m_buffer.append("null");
        } else {
            m_buffer.append('\n');
            format(th.toString());
            StackTraceElement[] elements = th.getStackTrace();
            for (int i = 0; elements != null && i < elements.length; i++) {
                m_buffer.append("\n\tat ");
                format(elements[i].toString());
            }
        }
    }

    @Override
    public String toString() {
        return m_name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Logger getLogger(final String name) {
        if (((name == null) && (m_name == null)) || ((name != null) && name.equals(m_name))) {
            return this;
        }
        return new JettyLogger(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return m_name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void warn(final String msg, final Object... args) {
        String d = DATE_CACHE.now();
        synchronized (m_buffer) {
            tag(d, ":WARN:");
            format(msg, args);
            m_logger.warn(m_buffer.toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void warn(final Throwable thrown) {
        m_logger.warn("", thrown);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void info(final String msg, final Object... args) {
        String d = DATE_CACHE.now();
        synchronized (m_buffer) {
            tag(d, ":INFO:");
            format(msg, args);
            m_logger.info(m_buffer.toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void info(final Throwable thrown) {
        m_logger.info("", thrown);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void info(final String msg, final Throwable thrown) {
        m_logger.info(msg, thrown);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void debug(final String msg, final Object... args) {
        String d = DATE_CACHE.now();
        synchronized (m_buffer) {
            tag(d, ":INFO:");
            format(msg, args);
            m_logger.debug(m_buffer.toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void debug(final Throwable thrown) {
        m_logger.debug("", thrown);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void ignore(final Throwable ignored) {
        // ignore
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void debug(final String msg, final long value) {
        debug(msg, new Object[] { value });
    }
}
