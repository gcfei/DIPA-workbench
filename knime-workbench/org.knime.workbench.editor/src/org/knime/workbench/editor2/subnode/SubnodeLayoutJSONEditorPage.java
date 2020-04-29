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
 *   Nov 16, 2015 (albrecht): created
 */
package org.knime.workbench.editor2.subnode;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Panel;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JApplet;
import javax.swing.JRootPane;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.web.WebTemplate;
import org.knime.core.node.web.WebViewContent;
import org.knime.core.node.wizard.ViewHideable;
import org.knime.core.node.wizard.WizardNode;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WebResourceController;
import org.knime.core.node.workflow.WorkflowLock;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.js.core.JavaScriptViewCreator;
import org.knime.js.core.layout.DefaultLayoutCreatorImpl;
import org.knime.js.core.layout.bs.JSONLayoutColumn;
import org.knime.js.core.layout.bs.JSONLayoutContent;
import org.knime.js.core.layout.bs.JSONLayoutPage;
import org.knime.js.core.layout.bs.JSONLayoutRow;
import org.knime.js.core.layout.bs.JSONLayoutViewContent;
import org.knime.js.core.layout.bs.JSONNestedLayout;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 *
 * @author Christian Albrecht, KNIME.com GmbH, Konstanz, Germany
 */
public class SubnodeLayoutJSONEditorPage extends WizardPage {

    private static NodeLogger LOGGER = NodeLogger.getLogger(SubnodeLayoutJSONEditorPage.class);

    private SubNodeContainer m_subNodeContainer;
    private WorkflowManager m_wfManager;
    private Map<NodeIDSuffix, ViewHideable> m_viewNodes;
    private String m_jsonDocument;
    private Label m_statusLine;
    private RSyntaxTextArea m_textArea;
    private int m_caretPosition = 5;
    private Text m_text;
    private List<Integer> m_documentNodeIDs = new ArrayList<Integer>();
    private boolean m_basicPanelAvailable = true;
    private final Map<NodeIDSuffix, BasicLayoutInfo> m_basicMap;
    private Composite m_basicComposite;
    private Label m_basicStatusLine;
    private NodeUsageComposite m_nodeUsageComposite;
    private Browser m_browser;
    private BrowserFunction m_visualLayoutUpdate;

