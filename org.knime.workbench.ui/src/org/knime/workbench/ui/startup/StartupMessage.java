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
 *   21.07.2014 (thor): created
 */
package org.knime.workbench.ui.startup;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;
import org.osgi.framework.Bundle;

/**
 * Message that is reported during startup.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
public class StartupMessage {
    /**
     * Returns all startup messages.
     *
     * @return a list with all startup messages, never <code>null</code>
     */
    public static List<StartupMessage> getAllStartupMessages() {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint("org.knime.workbench.ui.startupMessages"); //$NON-NLS-1$
        assert point != null : Messages.StartupMessage_1;

        List<StartupMessage> messages = new ArrayList<>();
        for (IExtension ext : point.getExtensions()) {
            IConfigurationElement[] elements = ext.getConfigurationElements();
            for (IConfigurationElement providerElement : elements) {
                try {
                    StartupMessageProvider provider =
                        (StartupMessageProvider)providerElement.createExecutableExtension("class"); //$NON-NLS-1$
                    messages.addAll(provider.getMessages());
                } catch (CoreException ex) {
                    NodeLogger.getLogger(StartupMessage.class).error(
                        Messages.StartupMessage_3 + providerElement.getAttribute("class") //$NON-NLS-2$
                            + Messages.StartupMessage_5 + providerElement.getNamespaceIdentifier() + ": " + ex.getMessage(), ex); //$NON-NLS-2$
                }
            }
        }

        return messages;
    }

    /**
     * Message type (bit mask, value 1) indicating this status is informational only.
     */
    public static final int INFO = 0x01;

    /**
     * Message type (bit mask, value 2) indicating this status represents a warning.
     */
    public static final int WARNING = 0x02;

    /**
     * Message type (bit mask, value 4) indicating this status represents an error.
     */
    public static final int ERROR = 0x04;

    private final String m_message;

    private final String m_shortMessage;

    private final Bundle m_bundle;

    private final int m_type;

    /**
     * Creates a new startup message.
     *
     * @param message the message
     * @param type the messages type, one of {@link #ERROR}, {@link #WARNING}, or {@link #INFO}
     * @param bundle the bundle that reports the message
     */
    public StartupMessage(final String message, final int type, final Bundle bundle) {
        m_message = message;
        m_shortMessage = message;
        m_type = type;
        m_bundle = bundle;
    }


    /**
     * Creates a new startup message.
     *
     * @param message the message, may include hyperlinks (see {@link #getMessage()})
     * @param shortMessage a short message
     * @param type the messages type, one of {@link #ERROR}, {@link #WARNING}, or {@link #INFO}
     * @param bundle the bundle that reports the message
     */
    public StartupMessage(final String message, final String shortMessage, final int type, final Bundle bundle) {
        m_message = message;
        m_shortMessage = shortMessage;
        m_type = type;
        m_bundle = bundle;
    }

    /**
     * Returns a short message that is display in the message view.
     *
     * @return a short message
     */
    public String getShortMessage() {
        return m_shortMessage;
    }


    /**
     * Returns the complete message. It may contain hyperlinks in HTML format (e.g. <pre><a href="...">...</a></pre>).
     *
     * @return the complete message message
     */
    public String getMessage() {
        return m_message;
    }

    /**
     * Returns the message's type.
     *
     * @return the type, one of {@link #ERROR}, {@link #WARNING}, or {@link #INFO}
     */
    public int getType() {
        return m_type;
    }

    /**
     * Returns the bundle that reported this message.
     *
     * @return a bundle
     */
    public Bundle getBundle() {
        return m_bundle;
    }
}
