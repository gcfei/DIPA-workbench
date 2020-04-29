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
 *
 * History
 *   Jun 3, 2011 (wiswedel): created
 */
package org.knime.workbench.explorer.filesystem;

import java.io.File;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.util.FileUtil;
import org.knime.core.util.VMFileLocker;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.DeletionConfirmationResult;
import org.knime.workbench.explorer.view.actions.ExplorerAction;
import org.knime.workbench.ui.navigator.ProjectWorkflowMap;
import org.knime.workbench.ui.navigator.WorkflowEditorAdapter;

/**
 * A set of static methods to deal with the creation/deletion of possibly locked
 * workflows in the explorer file system.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public final class ExplorerFileSystemUtils {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(ExplorerFileSystemUtils.class);

    /** Utility class, no public constructor. */
    private ExplorerFileSystemUtils() {
        // no op
    }

    /**
     * Tries to lock the workflows passed as first argument.
     *
     * @param workflowsToLock the workflows to be locked
     * @param unlockableWF the workflows that could not be locked
     * @param lockedWF the workflows that could be locked
     */
    public static void lockWorkflows(
            final List<? extends LocalExplorerFileStore> workflowsToLock,
            final List<LocalExplorerFileStore> unlockableWF,
            final List<LocalExplorerFileStore> lockedWF) {
        assert unlockableWF.size() == 0; // the result lists should be empty
        assert lockedWF.size() == 0;
        // open workflows can be locked multiple times in one instance
        for (LocalExplorerFileStore wf : workflowsToLock) {
            boolean locked = lockWorkflow(wf);
            if (locked) {
                lockedWF.add(wf);
            } else {
                unlockableWF.add(wf);
            }
        }
    }

    /**
     * Tries to lock the workflow.
     *
     * @param workflow the workflow to be locked
     * @return true if the workflow could be locked, false otherwise
     */
    public static boolean lockWorkflow(final LocalExplorerFileStore workflow) {
        assert AbstractExplorerFileStore.isWorkflow(workflow);
        File loc;
        try {
            loc = workflow.toLocalFile(EFS.NONE, null);
        } catch (CoreException e) {
            loc = null;
        }
        if (loc != null && VMFileLocker.lockForVM(loc)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Unlocks the specified workflows.
     *
     * @param workflows the workflows to be unlocked
     */
    public static void unlockWorkflows(
            final List<? extends LocalExplorerFileStore> workflows) {
        for (LocalExplorerFileStore lwf : workflows) {
            unlockWorkflow(lwf);
        }
    }

    /**
     * Unlocks the specified workflow.
     *
     * @param workflow the workflow to be unlocked
     */
    public static void unlockWorkflow(final LocalExplorerFileStore workflow) {
        File loc;
        try {
            loc = workflow.toLocalFile(EFS.NONE, null);
        } catch (CoreException e) {
            return;
        }
        if (!VMFileLocker.isLockedForVM(loc)) {
            return;
        }
        assert AbstractExplorerFileStore.isWorkflow(workflow);
        VMFileLocker.unlockForVM(loc);
    }

    /**
     * Closes the editor of the specified workflows.
     *
     * @param workflows the workflows to be closed
     */
    public static void closeOpenWorkflows(
            final List<? extends AbstractExplorerFileStore> workflows) {
        IWorkbenchPage page =
                PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                        .getActivePage();
        for (AbstractExplorerFileStore wf : workflows) {
            URI loc = null;
            try {
                File file = wf.toLocalFile(EFS.NONE, null);
                if (file != null) {
                    loc = file.toURI();
                }
            } catch (CoreException e) {
                //
            }
            if (loc == null) {
                loc = wf.toURI();
            }
            if (loc == null) {
                // not a local workflow nor a remote workflow job. Not open.
                continue;
            }
            NodeContainerUI wfm = ProjectWorkflowMap.getWorkflowUI(loc);
            if (wfm != null) {
                for (IEditorReference editRef : page.getEditorReferences()) {
                    IEditorPart editor = editRef.getEditor(false);
                    if (editor == null) {
                        // got closed in the mean time
                        continue;
                    }
                    WorkflowEditorAdapter wea =
                            editor
                            .getAdapter(WorkflowEditorAdapter.class);
                    NodeContainerUI editWFM = null;
                    if (wea != null) {
                        editWFM = wea.getWorkflowManagerUI();
                    }
                    if (wfm == editWFM) {
                        page.closeEditor(editor, false);
                    }
                }

            }
        }
    }

    /**
     * Closes the editor of the specified workflow reports.
     *
     * @param workflows the workflows to be closed
     * @since 8.4
     */
    public static void closeOpenReports(
            final List<? extends AbstractExplorerFileStore> workflows) {
        final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        for (AbstractExplorerFileStore workflow : workflows) {
            if (!(workflow instanceof LocalExplorerFileStore)) {
                // only consider local workflows
                continue;
            }

            File localFile = null;
            try {
                localFile = workflow.toLocalFile();
            } catch (CoreException e) {
                continue;
            }

            for (IEditorReference editRef : page.getEditorReferences()) {

                // Check if we have an actual BIRT editor.
                // Note: instead of checking of type IReportEditor, we check the ID so that this even works if
                // editor is null (report is open, but KNIME just has been started up and another editor is open.
                if (editRef.getId().startsWith("org.eclipse.birt")) { //$NON-NLS-1$
                    try {
                        final IEditorInput input = editRef.getEditorInput();

                        String reportLocation = ""; //$NON-NLS-1$
                        if (input instanceof IFileEditorInput) {
                            reportLocation = ((IFileEditorInput)input).getFile().getLocation().toString();
                        } else {
                            reportLocation = input.getToolTipText();
                        }

                        final int idx = reportLocation.lastIndexOf('/');

                        reportLocation = idx > -1 ? reportLocation.substring(0, idx) : reportLocation;

                        // Compare the path of the workflow with the path of the IEditorInput, which points to a
                        // .rptdesign file in the workflow folder.
                        if (localFile.toString().equals(reportLocation)) {
                            // Restore editor in case it hasn't been done yet as otherwise the editor is null.
                            page.closeEditor(editRef.getEditor(true), false);
                        }
                    } catch (PartInitException e) {
                        // do nothing.
                    }
                }

            }
        }
    }

    /**
     * @param workflows the workflows to check
     * @return true if at least one of the specified workflows are open, false
     *         otherwise
     */
    public static boolean hasOpenWorkflows(
            final List<? extends AbstractExplorerFileStore> workflows) {
        return  hasOpenWorkflows(workflows, false);
    }

    /**
     * Checks if reports are opened for the specified workflows.
     *
     * @param workflows The workflows to check.
     * @return {@code true} if at least one of the workflows has an open report, {@code false} otherwise.
     * @since 8.4
     */
    public static boolean hasOpenReports(
            final List<? extends AbstractExplorerFileStore> workflows) {
        final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        for (AbstractExplorerFileStore workflow : workflows) {
            if (!(workflow instanceof LocalExplorerFileStore)) {
                // only consider local workflows
                continue;
            }

            File localFile = null;
            try {
                localFile = workflow.toLocalFile();
            } catch (CoreException e) {
                continue;
            }

            for (IEditorReference editRef : page.getEditorReferences()) {

                // Check if we have an actual BIRT editor.
                // Note: instead of checking of type IReportEditor, we check the ID so that this even works if
                // editor is null (report is open, but KNIME just has been started up and another editor is open.
                if (editRef.getId().startsWith("org.eclipse.birt")) { //$NON-NLS-1$
                    try {
                        final IEditorInput input = editRef.getEditorInput();

                        String reportLocation = ""; //$NON-NLS-1$
                        if (input instanceof IFileEditorInput) {
                            reportLocation = ((IFileEditorInput)input).getFile().getLocation().toString();
                        } else {
                            reportLocation = input.getToolTipText();
                        }

                        final int idx = reportLocation.lastIndexOf('/');

                        reportLocation = idx > -1 ? reportLocation.substring(0, idx) : reportLocation;

                        // Compare the path of the workflow with the path of the IEditorInput, which points to a
                        // .rptdesign file in the workflow folder.
                        if (localFile.toString().equals(reportLocation)) {
                            return true;
                        }
                    } catch (PartInitException e) {
                        // do nothing.
                    }
                }

            }
        }
        return false;
    }

    /**
     * @param workflows the workflows to check
     * @param ignoreSavedFlows set to true if only dirty open flows shall
     *      be reported
     * @return true if at least one of the specified workflows are open, false
     *         otherwise
     */
    private static boolean hasOpenWorkflows(
            final List<? extends AbstractExplorerFileStore> workflows,
            final boolean ignoreSavedFlows) {
        IWorkbenchPage page =
                PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                        .getActivePage();
        for (AbstractExplorerFileStore wf : workflows) {
            File loc;
            try {
                loc = wf.toLocalFile(EFS.NONE, null);
            } catch (CoreException e) {
                loc = null;
            }
            if (loc == null) {
                // not a local workflow. Not open.
                continue;
            }
            NodeContainer wfm = ProjectWorkflowMap.getWorkflow(loc.toURI());
            if (wfm != null) {
                for (IEditorReference editRef : page.getEditorReferences()) {
                    IEditorPart editor = editRef.getEditor(false);
                    if (editor == null
                            || (ignoreSavedFlows && !editor.isDirty())) {
                        // got closed in the mean time or is not dirty
                        continue;
                    }
                    WorkflowEditorAdapter wea =
                            editor
                            .getAdapter(WorkflowEditorAdapter.class);
                    NodeContainer editWFM = null;
                    if (wea != null) {
                        editWFM = wea.getWorkflowManager();
                    }
                    if (wfm == editWFM) {
                        return true;
                    }
                }

            }
        }
        return false;
    }

    /**
     * Checks whether the file stores are lockable. Therefore all contained
     * workflows must be closed and not used by any other instance.
     *
     * @param fileStores the file stores to check
     * @param ignoreSavedFlows set to true if only dirty open flows shall
     *      be reported
     * @return an error message describing the problem or null, if the stores
     *      can be locked
     * @since 3.0
     */
    public static String isLockable(
            final List<AbstractExplorerFileStore> fileStores,
            final boolean ignoreSavedFlows) {
        // find affected workflows that are opened (and dirty)
        List<LocalExplorerFileStore> affectedFlows =
                ExplorerAction.getContainedLocalWorkflows(fileStores);
        if (ExplorerFileSystemUtils.hasOpenWorkflows(affectedFlows,
                ignoreSavedFlows)) {
            String msg = Messages.ExplorerFileSystemUtils_4
                + Messages.ExplorerFileSystemUtils_5
                + (ignoreSavedFlows ? Messages.ExplorerFileSystemUtils_6 : Messages.ExplorerFileSystemUtils_7) + Messages.ExplorerFileSystemUtils_8;
            LOGGER.warn(msg);
            return msg;
        }
        // check for unlockable flows
        List<LocalExplorerFileStore> lockedFlows =
                new LinkedList<LocalExplorerFileStore>();
        LinkedList<LocalExplorerFileStore> unlockableFlows =
                new LinkedList<LocalExplorerFileStore>();
        ExplorerFileSystemUtils.lockWorkflows(affectedFlows, unlockableFlows,
                lockedFlows);
        // unlock flows locked in here
        ExplorerFileSystemUtils.unlockWorkflows(lockedFlows);
        if (!unlockableFlows.isEmpty()) {
            StringBuilder sb =
                    new StringBuilder(
                            Messages.ExplorerFileSystemUtils_9
                           + Messages.ExplorerFileSystemUtils_10
                           + Messages.ExplorerFileSystemUtils_11);

            String msg = sb.toString();
            for (AbstractExplorerFileStore lockedFlow : unlockableFlows) {
                sb.append("\t" + lockedFlow.getMountIDWithFullPath() + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            LOGGER.warn(sb.toString());
            return msg;
        }
        return null;
    }

    /**
     * Delete workflows from argument list. If the workflows are locked by this
     * VM, they will be unlocked after this method returns.
     *
     * @param toDelWFs The list of directories associate with the workflows.
     * @param confirmationResults the result returned from the corresponding confirmation dialogs, can be empty, must
     *         not be null
     * @return true if that was successful, i.e. the workflow directory does not
     *         exist when this method returns, false if that fails (e.g. not
     *         locked by this VM)
     * @since 6.0
     **/
    public static boolean deleteLockedWorkflows(final List<? extends AbstractExplorerFileStore> toDelWFs,
        final Map<AbstractContentProvider, DeletionConfirmationResult> confirmationResults) {
        boolean success = true;
        for (AbstractExplorerFileStore wf : toDelWFs) {
            assert AbstractExplorerFileStore.isWorkflow(wf)
                    || AbstractExplorerFileStore.isWorkflowTemplate(wf);
            try {
                File loc = wf.toLocalFile(EFS.NONE, null);
                if (loc == null) {
                    // can't do any locking or fancy deletion
                    wf.delete(confirmationResults.get(wf.getContentProvider()), null);
                    continue;
                }
                assert VMFileLocker.isLockedForVM(loc);

                // delete the workflow file first
                File[] children = loc.listFiles();
                if (children == null) {
                    throw new CoreException(
                            new Status(IStatus.ERROR,
                                    ExplorerActivator.PLUGIN_ID,
                                    Messages.ExplorerFileSystemUtils_14));
                }

                // delete workflow file first
                File wfFile = new File(loc, WorkflowPersistor.WORKFLOW_FILE);
                if (wfFile.exists()) {
                    success &= wfFile.delete();
                } else {
                    File tempFile =
                            new File(loc, WorkflowPersistor.TEMPLATE_FILE);
                    success &= tempFile.delete();
                }

                children = loc.listFiles(); // get a list w/o workflow file
                for (File child : children) {
                    if (VMFileLocker.LOCK_FILE.equals(child.getName())) {
                        // delete the lock file last
                        continue;
                    }
                    boolean deletedIt = FileUtil.deleteRecursively(child);
                    success &= deletedIt;
                    if (!deletedIt) {
                        LOGGER.error(Messages.ExplorerFileSystemUtils_15 + child.toString());
                    }
                }

                // release lock in order to delete lock file
                VMFileLocker.unlockForVM(loc);
                // lock file resource may not exist
                File lockFile = new File(loc, VMFileLocker.LOCK_FILE);
                if (lockFile.exists()) {
                    success &= lockFile.delete();
                }
                // delete the workflow directory itself
                success &= FileUtil.deleteRecursively(loc);
            } catch (CoreException e) {
                success = false;
                LOGGER.error(Messages.ExplorerFileSystemUtils_16 + wf.toString()
                        + ": " + e.getMessage(), e); //$NON-NLS-1$
                // continue with next workflow...
            }
        }
        return success;
    }

    /**
     * Delete the files denoted by the argument list.
     *
     * @param toDel The list of files to be deleted.
     * @param delConfs deletion confirmation results from the corresponding providers
     * @return true if the files/directories don't exist when this method
     *         returns.
     * @since 6.0
     */
    public static boolean deleteTheRest(final List<? extends AbstractExplorerFileStore> toDel,
        final Map<AbstractContentProvider, DeletionConfirmationResult> delConfs) {
        boolean success = true;
        for (AbstractExplorerFileStore f : toDel) {
            // go by the local file. (Does EFS.delete() delete recursively??)
            try {
                if (f.getName().equals("/")) { //$NON-NLS-1$
                    // the root is represented by the mount point. Can't del it!
                    LOGGER.info(Messages.ExplorerFileSystemUtils_19
                            + Messages.ExplorerFileSystemUtils_20 + f.getMountIDWithFullPath() + ")"); //$NON-NLS-2$
                    continue;
                }
                if (f.fetchInfo().exists()) {
                    File loc = f.toLocalFile(EFS.NONE, null);
                    if (loc == null) {
                        f.delete(delConfs.get(f.getContentProvider()), null);
                    } else {
                        // if it is a workflow it would be gone already
                        if (loc.exists()) {
                            success &= FileUtil.deleteRecursively(loc);
                        }
                    }
                }
            } catch (CoreException e) {
                success = false;
                LOGGER.error(Messages.ExplorerFileSystemUtils_22 + f.toString() + ": " //$NON-NLS-2$
                        + e.getMessage(), e);
            }
        }
        return success;
    }

}
