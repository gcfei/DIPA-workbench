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
 * Created 25 October 2011
 * Author: Peter Ohl, KNIME AG, Zurich, Switzerland
 *
 */
package org.knime.workbench.ui.layout.actions;

import org.eclipse.jface.resource.ImageDescriptor;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.actions.AbstractNodeAction;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.ui.layout.Activator;
import org.knime.workbench.ui.layout.commands.VerticAlignCommand;

/**
 * Action to trigger vertical layout.
 *
 * @author Peter Ohl, KNIME.com AG, Zurich
 */
public class VerticAlignLayoutAction extends AbstractNodeAction {

    /** unique ID for this action. */
    public static final String ID = "knime.action.verticalignlayout"; //$NON-NLS-1$

    /**
     * @param editor The workflow editor
     */
    public VerticAlignLayoutAction(final WorkflowEditor editor) {
        super(editor);
        setLazyEnablementCalculation(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getImageDescriptor(Activator.PLUGIN_ID, "icons/valign.png"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getDisabledImageDescriptor() {
        return ImageRepository.getImageDescriptor(Activator.PLUGIN_ID, "icons/valign_disabled.png"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return Messages.VerticAlignLayoutAction_3;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void runOnNodes(final NodeContainerEditPart[] parts) {
        VerticAlignCommand hac = new VerticAlignCommand(getManager(), parts);
        getCommandStack().execute(hac); // enables undo

        // update the actions
        getEditor().updateActions();

        // Give focus to the editor again. Otherwise the actions (selection)
        // is not updated correctly.
        getWorkbenchPart().getSite().getPage().activate(getWorkbenchPart());

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean internalCalculateEnabled() {
        return !getSelectedObjects().isEmpty();
    }
}
