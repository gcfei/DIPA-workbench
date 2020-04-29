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
package org.knime.workbench.ui.wfvars;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * Dialog that let the user add, edit or remove workflow variables. Existing
 * variables are listed in a {@link WorkflowVariableTable} with name, type and
 * default (current) value. Workflow variables can be added or edited with the
 * {@link WorkflowVariablesEditDialog}.
 *
 * @author Fabian Dill, KNIME.com AG
 */
public class WorkflowVariablesDialog extends Dialog {

    private static final int SKIP_RESET_IDX = 0;
    private static final int RESET_IDX = 1;

    private WorkflowVariableTable m_table;

    private final WorkflowManager m_workflow;

    private Button m_addVarBtn;

    private Button m_editVarBtn;

    private Button m_removeVarBtn;

    private Label m_warningLabel;

    private final NodeStateChangeListener m_listener;
    private boolean m_buttonsHidden = false;

    /**
     *
     * @param shell parent shell
     * @param workflow selected workflow to create the workflow variables for
     */
    public WorkflowVariablesDialog(final Shell shell,
            final WorkflowManager workflow) {
        super(shell);
        if (workflow == null) {
            throw new IllegalArgumentException(Messages.WorkflowVariablesDialog_0);
        }
        m_workflow = workflow;
        m_listener = new NodeStateChangeListener() {

            @Override
            public void stateChanged(final NodeStateEvent state) {
                final boolean inProgress = !m_workflow.getNodeContainerState().isExecutionInProgress();
                // switch to SWT thread
                Display.getDefault().asyncExec(new Runnable() {
                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    public void run() {
                        setEditable(inProgress);
                    }
                });
            }

        };
        m_workflow.addNodeStateChangeListener(m_listener);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected boolean isResizable() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public Control createDialogArea(final Composite parent) {
        return createDialogArea(parent, false);
    }

    /**
     * Creates and returns the contents of this dialog with or without edit,
     * add and remove buttons.
     * @param parent the parent composite
     * @param hideButtons true to hide the button bar, false to show it
     * @return the control
     * @since 2.6
     */
    public Control createDialogArea(final Composite parent,
            final boolean hideButtons) {
        m_buttonsHidden  = hideButtons;
        parent.getShell().setText(Messages.WorkflowVariablesDialog_1);
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        // composite contains:

        // first row (new composite):
        Composite tableAndBtnsComp = new Composite(composite, SWT.NONE);
        tableAndBtnsComp.setLayoutData(new GridData(GridData.FILL_BOTH));
        tableAndBtnsComp.setLayout(new GridLayout(2, false));
        // first column: table
        Composite tableComp = new Composite(tableAndBtnsComp, SWT.NONE);
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.grabExcessHorizontalSpace = true;
        tableComp.setLayout(new FillLayout());
        tableComp.setLayoutData(gridData);
        m_table = new WorkflowVariableTable(tableComp);
        m_table.getViewer().getTable().pack();
        for (FlowVariable var : m_workflow.getWorkflowVariables()) {
            m_table.add(var);
        }
        m_table.getViewer().refresh();
        m_table.getViewer().addDoubleClickListener(new IDoubleClickListener() {
            /**
             *
             * {@inheritDoc}
             */
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                Table table = m_table.getViewer().getTable();
                int index = table.getSelectionIndex();
                // we only get a double-click event for existing items
                FlowVariable var = m_table.get(index);
                editWorkflowVariable(var, index);
            }
        });

        if (!hideButtons) {
            // second column: 3 buttons
            Composite btnsComp = new Composite(tableAndBtnsComp, SWT.NONE);
            btnsComp.setLayout(new GridLayout(1, false));
            gridData = new GridData();
            gridData.verticalAlignment = GridData.VERTICAL_ALIGN_CENTER;
            btnsComp.setLayoutData(gridData);


            m_addVarBtn = new Button(btnsComp, SWT.PUSH);
            m_addVarBtn.setText(Messages.WorkflowVariablesDialog_2);
            m_addVarBtn.addSelectionListener(new SelectionListener() {
                @Override
                public void widgetDefaultSelected(final SelectionEvent arg0) {
                    widgetSelected(arg0);
                }

                @Override
                public void widgetSelected(final SelectionEvent arg0) {
                    addWorkflowVariable();
                }

            });
            gridData = new GridData();
            gridData.widthHint = 80;
            m_addVarBtn.setLayoutData(gridData);

            m_editVarBtn = new Button(btnsComp, SWT.PUSH);
            m_editVarBtn.setText(Messages.WorkflowVariablesDialog_3);
            m_editVarBtn.setLayoutData(gridData);
            m_editVarBtn.addSelectionListener(new SelectionListener() {

                @Override
                public void widgetDefaultSelected(final SelectionEvent arg0) {
                    widgetSelected(arg0);
                }

                @Override
                public void widgetSelected(final SelectionEvent arg0) {
                    int selectionIdx = m_table.getViewer().getTable()
                        .getSelectionIndex();
                    if (selectionIdx < 0) {
                        MessageDialog.openWarning(getShell(), Messages.WorkflowVariablesDialog_4,
                        Messages.WorkflowVariablesDialog_5);
                        return;
                    }
                    FlowVariable selectedVar = m_table.get(selectionIdx);
                    editWorkflowVariable(selectedVar, selectionIdx);
                }
            });

            m_removeVarBtn = new Button(btnsComp, SWT.PUSH);
            m_removeVarBtn.setText(Messages.WorkflowVariablesDialog_6);
            m_removeVarBtn.setLayoutData(gridData);
            m_removeVarBtn.addSelectionListener(new SelectionListener() {
                @Override
                public void widgetDefaultSelected(final SelectionEvent arg0) {
                    widgetSelected(arg0);
                }

                @Override
                public void widgetSelected(final SelectionEvent arg0) {
                    int idx = m_table.getViewer().getTable().getSelectionIndex();
                    if (idx < 0) {
                        MessageDialog.openWarning(getShell(), Messages.WorkflowVariablesDialog_7,
                                Messages.WorkflowVariablesDialog_8);
                        return;
                    }
                    FlowVariable selectedParam =
                        (FlowVariable)((IStructuredSelection)m_table
                            .getViewer().getSelection()).getFirstElement();
                    removeWorkflowVariable(selectedParam);
                }
            });

        }
        // second row: the warning label (in case the edit buttons are disabled
        // due to executing workflow...)
        m_warningLabel = new Label(composite, SWT.NONE);
        m_warningLabel.setText(""); //$NON-NLS-1$
        m_warningLabel.setForeground(Display.getDefault().getSystemColor(
                SWT.COLOR_RED));
        m_warningLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        return composite;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void create() {
        // we have to override the create contents method, since the dialog area
        // is created before the button bar at the bottom is created, where we
        // have to disable the OK button in case the workflow is running
        super.create();
        // update button state...
        setEditable(!m_workflow.getNodeContainerState().isExecutionInProgress());
    }

    private int openConfirmationDialog() {
        // if there are nodes to be reset -> ask for it
        if (m_workflow.getParent().canResetNode(m_workflow.getID())) {
            MessageDialog dialog = new MessageDialog(getShell(),
                    Messages.WorkflowVariablesDialog_10, getShell()
                            .getDisplay().getSystemImage(SWT.ICON_QUESTION),
                    Messages.WorkflowVariablesDialog_11
                            + Messages.WorkflowVariablesDialog_12
                            + Messages.WorkflowVariablesDialog_13
                            + Messages.WorkflowVariablesDialog_14,
                    MessageDialog.QUESTION, new String[]{Messages.WorkflowVariablesDialog_15, Messages.WorkflowVariablesDialog_16,
                            Messages.WorkflowVariablesDialog_17}, 1);
            return dialog.open();
        } else {
            // return "reset" -> anyway there are no executed nodes
            // (not isResetable)
            return 1;
        }
    }

    private void addWorkflowVariable() {
        WorkflowVariablesEditDialog dialog = new WorkflowVariablesEditDialog();
        if (dialog.open() == Window.CANCEL) {
            // if the user has canceled the dialog there is nothing left to do
            return;
        }
        FlowVariable var = dialog.getScopeVariable();
        if (var == null) {
            // variables was not created
            return;
        }
        // do not add it do WFM directly -> this is done when closing the dialog
        if (!m_table.add(var)) {
            MessageDialog.openWarning(getShell(), Messages.WorkflowVariablesDialog_18,
                    Messages.WorkflowVariablesDialog_19
                    + Messages.WorkflowVariablesDialog_20);
        } else {
            m_table.getViewer().refresh();
        }
        getShell().forceFocus();
    }

    private void editWorkflowVariable(final FlowVariable selectedVar,
            final int selectionIdx) {
        if (selectedVar.isGlobalConstant()) {
            MessageDialog.openError(getParentShell(), Messages.WorkflowVariablesDialog_21,
                    selectedVar.getName()
                    + Messages.WorkflowVariablesDialog_22 + Messages.WorkflowVariablesDialog_23);
            return;
        }
        WorkflowVariablesEditDialog dialog = new WorkflowVariablesEditDialog();
        dialog.create();
        dialog.loadFrom(selectedVar);
        if (dialog.open() == Window.CANCEL) {
            // if the user has canceled the dialog there is nothing left to do
            return;
        } // else replace it...
        FlowVariable var = dialog.getScopeVariable();
        if (var != null) {
            m_table.replace(selectionIdx, var);
            m_table.getViewer().refresh();
        }
    }

    private void removeWorkflowVariable(final FlowVariable var) {
        if (var.isGlobalConstant()) {
            MessageDialog.openError(getParentShell(),
                    Messages.WorkflowVariablesDialog_24, var.getName()
                            + Messages.WorkflowVariablesDialog_25
                            + Messages.WorkflowVariablesDialog_26);
            return;
        }
        // remember that something has changed
        // -> will be deleted in replaceWorkflowVariable
        // remove it from the table anyway
        m_table.remove(var);
        m_table.getViewer().refresh();
    }

    private boolean hasValueChanged(final FlowVariable newVar1,
            final FlowVariable newVar2) {
        if (newVar1.equals(newVar2)) {
            // type and name are the same -> ScopeVariable#equals
            assert newVar1.getType().equals(newVar2.getType());
            assert newVar1.getName().equals(newVar2.getName());
            // now check whether the value has changed
            // without knowing the type of the WorkflowVariable
            FlowVariable.Type type = newVar1.getType();
            if (FlowVariable.Type.STRING.equals(type)) {
                if (newVar1.getStringValue().equals(newVar2.getStringValue())) {
                    return false;
                }
            } else if (FlowVariable.Type.DOUBLE.equals(type)) {
                if (Double.compare(
                        newVar1.getDoubleValue(),
                        newVar2.getDoubleValue()) == 0) {
                    return false;
                }
            } else if (FlowVariable.Type.INTEGER.equals(type)) {
                if (newVar1.getIntValue() == newVar2.getIntValue()) {
                    return false;
                }
            } else {
                assert false : type.name() + Messages.WorkflowVariablesDialog_27;
            }
        }
        return true;
    }

    private boolean hasChanges() {
        List<FlowVariable> wfmList = m_workflow.getWorkflowVariables();
        List<FlowVariable> dialogList = m_table.getVariables();
        // different number of elements -> must contain changes
        if (wfmList.size() != dialogList.size()) {
            return true;
        }
        boolean hasChanges = false;
        for (FlowVariable v : dialogList) {
            int idx = wfmList.indexOf(v);
            if (idx >= 0) {
                hasChanges |= hasValueChanged(v, wfmList.get(idx));
            } else {
                // if a variable is in the dialog but not in workflow
                // we have some changes
                return true;
            }
        }
        return hasChanges;
    }


    private void replaceWorkflowVariables(final boolean skipReset) {
        List<FlowVariable> toBeRemoved = new ArrayList<FlowVariable>();
        toBeRemoved.addAll(m_workflow.getWorkflowVariables());
        toBeRemoved.removeAll(m_table.getVariables());
        for (FlowVariable v : toBeRemoved) {
            m_workflow.removeWorkflowVariable(v.getName());
        }
        // replace
        FlowVariable[] vars = new FlowVariable[m_table.getVariables().size()];
        m_workflow.addWorkflowVariables(skipReset,
                m_table.getVariables().toArray(vars));
    }

    /** {@inheritDoc} */
    @Override
    public void okPressed() {
        // if one or more variables were added or edited
        // first ask flag -> if true do a closer investigation...
        if (hasChanges()) {
            if (m_buttonsHidden) {
                /* Workflow variables can only be edited, not deleted or added.
                 * (Called from the execution wizard) No confirmation is
                 * necessary. */
                replaceWorkflowVariables(false);
            } else {
                // -> ask for reset confirmation
                int returnCode = openConfirmationDialog();
                // 1. skip reset -> add table content to WFM and do not reset
                // 2. reset -> add table content to WFM and do reset
                // 3. cancel -> do nothing
                if (returnCode == RESET_IDX || returnCode == SKIP_RESET_IDX) {
                    replaceWorkflowVariables(returnCode == SKIP_RESET_IDX);
                } else {
                    // CANCEL -> let dialog open
                    return;
                }
            }
        }
        // no changes -> close dialog
        super.okPressed();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void cancelPressed() {
        // if has changes -> open confirmation dialog "discard changes?"
        if (hasChanges() && !MessageDialog.openConfirm(getShell(),
                    Messages.WorkflowVariablesDialog_28,
                    Messages.WorkflowVariablesDialog_29)) {
                // leave it open
                return;
        }
        super.cancelPressed();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean close() {
        // unregister listener from workflow
        m_workflow.removeNodeStateChangeListener(m_listener);
        return super.close();
    }


    private void setEditable(final boolean isEditable) {
        Shell shell = getShell();
        if (shell == null) {
            // in case the method is called before the dialog was actually open
            return;
        }
        Button okBtn = getButton(IDialogConstants.OK_ID);
        if (okBtn != null && !okBtn.isDisposed()) {
            okBtn.setEnabled(isEditable);
        }
        if (m_addVarBtn != null && !m_addVarBtn.isDisposed()) {
            m_addVarBtn.setEnabled(isEditable);
        }
        if (m_editVarBtn != null && !m_editVarBtn.isDisposed()) {
            m_editVarBtn.setEnabled(isEditable);
        }
        if (m_removeVarBtn != null && !m_removeVarBtn.isDisposed()) {
            m_removeVarBtn.setEnabled(isEditable);
        }
        // set message if not editable
        if (m_warningLabel != null && !m_warningLabel.isDisposed()) {
            if (!isEditable) {
                m_warningLabel.setText(Messages.WorkflowVariablesDialog_30
                        + Messages.WorkflowVariablesDialog_31);
            } else {
                // reset warning
                m_warningLabel.setText(""); //$NON-NLS-1$
            }
        }
        getShell().redraw();
    }

    /**
     * Return list of flow variables shown in this dialog.
     * @return list of flow variables
     * @since 2.6
     */
    public List<FlowVariable> getVariables() {
        if (m_table == null) {
            return Collections.emptyList();
        }
        return m_table.getVariables();
    }

}
