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
package org.knime.workbench.ui.navigator.actions.selection;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.dialogs.ISelectionValidator;

/**
 *
 * @author ohl, KNIME AG, Zurich, Switzerland
 */
public class TreeSelectionControl {

    private String m_message;

    private ISelectionValidator m_validator;

    private TreeSelectionChangeListener m_changeListener;

    private ITreeContentProvider m_contentProvider;

    private LabelProvider m_labelProvider;

    private Object m_root;

    private ISelection m_initialSelection;

    private TreeViewer m_treeViewer = null;

    private Label m_errMsg;

    private ViewerComparator m_comparator;

    private ViewerFilter m_filter;

    /**
     * Sets the message displayed above the selection tree. Has no effect, after
     * the component is created.
     *
     * @param message
     */
    public void setMessage(final String message) {
        m_message = message;
    }

    public void setValidator(final ISelectionValidator validator) {
        m_validator = validator;
    }

    public void setContentProvider(final ITreeContentProvider contentProvider) {
        m_contentProvider = contentProvider;
    }

    public void setLabelProvider(final LabelProvider labelProvider) {
        m_labelProvider = labelProvider;
    }

    public void setInitialSelection(final ISelection initialSelection) {
        m_initialSelection = initialSelection;
    }

    public void setComparator(final ViewerComparator comp) {
        m_comparator = comp;
    }
    public void addFilter(final ViewerFilter filter) {
        m_filter = filter;
    }
    /**
     * Notified when a new object in the tree is selected. Notified after the
     * validator.
     *
     * @param changeListener
     */
    public void setChangeListener(
            final TreeSelectionChangeListener changeListener) {
        m_changeListener = changeListener;
    }

    /**
     * Children of the parameter are displayed and selectable in the tree.
     * Object type must fit the content provider.
     *
     * @param treeRoot the children of it will be displayed and selectable
     */
    public void setInput(final Object treeRoot) {
        m_root = treeRoot;
    }

