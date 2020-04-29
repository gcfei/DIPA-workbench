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
 *   31.05.2011 (ohl): created
 */
package org.knime.workbench.explorer.view.actions.export;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.view.ContentDelegator;

/**
 * This wizard exports KNIME workflows and workflow groups if workflows are selected which are in different workflow
 * groups.
 */
public class WorkflowExportWizard extends Wizard implements INewWizard {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(WorkflowExportWizard.class);

    private WorkflowExportPage m_page;

    private ISelection m_selection;

    /**
     * Constructor.
     */
    public WorkflowExportWizard() {
        super();
        setWindowTitle(Messages.getString("WorkflowExportWizard.0")); //$NON-NLS-1$
        setNeedsProgressMonitor(true);
    }

    /**
     * Adding the page to the wizard.
     */
    @Override
    public void addPages() {
        AbstractExplorerFileStore initFile = null;
        if (m_selection instanceof IStructuredSelection) {
            initFile = ContentDelegator.getFileStore(((IStructuredSelection)m_selection).getFirstElement());
        }
        m_page = new WorkflowExportPage(initFile);
        addPage(m_page);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean canFinish() {
        return m_page.isPageComplete();
    }

    /**
     * This method is called when 'Finish' button is pressed in the wizard. We will create an operation and run it using
     * wizard as execution context.
     *
     * @return If finished successfully
     */
    @Override
    public boolean performFinish() {

        // first save dirty editors
        boolean canceled = !PlatformUI.getWorkbench().saveAllEditors(true);
        if (canceled) {
            return false;
        }

        m_page.saveDialogSettings();

        // get all workflows and workflow groups and stuff selected
        Collection<AbstractExplorerFileStore> elementsToExport = new ArrayList<>();
        elementsToExport.addAll(m_page.getElementsToExport());
        if (elementsToExport.isEmpty()) {
            m_page.setErrorMessage(Messages.getString("WorkflowExportWizard.1")); //$NON-NLS-1$
            return false;
        }
        final String filePath = m_page.getFileName().trim();
        final File exportFile = new File(filePath);
        final WorkflowExporter workflowExporter = new WorkflowExporter(exportFile,
            m_page.getSelectedStore(), elementsToExport, m_page.excludeData());

        // if the specified export file already exist ask the user
        // for confirmation

        if (exportFile.exists()) {
            // if it exists we have to check if we can write to:
            if (!exportFile.canWrite() || exportFile.isDirectory()) {
                // display error
                m_page.setErrorMessage(Messages.getString("WorkflowExportWizard.2")); //$NON-NLS-1$
                return false;
            }
            boolean overwrite =
                MessageDialog.openQuestion(getShell(), Messages.getString("WorkflowExportWizard.3"), //$NON-NLS-1$
                    Messages.getString("WorkflowExportWizard.4") + Messages.getString("WorkflowExportWizard.5")); //$NON-NLS-1$ //$NON-NLS-2$
            if (!overwrite) {
                return false;
            }
        } else {
            File parentFile = exportFile.getParentFile();
            if ((parentFile != null) && parentFile.exists()) {
                // check if we can write to
                if (!parentFile.canWrite() || !parentFile.isDirectory()) {
                    // display error
                    m_page.setErrorMessage(Messages.getString("WorkflowExportWizard.6")); //$NON-NLS-1$
                    return false;
                }
            } else if (parentFile != null && !parentFile.exists()) {
                if (!exportFile.getParentFile().mkdirs()) {
                    boolean wasRoot = false;
                    for (File root : File.listRoots()) {
                        if (exportFile.getParentFile().equals(root)) {
                            wasRoot = true;
                            break;
                        }
                    }
                    if (!wasRoot) {
                        m_page.setErrorMessage(Messages.getString("WorkflowExportWizard.7") + exportFile.getAbsolutePath() //$NON-NLS-1$
                            + Messages.getString("WorkflowExportWizard.8") + Messages.getString("WorkflowExportWizard.9")); //$NON-NLS-1$ //$NON-NLS-2$
                        return false;
                    }
                }
            }
        }

        IRunnableWithProgress op = new IRunnableWithProgress() {
            @Override
            public void run(final IProgressMonitor monitor) throws InvocationTargetException {
                try {
                    workflowExporter.doFinish(monitor);
                } catch (CoreException e) {
                    throw new InvocationTargetException(e);
                } finally {
                    monitor.done();
                }
            }
        };
        try {
            getContainer().run(true, true, op);
        } catch (InterruptedException e) {
            LOGGER.info(Messages.getString("WorkflowExportWizard.10")); //$NON-NLS-1$
            m_page.setErrorMessage(Messages.getString("WorkflowExportWizard.11")); //$NON-NLS-1$
            return false;
        } catch (InvocationTargetException e) {
            Throwable realException = e.getTargetException();
            String message = realException.getMessage();
            message = Messages.getString("WorkflowExportWizard.12") + realException.getMessage(); //$NON-NLS-1$
            LOGGER.debug(message);
            MessageDialog.openError(getShell(), "Error", message); //$NON-NLS-1$
            m_page.setErrorMessage(message);
            return false;
        }
        return true;
    }

    /**
     * We will accept the selection in the workbench to see if we can initialize from it.
     *
     * {@inheritDoc}
     */
    @Override
    public void init(final IWorkbench workbench, final IStructuredSelection selection) {
        this.m_selection = selection;
    }

}
