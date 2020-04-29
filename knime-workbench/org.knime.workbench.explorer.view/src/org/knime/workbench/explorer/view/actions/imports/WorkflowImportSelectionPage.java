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
 * ---------------------------------------------------------------------
 *
 * History
 *   12.08.2009 (Fabian Dill): created
 */
package org.knime.workbench.explorer.view.actions.imports;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.zip.ZipFile;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.internal.wizards.datatransfer.ArchiveFileManipulations;
import org.eclipse.ui.internal.wizards.datatransfer.ILeveledImportStructureProvider;
import org.eclipse.ui.internal.wizards.datatransfer.TarFile;
import org.eclipse.ui.internal.wizards.datatransfer.TarLeveledStructureProvider;
import org.eclipse.ui.internal.wizards.datatransfer.ZipLeveledStructureProvider;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.util.KnimeFileUtil;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.dialogs.SpaceResourceSelectionDialog;
import org.knime.workbench.explorer.dialogs.Validator;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.view.ContentObject;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 * Import page providing the hierarchical user interface to select workflows and workflow groups to import from either a
 * directory or an archive file.
 *
 * @author Fabian Dill, KNIME AG, Zurich, Switzerland
 */
public class WorkflowImportSelectionPage extends WizardPage {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(WorkflowImportSelectionPage.class);

    /** Identifier for this page within a wizard. */
    public static final String NAME = Messages.getString("WorkflowImportSelectionPage.0"); //$NON-NLS-1$