    public Control createTreeControl(final Composite parent) {

        Composite overall = new Composite(parent, SWT.NONE);
        GridData fillBoth = new GridData(GridData.FILL_BOTH);
        overall.setLayoutData(fillBoth);
        overall.setLayout(new GridLayout(1, true));

        if (m_message != null && !m_message.isEmpty()) {
            createMessagePanel(overall);
        }
        m_treeViewer =
                new TreeViewer(overall, SWT.BORDER | SWT.H_SCROLL
                        | SWT.V_SCROLL | SWT.SINGLE | SWT.FILL);
        m_treeViewer.getTree().setLayoutData(fillBoth);
        createErrorPanel(overall);
        if (m_filter != null) {
            m_treeViewer.setExpandPreCheckFilters(true);
            m_treeViewer.addFilter(m_filter);
        }
        if (m_contentProvider != null) {
            m_treeViewer.setContentProvider(m_contentProvider);
        }
        if (m_labelProvider != null) {
            m_treeViewer.setLabelProvider(m_labelProvider);
        }
        if (m_comparator != null) {
            m_treeViewer.setComparator(m_comparator);
        }
        m_treeViewer.setInput(m_root);
        if (m_initialSelection != null) {
            m_treeViewer.setSelection(m_initialSelection, true);
            if (m_initialSelection instanceof StructuredSelection) {
                if (((StructuredSelection)m_initialSelection).size() == 1) {
                    m_treeViewer.expandToLevel(((StructuredSelection)m_initialSelection).getFirstElement(), 1);
                }
            }
            treeSelectionChanged(getSelectedTreeObject());
        }
        m_treeViewer
                .addSelectionChangedListener(new ISelectionChangedListener() {
                    @Override
                    public void selectionChanged(
                            final SelectionChangedEvent event) {
                        Object sel = getSelectedTreeObject();
                        boolean v = treeSelectionChanged(sel);
                        if (m_changeListener != null) {
                            m_changeListener.treeSelectionChanged(sel, v);
                        }
                    }

                });

        m_treeViewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                ISelection selection = event.getSelection();
                if (selection instanceof IStructuredSelection && ((IStructuredSelection)selection).size() == 1) {
                    Object selectionObj = ((IStructuredSelection)selection).getFirstElement();
                    m_treeViewer.setExpandedState(selectionObj, !m_treeViewer.getExpandedState(selectionObj));
                }
            }
        });

        return overall;
    }

    public Composite createMessagePanel(final Composite parent) {
        Composite msgPanel = new Composite(parent, SWT.FILL);
        GridData leftTop = new GridData(GridData.FILL_HORIZONTAL);
        msgPanel.setLayoutData(leftTop);
        msgPanel.setLayout(new GridLayout(1, false));

        Label msg = new Label(msgPanel, SWT.LEFT);
        msg.setText(m_message);
        msg.setLayoutData(leftTop);
        return msgPanel;
    }

    protected Composite createErrorPanel(final Composite parent) {
        Composite errPanel = new Composite(parent, SWT.NONE);
        GridData leftTop = new GridData(GridData.FILL_HORIZONTAL);
        errPanel.setLayoutData(leftTop);
        errPanel.setLayout(new GridLayout(1, false));
        m_errMsg = new Label(errPanel, SWT.NONE);
        m_errMsg.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        m_errMsg.setText("");
        m_errMsg.setForeground(Display.getDefault().getSystemColor(
                SWT.COLOR_RED));
        return errPanel;
    }

    protected void setErrMsg(final String msg) {
        m_errMsg.setText(msg);
        m_errMsg.redraw();
    }

    protected boolean treeSelectionChanged(final Object newSelection) {
        boolean valid = true;
        String msg = "";
        if (m_validator != null) {
            msg = m_validator.isValid(newSelection);
            if (msg != null) {
                valid = false;
            } else {
                valid = true;
                msg = "";
            }
        }
        setErrMsg(msg);
        return valid;
    }

    /**
     * Path to the selected element. Segments of the path are the labels of the
     * parents of the selected element (while the last segment is the selected
     * itself). If no label provider is set, the segments are the toString
     * result of the elements.
     *
     * @return
     */
    public IPath getTreeSelectionPath() {
        return getTreeSelectionPath(getSelectedTreeObject());
    }

    private IPath getTreeSelectionPath(final Object selection) {
        if (selection == null) {
            return null;
        }
        IPath p = new Path("/");
        Object o = selection;
        while (o != null && !o.equals(m_root)) {
            String segm;
            if (m_labelProvider != null) {
                segm = m_labelProvider.getText(o);
            } else {
                segm = o.toString();
            }
            p = p.append(segm);
            if (m_contentProvider != null) {
                o = m_contentProvider.getParent(o);
            } else {
                o = null;
            }
        }
        return p;
    }

    protected void setTreeSelection(final ISelection selection) {
        if (m_treeViewer != null) {
            m_treeViewer.setSelection(selection);
        }
    }

    public ISelection getTreeSelection() {
        if (m_treeViewer == null) {
            return null;
        }
        return m_treeViewer.getSelection();
    }

    public Object getSelectedTreeObject() {
        if (m_treeViewer != null) {
            IStructuredSelection selection =
                    (IStructuredSelection)m_treeViewer.getSelection();
            if (selection != null && !selection.isEmpty()) {
                return selection.getFirstElement();
            }
        }
        return null;
    }

    /**
     * Refreshes the whole tree view.
     */
    public void refresh() {
        if (!m_treeViewer.getTree().isDisposed()) {
            m_treeViewer.refresh();
        }
    }

    /**
     * Returns the underlying tree.
     *
     * @return the tree
     */
    public Tree getTree() {
        return m_treeViewer.getTree();
    }

    public interface TreeSelectionChangeListener {
        void treeSelectionChanged(final Object newSelection, final boolean valid);
    }

}
