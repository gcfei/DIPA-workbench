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
 * -------------------------------------------------------------------
 *
 * History
 *   09.02.2005 (georg): created
 */
package org.knime.workbench.ui.wrapper;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.event.FocusListener;

import javax.swing.BoxLayout;
import javax.swing.JComponent;

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.knime.core.node.util.ViewUtils;

/**
 * Wrapper Composite that uses the SWT_AWT bridge to embed an AWT Panel into a SWT composite.
 *
 * @author Florian Georg, University of Konstanz
 */
public class Panel2CompositeWrapper extends Composite {
    /**
     * We track the root frame we create so that it can be handed off, on Mac platforms, to our
     *  key-tracking-instrumenter.
     */
    protected Frame m_rootFrame;

    /**
     * Creates a new wrapper.
     *
     * @param parent The parent composite
     * @param panel The AWT panel to wrap
     * @param style Style bits, ignored so far
     */
    public Panel2CompositeWrapper(final Composite parent, final JComponent panel, final int style) {
        super(parent, style | SWT.EMBEDDED);


        final GridLayout gridLayout = new GridLayout();
        gridLayout.verticalSpacing = 0;
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        gridLayout.horizontalSpacing = 0;
        setLayout(gridLayout);
        setLayoutData(new GridData(GridData.FILL_BOTH));

        m_rootFrame = SWT_AWT.new_Frame(this);
        // The lightweight Swing panel must be wrapped into a heavyweight AWT container-panel
        // otherwise the cursor changes when moving over a Swing component won't work correctly.
        // bug 6516
        final Panel heavyweightPanel = new Panel();
        heavyweightPanel.setLayout(new BoxLayout(heavyweightPanel, BoxLayout.LINE_AXIS));
        heavyweightPanel.setBackground(null);
        // use heavyweight panel as root
        heavyweightPanel.add(panel);
        m_rootFrame.add(heavyweightPanel);


        // on some Linux systems tabs are not properly passed to AWT without the next line
        if (Platform.OS_LINUX.equals(Platform.getOS())) {
            m_rootFrame.setFocusTraversalKeysEnabled(false);
        }

        // the size of the composite is 0x0 at this point. The above SWT_AWT.newFrame() determines the client
        // size BEFORE this constructor completes (via ViewUtils#invokeAndWaitInEDT - see below);
        // setting a dimension here in order to have reasonable defaults.
        // see bug 4431 (and the original bug 4418)
        Dimension size = panel.getPreferredSize();
        setSize(size.width, size.height);

        addFocusListener(new FocusAdapter() {
            /**
             * {@inheritDoc}
             * @param e focus event passed to the underlying AWT component
             */
            @Override
            public void focusGained(final FocusEvent e) {
                ViewUtils.runOrInvokeLaterInEDT(() -> {
                    panel.requestFocus();
                });
            }
        });

        // Bug 6275: Use a focus listener to check if wrapper component, AWT Frame and dialog panel
        // have the same size. The asynchronous queuing in SWT_AWT.new_Frame() leads to sizes not being correctly
        // set on the AWT components sometimes at this point. The following code corrects this.
        // Note that this only works in conjunction with "resizeHasOccurred = true; getShell().layout();
        // in WrappedNodeDialog (lines 271 following), otherwise the size of the wrapper component (this) is also not correct.
        final FocusListener sizeFocusListener = new FocusListener() {
            /** {@inheritDoc} */
            @Override
            public void focusLost(final java.awt.event.FocusEvent e) { /* do nothing */ }

            /** {@inheritDoc} */
            @Override
            public void focusGained(final java.awt.event.FocusEvent e) {
                final Dimension panelSize = heavyweightPanel.getSize();
                if (!isDisposed()) {
                    getDisplay().asyncExec(() -> {
                        if (!isDisposed()) {
                            final Point wrapperSize = getSize();
                            // Check if size of this component is the same as the panel
                            if (panelSize.width != wrapperSize.x || panelSize.height != wrapperSize.y) {
                                ViewUtils.invokeLaterInEDT(() -> {
                                    // Set sizes on AWT frame and dialog panel if different from
                                    // wrapper component
                                    m_rootFrame.setSize(wrapperSize.x, wrapperSize.y);
                                    heavyweightPanel.setSize(wrapperSize.x, wrapperSize.y);
                                    panel.setSize(wrapperSize.x, wrapperSize.y);
                                });
                            }
                        }
                    });
                }
            }
        };

        panel.addFocusListener(sizeFocusListener);

        // We reuse the panel on subsequent dialog opens but always create a new wrapper component.
        // Therefore we need to remove the focus listener on the dialog panel when the wrapper is disposed.
        addDisposeListener(new DisposeListener() {
            /** {@inheritDoc} */
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                panel.removeFocusListener(sizeFocusListener);
            }
        });
    }

    Frame getRootFrame () {
        return m_rootFrame;
    }
}
