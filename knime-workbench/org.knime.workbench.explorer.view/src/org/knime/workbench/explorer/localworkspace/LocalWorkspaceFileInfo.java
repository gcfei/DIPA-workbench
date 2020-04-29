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
package org.knime.workbench.explorer.localworkspace;

import java.io.File;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.FileSingleNodeContainerPersistor;
import org.knime.core.node.workflow.MetaNodeTemplateInformation;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.util.workflowalizer.TemplateMetadata;
import org.knime.core.util.workflowalizer.Workflowalizer;
import org.knime.core.util.workflowalizer.WorkflowalizerConfiguration;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileInfo;


public class LocalWorkspaceFileInfo extends AbstractExplorerFileInfo {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(LocalWorkspaceFileInfo.class);

    private final IFileStore m_file;

    //caches the isComponent flag
    private Boolean m_isComponent = null;

    /**
     * @param file The file store this file info belongs to
     */
    LocalWorkspaceFileInfo(final IFileStore file) {
        super(file.getName());
        m_file = file;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists() {
        return m_file.fetchInfo().exists();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirectory() {
        return m_file.fetchInfo().isDirectory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLastModified() {
        return m_file.fetchInfo().getLastModified();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLength() {
       return m_file.fetchInfo().getLength();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getAttribute(final int attribute) {
        return m_file.fetchInfo().getAttribute(attribute);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setAttribute(final int attribute, final boolean value) {
        throw new UnsupportedOperationException();
    }

    /**
     * @return true if the file represents a workflow, false otherwise
     */
    @Override
    public boolean isWorkflow() {
        return exists() && isWorkflow(m_file);
    }

    /**
     * @return true if the file represents a workflow group, false otherwise
     */
    @Override
    public boolean isWorkflowGroup() {
        return exists() && isWorkflowGroup(m_file);
    }

    /**
     * @return the isWorkflowTemplate
     */
    @Override
    public boolean isWorkflowTemplate() {
        return exists() && isWorkflowTemplate(m_file);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isComponentTemplate() {
        return exists() && isWorkflowTemplate() && isComponentTemplate(m_file);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMetaNodeTemplate() {
        return exists() && isWorkflowTemplate() && !isComponentTemplate(m_file);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNode() {
        return exists() && isNode(m_file);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFile() {
        return exists() && isDataFile(m_file);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReservedSystemItem() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMetaNode() {
        return exists() && isMetaNode(m_file);
    }

    private static boolean isWorkflow(final IFileStore file) {
        if (file == null || !file.fetchInfo().exists()) {
            return false;
        }
        IFileStore tmplateFile = file.getChild(WorkflowPersistor.TEMPLATE_FILE); // metanode, no workflow
        if (tmplateFile.fetchInfo().exists()) {
            return false;
        }

        IFileStore wfFile = file.getChild(WorkflowPersistor.WORKFLOW_FILE); // no workflow at all
        if (!wfFile.fetchInfo().exists()) {
            return false;
        }

        IFileStore parentFile = file.getParent();
        if (parentFile == null) {
            return false;
        }
        IFileStore parentWfFile = parentFile.getChild(WorkflowPersistor.WORKFLOW_FILE); // metanode inside a workflow
        return !parentWfFile.fetchInfo().exists();
    }

    private static boolean isWorkflowGroup(final IFileStore file) {
        if (file == null || !file.fetchInfo().exists()) {
            return false;
        }
        return file.fetchInfo().isDirectory() && !isWorkflow(file)
                && !isMetaNode(file) && !isNode(file)
                && !isWorkflowTemplate(file);
    }

    private static boolean isWorkflowTemplate(final IFileStore file) {
        if (file == null || !file.fetchInfo().exists()) {
            return false;
        }
        IFileStore templateFile = file.getChild(
                WorkflowPersistor.TEMPLATE_FILE);
        return templateFile.fetchInfo().exists();
    }

    private boolean isComponentTemplate(final IFileStore file) {
        if (m_isComponent == null) {
            IFileStore templateFile = file.getChild(WorkflowPersistor.TEMPLATE_FILE);
            try {
                TemplateMetadata templateMetadata =
                    Workflowalizer.readTemplate(templateFile.getParent().toLocalFile(EFS.NONE, null).toPath(),
                        WorkflowalizerConfiguration.builder().readWorkflowMeta().build());
                m_isComponent = templateMetadata.getTemplateInformation().getType()
                    .equals(MetaNodeTemplateInformation.TemplateType.SubNode.toString());
            } catch (Exception e) {
                LOGGER.error(Messages.LocalWorkspaceFileInfo_0, e);
                throw new IllegalStateException(Messages.LocalWorkspaceFileInfo_1, e);
            }
        }
        return m_isComponent;
    }

    private static boolean isMetaNode(final IFileStore file) {
        if (file == null || !file.fetchInfo().exists()) {
            return false;
        }
        IFileStore wfFile = file.getChild(WorkflowPersistor.WORKFLOW_FILE);
        IFileStore parentFile = file.getParent();
        if (parentFile == null) {
            return false;
        }
        IFileStore parentWfFile = parentFile.getChild(
                WorkflowPersistor.WORKFLOW_FILE);
        return wfFile.fetchInfo().exists() && parentWfFile.fetchInfo().exists();
    }

    private static boolean isNode(final IFileStore file) {
        if (file == null || !file.fetchInfo().exists() || isMetaNode(file)) {
            return false;
        }
        IFileStore containerFile = file.getChild(
                FileSingleNodeContainerPersistor.SETTINGS_FILE_NAME);
        return containerFile.fetchInfo().exists()
                && isWorkflow(file.getParent());
    }

    private static boolean isDataFile(final IFileStore file) {
        if (file == null) {
            return false;
        }
        final IFileInfo fileInfo = file.fetchInfo();
        return fileInfo.exists() && !fileInfo.isDirectory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isModifiable() {
        try {
            File f = m_file.toLocalFile(EFS.NONE, null);
            return f.canRead() && f.canWrite();
        } catch (CoreException ex) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReadable() {
        try {
            return m_file.toLocalFile(EFS.NONE, null).canRead();
        } catch (CoreException ex) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSnapshot() {
        return false;
    }
}
