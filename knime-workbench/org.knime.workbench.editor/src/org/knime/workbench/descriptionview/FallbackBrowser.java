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
 *   27.08.2007 (Fabian Dill): created
 */
package org.knime.workbench.descriptionview;

import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.NodeLogger;

/**
 * Converts HTML text into normal text by using its own XSLT transformation.
 *
 * @author Fabian Dill, University of Konstanz
 */
public class FallbackBrowser {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(FallbackBrowser.class);
    private static final String XSLT = "HTML2Text.xslt"; //$NON-NLS-1$
    private static final String WARNING = Messages.FallbackBrowser_1
        + Messages.FallbackBrowser_2;
    private static final String XML_OPENING_TAG = Messages.FallbackBrowser_3;


    private final Transformer m_transformer;

    private final StyledText m_text;

    private final StyleRange m_styleRange;

    private final boolean m_shouldDisplayFallbackWarning;

    /**
     * @param parent parent
     * @param displayFallbackWarning if true, then the displayed content will always be prefaced with a bold-red text
     *            stating that we're using the fallback browser because we weren't able to find the 'operating system's'
     *            web browser.
     * @param style SWT constants
     */
    public FallbackBrowser(final Composite parent, final boolean displayFallbackWarning, final int style) {
        m_text = new StyledText(parent, style);

        m_shouldDisplayFallbackWarning = displayFallbackWarning;
        if (displayFallbackWarning) {
            m_styleRange = new StyleRange();
            m_styleRange.start = 0;
            m_styleRange.length = WARNING.length();
            m_styleRange.fontStyle = SWT.BOLD;

            final Color red = new Color(m_text.getDisplay(), 255, 0, 0);
            m_styleRange.foreground = red;
        } else {
            m_styleRange = null;
        }

        @SuppressWarnings("resource")
        InputStream is = null;
        Transformer transformer = null;
        try {
            is = getClass().getResourceAsStream(XSLT);
            final StreamSource stylesheet = new StreamSource(is);
            transformer = TransformerFactory.newInstance().newTemplates(stylesheet).newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes"); //$NON-NLS-1$
        } catch (TransformerConfigurationException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (TransformerFactoryConfigurationError e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) { }  // NOPMD
            }
        }

        m_transformer = transformer;
    }

    /**
     * @return the {@code StyledText} instance which this instance wraps
     */
    public StyledText getStyledText() {
        return m_text;
    }

    /**
     * Sets the visibility of the wrapped text component.
     *
     * @param visible true to make the component visible
     */
    public void setVisible(final boolean visible) {
        m_text.setVisible(visible);
    }

    /**
     * Converts HTML to plain text.
     * @param string if HTML it is converted into plain text
     */
    public void setText(final String string) {
        if (!m_text.isDisposed()) {
            m_text.setText(""); //$NON-NLS-1$

            final StringBuilder result = new StringBuilder();
            if (m_shouldDisplayFallbackWarning) {
                result.append(WARNING).append('\n');
            }
            if (string.startsWith("<")) { //$NON-NLS-1$
                result.append(convertHTML(string));
            } else {
                result.append(string);
            }
            m_text.setText(result.toString());

            if (m_shouldDisplayFallbackWarning) {
                m_text.setStyleRange(m_styleRange);
            }
        }
    }

    /**
     * Delegate to the wrapped Text component.
     *
     * @return the result of calling {@link StyledText#setFocus()} on the wrapped instance
     */
    public boolean setFocus() {
        return m_text.setFocus();
    }


    /**
     * Delegate to the wrapped Text component.
     * @return the display of the Text
     */
    public Display getDisplay() {
        return m_text.getDisplay();
    }

    /**
     *
     * @param html HTML description.
     * @return the HTML as plain text
     */
    public String convertHTML(final String html) {
        if (html == null) {
            return Messages.FallbackBrowser_7;
        }
        StreamResult result = new StreamResult(new StringWriter());
        StreamSource source = new StreamSource(new StringReader(html));
        try {
            m_transformer.transform(source, result);
        } catch (TransformerException ex) {
            LOGGER.coding(Messages.FallbackBrowser_8 + Messages.FallbackBrowser_9
                    + ex.getMessage(), ex);
            return Messages.FallbackBrowser_10 + Messages.FallbackBrowser_11
                + ex.getMessage();
        }

        String converted = result.getWriter().toString();
        if (converted.startsWith(XML_OPENING_TAG)) {
            converted = converted.substring(XML_OPENING_TAG.length());
        }
        converted = converted.trim().replaceAll("\\&lt;", "<").replaceAll("\\&gt;", ">").replaceAll("\\&amp;", "&"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

        return converted;
    }
}
