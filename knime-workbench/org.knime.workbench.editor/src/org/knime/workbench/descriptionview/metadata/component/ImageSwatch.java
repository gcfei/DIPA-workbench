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
 *   Nov 6, 2019 (loki): created
 */
package org.knime.workbench.descriptionview.metadata.component;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;

/**
 * A widget to display an image swatch, with a 'delete' icon and listener functionality for that delete click.
 *
 * @author loki der quaeler
 */
class ImageSwatch extends AbstractSwatch {
    private Image m_image;
    private boolean m_imageWasConstructedByUs;
    private Rectangle m_imageBounds;

    ImageSwatch(final Composite parent, final Listener deleteListener) {
        super(parent, deleteListener);
    }

    @Override
    void drawContent(final GC gc) {
        gc.setInterpolation(SWT.HIGH);
        gc.drawImage(m_image, 0, 0, m_imageBounds.width, m_imageBounds.height, 0, 0, SWATCH_SIZE, SWATCH_SIZE);
    }

    @Override
    boolean hasContent() {
        return (m_image != null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        disposeOfImageIfPresent();

        super.dispose();
    }

    void setImage(final ImageData newImage) {
        setImage(((newImage != null) ? new Image(getDisplay(), newImage) : null), true);
    }

    void setImage(final Image newImage) {
        setImage(newImage, false);
    }

    private void setImage(final Image newImage, final boolean selfConstructedImage) {
        disposeOfImageIfPresent();

        m_imageWasConstructedByUs = selfConstructedImage;
        final GridData gd = (GridData)getLayoutData();
        if (newImage != null) {
            m_image = newImage;
            m_imageBounds = m_image.getBounds();

            gd.exclude = false;
            setVisible(true);
        } else {
            gd.exclude = true;
            setVisible(false);
        }
        setLayoutData(gd);

        redraw();
    }

    private void disposeOfImageIfPresent() {
        if ((m_image != null) && m_imageWasConstructedByUs) {
            m_image.dispose();
            m_image = null;
        }
    }
}