    private static final Image IMG_WARN =
        AbstractUIPlugin.imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID, "icons/warning.gif").createImage(); //$NON-NLS-1$

    // constant from WizardArchiveFileResourceImportPage1
    private static final String[] ZIP_FILE_MASK =
        {"*." + KNIMEConstants.KNIME_ARCHIVE_FILE_EXTENSION + ";" + "*." + KNIMEConstants.KNIME_WORKFLOW_FILE_EXTENSION //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            + ";" + "*.zip;" + "*.jar;" + "*.tar;" + "*.tar.gz;" + "*.tgz", "*.*"};  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$//$NON-NLS-7$

    // keys and fields for dialog settings
    private static final String KEY_ZIP_LOC = "initialZipLocation"; //$NON-NLS-1$

    private static final String KEY_DIR_LOC = "initialDirLocation"; //$NON-NLS-1$

    private static final String KEY_FROM_DIR = "initiallyFromDir"; //$NON-NLS-1$

    private static String initialZipLocation = ""; //$NON-NLS-1$

    private static String initialDirLocation = ""; //$NON-NLS-1$

    private static boolean initialFromDir = false;

    // the initial destination should not be static
    private AbstractExplorerFileStore m_initialDestination;

    private static final GridData FILL_BOTH = new GridData(GridData.FILL_BOTH);

    private final GridData m_btnData;

    // source selection part components
    private Button m_fromDirUI;

    private Text m_fromDirTextUI;

    private Button m_dirBrowseBtn;

    private Button m_fromZipUI;

    private Text m_fromZipTextUI;

    private Button m_zipBrowseBtn;

    // target selection part components
    private Text m_targetTextUI;

    private AbstractExplorerFileStore m_target;

    private Button m_browseWorkflowGroupsBtn;

    // workflow list part components
    private CheckboxTreeViewer m_workflowListUI;

    private IWorkflowImportElement m_importRoot;

    private final Collection<IWorkflowImportElement> m_invalidAndCheckedImports =
        new ArrayList<IWorkflowImportElement>();

    private final Collection<IWorkflowImportElement> m_validAndCheckedImports = new ArrayList<IWorkflowImportElement>();

    private final Collection<IWorkflowImportElement> m_uncheckedImports = new ArrayList<IWorkflowImportElement>();

    /** Set containing all the items displayed to the user (some files are filtered out). */
    private final Collection<IWorkflowImportElement> m_displayImports = new HashSet<IWorkflowImportElement>();

    /*
     * We keep the next page (the rename page here), because it is not possible
     * to dynamically change the page in the wizard. See the getNextPage method
     * where the correct next page (depending on duplicate workflows) is
     * returned.
     */
    private RenameWorkflowImportPage m_renamePage;

    private boolean m_presetZipLocation;

    /**
     *
     */
    public WorkflowImportSelectionPage() {
        super(NAME);
        setTitle(Messages.getString("WorkflowImportSelectionPage.16")); //$NON-NLS-1$
        setDescription(Messages.getString("WorkflowImportSelectionPage.17")); //$NON-NLS-1$
        setImageDescriptor(ImageRepository.getImageDescriptor(SharedImages.ImportBig));
        m_btnData = new GridData();
        m_btnData.widthHint = 70;
        m_btnData.horizontalAlignment = SWT.RIGHT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createControl(final Composite parent) {
        Composite overall = new Composite(parent, SWT.FILL);
        overall.setLayout(new GridLayout(1, false));
        overall.setLayoutData(FILL_BOTH);
        createImportComposite(overall);
        createTargetComposite(overall);
        createWorkflowListComposite(overall);
        setControl(overall);
    }

    /**
     *
     * @param parent parent
     * @return composite to select the destination of the import
     */
    protected Composite createTargetComposite(final Composite parent) {
        Group overall = new Group(parent, SWT.FILL | SWT.SHADOW_ETCHED_IN);
        overall.setLayout(new GridLayout(3, false));
        overall.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        overall.setText(Messages.getString("WorkflowImportSelectionPage.18")); //$NON-NLS-1$
        Label label = new Label(overall, SWT.NONE);
        label.setText(Messages.getString("WorkflowImportSelectionPage.19")); //$NON-NLS-1$
        m_targetTextUI = new Text(overall, SWT.BORDER | SWT.READ_ONLY);
        m_targetTextUI.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        m_targetTextUI.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(final ModifyEvent e) {
                validateWorkflows();
            }

        });
        m_target = null;
        m_browseWorkflowGroupsBtn = new Button(overall, SWT.PUSH);
        m_browseWorkflowGroupsBtn.setText(Messages.getString("WorkflowImportSelectionPage.20")); //$NON-NLS-1$
        m_browseWorkflowGroupsBtn.setLayoutData(m_btnData);
        m_browseWorkflowGroupsBtn.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }

            @Override
            public void widgetSelected(final SelectionEvent e) {
                handleWorkflowGroupBrowseButtonPressed();
            }
        });
        // set initial selection
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        if (m_initialDestination != null && !m_initialDestination.equals(root)) {
            m_targetTextUI.setText(m_initialDestination.getMountIDWithFullPath());
            m_target = m_initialDestination;
        } else {
            m_targetTextUI.setText(""); //$NON-NLS-1$
            m_target = null;
        }
        return overall;
    }

    /**
     *
     * @param parent the parent composite
     * @return the composite of the import page containing the selection between archive file or directory
     */
    protected Composite createImportComposite(final Composite parent) {
        Group overall = new Group(parent, SWT.FILL | SWT.SHADOW_ETCHED_IN);
        overall.setText(Messages.getString("WorkflowImportSelectionPage.22")); //$NON-NLS-1$
        GridData fillHorizontal = new GridData(GridData.FILL_HORIZONTAL);
        overall.setLayoutData(fillHorizontal);

        overall.setLayout(new GridLayout(3, false));
        m_fromZipUI = new Button(overall, SWT.RADIO);
        if (m_presetZipLocation && StringUtils.isNotEmpty(initialZipLocation)) {
            m_fromZipUI.setText(Messages.getString("WorkflowImportSelectionPage.23")); //$NON-NLS-1$
        } else {
            m_fromZipUI.setText(Messages.getString("WorkflowImportSelectionPage.24")); //$NON-NLS-1$
        }
        m_fromZipUI.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }

            @Override
            public void widgetSelected(final SelectionEvent e) {
                boolean fromZip = m_fromZipUI.getSelection();
                initialFromDir = !fromZip;
                enableDirComponents(!fromZip);
                enableZipComponents(fromZip);
                clear();
                // restore imports from previously selected file
                if (m_fromZipTextUI.getText() != null && !m_fromZipTextUI.getText().trim().isEmpty()) {
                    collectWorkflowsFromZipFile(m_fromZipTextUI.getText().trim());
                    validateWorkflows();
                }
            }
        });

        m_fromZipTextUI = new Text(overall, SWT.BORDER | SWT.READ_ONLY);
        m_fromZipTextUI.setLayoutData(fillHorizontal);
        m_zipBrowseBtn = new Button(overall, SWT.PUSH);
        m_zipBrowseBtn.setText(Messages.getString("WorkflowImportSelectionPage.25")); //$NON-NLS-1$
        m_zipBrowseBtn.setLayoutData(m_btnData);
        m_zipBrowseBtn.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }

            @Override
            public void widgetSelected(final SelectionEvent e) {
                handleZipBrowseButtonPressed();
            }
        });

        m_fromDirUI = new Button(overall, SWT.RADIO);
        m_fromDirUI.setText(Messages.getString("WorkflowImportSelectionPage.26")); //$NON-NLS-1$
        m_fromDirUI.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }

            @Override
            public void widgetSelected(final SelectionEvent e) {
                boolean fromDir = m_fromDirUI.getSelection();
                initialFromDir = fromDir;
                enableDirComponents(fromDir);
                enableZipComponents(!fromDir);
                clear();
                // restore imports from previously selected dir
                if (m_fromDirTextUI.getText() != null && !m_fromDirTextUI.getText().trim().isEmpty()) {
                    collectElementsFromDir(m_fromDirTextUI.getText().trim());
                    validateWorkflows();
                }
            }
        });

        m_fromDirTextUI = new Text(overall, SWT.BORDER | SWT.READ_ONLY);
        m_fromDirTextUI.setLayoutData(fillHorizontal);
        m_dirBrowseBtn = new Button(overall, SWT.PUSH);
        m_dirBrowseBtn.setText(Messages.getString("WorkflowImportSelectionPage.27")); //$NON-NLS-1$
        m_dirBrowseBtn.setLayoutData(m_btnData);
        m_dirBrowseBtn.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }

            @Override
            public void widgetSelected(final SelectionEvent e) {
                handleDirBrowseButtonPressed();
            }
        });

        // set the initial selection
        m_fromDirUI.setSelection(initialFromDir);
        enableDirComponents(initialFromDir);
        m_fromZipUI.setSelection(!initialFromDir);
        enableZipComponents(!initialFromDir);
        return overall;
    }

    /**
     *
     * @param enable true if the zip file selection UI elements should be enabled
     */
    public void enableZipComponents(final boolean enable) {
        m_fromZipTextUI.setEnabled(enable);
        m_zipBrowseBtn.setEnabled(enable);
    }

    /**
     *
     * @param enable true if the directory selection UI elements should be enabled
     */
    public void enableDirComponents(final boolean enable) {
        m_fromDirTextUI.setEnabled(enable);
        m_dirBrowseBtn.setEnabled(enable);
    }

    /**
     *
     * @param parent parent composite
     * @return the lower part of the workflow selection page, the list with selected workflows to import
     */
    protected Composite createWorkflowListComposite(final Composite parent) {
        Group overall = new Group(parent, SWT.FILL);
        overall.setText(Messages.getString("WorkflowImportSelectionPage.28")); //$NON-NLS-1$
        overall.setLayoutData(FILL_BOTH);
        overall.setLayout(new GridLayout(2, false));
        // list with checkboxes....
        m_workflowListUI = new CheckboxTreeViewer(overall);
        m_workflowListUI.getTree().setLayoutData(FILL_BOTH);
        m_workflowListUI.setContentProvider(new ITreeContentProvider() {

            @Override
            public Object[] getChildren(final Object parentElement) {
                if (parentElement instanceof IWorkflowImportElement) {
                    ArrayList<IWorkflowImportElement> display = new ArrayList();
                    Collection<IWorkflowImportElement> children = ((IWorkflowImportElement)parentElement).getChildren();
                    for (IWorkflowImportElement ch : children) {
                        // don't show knime/hidden files
                        if (!ch.getName().equals(WorkflowPersistor.METAINFO_FILE) && !ch.getName().startsWith(".")) { //$NON-NLS-1$
                            display.add(ch);
                        }
                    }
                    m_displayImports.addAll(display);
                    return display.toArray(new IWorkflowImportElement[display.size()]);
                }
                return new Object[0];
            }

            @Override
            public Object getParent(final Object element) {
                if (element instanceof IWorkflowImportElement) {
                    return ((IWorkflowImportElement)element).getParent();
                }
                return null;
            }

            @Override
            public boolean hasChildren(final Object element) {
                if (element instanceof IWorkflowImportElement) {
                    return ((IWorkflowImportElement)element).getChildren().size() > 0;
                }
                return false;
            }

            @Override
            public Object[] getElements(final Object inputElement) {
                return getChildren(inputElement);
            }

            @Override
            public void dispose() {
            }

            @Override
            public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
                // should never happen
            }
        });
        m_workflowListUI.setLabelProvider(new LabelProvider() {
            @Override
            public String getText(final Object element) {
                if (element instanceof IWorkflowImportElement) {
                    IWorkflowImportElement wf = (IWorkflowImportElement)element;
                    return wf.getName();
                }
                return element.toString();
            }

            @Override
            public Image getImage(final Object element) {
                if (element instanceof IWorkflowImportElement) {
                    IWorkflowImportElement wf = (IWorkflowImportElement)element;
                    // display invalid ones differently
                    if (wf.isInvalid()) {
                        return IMG_WARN;
                    } else if (wf.isWorkflow()) {
                        return ImageRepository.getIconImage(SharedImages.Workflow);
                    } else if (wf.isWorkflowGroup()) {
                        return ImageRepository.getIconImage(SharedImages.WorkflowGroup);
                    } else if (wf.isTemplate()) {
                        return ImageRepository.getIconImage(SharedImages.MetaNodeTemplate);
                    } else if (wf.isFile()) {
                        return ImageRepository.getIconImage(SharedImages.File);
                    }
                }
                return super.getImage(element);
            }
        });
        m_workflowListUI.setInput(m_importRoot);
        m_workflowListUI.addCheckStateListener(new ICheckStateListener() {
            @Override
            public void checkStateChanged(final CheckStateChangedEvent event) {
                Object o = event.getElement();
                boolean isChecked = event.getChecked();
                m_workflowListUI.setSubtreeChecked(o, isChecked);
                if (o instanceof IWorkflowImportElement) {
                    IWorkflowImportElement element = (IWorkflowImportElement)o;
                    setParentTreeChecked(m_workflowListUI, element, isChecked);
                }
                validateWorkflows();
            }
        });
        // and 3 buttons -> panel in second column
        GridData rightAlign = new GridData();
        rightAlign.horizontalAlignment = SWT.RIGHT;
        rightAlign.verticalAlignment = SWT.BEGINNING;
        Composite buttonPanel = new Composite(overall, SWT.NONE);
        buttonPanel.setLayout(new GridLayout(1, false));
        buttonPanel.setLayoutData(rightAlign);
        // select all
        Button selectAllBtn = new Button(buttonPanel, SWT.PUSH);
        selectAllBtn.setText(Messages.getString("WorkflowImportSelectionPage.30")); //$NON-NLS-1$
        selectAllBtn.setLayoutData(m_btnData);
        selectAllBtn.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }

            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (m_importRoot != null) {
                    m_workflowListUI.expandAll();
                    m_workflowListUI.setAllChecked(true);
                    validateWorkflows();
                }
            }
        });
        // deselect all
        Button deselectAllBtn = new Button(buttonPanel, SWT.PUSH);
        deselectAllBtn.setText(Messages.getString("WorkflowImportSelectionPage.31")); //$NON-NLS-1$
        deselectAllBtn.setLayoutData(m_btnData);
        deselectAllBtn.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }

            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (m_importRoot != null) {
                    m_workflowListUI.expandAll();
                    m_workflowListUI.setAllChecked(false);
                    validateWorkflows();
                }
            }
        });

        if (m_presetZipLocation && StringUtils.isNotEmpty(initialZipLocation)) {
            handleSelectedZipFileChange(initialZipLocation);
        }
        return parent;
    }

    /**
     *
     * @return the underlying tree that displays the workflows to import
     */
    public CheckboxTreeViewer getViewer() {
        return m_workflowListUI;
    }

    /**
     *
     * @return the valid and checked workflow imports
     */
    public Collection<IWorkflowImportElement> getWorkflowsToImport() {
        return m_validAndCheckedImports;
    }

    /**
     * Returns the unchecked workflows, i.e. the ones that shouldn't be imported.
     *
     * @return the unchecked workflows
     * @since 8.6
     */
    public Collection<IWorkflowImportElement> getUncheckedWorkflows() {
        return m_uncheckedImports;
    }

    /**
     *
     * @return the destination
     */
    protected AbstractExplorerFileStore getDestination() {
        return m_target;
    }

    /**
     *
     * @param viewer the viewer which items should be (un-)checked
     * @param element the element whose parents should be checked
     * @param state true for checked, false for uncheck
     */
    private void setParentTreeChecked(final CheckboxTreeViewer viewer, final IWorkflowImportElement element,
        final boolean state) {
        // trivial case -> go up and set active
        if (state) {
            IWorkflowImportElement parent = element.getParent();
            while (parent != null) {
                if (!viewer.setChecked(parent, state)) {
                    break;
                }
                parent = parent.getParent();
            }
        } else {
            // go up and set parents inactive that have no checked children.
            IWorkflowImportElement parent = element.getParent();
            while (parent != null) {
                boolean hasCheckedChild = false;
                for (IWorkflowImportElement wie : parent.getChildren()) {
                    if (viewer.getChecked(wie)) {
                        hasCheckedChild = true;
                        break;
                    }
                }
                if (!hasCheckedChild) {
                    if (!viewer.setChecked(parent, false)) {
                        // check mark couldn't be changed: done
                        break;
                    }
                    parent = parent.getParent();
                } else {
                    // parent stays checked - and with it all its grand-parents
                    break;
                }
            }
        }
    }

    private void handleZipBrowseButtonPressed() {
        // open a FileSelection dialog
        FileDialog dialog = new FileDialog(getShell());
        // allow only archive files
        dialog.setFilterExtensions(ZIP_FILE_MASK);

        // set initial location (either an already selected one)
        String fileName = m_fromZipTextUI.getText().trim();
        if (fileName.isEmpty()) {
            // restore
            fileName = initialZipLocation;
        }
        // if still empty -> set to workspace root
        if (fileName.isEmpty()) {
            fileName = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
        }
        File initialPath = new File(fileName);
        if (initialPath.isFile()) {
            String filterPath = new Path(initialPath.getParent()).toOSString();
            dialog.setFilterPath(filterPath);
            initialZipLocation = filterPath;
        }
        String selectedFile = dialog.open();
        handleSelectedZipFileChange(selectedFile);
    }

    /**
     * Sets a predefined file and updates the workflow selection view
     *
     * @param selectedFile the file to be selected
     * @since 7.3
     */
    public void setSelectedZipFile(final String selectedFile) {
        //m_zipBrowseBtn.setEnabled(false);
        //handleSelectedZipFileChange(selectedFile);
        m_presetZipLocation = true;
        initialZipLocation = selectedFile;
        initialFromDir = false;
    }

    private void handleSelectedZipFileChange(final String selectedFile) {
        // null if dialog was canceled/error occurred
        if (selectedFile != null) {
            clear();
            initialZipLocation = selectedFile;
            m_fromZipTextUI.setText(selectedFile);
            // do this in separate thread...
            collectWorkflowsFromZipFile(selectedFile);
        }
        validateWorkflows();
    }

    private void handleDirBrowseButtonPressed() {
        DirectoryDialog dialog = new DirectoryDialog(getShell());
        // get initial root
        // 1. already something entered?
        String fileName = m_fromDirTextUI.getText().trim();
        if (fileName.isEmpty()) {
            // 2. something stored?
            fileName = initialDirLocation;
        }
        if (fileName.isEmpty()) {
            // set to workspace root
            fileName = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
        }
        File initialDir = new File(fileName);
        if (initialDir.exists()) {
            if (!initialDir.isDirectory()) {
                initialDir = initialDir.getParentFile();
            }
            dialog.setFilterPath(initialDir.getAbsolutePath());
        }
        final String selectedDir = dialog.open();
        // null if dialog was canceled/error occurred
        if (selectedDir != null) {
            collectElementsFromDir(selectedDir);
        }
        validateWorkflows();
    }

    private void handleWorkflowGroupBrowseButtonPressed() {
        // get mounted local destinations to select from
        List<String> mountIDs = ExplorerMountTable.getAllVisibleMountIDs();
        SpaceResourceSelectionDialog dlg = new SpaceResourceSelectionDialog(getShell(), mountIDs.toArray(new String[0]),
            ContentObject.forFile(m_target));
        dlg.setTitle(Messages.getString("WorkflowImportSelectionPage.32")); //$NON-NLS-1$
        dlg.setHeader(Messages.getString("WorkflowImportSelectionPage.33")); //$NON-NLS-1$
        dlg.setDescription(Messages.getString("WorkflowImportSelectionPage.34")); //$NON-NLS-1$
        dlg.setValidator(new Validator() {
            @Override
            public String validateSelectionValue(final AbstractExplorerFileStore selection, final String name) {
                if (!AbstractExplorerFileStore.isWorkflowGroup(selection)) {
                    return Messages.getString("WorkflowImportSelectionPage.35"); //$NON-NLS-1$
                }
                return null;
            }
        });
        int returnCode = dlg.open();
        if (returnCode == IDialogConstants.OK_ID) {
            // set the newly selected workflow group as destination
            AbstractExplorerFileStore target = dlg.getSelection();
            if (AbstractExplorerFileStore.isWorkflowGroup(target)) {
                m_targetTextUI.setText(target.getMountIDWithFullPath());
                m_target = target;
            }
        }
        validateWorkflows();
    }

    /**
     *
     * @param selectedDir the directory to collect the contained workflows from
     * @since 7.1
     */
    public void collectElementsFromDir(final String selectedDir) {
        clear();
        initialDirLocation = selectedDir;
        m_fromDirTextUI.setText(selectedDir);
        File dir = new File(selectedDir);
        IWorkflowImportElement root = null;
        // FIXME: this is a hack - should work if the user selects a
        // workflow. Flag workflowSelected indicates that not the path but
        // the name only is considered when the resource is created
        if (KnimeFileUtil.isWorkflow(dir)) {
            // if the user selected a workflow we set the parent and the
            // child to this workflow - otherwise it would not be displayed
            root = new WorkflowImportElementFromFile(dir, true);
            root.addChild(new WorkflowImportElementFromFile(dir));
        } else {
            root = new WorkflowImportElementFromFile(dir);
        }
        m_importRoot = root;
        try {
            getContainer().run(true, true, new IRunnableWithProgress() {
                @Override
                public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    monitor.beginTask(Messages.getString("WorkflowImportSelectionPage.36"), IProgressMonitor.UNKNOWN); //$NON-NLS-1$
                    collectWorkflowsFromDir((WorkflowImportElementFromFile)m_importRoot, monitor);
                    monitor.done();
                }
            });
        } catch (Exception e) {
            String message = Messages.getString("WorkflowImportSelectionPage.37") + selectedDir; //$NON-NLS-1$
            IStatus status = new Status(IStatus.ERROR, KNIMEUIPlugin.PLUGIN_ID, message, e);
            setErrorMessage(message);
            LOGGER.error(message, e);
            ErrorDialog.openError(getShell(), Messages.getString("WorkflowImportSelectionPage.38"), null, status); //$NON-NLS-1$
        }
        validateWorkflows();
        m_workflowListUI.setInput(m_importRoot);
        m_workflowListUI.expandAll();
        m_workflowListUI.setAllChecked(true);
        m_workflowListUI.refresh(true);
    }

    private void collectWorkflowsFromDir(final WorkflowImportElementFromFile parent, final IProgressMonitor monitor) {
        if (monitor.isCanceled()) {
            m_importRoot = null;
            Display.getDefault().syncExec(new Runnable() {

                @Override
                public void run() {
                    m_fromDirTextUI.setText(""); //$NON-NLS-1$
                }

            });
            return;
        }
        File rootDir = parent.getFile();
        if (rootDir == null) {
            throw new IllegalArgumentException(Messages.getString("WorkflowImportSelectionPage.40")); //$NON-NLS-1$
        }
        if (rootDir.isFile() || KnimeFileUtil.isWorkflow(rootDir) || KnimeFileUtil.isMetaNodeTemplate(rootDir)) {
            // abort recursion!
            return;
        }
        monitor.subTask(rootDir.getName());
        // else go through all files
        if (rootDir.canRead()) {
            for (File f : rootDir.listFiles()) {
                WorkflowImportElementFromFile child = new WorkflowImportElementFromFile(f);
                if (f.isDirectory()) {
                    collectWorkflowsFromDir(child, monitor);
                    if (child.isWorkflow() || child.isWorkflowGroup() || child.isTemplate()) {
                        parent.addChild(child);
                    }
                } else {
                    if (child.isFile()) {
                        parent.addChild(child);
                    }
                }
            }
        }
    }

    private void collectWorkflowsFromZipFile(final String path) {
        ILeveledImportStructureProvider provider = null;
        if (ArchiveFileManipulations.isTarFile(path)) {
            try {
                TarFile sourceTarFile = new TarFile(path);
                provider = new TarLeveledStructureProvider(sourceTarFile);
            } catch (Exception io) {
                // no file -> list stays empty
                setErrorMessage(Messages.getString("WorkflowImportSelectionPage.41") + path + Messages.getString("WorkflowImportSelectionPage.42")); //$NON-NLS-1$ //$NON-NLS-2$
            }
        } else if (ArchiveFileManipulations.isZipFile(path)) {
            try {
                ZipFile sourceFile = new ZipFile(path);
                provider = new ZipLeveledStructureProvider(sourceFile);
            } catch (Exception e) {
                // no file -> list stays empty
                setErrorMessage(Messages.getString("WorkflowImportSelectionPage.43") + path + Messages.getString("WorkflowImportSelectionPage.44")); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        // TODO: store only the workflows (dirs are created automatically)
        final ILeveledImportStructureProvider finalProvider = provider;
        if (provider != null) {
            // reset error
            setErrorMessage(null);
            try {
                getContainer().run(true, true, new IRunnableWithProgress() {
                    @Override
                    public void run(final IProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException {
                        Object child = finalProvider.getRoot();
                        m_importRoot = new WorkflowImportElementFromArchive(finalProvider, child, 0);
                        monitor.beginTask(Messages.getString("WorkflowImportSelectionPage.45"), IProgressMonitor.UNKNOWN); //$NON-NLS-1$
                        collectWorkflowsFromProvider((WorkflowImportElementFromArchive)m_importRoot, monitor);
                    }

                });
            } catch (Exception e) {
                String message = Messages.getString("WorkflowImportSelectionPage.46") + path; //$NON-NLS-1$
                IStatus status = new Status(IStatus.ERROR, KNIMEUIPlugin.PLUGIN_ID, message, e);
                setErrorMessage(message);
                LOGGER.error(message, e);
                ErrorDialog.openError(getShell(), Messages.getString("WorkflowImportSelectionPage.47"), null, status); //$NON-NLS-1$
            }
            validateWorkflows();
            m_workflowListUI.setInput(m_importRoot);
            m_workflowListUI.expandAll();
            m_workflowListUI.setAllChecked(true);
            m_workflowListUI.refresh(true);
        }
    }

    /**
     *
     * @param parent the archive element to collect the workflows from
     * @param monitor progress monitor
     */
    public void collectWorkflowsFromProvider(final WorkflowImportElementFromArchive parent,
        final IProgressMonitor monitor) {
        if (monitor.isCanceled()) {
            m_importRoot = null;
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    m_fromZipTextUI.setText(""); //$NON-NLS-1$
                }
            });
            return;
        }
        // public in order to make it possible to import from a given zip entry
        ILeveledImportStructureProvider provider = parent.getProvider();
        Object entry = parent.getEntry();
        if (parent.isWorkflow() || parent.isTemplate() || parent.isFile()) {
            // abort recursion
            return;
        }
        List children = provider.getChildren(entry);
        if (children == null) {
            return;
        }
        monitor.subTask(provider.getLabel(entry));
        Iterator childrenEnum = children.iterator();
        while (childrenEnum.hasNext()) {
            Object child = childrenEnum.next();
            WorkflowImportElementFromArchive childElement =
                new WorkflowImportElementFromArchive(provider, child, parent.getLevel() + 1);
            parent.addChild(childElement);
            if (provider.isFolder(child)) {
                collectWorkflowsFromProvider(childElement, monitor);
            }
        }
    }

    /**
     * Sets a predefined import root so that it is not selectable by the user.
     *
     * @param importRoot the workflow import element to import from (might be a zip entry)
     */
    public void setImportRoot(final IWorkflowImportElement importRoot) {
        // public in order to make it possible to use this wizard to import
        // workflows from a zip entry
        boolean isFile = importRoot instanceof WorkflowImportElementFromFile;
        m_fromDirUI.setSelection(isFile);
        m_fromZipUI.setSelection(!isFile);
        m_fromDirUI.setEnabled(false);
        m_fromZipUI.setEnabled(false);
        m_importRoot = importRoot;
    }

    public void enableTargetSelection(final boolean enable) {
        m_browseWorkflowGroupsBtn.setEnabled(enable);
    }

    /**
     *
     * @return true if there are some workflows selected which already exist in the target location
     */
    public boolean containsInvalidAndCheckedImports() {
        return m_invalidAndCheckedImports.size() > 0;
    }

    /**
     * Checks the tree for selected workflows which already exist in the target location.
     */
    protected void collectInvalidAndCheckedImports() {
        m_invalidAndCheckedImports.clear();
        m_validAndCheckedImports.clear();
        m_uncheckedImports.clear();
        if (m_importRoot == null) {
            return;
        }
        collectInvalids(m_validAndCheckedImports, m_invalidAndCheckedImports, m_uncheckedImports, m_importRoot);
    }

    /**
     *
     * @param valids list of valid workflows
     * @param invalids list of invalid (already existing) workflows
     * @param node current tree node
     */
    protected void collectInvalids(final Collection<IWorkflowImportElement> valids,
        final Collection<IWorkflowImportElement> invalids, final IWorkflowImportElement node) {
        collectInvalids(valids, invalids, null, node);
    }

    /**
     *
     * @param valids list of valid workflows
     * @param invalids list of invalid (already existing) workflows
     * @param unchecked list of unchecked workflows
     * @param node current tree node
     * @since 8.6
     */
    protected void collectInvalids(final Collection<IWorkflowImportElement> valids,
        final Collection<IWorkflowImportElement> invalids, final Collection<IWorkflowImportElement> unchecked,
        final IWorkflowImportElement node) {
        if (m_workflowListUI.getChecked(node)) {
            if (node.isInvalid()) {
                invalids.add(node);
            } else {
                valids.add(node);
            }
        } else if (unchecked != null && m_displayImports.contains(node)) {
            /* Only add the files that have been actually unchecked by the user,
             * i.e. don't exclude meta files. */
            unchecked.add(node);
        }
        for (IWorkflowImportElement child : node.getChildren()) {
            collectInvalids(valids, invalids, unchecked, child);
        }
    }

    /**
     * @return a list of top level elements (items below the root) selected in the import tree
     * @since 7.1
     */
    protected ArrayList<IWorkflowImportElement> getSelectedTopLevelElements() {
        ArrayList<IWorkflowImportElement> result = new ArrayList<IWorkflowImportElement>();
        if (m_importRoot == null) {
            return result;
        }
        for (IWorkflowImportElement topchild : m_importRoot.getChildren()) {
            if (m_workflowListUI.getChecked(topchild)) {
                result.add(topchild);
            }
        }
        return result;
    }

    /**
     * Collects those elements which have to be renamed (not their children) and those which have been renamed in order
     * to let the user change the name again.
     *
     * @param renameElements list to collect the elements which have to be renamed or were renamed
     * @param node element to check
     */
    protected void collectRenameElements(final Collection<IWorkflowImportElement> renameElements,
        final IWorkflowImportElement node) {
        if (m_workflowListUI.getChecked(node)) {
            if (node.isInvalid()) {
                // only add top elements to rename
                // a resource within an invalid folder is valid if the folder
                // is successfully renamed
                if (node.getParent().equals(m_importRoot)) {
                    renameElements.add(node);
                } else if (!node.getParent().isInvalid()) {
                    renameElements.add(node);
                }
            }
            // and we also want to be able to change the name of a renamed
            // element again
            if (!node.getOriginalName().equals(node.getName())) {
                renameElements.add(node);
            }
        }
        for (IWorkflowImportElement child : node.getChildren()) {
            collectRenameElements(renameElements, child);
        }
    }

    /**
     * Updates the wizard for the current selected import and target location.
     */
    public void validateWorkflows() {
        if (m_importRoot == null) {
            setPageComplete(false);
            // nothing to validate
            return;
        }
        setErrorMessage(null);
        if (m_target == null) {
            setPageComplete(false);
            setErrorMessage(Messages.getString("WorkflowImportSelectionPage.49")); //$NON-NLS-1$
            return;
        }

        // clear invalid list
        m_invalidAndCheckedImports.clear();
        // traverse over all items in the tree
        isValidImport(m_target, m_importRoot);
        collectInvalidAndCheckedImports();
        setPageComplete(!containsInvalidAndCheckedImports());
        updateMessages();
        // call can finish in order to update the buttons
        getWizard().canFinish();
        m_workflowListUI.refresh(true);
    }

    /**
     *
     * @return the destination path
     */
    protected AbstractExplorerFileStore getDestinationPath() {
        return m_target;
    }

    /**
     * Sets the invalid flag of the import element to true if the a resource with the same name already exists in the
     * destination location.
     *
     * @param destination the destination path
     * @param element the workflow import element to check
     */
    protected void isValidImport(final AbstractExplorerFileStore destination, final IWorkflowImportElement element) {
        // get path
        IPath childPath = element.getRenamedPath();
        boolean exists = false;
        if (childPath.segmentCount() > 0) {
            // append to the destination path
            AbstractExplorerFileStore result = destination.getChild(childPath.toString());
            // check whether this exists
            exists = result.fetchInfo().exists();
        }
        element.setInvalid(exists);
        if (!exists) {
            // user renames could lead to duplicates in the imported items (through back button)
            IWorkflowImportElement parent = element.getParent();
            if (parent != null) {
                for (IWorkflowImportElement c : parent.getChildren()) {
                    if (c != element && c.getRenamedPath().equals(element.getRenamedPath())) {
                        if (m_workflowListUI.getChecked(c)) {
                            element.setInvalid(true);
                        }
                        break;
                    }
                }
            }
        }
        for (IWorkflowImportElement child : element.getChildren()) {
            isValidImport(destination, child);
        }
    }

    /**
     *
     * @param destination the initial destination container
     */
    public void setInitialTarget(final AbstractExplorerFileStore destination) {
        m_initialDestination = destination;
    }

    /**
     * Saves the settings of this dialog.
     */
    protected void saveDialogSettings() {
        IDialogSettings settings = getDialogSettings();
        if (settings != null) {
            settings.put(KEY_ZIP_LOC, initialZipLocation);
            settings.put(KEY_DIR_LOC, initialDirLocation);
            settings.put(KEY_FROM_DIR, initialFromDir);
        }
        // last selected dir/file
    }

    /**
     * Restores the settings of this dialog from last opened wizard.
     */
    protected void restoreDialogSettings() {
        // TODO: restore the stored settings - if there are any
        IDialogSettings settings = getDialogSettings();
        if (settings != null) {
            initialZipLocation = settings.get(KEY_ZIP_LOC);
            initialDirLocation = settings.get(KEY_DIR_LOC);
            initialFromDir = settings.getBoolean(KEY_FROM_DIR);
        }
    }

    /**
     * Updates the wizard messages.
     */
    protected void updateMessages() {
        if (containsInvalidAndCheckedImports()) {
            setErrorMessage(Messages.getString("WorkflowImportSelectionPage.50") //$NON-NLS-1$
                + Messages.getString("WorkflowImportSelectionPage.51")); //$NON-NLS-1$
        } else {
            setErrorMessage(null);
        }
        if (m_importRoot == null) {
            setMessage(Messages.getString("WorkflowImportSelectionPage.52") //$NON-NLS-1$
                + Messages.getString("WorkflowImportSelectionPage.53"), INFORMATION); //$NON-NLS-1$
        } else if (m_validAndCheckedImports.size() == 0) {
            setMessage(Messages.getString("WorkflowImportSelectionPage.54") + Messages.getString("WorkflowImportSelectionPage.55"), //$NON-NLS-1$ //$NON-NLS-2$
                INFORMATION);
        } else {
            // clear message
            setMessage(null);
        }
    }

    /**
     * Clears everything, the import elements tree, messages, selected imports, use only if different location was
     * selected.
     */
    protected void clear() {
        // delete the rename page to prevent obsolete imports showing up
        setErrorMessage(null);
        m_renamePage = null;
        m_invalidAndCheckedImports.clear();
        m_validAndCheckedImports.clear();
        m_uncheckedImports.clear();
        m_displayImports.clear();
        m_importRoot = null;
        m_workflowListUI.setInput(null);
        m_workflowListUI.refresh();
    }

    /**
     *
     * @return true if there are checked and valid imports and no checked an invalid imports
     */
    public boolean canFinish() {
        // check if we have checked and valid imports
        if (!isCurrentPage()) {
            return m_renamePage != null && m_renamePage.canFinish();
        }
        return isPageComplete() && m_validAndCheckedImports.size() > 0 && (!containsInvalidAndCheckedImports());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCurrentPage() {
        // make this method publically available
        return super.isCurrentPage();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean canFlipToNextPage() {
        return !getRenameElements().isEmpty();
    }

    private Collection<IWorkflowImportElement> getRenameElements() {
        // use a set in order to ensure that each element is only added once
        // use the LinkedHashSet in order to achieve that the order is the same
        // when switching back and forth
        Collection<IWorkflowImportElement> rename = new LinkedHashSet<IWorkflowImportElement>();
        if (m_importRoot == null) {
            return rename;
        }
        collectRenameElements(rename, m_importRoot);
        return rename;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public IWizardPage getNextPage() {
        if (canFlipToNextPage()) {
            setMessage(null);
        }
        m_renamePage = createRenamePage();
        return m_renamePage;
    }

    /**
     *
     * @return the rename page populated with the top level elements which have to be renamed or which were renamed
     */
    private RenameWorkflowImportPage createRenamePage() {
        RenameWorkflowImportPage renamePage = null;
        Collection<IWorkflowImportElement> rename = getRenameElements();
        if (!rename.isEmpty()) {
            renamePage = new RenameWorkflowImportPage(this, rename);
            renamePage.setWizard(getWizard());
            renamePage.setPreviousPage(this);
        }
        return renamePage;
    }

}
