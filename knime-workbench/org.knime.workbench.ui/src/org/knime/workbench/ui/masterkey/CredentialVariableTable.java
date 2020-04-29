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
 */
package org.knime.workbench.ui.masterkey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.knime.core.node.workflow.Credentials;
import org.knime.core.node.workflow.FlowVariable;

/**
 *
 * @author Thomas Gabriel, KNIME.com AG
 */
public class CredentialVariableTable implements Iterable<Credentials> {

    private static final String VAR_NAME = "Name"; //$NON-NLS-1$
    private static final String VAR_LOGIN = "Login"; //$NON-NLS-1$
    private static final String VAR_PASSWORD = "Password"; //$NON-NLS-1$

    private final List<Credentials> m_params
        = new ArrayList<Credentials>();

    private final TableViewer m_viewer;

    /**
     *
     * @param parent parent composite
     */
    public CredentialVariableTable(final Composite parent) {
        m_viewer = createViewer(parent);
        m_viewer.setInput(m_params);
    }

    /**
     *
     * @param cred workflow credentials
     * @return true if the variable was added, false if there was already a
     *  variable with same name and type
     *
     */
    public boolean add(final Credentials cred) {
        if (m_params.contains(cred)) {
            return false;
        }
        m_params.add(cred);
        return true;
    }

    /** Removes the given credential.
     * @param credential to be removed from the table
     */
    public void remove(final Credentials credential) {
        m_params.remove(credential);
    }

    /**
     *
     * @param idx index of the variable
     * @return the variable
     */
    public Credentials get(final int idx) {
        return m_params.get(idx);
    }


    /**
     *
     * @return an unmodifiable list of the represented {@link FlowVariable}s.
     */
    public List<Credentials> getCredentials() {
        return Collections.unmodifiableList(m_params);
    }

    /**
     * Replaces the variable at the given index with the new one.
     * @param index index of variable to be replaced (starts at 0)
     * @param credential which should be inserted at position index
     */
    public void replace(final int index, final Credentials credential) {
        m_params.set(index, credential);
    }

    /**
     *
     * @return the underlying {@link TableViewer}
     */
    public TableViewer getViewer() {
        return m_viewer;
    }

    private TableViewer createViewer(final Composite parent) {
        TableViewer viewer = new TableViewer(parent, SWT.SINGLE
                | SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL
                | SWT.V_SCROLL);

        TableViewerColumn nameCol = new TableViewerColumn(viewer, SWT.NONE);
        nameCol.getColumn().setText(VAR_NAME);
        nameCol.getColumn().setWidth(100);

        TableViewerColumn loginCol = new TableViewerColumn(viewer, SWT.NONE);
        loginCol.getColumn().setText(VAR_LOGIN);
        loginCol.getColumn().setWidth(100);

        TableViewerColumn typeCol = new TableViewerColumn(viewer, SWT.NONE);
        typeCol.getColumn().setText(VAR_PASSWORD);
        typeCol.getColumn().setWidth(100);

        viewer.getTable().setHeaderVisible(true);
        viewer.getTable().setLinesVisible(true);

        viewer.setContentProvider(new IStructuredContentProvider() {
            @Override
            public Object[] getElements(final Object arg) {
                return ((List<Credentials>) arg).toArray();
            }
            @Override
            public void dispose() {
                // do nothing -> images are static
            }
            @Override
            public void inputChanged(final Viewer arg0, final Object arg1,
                    final Object arg2) {
                // do nothing
            }
        });
        viewer.setLabelProvider(new WorkflowVariableLabelProvider());

        viewer.setInput(m_params);

        return viewer;
    }

    private static class WorkflowVariableLabelProvider
            extends LabelProvider implements ITableLabelProvider {

        @Override
        public Image getColumnImage(final Object arg0, final int arg1) {
            return null;
        }

        @Override
        public String getColumnText(final Object arg0, final int arg1) {
            Credentials cred = (Credentials) arg0;
            switch (arg1) {
            case 0: return cred.getName();
            case 1: return cred.getLogin();
            case 2:
                String passString = cred.getPassword();
                if (passString == null || passString.isEmpty()) {
                    return "<not set>"; //$NON-NLS-1$
                }
                StringBuilder buf = new StringBuilder();
                for (int i = 0;
                        i < Integer.bitCount(passString.hashCode()); i++) {
                    buf.append('*');
                }
                return buf.toString();
            default: throw new IllegalArgumentException(
                    Messages.CredentialVariableTable_4 + arg1);
            }
        }

    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Iterator<Credentials> iterator() {
        return m_params.iterator();
    }
}