    /**
     * Crates a new page instance with a given page name
     *
     * @param pageName the page name
     */
    protected SubnodeLayoutJSONEditorPage(final String pageName) {
        super(pageName);
        setDescription(Messages.SubnodeLayoutJSONEditorPage_0
            + Messages.SubnodeLayoutJSONEditorPage_1);
        m_jsonDocument = ""; //$NON-NLS-1$
        m_basicMap = new LinkedHashMap<NodeIDSuffix, BasicLayoutInfo>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createControl(final Composite parent) {
        TabFolder tabs = new TabFolder(parent, SWT.BORDER);

        TabItem usageTab = new TabItem(tabs, SWT.NONE);
        usageTab.setText(Messages.SubnodeLayoutJSONEditorPage_3);
        m_nodeUsageComposite = new NodeUsageComposite(tabs, m_viewNodes, m_subNodeContainer);
        usageTab.setControl(m_nodeUsageComposite);

        TabItem visualTab = new TabItem(tabs, SWT.NONE);
        visualTab.setText(Messages.SubnodeLayoutJSONEditorPage_4);

        TabItem basicTab = new TabItem(tabs, SWT.NONE);
        basicTab.setText(Messages.SubnodeLayoutJSONEditorPage_5);
        basicTab.setControl(createBasicComposite(tabs));

        TabItem jsonTab = new TabItem(tabs, SWT.NONE);
        jsonTab.setText(Messages.SubnodeLayoutJSONEditorPage_6);
        jsonTab.setControl(createJSONEditorComposite(tabs));

        // The visual layout tab should be the second tab, but its control should be made
        // after the Advanced tab. This ensures that the JSON document is created before
        // it is used in the creation of the visual layout tab
        visualTab.setControl(createVisualLayoutComposite(tabs));

        tabs.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                final String tabText = tabs.getSelection()[0].getText();
                applyUsageChanges();

                // clean JSON
                final ObjectMapper mapper = createObjectMapperForUpdating();
                final ObjectReader reader = mapper.readerForUpdating(new JSONLayoutPage());
                try {
                    JSONLayoutPage page = reader.readValue(m_jsonDocument);
                    cleanJSONPage(page);
                    m_jsonDocument = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(page);
                } catch (IOException ex) {
                    LOGGER.error(Messages.SubnodeLayoutJSONEditorPage_7 + ex.getMessage(), ex);
                }

                switch (tabText) {
                    case "Visual Layout":
                        updateVisualLayout();
                        break;
                    case "Basic Layout":
                        updateModelFromJson();
                        break;
                    case "Advanced Layout":
                        updateJsonTextArea();
                        break;
                }
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                // Do nothing
            }
        });

        tabs.setSelection(1);
        setControl(tabs);
    }

    boolean applyUsageChanges() {
        try (WorkflowLock lock = m_subNodeContainer.lock()) { // each node will cause lock acquisition, do it as bulk
            for (Entry<NodeID, Button> wUsage : m_nodeUsageComposite.getWizardUsageMap().entrySet()) {
                NodeID id = wUsage.getKey();
                boolean hide = !wUsage.getValue().getSelection();
                try {
                    m_subNodeContainer.setHideNodeFromWizard(id, hide);
                } catch (IllegalArgumentException e) {
                    LOGGER.error(Messages.SubnodeLayoutJSONEditorPage_11 + e.getMessage(), e);
                    return false;
                }
            }

            for (Entry<NodeID, Button> dUsage : m_nodeUsageComposite.getDialogUsageMap().entrySet()) {
                NodeID id = dUsage.getKey();
                boolean hide = !dUsage.getValue().getSelection();
                try {
                    m_subNodeContainer.setHideNodeFromDialog(id, hide);
                } catch (IllegalArgumentException e) {
                    LOGGER.error(Messages.SubnodeLayoutJSONEditorPage_12 + e.getMessage(), e);
                    return false;
                }
            }
        }

        return true;
    }

    private Composite createVisualLayoutComposite(final Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // Web resources
        final WebTemplate template = WebResourceController.getWebTemplateFromBundleID("knimeLayoutEditor_1.0.0"); //$NON-NLS-1$
        final WebTemplate dT = WebResourceController.getWebTemplateFromBundleID("knimeLayoutEditor_1.0.0_Debug"); //$NON-NLS-1$
        VisualLayoutViewCreator creator = new VisualLayoutViewCreator(template, dT);
        String html = ""; //$NON-NLS-1$
        try {
            html = creator.createWebResources(Messages.SubnodeLayoutJSONEditorPage_16, null, null, ""); //$NON-NLS-2$
        } catch (final IOException e) {
            LOGGER.error(Messages.SubnodeLayoutJSONEditorPage_18, e);
        }

        // Create browser
        m_browser = new Browser(composite, SWT.NONE);

        try {
            m_browser.setUrl(new File(html).toURI().toURL().toString());
        } catch (MalformedURLException e) {
            LOGGER.error(e.getMessage(), e);
        }
        m_browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // Create JSON Objects
        final List<VisualLayoutEditorJSONNode> nodes = createJSONNodeList();
        // ensure node layout is written the same as the metanode layout
        final ObjectMapper mapper = JSONLayoutPage.getConfiguredObjectMapper();
        String JSONNodes = ""; //$NON-NLS-1$
        try {
            JSONNodes = mapper.writeValueAsString(nodes);
        } catch (JsonProcessingException e) {
            LOGGER.error(Messages.SubnodeLayoutJSONEditorPage_20 + e.getMessage(), e);
        }
        // variables in progress listener must be final
        final String JSONLayout = getJsonDocument();
        final String jsonNodes = JSONNodes;
        m_browser.addProgressListener(new ProgressListener() {

            @Override
            public void changed(final ProgressEvent event) {
                // do nothing
            }

            @Override
            public void completed(final ProgressEvent event) {
                m_browser.evaluate(Messages.SubnodeLayoutJSONEditorPage_21 + jsonNodes + "\');"); //$NON-NLS-2$
                m_browser.evaluate(Messages.SubnodeLayoutJSONEditorPage_23 + JSONLayout + "\');"); //$NON-NLS-2$
            }
        });
        m_visualLayoutUpdate = new UpdateLayoutFunction(m_browser, "pushLayout"); //$NON-NLS-1$
        return composite;
    }

    private Composite createBasicComposite(final Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, true));
        GridData gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.GRAB_VERTICAL);
        composite.setLayoutData(gridData);
        ScrolledComposite scrollPane = new ScrolledComposite(composite, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
        scrollPane.setLayout(new FillLayout(SWT.VERTICAL));
        scrollPane.setExpandHorizontal(true);
        scrollPane.setExpandVertical(true);
        m_basicComposite = new Composite(scrollPane, SWT.NONE);
        scrollPane.setContent(m_basicComposite);
        scrollPane.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));

        fillBasicComposite();

        return composite;
    }

    private void fillBasicComposite() {
        if (!m_basicPanelAvailable) {
            m_basicComposite.setLayout(new GridLayout(1, true));
            GridData gridData = new GridData();
            gridData.grabExcessHorizontalSpace = true;
            gridData.horizontalAlignment = SWT.CENTER;
            gridData.grabExcessVerticalSpace = true;
            Label infoLabel = new Label(m_basicComposite, SWT.FILL);
            infoLabel
                .setText(Messages.SubnodeLayoutJSONEditorPage_26);
            infoLabel.setLayoutData(gridData);

            // Ensure scroll bar appears when composite data changes
            ((ScrolledComposite)m_basicComposite.getParent())
                .setMinSize(m_basicComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
            ((ScrolledComposite)m_basicComposite.getParent()).requestLayout();
            return;
        }

        m_basicComposite.setLayout(new GridLayout(7, false));
        m_basicComposite.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));

        Label titleLabel = new Label(m_basicComposite, SWT.LEFT);
        FontData fontData = titleLabel.getFont().getFontData()[0];
        Font boldFont =
            new Font(Display.getCurrent(), new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
        titleLabel.setText(Messages.SubnodeLayoutJSONEditorPage_27);
        titleLabel.setFont(boldFont);
        new Composite(m_basicComposite, SWT.NONE); /* Warning placeholder */
        Label rowLabel = new Label(m_basicComposite, SWT.CENTER);
        rowLabel.setText(Messages.SubnodeLayoutJSONEditorPage_28);
        rowLabel.setFont(boldFont);
        Label colLabel = new Label(m_basicComposite, SWT.CENTER);
        colLabel.setText(Messages.SubnodeLayoutJSONEditorPage_29);
        colLabel.setFont(boldFont);
        Label widthLabel = new Label(m_basicComposite, SWT.CENTER);
        widthLabel.setText(Messages.SubnodeLayoutJSONEditorPage_30);
        widthLabel.setFont(boldFont);
        new Composite(m_basicComposite, SWT.NONE); /* More placeholder */
        new Composite(m_basicComposite, SWT.NONE); /* Remove placeholder */

        for (final Entry<NodeIDSuffix, BasicLayoutInfo> entry : m_basicMap.entrySet()) {
            final NodeIDSuffix suffix = entry.getKey();
            final BasicLayoutInfo layoutInfo = entry.getValue();
            final NodeID nodeID = suffix.prependParent(m_subNodeContainer.getWorkflowManager().getID());
            final NodeContainer nodeContainer = m_viewNodes.containsKey(suffix) ? m_wfManager.getNodeContainer(nodeID) : null;

            createNodeLabelComposite(m_basicComposite, nodeID, nodeContainer);

            final Label warningLabel = new Label(m_basicComposite, SWT.CENTER);
            if (nodeContainer != null && m_viewNodes.get(suffix).isHideInWizard()) {
                warningLabel
                    .setImage(ImageRepository.getIconImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/layout/warning.png")); //$NON-NLS-1$
                warningLabel
                    .setToolTipText(Messages.SubnodeLayoutJSONEditorPage_32);
            }

            GridData gridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
            gridData.widthHint = 50;
            final Spinner rowSpinner =
                createBasicPanelSpinner(m_basicComposite, layoutInfo.getRow(), 1, 999);
            rowSpinner.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(final SelectionEvent e) {
                    layoutInfo.setRow(rowSpinner.getSelection());
                    tryUpdateJsonFromBasic();
                }
            });
            final Spinner colSpinner =
                createBasicPanelSpinner(m_basicComposite, layoutInfo.getCol(), 1, 99);
            colSpinner.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(final SelectionEvent e) {
                    layoutInfo.setCol(colSpinner.getSelection());
                    tryUpdateJsonFromBasic();
                }
            });
            final Spinner widthSpinner =
                createBasicPanelSpinner(m_basicComposite, layoutInfo.getColWidth(), 1, 12);
            widthSpinner.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(final SelectionEvent e) {
                    layoutInfo.setColWidth(widthSpinner.getSelection());
                    tryUpdateJsonFromBasic();
                }
            });

            final Button advancedButton = new Button(m_basicComposite, SWT.PUSH | SWT.CENTER);
            advancedButton
                .setImage(ImageRepository.getIconImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/layout/settings.png")); //$NON-NLS-1$
            advancedButton.setToolTipText(Messages.SubnodeLayoutJSONEditorPage_34);
            if (nodeContainer == null || !(nodeContainer instanceof NativeNodeContainer)) {
                advancedButton.setEnabled(false);
            } else {
                advancedButton.addSelectionListener(new SelectionAdapter() {

                    @Override
                    public void widgetSelected(final SelectionEvent e) {
                        JSONLayoutViewContent defaultViewContent =
                            DefaultLayoutCreatorImpl.getDefaultViewContentForNode(suffix, m_viewNodes.get(suffix));
                        ViewContentSettingsDialog settingsDialog =
                            new ViewContentSettingsDialog(m_basicComposite.getShell(),
                                (JSONLayoutViewContent)layoutInfo.getView(), defaultViewContent);
                        if (settingsDialog.open() == Window.OK) {
                            layoutInfo.setView(settingsDialog.getViewSettings());
                            tryUpdateJsonFromBasic();
                        }
                    }
                });
            }

            final Button removeButton = new Button(m_basicComposite, SWT.PUSH | SWT.CENTER);
            removeButton.setImage(ImageRepository.getIconImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/layout/remove.png")); //$NON-NLS-1$
            removeButton.setToolTipText(Messages.SubnodeLayoutJSONEditorPage_36);
            removeButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    if (nodeContainer != null) {
                        if (!MessageDialog.openConfirm(m_basicComposite.getShell(), Messages.SubnodeLayoutJSONEditorPage_37,
                            Messages.SubnodeLayoutJSONEditorPage_38 + suffix + Messages.SubnodeLayoutJSONEditorPage_39)) {
                            return;
                        }
                    }
                    m_basicMap.remove(suffix);
                    tryUpdateJsonFromBasic();
                    // repaint
                    updateModelFromJson();
                }
            });
        }

        for (final Entry<NodeIDSuffix, ViewHideable> entry : m_viewNodes.entrySet()) {
            if (m_basicMap.containsKey(entry.getKey())) {
                continue;
            }
            // Node not in layout
            NodeID nodeID = entry.getKey().prependParent(m_subNodeContainer.getWorkflowManager().getID());
            NodeContainer nodeContainer = m_wfManager.getNodeContainer(nodeID);
            createNodeLabelComposite(m_basicComposite, nodeID, nodeContainer);

            final Label warningLabel = new Label(m_basicComposite, SWT.CENTER);
            if (nodeContainer != null && m_viewNodes.get(entry.getKey()).isHideInWizard()) {
                warningLabel
                    .setImage(ImageRepository.getIconImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/layout/warning.png")); //$NON-NLS-1$
                warningLabel
                    .setToolTipText(Messages.SubnodeLayoutJSONEditorPage_41);
            }

            final Button addButton = new Button(m_basicComposite, SWT.PUSH | SWT.CENTER);
            addButton.setImage(ImageRepository.getIconImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/layout/add.png")); //$NON-NLS-1$
            addButton.setToolTipText(Messages.SubnodeLayoutJSONEditorPage_43);
            addButton.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(final SelectionEvent e) {
                    int lastRow = 0;
                    for (BasicLayoutInfo basicLayoutInfo : m_basicMap.values()) {
                        lastRow = Math.max(lastRow, basicLayoutInfo.getRow());
                    }
                    BasicLayoutInfo newInfo = new BasicLayoutInfo();
                    newInfo.setRow(lastRow + 1);
                    newInfo.setCol(1);
                    newInfo.setColWidth(12);
                    ViewHideable view = entry.getValue();
                    if (view instanceof SubNodeContainer) {
                        JSONNestedLayout nestedLayout = new JSONNestedLayout();
                        nestedLayout.setNodeID(entry.getKey().toString());
                        newInfo.setView(nestedLayout);
                    } else {
                        newInfo.setView(DefaultLayoutCreatorImpl.getDefaultViewContentForNode(entry.getKey(), view));
                    }
                    m_basicMap.put(entry.getKey(), newInfo);
                    tryUpdateJsonFromBasic();
                    // repaint
                    updateModelFromJson();
                }
            });
            GridData gridData = new GridData();
            gridData.horizontalSpan = 4;
            Label space = new Label(m_basicComposite, SWT.NONE);
            space.setLayoutData(gridData);
        }

        GridData fillRow = new GridData();
        fillRow.grabExcessHorizontalSpace = true;
        fillRow.horizontalAlignment = SWT.CENTER;
        fillRow.horizontalSpan = 7;

        Button resetButton = new Button(m_basicComposite, SWT.PUSH | SWT.CENTER);
        resetButton.setImage(ImageRepository.getIconImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/layout/reset.png")); //$NON-NLS-1$
        resetButton.setText(Messages.SubnodeLayoutJSONEditorPage_45);
        resetButton.setToolTipText(Messages.SubnodeLayoutJSONEditorPage_46);
        resetButton.setLayoutData(fillRow);
        resetButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (!MessageDialog.openConfirm(m_basicComposite.getShell(), Messages.SubnodeLayoutJSONEditorPage_47,
                    Messages.SubnodeLayoutJSONEditorPage_48)) {
                    return;
                }
                resetLayout();
                // repaint
                updateModelFromJson();
            }
        });

        // Add status line
        m_basicStatusLine = new Label(m_basicComposite, SWT.SHADOW_NONE | SWT.WRAP);
        m_basicStatusLine.setForeground(new Color(Display.getCurrent(), 255, 140, 0));
        final GridData statusGridData = new GridData(SWT.LEFT, SWT.BOTTOM, true, false);
        statusGridData.horizontalSpan = 7;
        statusGridData.heightHint =  new PixelConverter(m_basicStatusLine).convertHeightInCharsToPixels(2);
        statusGridData.minimumHeight = new PixelConverter(m_basicStatusLine).convertHeightInCharsToPixels(1);
        statusGridData.minimumWidth = (int) (m_basicComposite.getBounds().width * 0.75);
        m_basicStatusLine.setLayoutData(statusGridData);

        // Ensure scroll bar appears when composite data changes
        ((ScrolledComposite)m_basicComposite.getParent())
            .setMinSize(m_basicComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        ((ScrolledComposite)m_basicComposite.getParent()).requestLayout();
    }

    private Composite createNodeLabelComposite(final Composite parent, final NodeID nodeID,
        final NodeContainer nodeContainer) {

        Composite labelComposite = new Composite(m_basicComposite, SWT.NONE);
        labelComposite.setLayout(new GridLayout(2, false));
        labelComposite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

        Label iconLabel = new Label(labelComposite, SWT.CENTER);
        iconLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        if (nodeContainer == null) {
            iconLabel.setImage(ImageRepository.getIconImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/layout/missing.png")); //$NON-NLS-1$
            iconLabel.setToolTipText(Messages.SubnodeLayoutJSONEditorPage_50);
        } else if (nodeContainer instanceof SubNodeContainer) {
            iconLabel.setImage(ImageRepository.getIconImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/layout_16.png")); //$NON-NLS-1$
            iconLabel.setToolTipText(
                Messages.SubnodeLayoutJSONEditorPage_52);
        } else {
            try (InputStream iconURLStream = FileLocator.resolve(nodeContainer.getIcon()).openStream()) {
                iconLabel.setImage(new Image(Display.getCurrent(), iconURLStream));
            } catch (IOException e) {
                /* do nothing */ }
        }

        Label nodeLabel = new Label(labelComposite, SWT.LEFT);
        nodeLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        String nodeName;
        String annotation = null;
        if (nodeContainer == null) {
            nodeName = Messages.SubnodeLayoutJSONEditorPage_53;
            FontData font = nodeLabel.getFont().getFontData()[0];
            nodeLabel.setFont(
                new Font(m_basicComposite.getDisplay(), new FontData(font.getName(), font.getHeight(), SWT.ITALIC)));
            nodeLabel.setToolTipText(Messages.SubnodeLayoutJSONEditorPage_54);
        } else {
            nodeName = nodeContainer.getName();
            annotation = nodeContainer.getNodeAnnotation().getText();
            if (annotation.length() > 42) {
                nodeLabel.setToolTipText(annotation);
            }
            annotation = StringUtils.abbreviateMiddle(annotation, " [...] ", 50).replaceAll("[\n|\r]", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
        String nodeLabelText = nodeName + "\nID: " + nodeID.getIndex(); //$NON-NLS-1$
        if (StringUtils.isNoneBlank(annotation)) {
            nodeLabelText += "\n" + annotation; //$NON-NLS-1$
        }
        nodeLabel.setText(nodeLabelText);
        return labelComposite;
    }

    private static Spinner createBasicPanelSpinner(final Composite parent, final int initialValue, final int min,
        final int max) {
        Spinner spinner = new Spinner(parent, SWT.BORDER);
        spinner.setMinimum(min);
        spinner.setMaximum(max);
        spinner.setIncrement(1);
        spinner.setDigits(0);
        spinner.setSelection(initialValue);
        GridData gridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        gridData.widthHint = 50;
        if (Platform.OS_LINUX.equals(Platform.getOS())) {
            gridData.widthHint = 100;
        }
        spinner.setLayoutData(gridData);
        return spinner;
    }

    private Composite createJSONEditorComposite(final Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, true));
        composite.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));

        if (isWindows()) {

            Composite embedComposite = new Composite(composite, SWT.EMBEDDED | SWT.NO_BACKGROUND);
            final GridLayout gridLayout = new GridLayout();
            gridLayout.verticalSpacing = 0;
            gridLayout.marginWidth = 0;
            gridLayout.marginHeight = 0;
            gridLayout.horizontalSpacing = 0;
            embedComposite.setLayout(gridLayout);
            embedComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            Frame frame = SWT_AWT.new_Frame(embedComposite);
            Panel heavyWeightPanel = new Panel();
            heavyWeightPanel.setLayout(new BoxLayout(heavyWeightPanel, BoxLayout.Y_AXIS));
            frame.add(heavyWeightPanel);
            frame.setFocusTraversalKeysEnabled(false);

            // Use JApplet with JRootPane as layer in between heavyweightPanel and RTextScrollPane
            // This reduces flicker on resize in RSyntaxTextArea
            JApplet applet = new JApplet();
            JRootPane root = applet.getRootPane();
            Container contentPane = root.getContentPane();
            contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
            heavyWeightPanel.add(applet);

            m_textArea = new RSyntaxTextArea(10, 60);
            m_textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
            m_textArea.setCodeFoldingEnabled(true);
            m_textArea.setAntiAliasingEnabled(true);
            RTextScrollPane sp = new RTextScrollPane(m_textArea);
            sp.setDoubleBuffered(true);
            m_textArea.setText(m_jsonDocument);
            m_textArea.setEditable(true);
            m_textArea.setEnabled(true);
            contentPane.add(sp);

            Dimension size = sp.getPreferredSize();
            embedComposite.setSize(size.width, size.height);

            // forward focus to RSyntaxTextArea
            embedComposite.addFocusListener(new FocusAdapter() {

                @Override
                public void focusGained(final FocusEvent e) {
                    ViewUtils.runOrInvokeLaterInEDT(new Runnable() {
                        @Override
                        public void run() {
                            m_textArea.requestFocus();
                            m_textArea.setCaretPosition(m_caretPosition);
                        }
                    });
                }

                @Override
                public void focusLost(final FocusEvent e) {
                    // do nothing
                }
            });

            // delete content of status line, when something is inserted or deleted
            m_textArea.getDocument().addDocumentListener(new DocumentListener() {

                @Override
                public void changedUpdate(final DocumentEvent arg0) {
                    if (!composite.isDisposed()) {
                        composite.getDisplay().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                m_jsonDocument = m_textArea.getText();
                                if (m_statusLine != null && !m_statusLine.isDisposed()) {
                                    m_statusLine.setText(""); //$NON-NLS-1$
                                    isJSONValid();
                                }
                            }
                        });
                    }
                }

                @Override
                public void insertUpdate(final DocumentEvent arg0) {
                    /* do nothing */ }

                @Override
                public void removeUpdate(final DocumentEvent arg0) {
                    /* do nothing */ }

            });

            // remember caret position
            m_textArea.addCaretListener(new CaretListener() {
                @Override
                public void caretUpdate(final CaretEvent arg0) {
                    m_caretPosition = arg0.getDot();
                }
            });

        } else {
            m_text = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
            GridData layoutData = new GridData(GridData.FILL_BOTH);
            layoutData.widthHint = 600;
            layoutData.heightHint = 400;
            m_text.setLayoutData(layoutData);
            m_text.addModifyListener(new ModifyListener() {

                @Override
                public void modifyText(final ModifyEvent e) {
                    m_jsonDocument = m_text.getText();
                    if (m_statusLine != null && !m_statusLine.isDisposed()) {
                        m_statusLine.setText(""); //$NON-NLS-1$
                        isJSONValid();
                    }
                }
            });
            m_text.setText(m_jsonDocument);
        }

        // add status line
        m_statusLine = new Label(composite, SWT.SHADOW_NONE | SWT.WRAP);
        GridData statusGridData = new GridData(SWT.LEFT | SWT.FILL, SWT.BOTTOM, true, false);
        int maxHeight = new PixelConverter(m_statusLine).convertHeightInCharsToPixels(3);
        statusGridData.heightHint = maxHeight + 5;
        // seems to have no impact on the layout. The height will still be 3 rows (at least on Windows 8)
        statusGridData.minimumHeight = new PixelConverter(m_statusLine).convertHeightInCharsToPixels(1);
        m_statusLine.setLayoutData(statusGridData);
        compareNodeIDs();

        return composite;
    }

    /**
     * Sets all currently available view nodes on this editor page.
     *
     * @param manager the workflow manager
     * @param subnodeContainer the component container
     * @param viewNodes a map of all available view nodes
     */
    public void setNodes(final WorkflowManager manager, final SubNodeContainer subnodeContainer,
        final Map<NodeIDSuffix, ViewHideable> viewNodes) {
        m_wfManager = manager;
        m_subNodeContainer = subnodeContainer;
        m_viewNodes = viewNodes;
        JSONLayoutPage page = null;
        String layout = m_subNodeContainer.getLayoutJSONString();
        if (StringUtils.isNotEmpty(layout)) {
            try {
                ObjectMapper mapper = createObjectMapperForUpdating();
                page = mapper.readValue(layout, JSONLayoutPage.class);
                if (page.getRows() == null) {
                    page = null;
                } else {
                    cleanJSONPage(page);
                    m_jsonDocument = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(page);
                }
            } catch (IOException e) {
                LOGGER.error(Messages.SubnodeLayoutJSONEditorPage_62 + e.getMessage(), e);
                m_jsonDocument = layout;
            }
        }
        if (page == null) {
            page = resetLayout();
        }
        List<JSONLayoutRow> rows = page.getRows();
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            JSONLayoutRow row = rows.get(rowIndex);
            populateDocumentNodeIDs(row);
            processBasicLayoutRow(row, rowIndex);
        }
    }

    /**
     * @param page
     */
    private JSONLayoutPage resetLayout() {
        m_documentNodeIDs.clear();
        m_basicMap.clear();
        return generateInitialJson();
    }

    private JSONLayoutPage generateInitialJson() {
        JSONLayoutPage page = DefaultLayoutCreatorImpl.createDefaultLayoutStructure(m_viewNodes);
        ObjectMapper mapper = JSONLayoutPage.getConfiguredObjectMapper();
        try {
            String initialJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(page);
            m_jsonDocument = initialJson;
            return page;
        } catch (JsonProcessingException e) {
            LOGGER.error(Messages.SubnodeLayoutJSONEditorPage_63 + e.getMessage(), e);
            return null;
        }
    }

    private void populateDocumentNodeIDs(final JSONLayoutContent content) {
        if (content instanceof JSONLayoutRow) {
            JSONLayoutRow row = (JSONLayoutRow)content;
            if (row.getColumns() != null && row.getColumns().size() > 0) {
                for (JSONLayoutColumn col : row.getColumns()) {
                    if (col.getContent() != null && col.getContent().size() > 0) {
                        for (JSONLayoutContent c : col.getContent()) {
                            populateDocumentNodeIDs(c);
                        }
                    }
                }
            }
        } else if (content instanceof JSONLayoutViewContent) {
            String id = ((JSONLayoutViewContent)content).getNodeID();
            if (id != null && !id.isEmpty()) {
                m_documentNodeIDs.add(Integer.parseInt(id));
            }
        } else if (content instanceof JSONNestedLayout) {
            String id = ((JSONNestedLayout)content).getNodeID();
            if (id != null && !id.isEmpty()) {
                m_documentNodeIDs.add(Integer.parseInt(id));
            }
        }
    }

    private void updateBasicLayout() {
        ObjectMapper mapper = createObjectMapperForUpdating();
        JSONLayoutPage page = new JSONLayoutPage();
        ObjectReader reader = mapper.readerForUpdating(page);
        try {
            page = reader.readValue(getJsonDocument());
        } catch (Exception e) {
            /* do nothing, input needs to be validated beforehand */ }
        m_basicMap.clear();
        m_basicPanelAvailable = true;
        List<JSONLayoutRow> rows = page.getRows();
        if (rows == null) {
            return;
        }
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            JSONLayoutRow row = rows.get(rowIndex);
            populateDocumentNodeIDs(row);
            processBasicLayoutRow(row, rowIndex);
        }
    }

    /**
     * Processes one layout row, creates {@link BasicLayoutInfo} for each contained node and determines if basic layout
     * is available. Some advanced configurations (additional styles or classes, HTML content or nested layouts) can not
     * be represented in a basic layout.
     *
     * @param row the row to process
     * @param rowIndex the index of the row as it appears in the layout
     */
    private void processBasicLayoutRow(final JSONLayoutRow row, final int rowIndex) {
        if (listNotNullOrEmpty(row.getAdditionalStyles()) || listNotNullOrEmpty(row.getAdditionalClasses())) {
            // basic layout not possible, show only advanced tab
            m_basicPanelAvailable = false;
            return;
        }
        List<JSONLayoutColumn> columns = row.getColumns();
        for (int colIndex = 0; colIndex < columns.size(); colIndex++) {
            JSONLayoutColumn column = columns.get(colIndex);
            if (listNotNullOrEmpty(column.getAdditionalStyles()) || listNotNullOrEmpty(column.getAdditionalClasses())) {
                // basic layout not possible, show only advanced tab
                m_basicPanelAvailable = false;
                return;
            }
            List<JSONLayoutContent> content = column.getContent();
            if (content != null) {
                for (JSONLayoutContent jsonLayoutContent : content) {
                    if (jsonLayoutContent != null) {
                        boolean isView = jsonLayoutContent instanceof JSONLayoutViewContent;
                        boolean isNested = jsonLayoutContent instanceof JSONNestedLayout;
                        if (isView || isNested) {
                            NodeIDSuffix id;
                            if (isView) {
                                JSONLayoutViewContent viewContent = (JSONLayoutViewContent)jsonLayoutContent;
                                if (listNotNullOrEmpty(viewContent.getAdditionalStyles())
                                    || listNotNullOrEmpty(viewContent.getAdditionalClasses())) {
                                    // basic layout not possible, show only advanced tab
                                    m_basicPanelAvailable = false;
                                    return;
                                }
                                id = NodeIDSuffix.fromString(viewContent.getNodeID());
                            } else {
                                JSONNestedLayout nestedLayout = (JSONNestedLayout)jsonLayoutContent;
                                id = NodeIDSuffix.fromString(nestedLayout.getNodeID());
                            }
                            BasicLayoutInfo basicInfo = new BasicLayoutInfo();
                            basicInfo.setRow(rowIndex + 1);
                            basicInfo.setCol(colIndex + 1);
                            basicInfo.setColWidth(column.getWidthXS() != null ? column.getWidthXS() : column.getWidthMD());
                            basicInfo.setView(jsonLayoutContent);

                            m_basicMap.put(id, basicInfo);
                        } else {
                            // basic layout not possible, show only advanced tab
                            m_basicPanelAvailable = false;
                            return;
                        }
                    }
                }
            }
        }
    }

    private static boolean listNotNullOrEmpty(final List<?> list) {
        return list != null && list.size() > 0;
    }

    private void tryUpdateJsonFromBasic() {
        try {
            basicLayoutToJson();
        } catch (Exception e) {
            //TODO show error in dialog?, this should not happen
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void basicLayoutToJson() throws Exception {
        JSONLayoutPage page = new JSONLayoutPage();
        List<JSONLayoutRow> rows = new ArrayList<JSONLayoutRow>();
        for (BasicLayoutInfo basicLayoutInfo : m_basicMap.values()) {
            while (rows.size() < basicLayoutInfo.getRow()) {
                rows.add(new JSONLayoutRow());
            }
            JSONLayoutRow row = rows.get(basicLayoutInfo.getRow() - 1);
            if (row.getColumns() == null) {
                row.setColumns(new ArrayList<JSONLayoutColumn>());
            }
            List<JSONLayoutColumn> columns = row.getColumns();
            while (columns.size() < basicLayoutInfo.getCol()) {
                columns.add(new JSONLayoutColumn());
            }
            JSONLayoutColumn column = columns.get(basicLayoutInfo.getCol() - 1);
            int colWidth = basicLayoutInfo.getColWidth();
            // avoid creating responsive layout from basic editor, set only smallest col
            column.setWidthXS(colWidth);
            List<JSONLayoutContent> contentList = column.getContent();
            if (contentList == null) {
                contentList = new ArrayList<JSONLayoutContent>(1);
            }
            contentList.add(basicLayoutInfo.getView());
            column.setContent(contentList);
        }
        page.setRows(rows);
        final String errorMsg = cleanJSONPage(page);
        m_basicStatusLine.setText(errorMsg);
        ObjectMapper mapper = createObjectMapperForUpdating();
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(page);
        m_jsonDocument = json;
    }

    private static String cleanJSONPage(final JSONLayoutPage page) {
        if (page.getRows() == null) {
            return ""; //$NON-NLS-1$
        }
        String errorMsg = ""; //$NON-NLS-1$
        final List<JSONLayoutRow> cleanedRows = new ArrayList<>();
        boolean emptyRows = false;
        boolean emptyCols = false;
        for(final JSONLayoutRow row : page.getRows()) {
            if (!row.getColumns().isEmpty()) {
                final List<JSONLayoutColumn> cleanedColumns = new ArrayList<>();
                for (final JSONLayoutColumn col : row.getColumns()) {
                    if (col.getContent() != null) {
                        cleanedColumns.add(col);
                    } else {
                        emptyCols = true;
                    }
                }
                if (!cleanedColumns.isEmpty()) {
                    row.setColumns(cleanedColumns);
                    cleanedRows.add(row);
                } else {
                    emptyRows = true;
                }
            } else {
                emptyRows = true;
            }
        }
        if (emptyRows && !cleanedRows.isEmpty()) {
            errorMsg += Messages.SubnodeLayoutJSONEditorPage_66;
        }
        if (emptyCols && !cleanedRows.isEmpty()) {
            if (!errorMsg.isEmpty()) {
                errorMsg += "\n"; //$NON-NLS-1$
            }
            errorMsg += Messages.SubnodeLayoutJSONEditorPage_68;
        }
        page.setRows(cleanedRows);
        return errorMsg;
    }

    private void compareNodeIDs() {
        Set<Integer> missingIDs = new HashSet<Integer>();
        Set<Integer> notExistingIDs = new HashSet<Integer>(m_documentNodeIDs);
        Set<Integer> duplicateIDCheck = new HashSet<Integer>(m_documentNodeIDs);
        Set<Integer> duplicateIDs = new HashSet<Integer>();
        for (NodeIDSuffix id : m_viewNodes.keySet()) {
            int i = NodeID.fromString(id.toString()).getIndex();
            if (m_documentNodeIDs.contains(i)) {
                notExistingIDs.remove(i);
            } else {
                missingIDs.add(i);
            }
        }
        for (int id : m_documentNodeIDs) {
            if (!duplicateIDCheck.remove(id)) {
                duplicateIDs.add(id);
            }
        }
        StringBuilder error = new StringBuilder();
        if (notExistingIDs.size() > 0) {
            error.append(Messages.SubnodeLayoutJSONEditorPage_69);
            for (int id : notExistingIDs) {
                error.append(id);
                error.append(", "); //$NON-NLS-1$
            }
            error.setLength(error.length() - 2);
            if (missingIDs.size() > 0 || duplicateIDs.size() > 0) {
                error.append("\n"); //$NON-NLS-1$
            }
        }
        if (missingIDs.size() > 0) {
            error.append(Messages.SubnodeLayoutJSONEditorPage_72);
            for (int id : missingIDs) {
                error.append(id);
                error.append(", "); //$NON-NLS-1$
            }
            error.setLength(error.length() - 2);
            if (duplicateIDs.size() > 0) {
                error.append("\n"); //$NON-NLS-1$
            }
        }
        if (duplicateIDs.size() > 0) {
            error.append(Messages.SubnodeLayoutJSONEditorPage_75);
            for (int id : duplicateIDs) {
                error.append(id);
                error.append(", "); //$NON-NLS-1$
            }
            error.setLength(error.length() - 2);
        }
        if (error.length() > 0 && m_statusLine != null && !m_statusLine.isDisposed()) {
            int textWidth = isWindows() ? m_textArea.getSize().width : m_text.getSize().x;
            Point newSize = m_statusLine.computeSize(textWidth, m_statusLine.getSize().y, true);
            m_statusLine.setSize(newSize);
            m_statusLine.setForeground(new Color(Display.getCurrent(), 255, 140, 0));
            m_statusLine.setText(error.toString());
        }
    }

    private void updateModelFromJson() {
        if (isJSONValid()) {
            updateBasicLayout();
        } else {
            m_basicPanelAvailable = false;
        }
        for (Control control : m_basicComposite.getChildren()) {
            control.dispose();
        }
        fillBasicComposite();
        m_basicComposite.layout(true);
    }

    private void updateVisualLayout() {
        final List<VisualLayoutEditorJSONNode> nodes = createJSONNodeList();
        final ObjectMapper mapper = JSONLayoutPage.getConfiguredObjectMapper();
        String JSONNodes = ""; //$NON-NLS-1$
        try {
            JSONNodes = mapper.writeValueAsString(nodes);
        } catch (JsonProcessingException e) {
            LOGGER.error(Messages.SubnodeLayoutJSONEditorPage_78 + e.getMessage(), e);
        }
        m_browser.evaluate(Messages.SubnodeLayoutJSONEditorPage_79 + JSONNodes + "\');"); //$NON-NLS-2$
        m_browser.evaluate(Messages.SubnodeLayoutJSONEditorPage_81 + getJsonDocument() + "\');"); //$NON-NLS-2$
    }

    /**
     * @return true, if current JSON layout structure is valid
     */
    protected boolean isJSONValid() {
        ObjectMapper mapper = createObjectMapperForUpdating();
        ObjectReader reader = mapper.readerForUpdating(new JSONLayoutPage());
        try {
            String json = isWindows() ? m_textArea.getText() : m_jsonDocument;
            JSONLayoutPage page = reader.readValue(json);
            m_documentNodeIDs.clear();
            if (page.getRows() != null) {
                for (JSONLayoutRow row : page.getRows()) {
                    populateDocumentNodeIDs(row);
                }
                compareNodeIDs();
                final String msg = cleanJSONPage(page);
                if (msg != null && !msg.isEmpty() && m_statusLine != null && !m_statusLine.isDisposed()) {
                    int textWidth = isWindows() ? m_textArea.getSize().width : m_text.getSize().x;
                    Point newSize = m_statusLine.computeSize(textWidth, m_statusLine.getSize().y, true);
                    m_statusLine.setSize(newSize);
                    m_statusLine.setForeground(new Color(Display.getCurrent(), 255, 140, 0));
                    m_statusLine.setText(msg);
                }
            }
            return true;
        } catch (IOException | NumberFormatException e) {
            String errorMessage;
            if (e instanceof JsonProcessingException) {
                JsonProcessingException jsonException = (JsonProcessingException)e;
                Throwable cause = null;
                Throwable newCause = jsonException.getCause();
                while (newCause instanceof JsonProcessingException) {
                    if (cause == newCause) {
                        break;
                    }
                    cause = newCause;
                    newCause = cause.getCause();
                }
                if (cause instanceof JsonProcessingException) {
                    jsonException = (JsonProcessingException)cause;
                }
                errorMessage = jsonException.getOriginalMessage().split("\n")[0]; //$NON-NLS-1$
                JsonLocation location = jsonException.getLocation();
                if (location != null) {
                    errorMessage += Messages.SubnodeLayoutJSONEditorPage_84 + (location.getLineNr() + 1) + Messages.SubnodeLayoutJSONEditorPage_85 + location.getColumnNr();
                }
            } else {
                String message = e.getMessage();
                errorMessage = message;
            }
            if (m_statusLine != null && !m_statusLine.isDisposed()) {
                m_statusLine.setForeground(new Color(Display.getCurrent(), 255, 0, 0));
                m_statusLine.setText(errorMessage);
                int textWidth = isWindows() ? m_textArea.getSize().width : m_text.getSize().x;
                Point newSize = m_statusLine.computeSize(textWidth, m_statusLine.getSize().y, true);
                m_statusLine.setSize(newSize);
            }
        }
        return false;
    }

    /**
     * @return the jsonDocument
     */
    public String getJsonDocument() {
        // keep empty fields
        ObjectMapper mapper = createObjectMapperForUpdating();
        ObjectReader reader = mapper.readerForUpdating(new JSONLayoutPage());
        try {
            JSONLayoutPage page = reader.readValue(m_jsonDocument);
            String layoutString = mapper.writeValueAsString(page);
            return layoutString;
        } catch (IOException e) {
            LOGGER.error(Messages.SubnodeLayoutJSONEditorPage_86 + e.getMessage(), e);
        }

        return ""; //$NON-NLS-1$
    }

    private static boolean isWindows() {
        return Platform.OS_WIN32.equals(Platform.getOS());
    }

    private void updateJsonTextArea() {
        if (isWindows()) {
            m_textArea.setText(m_jsonDocument);
        } else {
            m_text.setText(m_jsonDocument);
        }
    }

    @Override
    public void dispose() {
        if (m_browser != null && !m_browser.isDisposed()) {
            m_browser.dispose();
        }
        if (m_visualLayoutUpdate != null && !m_visualLayoutUpdate.isDisposed()) {
            m_visualLayoutUpdate.dispose();
        }
        m_browser = null;
        m_visualLayoutUpdate = null;
        super.dispose();
    }

    /**
     * @return an {@link ObjectMapper} configured to skip non-empty fields, with the exception of empty content fields
     */
    private static ObjectMapper createObjectMapperForUpdating() {
        final ObjectMapper mapper = JSONLayoutPage.getConfiguredObjectMapper();
        final SimpleModule module = new SimpleModule();
        module.addSerializer(new JSONLayoutColumnSerializer());
        mapper.registerModule(module);
        return mapper;
    }

    private List<VisualLayoutEditorJSONNode> createJSONNodeList() {
        final List<VisualLayoutEditorJSONNode> nodes = new ArrayList<>();
        for (final Entry<NodeIDSuffix, ViewHideable> viewNode : m_viewNodes.entrySet()) {
            final ViewHideable node = viewNode.getValue();
            final NodeID nodeID = viewNode.getKey().prependParent(m_subNodeContainer.getWorkflowManager().getID());
            final NodeContainer nodeContainer = m_wfManager.getNodeContainer(nodeID);
            final VisualLayoutEditorJSONNode jsonNode =
                new VisualLayoutEditorJSONNode(nodeContainer.getID().getIndex(), nodeContainer.getName(),
                    nodeContainer.getNodeAnnotation().getText(), getLayout(viewNode.getValue(), viewNode.getKey()),
                    getIcon(nodeContainer), !node.isHideInWizard(), getType(node));
            nodes.add(jsonNode);
        }
        return nodes;
    }

    private static JSONLayoutContent getLayout(final ViewHideable node, final NodeIDSuffix id) {
        if (node instanceof SubNodeContainer) {
            final JSONNestedLayout layout = new JSONNestedLayout();
            layout.setNodeID(id.toString());
            return layout;
        }
        return DefaultLayoutCreatorImpl.getDefaultViewContentForNode(id, node);
    }

    private static String getType(final ViewHideable node) {
        final boolean isWizardNode = node instanceof WizardNode;
        if (isWizardNode) {
            if (node instanceof DialogNode) {
                return "quickform"; //$NON-NLS-1$
            }
            return "view"; //$NON-NLS-1$
        }
        if (node instanceof SubNodeContainer) {
            return "nestedLayout"; //$NON-NLS-1$
        }
        throw new IllegalArgumentException(Messages.SubnodeLayoutJSONEditorPage_91 + node.getClass());
    }

    private static String getIcon(final NodeContainer nodeContainer) {
        if (nodeContainer == null) {
            return createIcon(ImageRepository.getIconImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/layout/missing.png")); //$NON-NLS-1$
        }
        String iconBase64 = ""; //$NON-NLS-1$
        if (nodeContainer instanceof SubNodeContainer) {
            return createIcon(ImageRepository.getIconImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/layout_16.png")); //$NON-NLS-1$
        }
        try {
            final URL url = FileLocator.resolve(nodeContainer.getIcon());
            final String mimeType = URLConnection.guessContentTypeFromName(url.getFile());
            byte[] imageBytes = null;
            try (InputStream s = url.openStream()) {
                imageBytes = IOUtils.toByteArray(s);
            }
            iconBase64 = "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(imageBytes); //$NON-NLS-1$ //$NON-NLS-2$
        } catch (final IOException e) {
            // Do nothing
        }

        if (iconBase64.isEmpty()) {
            return createIcon(ImageRepository.getIconImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/layout/missing.png")); //$NON-NLS-1$
        }
        return iconBase64;
    }

    private static String createIcon(final Image i) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ImageLoader loader = new ImageLoader();
        loader.data = new ImageData[]{i.getImageData()};
        loader.save(out, SWT.IMAGE_PNG);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray()); //$NON-NLS-1$
    }

    private static class BasicLayoutInfo {

        private int m_row;
        private int m_col;
        private int m_colWidth;
        private JSONLayoutContent m_view;

        /**
         * @return the row
         */
        public int getRow() {
            return m_row;
        }

        /**
         * @param row the row to set
         */
        public void setRow(final int row) {
            m_row = row;
        }

        /**
         * @return the col
         */
        public int getCol() {
            return m_col;
        }

        /**
         * @param col the col to set
         */
        public void setCol(final int col) {
            m_col = col;
        }

        /**
         * @return the colWidth
         */
        public int getColWidth() {
            return m_colWidth;
        }

        /**
         * @param colWidth the colWidth to set
         */
        public void setColWidth(final int colWidth) {
            m_colWidth = colWidth;
        }

        /**
         * @return the view
         */
        JSONLayoutContent getView() {
            return m_view;
        }

        /**
         * @param view the view to set
         */
        void setView(final JSONLayoutContent view) {
            m_view = view;
        }

    }

    private static final class VisualLayoutViewCreator extends JavaScriptViewCreator<WebViewContent, WebViewContent> {
        VisualLayoutViewCreator(final WebTemplate template, final WebTemplate debugTemplate) {
            super(null);
            setWebTemplate(isDebug() ? debugTemplate : template);
        }
    }

    private class UpdateLayoutFunction extends BrowserFunction {

        /**
         * @param browser
         * @param name
         */
        public UpdateLayoutFunction(final Browser browser, final String name) {
            super(browser, name);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object function(final Object[] arguments) {
            if (arguments == null || arguments.length < 1) {
                return false;
            }
            final String layout = arguments[0].toString();
            final ObjectMapper mapper = createObjectMapperForUpdating();
            JSONLayoutPage page = new JSONLayoutPage();
            final ObjectReader reader = mapper.readerForUpdating(page);
            try {
                page = reader.readValue(layout);
            } catch (Exception e) {
                LOGGER.error(Messages.SubnodeLayoutJSONEditorPage_99 + e.getMessage(), e);
                return false;
            }

            try {
                final String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(page);
                m_jsonDocument = json;
            } catch (Exception e) {
                LOGGER.error(Messages.SubnodeLayoutJSONEditorPage_100 + e.getMessage(), e);
                return false;
            }

            return true;
        }

    }

    /**
     * Custom serializer for {@link JSONLayoutColumn}. This will only serialize non-empty fields with the exception of
     * "content" which can be empty but not null. This was needed because there's no way to override Jackson's
     * serialization inclusion rule.
     */
    private static final class JSONLayoutColumnSerializer extends StdSerializer<JSONLayoutColumn> {

        private static final long serialVersionUID = 1L;

        protected JSONLayoutColumnSerializer() {
            super(JSONLayoutColumn.class);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void serialize(final JSONLayoutColumn value, final JsonGenerator gen,
            final SerializerProvider serializers) throws IOException, JsonProcessingException {
            final List<String> additionalClasses = value.getAdditionalClasses();
            final List<String> additionalStyles = value.getAdditionalStyles();
            final List<JSONLayoutContent> content = value.getContent();
            final Integer widthLG = value.getWidthLG();
            final Integer widthMD = value.getWidthMD();
            final Integer widthSM = value.getWidthSM();
            final Integer widthXL = value.getWidthXL();
            final Integer widthXS = value.getWidthXS();
            gen.writeStartObject();
            if (additionalClasses != null && !additionalClasses.isEmpty()) {
                gen.writeArrayFieldStart("additionalClasses"); //$NON-NLS-1$
                for (final String s : additionalClasses) {
                    gen.writeString(s);
                }
                gen.writeEndArray();
            }
            if (additionalStyles != null && !additionalStyles.isEmpty()) {
                gen.writeArrayFieldStart("additionalStyles"); //$NON-NLS-1$
                for (final String s : additionalStyles) {
                    gen.writeString(s);
                }
                gen.writeEndArray();
            }
            if (content != null) {
                gen.writeArrayFieldStart("content"); //$NON-NLS-1$
                for (final JSONLayoutContent c : content) {
                    gen.writeObject(c);
                }
                gen.writeEndArray();
            }
            if (widthLG != null) {
                gen.writeNumberField("widthLG", widthLG); //$NON-NLS-1$
            }
            if (widthMD != null) {
                gen.writeNumberField("widthMD", widthMD); //$NON-NLS-1$
            }
            if (widthSM != null) {
                gen.writeNumberField("widthSM", widthSM); //$NON-NLS-1$
            }
            if (widthXL != null) {
                gen.writeNumberField("widthXL", widthXL); //$NON-NLS-1$
            }
            if (widthXS != null) {
                gen.writeNumberField("widthXS", widthXS); //$NON-NLS-1$
            }
            gen.writeEndObject();
        }
    }
}
