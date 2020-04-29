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
 *   19.10.2011 (Bernd Wiswedel): created
 */
package org.knime.workbench.editor2.editparts;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.knime.core.node.workflow.WorkflowCipherPrompt;
import org.knime.core.ui.util.SWTUtilities;

/**
 * Callback prompt to unlock metanode.
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public final class GUIWorkflowCipherPrompt extends WorkflowCipherPrompt {

    /** {@inheritDoc} */
    @Override
    public String prompt(final String message,
            final String errorFromPrevious) throws PromptCancelled {
        Shell shell = SWTUtilities.getActiveShell();
        String msg = message;
        if (errorFromPrevious != null) {
            msg = msg.concat("\n\n").concat(errorFromPrevious); //$NON-NLS-1$
        }
        InputDialog inputDialog = new InputDialog(shell,
                Messages.GUIWorkflowCipherPrompt_1, msg, null, null);
        if (inputDialog.open() == Window.OK) {
            return inputDialog.getValue();
        } else {
            throw new PromptCancelled();
        }
    }
}
