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
 *   17.01.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2;

import java.net.URI;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class WorkflowManagerInput implements IEditorInput {
    private final WorkflowManagerUI m_manager;
    private final WorkflowEditor m_parent;
    private final URI m_workflowLocation;

    public WorkflowManagerInput(final WorkflowManagerUI manager, final WorkflowEditor parent) {
        m_manager = manager;
        m_parent = parent;
        m_workflowLocation = null;
    }

    public WorkflowManagerInput(final WorkflowManagerUI manager, final URI workflowLocation) {
        m_manager = manager;
        m_parent = null;
        m_workflowLocation = workflowLocation;
    }

    public WorkflowManagerUI getWorkflowManager() {
        return m_manager;
    }

    public WorkflowEditor getParentEditor() {
        return m_parent;
    }

    public URI getWorkflowLocation() {
        return m_workflowLocation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists() {
        return m_manager != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
         return m_manager.getDisplayLabel();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public IPersistableElement getPersistable() {
        // TODO: what if?
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
       return m_manager.getName() + "(not persisted yet)";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getAdapter(final Class adapter) {
        return null;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof WorkflowManagerInput)) {
            return false;
        }
        WorkflowManagerInput in = (WorkflowManagerInput)obj;
        return m_manager.equals(in.getWorkflowManager())
            || ((m_workflowLocation != null) && m_workflowLocation.equals(in.m_workflowLocation));
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_manager.hashCode();
    }
}
