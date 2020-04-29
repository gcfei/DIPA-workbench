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
 *   14.01.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.meta;


import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.core.node.port.database.DatabaseConnectionPortObject;
import org.knime.core.node.port.database.DatabasePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.pmml.PMMLPortObject;

/**
 * Dialog to enter the port name and type.
 *
 * @author Fabian Dill, University of Konstanz
 */
public class MetaPortDialog extends Dialog {

    private Shell m_shell;
    private Label m_error;
    private Label m_typeLabel;
    private Combo m_type;

    private PortType m_port = null;
    private final Predicate<PortType> m_acceptsPort;

    private static final Comparator<PortType> PORT_TYPE_COMPARATOR = new Comparator<PortType>() {
        @Override
        public int compare(final PortType o1, final PortType o2) {
            if (o1.equals(o2)) {
                return 0;
            } else if (o1.equals(BufferedDataTable.TYPE)) {
                return -1;
            } else if (o2.equals(BufferedDataTable.TYPE)) {
                return 1;
            } else if (o1.equals(FlowVariablePortObject.TYPE)) {
                return -1;
            } else if (o2.equals(FlowVariablePortObject.TYPE)) {
                return 1;
            } else if (o1.equals(PMMLPortObject.TYPE)) {
                return -1;
            } else if (o2.equals(PMMLPortObject.TYPE)) {
                return 1;
            } else if (o1.equals(DatabaseConnectionPortObject.TYPE)) {
                return -1;
            } else if (o2.equals(DatabaseConnectionPortObject.TYPE)) {
                return 1;
            } else if (o1.equals(DatabasePortObject.TYPE)) {
                return -1;
            } else if (o2.equals(DatabasePortObject.TYPE)) {
                return 1;
            } else {
                return o1.getName().compareTo(o2.getName());
            }
        }
    };

    private static final List<PortType> PORT_TYPES = PortTypeRegistry.getInstance().availablePortTypes().stream()
        .filter(pt -> !pt.isHidden()).sorted(PORT_TYPE_COMPARATOR).collect(Collectors.toList());

    /**
     *
     * @param parent the parent
     */
    public MetaPortDialog(final Shell parent) {
        this(parent, Messages.MetaPortDialog_0, p -> true);
    }

    /**
     * Constructor.
     *
     * @param parent the parent shell
     * @param title the dialog's title
     * @param supportedPorts supported port types
     */
    public MetaPortDialog(final Shell parent, final String title, final PortType[] supportedPorts) {
        this(parent,title, p1 -> Arrays.stream(supportedPorts).anyMatch(p2 -> p1.equals(p2)));
    }


    /**
     * Constructor.
     *
     * @param parent the parent shell
     * @param title the dialog's title
     * @param acceptsPorts predicate defining the supported port types
     */
    private MetaPortDialog(final Shell parent, final String title, final Predicate<PortType> acceptsPorts) {
        super(parent);
        setText(title);
        m_acceptsPort = acceptsPorts;
    }

    /**
     * Opens the dialog and returns the entered port (with name and type).
     *
     * @return the entered port (with name and type)
     */
    public PortType open() {
        //create shell and contents
        Shell parent = getParent();
        m_shell = new Shell(parent, SWT.DIALOG_TRIM
                | SWT.APPLICATION_MODAL);
        m_shell.setText(getText());
        m_shell.setLayout(new FillLayout());
        createControl(m_shell);

        //calculate size and set location
        Point location = parent.getLocation();
        Point parentSize = parent.getSize();
        Point shellSize = m_shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        int shellWidth = shellSize.x;
        int shellHeight = shellSize.y;
        m_shell.setLocation(location.x + (parentSize.x / 2) - (shellWidth / 2),
            location.y + (parentSize.y / 2) - (shellHeight / 2));
        m_shell.setSize(shellWidth, shellHeight);

        m_shell.open();
        Display display = parent.getDisplay();
        while (!m_shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        return m_port;
    }

    private void createControl(final Shell parent) {
        Composite composite = new Composite(parent, SWT.BORDER);
        composite.setLayout(new GridLayout(2, false));
        m_error = new Label(composite, SWT.NONE);
        GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL
                | GridData.GRAB_HORIZONTAL);
        gridData.horizontalSpan = 2;
        m_error.setLayoutData(gridData);

        m_typeLabel = new Label(composite, SWT.NONE);
        m_typeLabel.setText(Messages.MetaPortDialog_1);
        m_type = new Combo(composite,
                SWT.DROP_DOWN | SWT.SIMPLE | SWT.READ_ONLY | SWT.BORDER);

        final String[] names = PORT_TYPES.stream()//
            .filter(m_acceptsPort)//
            .map(PortType::getName).toArray(String[]::new);

        m_type.setItems(names);
        m_type.select(0);
        m_type.addFocusListener(new FocusAdapter() {

            @Override
            public void focusGained(final FocusEvent e) {
                resetError();
            }

        });
        Button ok = new Button(composite, SWT.PUSH);
        ok.setText(Messages.MetaPortDialog_2);
        ok.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }
            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (m_type.getSelectionIndex() < 0) {
                    setError(Messages.MetaPortDialog_3);
                    return;
                }
                resetError();
                String selected = m_type.getItem(m_type.getSelectionIndex());

                Optional<PortType> pt = PORT_TYPES.stream().filter(p -> p.getName().equals(selected)).findFirst();
                m_port = pt.orElseThrow(() -> new IllegalStateException(Messages.MetaPortDialog_4 + selected));
                m_shell.dispose();
            }

        });
        gridData = new GridData();
        gridData.widthHint = 80;
        ok.setLayoutData(gridData);

        Button cancel = new Button(composite, SWT.PUSH);
        cancel.setText(Messages.MetaPortDialog_5);
        cancel.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }

            @Override
            public void widgetSelected(final SelectionEvent e) {
                m_port = null;
                m_shell.dispose();
            }

        });
        gridData = new GridData();
        gridData.widthHint = 80;
        cancel.setLayoutData(gridData);
    }


    private void setError(final String error) {
        m_error.setForeground(ColorConstants.red);
        m_error.setText(error);
    }

    private void resetError() {
        m_error.setText(""); //$NON-NLS-1$
    }
}
