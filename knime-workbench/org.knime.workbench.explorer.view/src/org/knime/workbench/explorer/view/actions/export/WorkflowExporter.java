/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Apr 29, 2017 (wiswedel): created
 */
package org.knime.workbench.explorer.view.actions.export;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.knime.core.node.FileNodePersistor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodePersistor;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.util.KnimeFileUtil;
import org.knime.core.util.VMFileLocker;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;

/**
 * Encapsulates the logic of the actual 'export'; it does not do any prompting etc.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public final class WorkflowExporter {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(WorkflowExporter.class);

    private final Collection<AbstractExplorerFileStore> m_elementsToExport;

    private final boolean m_excludeData;

    private final AbstractExplorerFileStore m_commonParent;

    private final File m_exportFile;

    /**
     * @param exportFile
     * @param commonParent
     * @param elementsToExport
     * @param excludeData
     */
    public WorkflowExporter(final File exportFile, final AbstractExplorerFileStore commonParent,
        final Collection<AbstractExplorerFileStore> elementsToExport, final boolean excludeData) {
        m_commonParent = CheckUtils.checkArgumentNotNull(commonParent);
        m_exportFile = CheckUtils.checkArgumentNotNull(exportFile);
        m_excludeData = CheckUtils.checkArgumentNotNull(excludeData);
        m_elementsToExport = CheckUtils.checkArgumentNotNull(elementsToExport);
    }

    /**
     * The worker method. It will find the container, create the export file if missing or just replace its contents.
     */
    public void doFinish(final IProgressMonitor monitor) throws CoreException {

        // start zipping
        monitor.beginTask(Messages.getString("WorkflowExporter.0"), 10); //$NON-NLS-1$
        // if the data should be excluded from the export
        // iterate over the resources and add only the wanted stuff
        // i.e. the "intern" folder and "*.zip" files are excluded
        final List<File> resourceList = new ArrayList<File>();
        for (AbstractExplorerFileStore fs : m_elementsToExport) {
            // add all files within the workflow or group
            addResourcesFor(resourceList, fs, m_excludeData);
        }

        monitor.worked(1); // 10% for collecting the files...
        try {
            SubProgressMonitor sub = new SubProgressMonitor(monitor, 9);

            File parentLoc = m_commonParent.toLocalFile();
            if (parentLoc == null) {
                throw new CoreException(new Status(IStatus.ERROR, ExplorerActivator.PLUGIN_ID,
                    Messages.getString("WorkflowExporter.1") + m_commonParent.getFullName() + Messages.getString("WorkflowExporter.2"), null)); //$NON-NLS-1$ //$NON-NLS-2$
            }

            int stripOff = new Path(parentLoc.getAbsolutePath()).segmentCount();
            if (!m_commonParent.getFullName().equals("/")) { //$NON-NLS-1$
                // keep the common workflow group (if exists) in the archive
                stripOff = stripOff - 1;
            }
            Zipper.zipFiles(resourceList, m_exportFile, stripOff, sub);

        } catch (final IOException t) {
            LOGGER.debug(Messages.getString("WorkflowExporter.4") + t.getMessage(), t); //$NON-NLS-1$
            throw new CoreException(new Status(IStatus.ERROR, ExplorerActivator.PLUGIN_ID, t.getMessage(), t));
        }
        monitor.done();
    }

    /**
     * Implements the exclude policy. Called only if "exclude data" is checked.
     *
     * @param store the resource to check
     * @return true if the given resource should be excluded, false if it should be included
     */
    protected static boolean excludeResource(final AbstractExplorerFileStore store) {
        String name = store.getName();
        if (name.equals("internal")) { //$NON-NLS-1$
            return true;
        }
        if (store.fetchInfo().isDirectory()) {
            // directories to exclude:
            if (name.startsWith(FileNodePersistor.PORT_FOLDER_PREFIX)) {
                return true;
            }
            if (name.startsWith(FileNodePersistor.INTERNAL_TABLE_FOLDER_PREFIX)) {
                return true;
            }
            if (name.startsWith(FileNodePersistor.FILESTORE_FOLDER_PREFIX)) {
                return true;
            }
            if (name.startsWith(NodePersistor.INTERN_FILE_DIR)) {
                return true;
            }
            if (name.startsWith(SingleNodeContainer.DROP_DIR_NAME)) {
                return true;
            }
        } else {
            // files to exclude:
            if (name.startsWith("model_")) { //$NON-NLS-1$
                return true;
            }
            if (name.equals("data.xml")) { //$NON-NLS-1$
                return true;
            }
            if (name.startsWith(WorkflowPersistor.SAVED_WITH_DATA_FILE)) {
                return true;
            }
            if (name.startsWith(VMFileLocker.LOCK_FILE)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Implements the exclude policy. Called only if "exclude data" is checked.
     *
     * @param store the resource to check
     * @return true if the given resource should be excluded, false if it should be included
     * @since 7.1
     */
    protected static boolean excludeResource(final File store) {
        String name = store.getName();
        if (name.equals("internal")) { //$NON-NLS-1$
            return true;
        }
        if (store.isDirectory()) {
            // directories to exclude:
            if (name.startsWith(FileNodePersistor.PORT_FOLDER_PREFIX)) {
                return true;
            }
            if (name.startsWith(FileNodePersistor.INTERNAL_TABLE_FOLDER_PREFIX)) {
                return true;
            }
            if (name.startsWith(FileNodePersistor.FILESTORE_FOLDER_PREFIX)) {
                return true;
            }
            if (name.startsWith(NodePersistor.INTERN_FILE_DIR)) {
                return true;
            }
        } else {
            // files to exclude:
            if (name.startsWith("model_")) { //$NON-NLS-1$
                return true;
            }
            if (name.equals("data.xml")) { //$NON-NLS-1$
                return true;
            }
            if (name.startsWith(WorkflowPersistor.SAVED_WITH_DATA_FILE)) {
                return true;
            }
            if (name.startsWith(VMFileLocker.LOCK_FILE)) {
                return true;
            }
        }

        return false;
    }


    /**
     * Collects the files (files only) that are contained in the passed workflow or workflow group and are that are not
     * excluded. For workflows it does include all files contained in sub dirs (unless excluded).
     *
     * @param resourceList result list of local resources to export
     * @param element the resource representing the thing to export
     * @param excludeData true if KNIME data files should be excluded
     * @throws CoreException
     */
    public static void addResourcesFor(final List<File> resourceList, final AbstractExplorerFileStore element,
        final boolean excludeData) throws CoreException {
        if (resourceList == null) {
            throw new NullPointerException(Messages.getString("WorkflowExporter.11")); //$NON-NLS-1$
        }
        if (AbstractExplorerFileStore.isWorkflow(element) || AbstractExplorerFileStore.isWorkflowTemplate(element)) {
            addWorkflowContent(resourceList, element, excludeData);
        } else if (AbstractExplorerFileStore.isDataFile(element)) {
            addFile(resourceList, element);
        } else if (AbstractExplorerFileStore.isWorkflowGroup(element)) {
            addWorkflowGroupContent(resourceList, element);
        } else {
            throw new IllegalArgumentException(Messages.getString("WorkflowExporter.12") //$NON-NLS-1$
                    + element.getMountIDWithFullPath() + "\")"); //$NON-NLS-1$
        }
    }

    /*
     * Adds files contained in workflow groups. Doesn't recurse. Adds the meta info file.
     */
    private static void addWorkflowGroupContent(final List<File> resourceList, final AbstractExplorerFileStore group) throws CoreException {
        assert group.fetchInfo().isDirectory();
        File loc = group.toLocalFile();
        if (loc == null) {
            throw new CoreException(new Status(IStatus.ERROR, ExplorerActivator.PLUGIN_ID,
                Messages.getString("WorkflowExporter.14") + group.getFullName() + Messages.getString("WorkflowExporter.15"), null)); //$NON-NLS-1$ //$NON-NLS-2$
        }
        resourceList.add(new File(loc, WorkflowPersistor.METAINFO_FILE));
    }

    /**
     * Adds a file of the passed resourcelist.
     *
     * @param resourceList
     * @param dataFile a file!
     * @throws CoreException
     */
    private static void addFile(final List<File> resourceList, final AbstractExplorerFileStore dataFile)
        throws CoreException {
        assert dataFile.fetchInfo().isFile();
        File loc = dataFile.toLocalFile();
        if (loc == null) {
            throw new CoreException(new Status(IStatus.ERROR, ExplorerActivator.PLUGIN_ID,
                Messages.getString("WorkflowExporter.16") + dataFile.getFullName() + Messages.getString("WorkflowExporter.17"), null)); //$NON-NLS-1$ //$NON-NLS-2$
        }
        resourceList.add(loc);
    }

    /*
     * Call this on workflows or templates only. Includes everything except data tables, if excluded
     */

    private static void addWorkflowContent(final List<File> resources, final AbstractExplorerFileStore flow,
        final boolean excludeData) throws CoreException {
        assert flow.fetchInfo().isDirectory();
        File loc = flow.toLocalFile();
        if (loc == null) {
            throw new CoreException(new Status(IStatus.ERROR, ExplorerActivator.PLUGIN_ID,
                Messages.getString("WorkflowExporter.18") + flow.getFullName() + Messages.getString("WorkflowExporter.19"), null)); //$NON-NLS-1$ //$NON-NLS-2$
        }
        addEntireDirContent(resources, loc, excludeData);
    }

    private static void addEntireDirContent(final List<File> resources, final File dir, final boolean excludeData)
        throws CoreException {
        File[] content = dir.listFiles();
        if (content == null) {
            throw new CoreException(new Status(IStatus.ERROR, ExplorerActivator.PLUGIN_ID,
                Messages.getString("WorkflowExporter.20") + dir.getAbsolutePath() + Messages.getString("WorkflowExporter.21"), null)); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (content.length == 0) { // see AP-13538 (empty dirs are ignored -- so we add them)
            resources.add(dir);
        }
        for (File child : content) {
            if (!KnimeFileUtil.isMetaNode(child) && excludeData && excludeResource(child)) {
                continue;
            }
            if (!child.isDirectory()) {
                resources.add(child);
            } else {
                addEntireDirContent(resources, child, excludeData);
            }
        }
    }

}
