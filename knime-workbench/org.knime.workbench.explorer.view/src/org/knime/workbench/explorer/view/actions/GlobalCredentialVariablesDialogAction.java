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

package org.knime.workbench.explorer.view.actions;

import java.util.List;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.Credentials;
import org.knime.core.node.workflow.CredentialsStore;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystemUtils;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.view.ExplorerView;
import org.knime.workbench.explorer.view.dnd.DragAndDropUtils;
import org.knime.workbench.ui.masterkey.CredentialVariablesDialog;

/**
 *
 * @author Dominik Morent, KNIME AG, Zurich, Switzerland
 *
 */
public class GlobalCredentialVariablesDialogAction extends ExplorerAction {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(GlobalCredentialVariablesDialogAction.class);

    /** ID of the global rename action in the explorer menu. */
    public static final String CREDENTIAL_ACTION_ID =
            "org.knime.workbench.explorer.action.credentials-dialog"; //$NON-NLS-1$

    /**
     * @param viewer the associated tree viewer
     */
    public GlobalCredentialVariablesDialogAction(final ExplorerView viewer) {
        super(viewer, Messages.getString("GlobalCredentialVariablesDialogAction.1")); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return CREDENTIAL_ACTION_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        List<AbstractExplorerFileStore> fileStores =
                DragAndDropUtils.getExplorerFileStores(getSelection());
        AbstractExplorerFileStore wfStore = fileStores.get(0);
        if (!(wfStore instanceof LocalExplorerFileStore)) {
            LOGGER.error(Messages.getString("GlobalCredentialVariablesDialogAction.2")); //$NON-NLS-1$
            return;
        }
        WorkflowManager workflow = getWorkflow();

        if (ExplorerFileSystemUtils
                .lockWorkflow((LocalExplorerFileStore)wfStore)) {
            try {
                showDialog(workflow);
            } finally {
                ExplorerFileSystemUtils
                        .unlockWorkflow((LocalExplorerFileStore)wfStore);
            }
        } else {
            LOGGER.info(Messages.getString("GlobalCredentialVariablesDialogAction.3") //$NON-NLS-1$
                    + Messages.getString("GlobalCredentialVariablesDialogAction.4")); //$NON-NLS-1$
            showCantEditCredentialsLockMessage();
        }

    }

    private void showDialog(final WorkflowManager wfm) {
        // open the dialog
        final Display d = Display.getDefault();
        // run in UI thread
        d.asyncExec(new Runnable() {
            @Override
            public void run() {
                CredentialsStore store = wfm.getCredentialsStore();
                CredentialVariablesDialog dialog =
                        new CredentialVariablesDialog(d.getActiveShell(),
                                store, wfm.getName());
                if (dialog.open() == Window.OK) {
                    List<Credentials> credentials = dialog.getCredentials();
                    wfm.updateCredentials(credentials.toArray(new Credentials[0]));
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return getWorkflow() != null;
    }

    private void showCantEditCredentialsLockMessage() {
        MessageBox mb =
                new MessageBox(getParentShell(), SWT.ICON_ERROR | SWT.OK);
        mb.setText(Messages.getString("GlobalCredentialVariablesDialogAction.5")); //$NON-NLS-1$
        mb.setMessage(Messages.getString("GlobalCredentialVariablesDialogAction.6") //$NON-NLS-1$
                + Messages.getString("GlobalCredentialVariablesDialogAction.7")); //$NON-NLS-1$
        mb.open();
    }
}
