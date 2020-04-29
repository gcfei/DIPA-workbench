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
 *   Oct 31, 2019 (loki): created
 */
package org.knime.workbench.descriptionview.metadata;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.descriptionview.metadata.atoms.LinkMetaInfoAtom;
import org.knime.workbench.descriptionview.metadata.atoms.MetaInfoAtom;
import org.knime.workbench.editor2.directannotationedit.FlatButton;
import org.knime.workbench.repository.util.NodeUtil;

/**
 * This is the abstract view reponsible for displaying, and potentially allowing the editing of, the meta-information
 * containing attributes such as:
 *      . description
 *      . tags
 *      . links
 *      . license
 *      . author
 *
 * The genesis for the concrete subclass workflow view is https://knime-com.atlassian.net/browse/AP-11628
 * The genesis for the concrete subclass component view, and so therefore the abstraction of this superclass, is
 *      https://knime-com.atlassian.net/browse/AP-12738
 *
 * @author loki der quaeler
 */
public abstract class AbstractMetaView extends ScrolledComposite implements AbstractMetadataModelFacilitator.ModelObserver {
    /** Display font which the author read-only should use. **/
    public static final Font ITALIC_CONTENT_FONT;
    /** Display font which the read-only versions of metadata should use. **/
    public static final Font VALUE_DISPLAY_FONT;
    /** Font which should be used with the n-ary close character. **/
    public static final Font BOLD_CONTENT_FONT;
    /** The read-only text color. **/  // in 4.0.0 was: 128, 128, 128
    public static final Color TEXT_COLOR = new Color(PlatformUI.getWorkbench().getDisplay(), 62, 58, 57);
    /** The fill color for the header bar and other widgets (like tag chiclets.) **/
    public static final Color GENERAL_FILL_COLOR = new Color(PlatformUI.getWorkbench().getDisplay(), 240, 240, 242);

    /** Available for subclasses **/
    public static final Color SECTION_LABEL_TEXT_COLOR = TEXT_COLOR;

    /** AP-12082 **/
    protected static final boolean SHOW_LICENSE_ONLY_FOR_HUB = true;

    /**
     * Subclasses can use this to describe what sections of the view should not be displayed to the user.
     */
    protected enum HiddenSection {
            /** The title section **/
            TITLE,
            /** The description section **/
            DESCRIPTION,
            /**
             * The upper section (which subclasses will populate via
             * {@link AbstractMetaView#populateUpperSection(Composite)})
             */
            UPPER,
            /** The tags section **/
            TAGS,
            /** The links section **/
            LINKS,
            /**
             * The license section - if the subclass view has its own logic as to whether to display this, then they
             * should not specify this enum value.
             */
            LICENSE,
            /** The creation date section **/
            CREATION_DATE,
            /** The author section **/
            AUTHOR,
            /**
             * The upper section (which subclasses will populate via
             * {@link AbstractMetaView#populateLowerSection(Composite)}
             */
            LOWER;
    }

    /*private static final String NO_TITLE_TEXT = "No title has been set yet.";*/
    private static final String NO_TITLE_TEXT = Messages.getString("AbstractMetaView.0"); //$NON-NLS-1$
    private static final String NO_DESCRIPTION_TEXT = Messages.getString("AbstractMetaView.1"); //$NON-NLS-1$
    private static final String NO_LINKS_TEXT = Messages.getString("AbstractMetaView.2"); //$NON-NLS-1$
    private static final String NO_TAGS_TEXT = Messages.getString("AbstractMetaView.3"); //$NON-NLS-1$

    private static final String SERVER_WORKFLOW_TEXT =
        Messages.getString("AbstractMetaView.4") //$NON-NLS-1$
            + Messages.getString("AbstractMetaView.5"); //$NON-NLS-1$

    private static final String SERVER_WORKFLOW_FETCH_FAILED_TEXT =
            Messages.getString("AbstractMetaView.6") //$NON-NLS-1$
                + Messages.getString("AbstractMetaView.7"); //$NON-NLS-1$

    private static final String NOT_READABLE_TEXT =
        Messages.getString("AbstractMetaView.8"); //$NON-NLS-1$

    private static final String NO_METADATA_TEXT =
        Messages.getString("AbstractMetaView.9"); //$NON-NLS-1$

    private static final Font HEADER_FONT;
    private static final Color HEADER_BORDER_COLOR = new Color(PlatformUI.getWorkbench().getDisplay(), 229, 229, 229);
    private static final Color HEADER_TEXT_COLOR = new Color(PlatformUI.getWorkbench().getDisplay(), 87, 87, 87);

    private static final Image CANCEL_IMAGE =
        ImageRepository.getImage(KNIMEEditorPlugin.PLUGIN_ID, "/icons/meta-view-cancel.png"); //$NON-NLS-1$

    private static final Image EDIT_IMAGE =
        ImageRepository.getImage(KNIMEEditorPlugin.PLUGIN_ID, "/icons/meta-view-edit.png"); //$NON-NLS-1$

    private static final Image SAVE_IMAGE =
        ImageRepository.getImage(KNIMEEditorPlugin.PLUGIN_ID, "/icons/meta-view-save.png"); //$NON-NLS-1$

    private static final int MINIMUM_CONTENT_PANE_WIDTH = 300;

    private static final int HEADER_VERTICAL_INSET = 10;
    private static final int HEADER_TEXT_DRAW_Y = HEADER_VERTICAL_INSET + 5;
    private static final int HEADER_MARGIN_RIGHT = 9;
    private static final int LEFT_INDENT_HEADER_SUB_PANES = 9;
    private static final int TOTAL_HEADER_PADDING
            = HEADER_MARGIN_RIGHT + (2 * LEFT_INDENT_HEADER_SUB_PANES) + (new GridLayout()).horizontalSpacing;
    private static final int CONTENT_VERTICAL_INDENT = 30 + (2 * HEADER_VERTICAL_INSET);

    static {
        final Optional<Object> headerFontSize =
            PlatformSpecificUIisms.getDetail(PlatformSpecificUIisms.HEADER_FONT_SIZE_DETAIL);
        final Optional<Object> contentFontSize =
            PlatformSpecificUIisms.getDetail(PlatformSpecificUIisms.CONTENT_FONT_SIZE_DETAIL);
        final int headerSize = headerFontSize.isPresent() ? ((Integer)headerFontSize.get()).intValue() : 16;
        final int contentSize = contentFontSize.isPresent() ? ((Integer)contentFontSize.get()).intValue() : 12;

        @SuppressWarnings("resource")   // stream is closed in loadFontFromInputStream(...)
        final InputStream is = NodeUtil.class.getResourceAsStream("Proboto-Bold.ttf"); //$NON-NLS-1$
        Optional<Font> f = SWTUtilities.loadFontFromInputStream(is, contentSize, SWT.BOLD);

        if (f.isPresent()) {
            BOLD_CONTENT_FONT = f.get();
        } else {
            NodeLogger.getLogger(AbstractMetaView.class).warn("Could not load bold font."); //$NON-NLS-1$
            BOLD_CONTENT_FONT = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
        }


        // We do this annoying new declaration of InputStream instances because we otherwise can't apply
        //      the warning suppression to anywhere but at class level
        @SuppressWarnings("resource")   // stream is closed in loadFontFromInputStream(...)
        final InputStream is2 = NodeUtil.class.getResourceAsStream("Proboto-Regular.ttf"); //$NON-NLS-1$
        f = SWTUtilities.loadFontFromInputStream(is2, contentSize, SWT.NORMAL);

        if (f.isPresent()) {
            VALUE_DISPLAY_FONT = f.get();
        } else {
            NodeLogger.getLogger(AbstractMetaView.class).warn("Could not load regular font."); //$NON-NLS-1$
            VALUE_DISPLAY_FONT = JFaceResources.getFont(JFaceResources.DIALOG_FONT);
        }


        // We do this annoying new declaration of InputStream instances because we otherwise can't apply
        //      the warning suppression to anywhere but at class level
        @SuppressWarnings("resource")   // stream is closed in loadFontFromInputStream(...)
        final InputStream is3 = NodeUtil.class.getResourceAsStream("Proboto-Italic.ttf"); //$NON-NLS-1$
        f = SWTUtilities.loadFontFromInputStream(is3, contentSize, SWT.ITALIC);

        if (f.isPresent()) {
            ITALIC_CONTENT_FONT = f.get();
        } else {
            NodeLogger.getLogger(AbstractMetaView.class).warn("Could not load italic font."); //$NON-NLS-1$
            ITALIC_CONTENT_FONT = JFaceResources.getFontRegistry().getItalic(JFaceResources.DIALOG_FONT);
        }


        final FontData[] baseFD = BOLD_CONTENT_FONT.getFontData();
        final FontData headerFD = new FontData(baseFD[0].getName(), headerSize, baseFD[0].getStyle());
        HEADER_FONT = new Font(PlatformUI.getWorkbench().getDisplay(), headerFD);
    }

    private static Text addLabelTextFieldCouplet(final Composite parent, final String labelText,
        final String placeholderText) {
        final Label l = new Label(parent, SWT.LEFT);
        l.setText(labelText);
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        l.setLayoutData(gd);

        final Text textField = createTextWithPlaceholderText(parent, placeholderText);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        textField.setLayoutData(gd);

        return textField;
    }

    private static Text createTextWithPlaceholderText(final Composite parent, final String text) {
        final Text textField = new Text(parent, SWT.BORDER);
        final GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        textField.setLayoutData(gd);

        textField.addPaintListener((event) -> {
            if (textField.getCharCount() == 0) {
                final GC gc = event.gc;
                final Rectangle size = textField.getClientArea();

                gc.setAdvanced(true);
                gc.setTextAntialias(SWT.ON);
                gc.setFont(ITALIC_CONTENT_FONT);
                gc.setForeground(TEXT_COLOR);
                gc.drawString(text, size.x + 3, size.y + 3, true);
            }
        });

        return textField;
    }


    /** This is presumed to be set in the subclass` {@link #selectionChanged(IStructuredSelection)} method **/
    protected String m_currentAssetName;

    /** This is presumed to be set in the subclass` {@link #selectionChanged(IStructuredSelection)} method **/
    protected AbstractMetadataModelFacilitator m_modelFacilitator;

    /** Dictates whether we allow the user to edit the current metadata being displayed **/
    protected final AtomicBoolean m_metadataCanBeEdited;

    /** Subclasses should set this to indicate whether we're waiting for an asynchronous delivery of the metadata. **/
    protected final AtomicBoolean m_waitingForAsynchronousMetadata;
    /** Subclasses should set this to indicate whether the asynchronous delivery of the metadata has failed. **/
    protected final AtomicBoolean m_asynchronousMetadataFetchFailed;

    // TODO consider condensing this and the following to a single enum typed variable
    /** Subclasses should set this to indicate whether the asset is a template. **/
    protected final AtomicBoolean m_assetRepresentsATemplate;
    /** Subclasses should set this to indicate whether the asset is a job. **/
    protected final AtomicBoolean m_assetRepresentsAJob;
    /** Subclasses should set this to indicate whether the asset is readable. */
    protected final AtomicBoolean m_assetIsReadable;

    /**
     * Subclasses should set this during selection change to indicate whether we should display the license section.
     */
    protected final AtomicBoolean m_shouldDisplayLicenseSection;

    private final EnumSet<HiddenSection> m_hiddenSections;

    private final Composite m_contentPane;

    private final Composite m_headerBar;
    private String m_headerText;
    private final Label m_headerLabelPlaceholder;

    private final AtomicInteger m_headerDrawX;
    private FlatButton m_editSaveButton;
    private final Composite m_headerButtonPane;

    private final Composite m_remoteServerNotificationPane;

    private final Composite m_remoteServerFailureNotificationPane;

    private final Composite m_notReadableNotificationPane;

    private final Composite m_noUsableMetadataNotificationPane;

    private final Composite m_titleSection;
    private final Composite m_titleNoDataPane;
    private final Composite m_titleContentPane;

    private final Composite m_descriptionSection;
    private final Composite m_descriptionNoDataLabelPane;
    private final Composite m_descriptionContentPane;

    private final Composite m_authorSection;
    private final Composite m_authorContentPane;

    // The upper section sits beneath the description section and above the tags section
    private final Composite m_upperSection;
    // The lower section sits beneath the author section - it is the last section of the view
    private final Composite m_lowerSection;

    private final Composite m_tagsSection;
    private final Composite m_tagsNoDataLabelPane;
    private final Composite m_tagsContentPane;
    private final Composite m_tagsAddContentPane;
    private final Composite m_tagsTagsContentPane;
    private Text m_tagAddTextField;
    private Button m_tagsAddButton;

    private final Composite m_linksSection;
    private final Composite m_linksNoDataLabelPane;
    private final Composite m_linksContentPane;
    private final Composite m_linksAddContentPane;
    private final Composite m_linksLinksContentPane;
    private Text m_linksAddURLTextField;
    private Text m_linksAddTitleTextField;
    private ComboViewer m_linksAddTypeComboViewer;
    private Button m_linksAddButton;

    private final Composite m_licenseSection;
    private final Composite m_licenseContentPane;

    private final Composite m_creationDateSection;
    private final Composite m_creationDateContentPane;

    private final AtomicBoolean m_inEditMode;

    private final AtomicBoolean m_assetNameHasChanged;

    private final AtomicInteger m_lastRenderedViewportWidth;
    private final AtomicInteger m_lastRenderedViewportOriginX;
    private final AtomicInteger m_lastRenderedViewportOriginY;
    private final FloatingHeaderBarPositioner m_floatingHeaderPositioner;

    /**
     * @param parent
     * @param hiddenSections 0 or more sections which will not be included in the view. This can be null.
     */
    protected AbstractMetaView(final Composite parent, final EnumSet<HiddenSection> hiddenSections) {
        super(parent, SWT.H_SCROLL | SWT.V_SCROLL);

        if (hiddenSections == null) {
            m_hiddenSections = EnumSet.noneOf(HiddenSection.class);
        } else {
            m_hiddenSections = hiddenSections;
        }

        m_inEditMode = new AtomicBoolean(false);
        m_metadataCanBeEdited = new AtomicBoolean(false);

        m_waitingForAsynchronousMetadata = new AtomicBoolean(false);
        m_asynchronousMetadataFetchFailed = new AtomicBoolean(false);
        m_assetRepresentsATemplate = new AtomicBoolean(false);
        m_assetRepresentsAJob = new AtomicBoolean(false);
        m_assetIsReadable = new AtomicBoolean(true);

        m_shouldDisplayLicenseSection = new AtomicBoolean(!SHOW_LICENSE_ONLY_FOR_HUB);
        m_assetNameHasChanged = new AtomicBoolean(false);

        m_lastRenderedViewportWidth = new AtomicInteger(Integer.MIN_VALUE);
        m_lastRenderedViewportOriginX = new AtomicInteger(Integer.MIN_VALUE);
        m_lastRenderedViewportOriginY = new AtomicInteger(Integer.MIN_VALUE);

        m_headerDrawX = new AtomicInteger(0);

        setBackgroundMode(SWT.INHERIT_DEFAULT);


        m_contentPane = new Composite(this, SWT.NONE);
        m_contentPane.setBackground(ColorConstants.white);
        setContent(m_contentPane);

        GridLayout gl = new GridLayout(1, false);
        gl.marginHeight = 0;
        gl.marginBottom = 3;
        gl.marginWidth = 3;
        m_contentPane.setLayout(gl);


        m_headerBar = new Composite(m_contentPane, SWT.NONE);
        GridData gd = new GridData();
        gd.exclude = true;
        m_headerBar.setLayoutData(gd);
        gl = new GridLayout(2, false);
        gl.marginHeight = HEADER_VERTICAL_INSET;
        gl.marginWidth = 0;
        gl.marginRight = HEADER_MARGIN_RIGHT;
        m_headerBar.setLayout(gl);
        m_headerBar.addPaintListener((event) -> {
            final GC gc = event.gc;
            final Rectangle size = m_headerBar.getClientArea();

            gc.setAdvanced(true);
            gc.setBackground(GENERAL_FILL_COLOR);
            gc.setForeground(HEADER_BORDER_COLOR);
            // the node description header bar actually renders as only the top left being a rounded rectangle
            //      so we fill and draw something larger, letting the clip sort it out, and the paint the east
            //      and south border lines individually.
            gc.fillRoundRectangle(size.x, size.y, size.width + 15, size.height + 15, 10, 10);
            final int x = size.x + 1;
            final int y = size.y + 1;
            gc.drawRoundRectangle(x, y, size.width + 15, size.height + 15, 10, 10);
            final int x2 = x + size.width - 2;
            final int y2 = y + size.height - 2;
            gc.drawLine(x2, y, x2, y2);
            gc.drawLine(x, y2, x2, y2);

            if (m_headerText != null) {
                gc.setFont(HEADER_FONT);
                gc.setForeground(HEADER_TEXT_COLOR);
                gc.setTextAntialias(SWT.ON);
                gc.drawString(m_headerText, m_headerDrawX.intValue(), HEADER_TEXT_DRAW_Y);
            }
        });

        m_headerLabelPlaceholder = new Label(m_headerBar, SWT.LEFT);
        m_headerLabelPlaceholder.setText(""); //$NON-NLS-1$
        gd = new GridData();
        gd.horizontalIndent = LEFT_INDENT_HEADER_SUB_PANES;
        m_headerLabelPlaceholder.setLayoutData(gd);
        m_headerLabelPlaceholder.setFont(HEADER_FONT);
        m_headerLabelPlaceholder.setVisible(false);

        m_headerButtonPane = new Composite(m_headerBar, SWT.NONE);
        m_headerButtonPane.setBackground(GENERAL_FILL_COLOR);
        gd = new GridData();
        gd.horizontalAlignment = SWT.RIGHT;
        gd.grabExcessHorizontalSpace = true;
        gd.heightHint = 24;
        gd.widthHint = 45;
        gd.horizontalIndent = LEFT_INDENT_HEADER_SUB_PANES;
        m_headerButtonPane.setLayoutData(gd);
        gl = new GridLayout(2, true);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        m_headerButtonPane.setLayout(gl);



        final Composite underHeaderBufferSpace = new Composite(m_contentPane, SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.heightHint = CONTENT_VERTICAL_INDENT + 8;
        underHeaderBufferSpace.setLayoutData(gd);



        m_remoteServerNotificationPane = new Composite(m_contentPane, SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        m_remoteServerNotificationPane.setLayoutData(gd);
        m_remoteServerNotificationPane.setLayout(new GridLayout(1, false));
        Label l = new Label(m_remoteServerNotificationPane, SWT.CENTER | SWT.WRAP);
        l.setText(SERVER_WORKFLOW_TEXT);
        l.setForeground(TEXT_COLOR);
        l.setFont(ITALIC_CONTENT_FONT);
        gd.horizontalAlignment = SWT.LEFT;
        gd.verticalAlignment = SWT.BOTTOM;
        gd.grabExcessHorizontalSpace = true;
        l.setLayoutData(gd);



        m_remoteServerFailureNotificationPane = new Composite(m_contentPane, SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        m_remoteServerFailureNotificationPane.setLayoutData(gd);
        m_remoteServerFailureNotificationPane.setLayout(new GridLayout(1, false));
        l = new Label(m_remoteServerFailureNotificationPane, SWT.CENTER | SWT.WRAP);
        l.setText(SERVER_WORKFLOW_FETCH_FAILED_TEXT);
        l.setForeground(TEXT_COLOR);
        l.setFont(ITALIC_CONTENT_FONT);
        gd.horizontalAlignment = SWT.LEFT;
        gd.verticalAlignment = SWT.BOTTOM;
        gd.grabExcessHorizontalSpace = true;
        l.setLayoutData(gd);

        m_notReadableNotificationPane = new Composite(m_contentPane, SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        m_notReadableNotificationPane.setLayoutData(gd);
        m_notReadableNotificationPane.setLayout(new GridLayout(1, false));
        l = new Label(m_notReadableNotificationPane, SWT.CENTER | SWT.WRAP);
        l.setText(NOT_READABLE_TEXT);
        l.setForeground(TEXT_COLOR);
        l.setFont(ITALIC_CONTENT_FONT);
        gd.horizontalAlignment = SWT.LEFT;
        gd.verticalAlignment = SWT.BOTTOM;
        gd.grabExcessHorizontalSpace = true;
        l.setLayoutData(gd);



        m_noUsableMetadataNotificationPane = new Composite(m_contentPane, SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        m_noUsableMetadataNotificationPane.setLayoutData(gd);
        m_noUsableMetadataNotificationPane.setLayout(new GridLayout(1, false));
        l = new Label(m_noUsableMetadataNotificationPane, SWT.CENTER | SWT.WRAP);
        l.setText(NO_METADATA_TEXT);
        l.setForeground(TEXT_COLOR);
        l.setFont(ITALIC_CONTENT_FONT);
        gd.horizontalAlignment = SWT.LEFT;
        gd.verticalAlignment = SWT.BOTTOM;
        gd.grabExcessHorizontalSpace = true;
        l.setLayoutData(gd);



        Composite[] sectionAndContentPane = createHorizontalSection(Messages.getString("AbstractMetaView.20"), NO_TITLE_TEXT); //$NON-NLS-1$
        m_titleSection = sectionAndContentPane[0];
        m_titleNoDataPane = sectionAndContentPane[1];
        m_titleContentPane = sectionAndContentPane[2];


        sectionAndContentPane = createVerticalSection(Messages.getString("AbstractMetaView.21"), NO_DESCRIPTION_TEXT); //$NON-NLS-1$
        m_descriptionSection = sectionAndContentPane[0];
        m_descriptionNoDataLabelPane = sectionAndContentPane[1];
        m_descriptionContentPane = sectionAndContentPane[2];


        m_upperSection = new Composite(m_contentPane, SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        m_upperSection.setLayoutData(gd);


        sectionAndContentPane = createVerticalSection(Messages.getString("AbstractMetaView.22"), NO_TAGS_TEXT); //$NON-NLS-1$
        m_tagsSection = sectionAndContentPane[0];
        m_tagsNoDataLabelPane = sectionAndContentPane[1];
        m_tagsContentPane = sectionAndContentPane[2];
        m_tagsAddContentPane = new Composite(m_tagsContentPane, SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.heightHint = 27;
        m_tagsAddContentPane.setLayoutData(gd);
        gl = new GridLayout(2, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        gl.horizontalSpacing = 3;
        m_tagsAddContentPane.setLayout(gl);
        m_tagsTagsContentPane = new Composite(m_tagsContentPane, SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        m_tagsTagsContentPane.setLayoutData(gd);
        final RowLayout rl = new RowLayout();
        rl.wrap = true;
        rl.pack = true;
        rl.type = SWT.HORIZONTAL;
        rl.marginWidth = 3;
        rl.marginHeight = 2;
        m_tagsTagsContentPane.setLayout(rl);


        sectionAndContentPane = createVerticalSection(Messages.getString("AbstractMetaView.23"), NO_LINKS_TEXT); //$NON-NLS-1$
        m_linksSection = sectionAndContentPane[0];
        m_linksNoDataLabelPane = sectionAndContentPane[1];
        m_linksContentPane = sectionAndContentPane[2];
        m_linksAddContentPane = new Composite(m_linksContentPane, SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        m_linksAddContentPane.setLayoutData(gd);
        gl = new GridLayout(2, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        gl.horizontalSpacing = 3;
        m_linksAddContentPane.setLayout(gl);
        m_linksLinksContentPane = new Composite(m_linksContentPane, SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        m_linksLinksContentPane.setLayoutData(gd);
        gl = new GridLayout(1, false);
        gl.marginHeight = 0;
        m_linksLinksContentPane.setLayout(gl);


        sectionAndContentPane = createHorizontalSection(Messages.getString("AbstractMetaView.24"), null); //$NON-NLS-1$
        m_licenseSection = sectionAndContentPane[0];
        m_licenseContentPane = sectionAndContentPane[1];
        gd = (GridData)m_licenseContentPane.getLayoutData();
        gd.heightHint = 24;
        m_licenseContentPane.setLayoutData(gd);


        sectionAndContentPane = createHorizontalSection(Messages.getString("AbstractMetaView.25"), null); //$NON-NLS-1$
        m_creationDateSection = sectionAndContentPane[0];
        m_creationDateContentPane = sectionAndContentPane[1];


        sectionAndContentPane = createHorizontalSection(Messages.getString("AbstractMetaView.26"), null); //$NON-NLS-1$
        m_authorSection = sectionAndContentPane[0];
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.TOP;
        gd.grabExcessHorizontalSpace = true;
        m_authorSection.setLayoutData(gd);
        m_authorContentPane = sectionAndContentPane[1];


        m_lowerSection = new Composite(m_contentPane, SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        m_lowerSection.setLayoutData(gd);


        if (!m_hiddenSections.contains(HiddenSection.UPPER)) {
            populateUpperSection(m_upperSection);
        }

        if (m_hiddenSections.contains(HiddenSection.LOWER)) {
            gd = (GridData)m_authorSection.getLayoutData();
            gd.grabExcessVerticalSpace = true;
            // avoiding future clone design issues and re-set-ing
            m_authorSection.setLayoutData(gd);

            gd = (GridData)m_lowerSection.getLayoutData();
            gd.exclude = true;
            // avoiding future clone design issues and re-set-ing
            m_lowerSection.setLayoutData(gd);
        } else {
            populateLowerSection(m_lowerSection);
        }


        configureFloatingHeaderBarButtons();

        SWTUtilities.spaceReclaimingSetVisible(m_remoteServerNotificationPane, false);
        SWTUtilities.spaceReclaimingSetVisible(m_remoteServerFailureNotificationPane, false);
        SWTUtilities.spaceReclaimingSetVisible(m_notReadableNotificationPane, false);
        SWTUtilities.spaceReclaimingSetVisible(m_noUsableMetadataNotificationPane, false);
        SWTUtilities.spaceReclaimingSetVisible(m_titleContentPane, false);
        SWTUtilities.spaceReclaimingSetVisible(m_descriptionContentPane, false);
        SWTUtilities.spaceReclaimingSetVisible(m_upperSection, false);
        SWTUtilities.spaceReclaimingSetVisible(m_tagsContentPane, false);
        SWTUtilities.spaceReclaimingSetVisible(m_linksContentPane, false);
        SWTUtilities.spaceReclaimingSetVisible(m_licenseSection, m_shouldDisplayLicenseSection.get());
        SWTUtilities.spaceReclaimingSetVisible(m_lowerSection, false);

        setMinWidth(MINIMUM_CONTENT_PANE_WIDTH);
        setMinHeight(625);
        setExpandHorizontal(true);
        setExpandVertical(true);

        addListener(SWT.Resize, (event) -> {
            updateFloatingHeaderBar();
        });

        ScrollBar sb = getHorizontalBar();
        if (sb != null) {
            sb.addListener(SWT.Selection, (event) -> {
                updateFloatingHeaderBar();
            });
        }
        sb = getVerticalBar();
        if (sb != null) {
            sb.addListener(SWT.Selection, (event) -> {
                updateFloatingHeaderBar();
            });
            sb.addListener(SWT.Show, (event) -> {
                updateFloatingHeaderBar();
            });
            sb.addListener(SWT.Hide, (event) -> {
                updateFloatingHeaderBar();
            });
        }

        pack();

        m_floatingHeaderPositioner = new FloatingHeaderBarPositioner();
    }

    /**
     * Provides an opportunity to populate the upper section. Anything may be affected on this Composite except for
     * the instance's own layout data. This will not be invoked if the instance was constructed specifying
     * {@link HiddenSection#UPPER}
     *
     * @param upperSection the {@link Composite} into which to populate
     */
    protected void populateUpperSection(final Composite upperSection) { }

    /**
     * Provides an opportunity to populate the lower section. Anything may be affected on this Composite except for
     * the instance's own layout data. This will not be invoked if the instance was constructed specifying
     * {@link HiddenSection#LOWER}
     *
     * @param lowerSection the {@link Composite} into which to populate
     */
    protected void populateLowerSection(final Composite lowerSection) { }

    /**
     * @return whether the view is currently in edit mode
     */
    public boolean inEditMode() {
        return m_inEditMode.get();
    }

    /**
     * @return whether the model is dirty - this is meaningless if {@link #inEditMode()} returns false
     */
    public boolean modelIsDirty() {
        return m_modelFacilitator.modelIsDirty();
    }

    /**
     * If the view is currently in edit mode, the mode is ended with either a save or cancel.
     *
     * @param shouldSave if true, then the model state is committed, otherwise restored.
     */
    public void endEditMode(final boolean shouldSave) {
        if (m_inEditMode.getAndSet(false)) {
            if (shouldSave) {
                performSave();
            } else {
                performDiscard();
            }
        }
    }

    /**
     * @param selection the selection passed along from the ISelectionListener
     */
    public abstract void selectionChanged(final IStructuredSelection selection);

    /**
     * This can be overridden by sublasses, as its implementation here does nothing. This method is invoked from inside
     * updateDisplay() once there exists metadata to populate with, after sections have been made visible and after the
     * default sections have been populated from the metadata, but before layout is invoked.
     */
    protected void updateLocalDisplay() {

    }

    /**
     * This can be invoked to refresh the view's contents.
     */
    protected final void updateDisplay() {
        boolean focusFirstEditElement = false;
        final boolean isReadable = m_assetIsReadable.get();

        if (m_waitingForAsynchronousMetadata.get() || m_asynchronousMetadataFetchFailed.get()
            || m_assetRepresentsATemplate.get() || m_assetRepresentsAJob.get() || !isReadable) {
            SWTUtilities.spaceReclaimingSetVisible(m_remoteServerNotificationPane,
                m_waitingForAsynchronousMetadata.get() && isReadable);
            SWTUtilities.spaceReclaimingSetVisible(m_remoteServerFailureNotificationPane,
                m_asynchronousMetadataFetchFailed.get());
            SWTUtilities.spaceReclaimingSetVisible(m_notReadableNotificationPane, !isReadable);
            SWTUtilities.spaceReclaimingSetVisible(m_noUsableMetadataNotificationPane,
                m_assetRepresentsATemplate.get() || m_assetRepresentsAJob.get());

            SWTUtilities.spaceReclaimingSetVisible(m_titleSection, false);
            SWTUtilities.spaceReclaimingSetVisible(m_descriptionSection, false);
            SWTUtilities.spaceReclaimingSetVisible(m_upperSection, false);
            SWTUtilities.spaceReclaimingSetVisible(m_tagsSection, false);
            SWTUtilities.spaceReclaimingSetVisible(m_linksSection, false);
            SWTUtilities.spaceReclaimingSetVisible(m_licenseSection, false);
            SWTUtilities.spaceReclaimingSetVisible(m_creationDateSection, false);
            SWTUtilities.spaceReclaimingSetVisible(m_authorSection, false);
            SWTUtilities.spaceReclaimingSetVisible(m_lowerSection, false);
        } else {
            final boolean editMode = m_inEditMode.get();

            focusFirstEditElement = editMode;

            SWTUtilities.spaceReclaimingSetVisible(m_remoteServerNotificationPane, false);
            SWTUtilities.spaceReclaimingSetVisible(m_remoteServerFailureNotificationPane, false);
            SWTUtilities.spaceReclaimingSetVisible(m_notReadableNotificationPane, false);
            SWTUtilities.spaceReclaimingSetVisible(m_noUsableMetadataNotificationPane, false);

            SWTUtilities.spaceReclaimingSetVisible(m_upperSection, !m_hiddenSections.contains(HiddenSection.UPPER));
            SWTUtilities.spaceReclaimingSetVisible(m_lowerSection, !m_hiddenSections.contains(HiddenSection.LOWER));


            if (m_hiddenSections.contains(HiddenSection.TITLE)) {
                SWTUtilities.spaceReclaimingSetVisible(m_titleSection, false);
            } else {
                SWTUtilities.spaceReclaimingSetVisible(m_titleSection, true);
                SWTUtilities.removeAllChildren(m_titleContentPane);

                final MetaInfoAtom mia = m_modelFacilitator.getTitle();
                if (editMode || mia.hasContent()) {
                    if (editMode) {
                        mia.populateContainerForEdit(m_titleContentPane);
                    } else {
                        mia.populateContainerForDisplay(m_titleContentPane);
                    }

                    SWTUtilities.spaceReclaimingSetVisible(m_titleNoDataPane, false);
                    SWTUtilities.spaceReclaimingSetVisible(m_titleContentPane, true);
                } else {
                    SWTUtilities.spaceReclaimingSetVisible(m_titleContentPane, false);
                    SWTUtilities.spaceReclaimingSetVisible(m_titleNoDataPane, true);
                }
            }

            if (m_hiddenSections.contains(HiddenSection.DESCRIPTION)) {
                SWTUtilities.spaceReclaimingSetVisible(m_descriptionSection, false);
            } else {
                SWTUtilities.spaceReclaimingSetVisible(m_descriptionSection, true);
                SWTUtilities.removeAllChildren(m_descriptionContentPane);

                final MetaInfoAtom mia = m_modelFacilitator.getDescription();
                if (editMode || mia.hasContent()) {
                    if (editMode) {
                        mia.populateContainerForEdit(m_descriptionContentPane);
                    } else {
                        mia.populateContainerForDisplay(m_descriptionContentPane);
                    }

                    SWTUtilities.spaceReclaimingSetVisible(m_descriptionNoDataLabelPane, false);
                    SWTUtilities.spaceReclaimingSetVisible(m_descriptionContentPane, true);
                } else {
                    SWTUtilities.spaceReclaimingSetVisible(m_descriptionContentPane, false);
                    SWTUtilities.spaceReclaimingSetVisible(m_descriptionNoDataLabelPane, true);
                }
            }

            if (m_hiddenSections.contains(HiddenSection.TAGS)) {
                SWTUtilities.spaceReclaimingSetVisible(m_tagsSection, false);
            } else {
                SWTUtilities.spaceReclaimingSetVisible(m_tagsSection, true);
                SWTUtilities.removeAllChildren(m_tagsAddContentPane);
                SWTUtilities.removeAllChildren(m_tagsTagsContentPane);

                final List<? extends MetaInfoAtom> atoms = m_modelFacilitator.getTags();
                if (editMode || (atoms.size() > 0)) {
                    if (editMode) {
                        SWTUtilities.spaceReclaimingSetVisible(m_tagsAddContentPane, true);
                        createTagsAddUI();
                    } else {
                        SWTUtilities.spaceReclaimingSetVisible(m_tagsAddContentPane, false);
                    }

                    atoms.stream().forEach((atom) -> {
                        if (editMode) {
                            atom.populateContainerForEdit(m_tagsTagsContentPane);
                        } else {
                            atom.populateContainerForDisplay(m_tagsTagsContentPane);
                        }
                    });

                    SWTUtilities.spaceReclaimingSetVisible(m_tagsNoDataLabelPane, false);
                    SWTUtilities.spaceReclaimingSetVisible(m_tagsContentPane, true);
                } else {
                    SWTUtilities.spaceReclaimingSetVisible(m_tagsContentPane, false);
                    SWTUtilities.spaceReclaimingSetVisible(m_tagsNoDataLabelPane, true);
                }
            }

            if (m_hiddenSections.contains(HiddenSection.LINKS)) {
                SWTUtilities.spaceReclaimingSetVisible(m_linksSection, false);
            } else {
                SWTUtilities.spaceReclaimingSetVisible(m_linksSection, true);
                SWTUtilities.removeAllChildren(m_linksAddContentPane);
                SWTUtilities.removeAllChildren(m_linksLinksContentPane);

                final List<? extends MetaInfoAtom> atoms = m_modelFacilitator.getLinks();
                if (editMode || (atoms.size() > 0)) {
                    if (editMode) {
                        SWTUtilities.spaceReclaimingSetVisible(m_linksAddContentPane, true);
                        createLinksAddUI();
                    } else {
                        SWTUtilities.spaceReclaimingSetVisible(m_linksAddContentPane, false);
                    }

                    atoms.stream().forEach((atom) -> {
                        if (editMode) {
                            atom.populateContainerForEdit(m_linksLinksContentPane);
                        } else {
                            atom.populateContainerForDisplay(m_linksLinksContentPane);
                        }
                    });

                    SWTUtilities.spaceReclaimingSetVisible(m_linksNoDataLabelPane, false);
                    SWTUtilities.spaceReclaimingSetVisible(m_linksContentPane, true);
                } else {
                    SWTUtilities.spaceReclaimingSetVisible(m_linksContentPane, false);
                    SWTUtilities.spaceReclaimingSetVisible(m_linksNoDataLabelPane, true);
                }
            }

            if (!m_shouldDisplayLicenseSection.get() || m_hiddenSections.contains(HiddenSection.LICENSE)) {
                SWTUtilities.spaceReclaimingSetVisible(m_licenseSection, false);
            } else {
                SWTUtilities.spaceReclaimingSetVisible(m_licenseSection, true);
                SWTUtilities.removeAllChildren(m_licenseContentPane);

                final MetaInfoAtom mia = m_modelFacilitator.getLicense();
                // We currently *always* have a license - this if block is 'just in case'
                if (editMode || mia.hasContent()) {
                    final GridData gd = (GridData)m_licenseContentPane.getLayoutData();
                    if (editMode) {
                        mia.populateContainerForEdit(m_licenseContentPane);
                        gd.verticalIndent = 0;
                    } else {
                        mia.populateContainerForDisplay(m_licenseContentPane);
                        gd.verticalIndent = Math.max(0, (gd.heightHint - 22));
                    }
                    m_licenseContentPane.setLayoutData(gd);
                }
            }

            if (m_hiddenSections.contains(HiddenSection.CREATION_DATE)) {
                SWTUtilities.spaceReclaimingSetVisible(m_creationDateSection, false);
            } else {
                SWTUtilities.spaceReclaimingSetVisible(m_creationDateSection, true);
                SWTUtilities.removeAllChildren(m_creationDateContentPane);

                final MetaInfoAtom mia = m_modelFacilitator.getCreationDate();
                if (editMode) {
                    mia.populateContainerForEdit(m_creationDateContentPane);
                } else {
                    mia.populateContainerForDisplay(m_creationDateContentPane);
                }
            }

            if (m_hiddenSections.contains(HiddenSection.AUTHOR)) {
                SWTUtilities.spaceReclaimingSetVisible(m_authorSection, false);
            } else {
                SWTUtilities.spaceReclaimingSetVisible(m_authorSection, true);
                SWTUtilities.removeAllChildren(m_authorContentPane);

                final MetaInfoAtom mia = m_modelFacilitator.getAuthor();
                if (editMode) {
                    mia.populateContainerForEdit(m_authorContentPane);
                } else {
                    mia.populateContainerForDisplay(m_authorContentPane);
                }
            }

            updateLocalDisplay();
        }

        layout(true, true);

        updateFloatingHeaderBar();

        updateMinimumSizes();

        if (focusFirstEditElement) {
            if (!m_hiddenSections.contains(HiddenSection.TITLE)) {
                m_modelFacilitator.getTitle().focus();
            } else if (!m_hiddenSections.contains(HiddenSection.DESCRIPTION)) {
                m_modelFacilitator.getDescription().focus();
            }
        }
    }

    private void updateMinimumSizes() {
        final Point minSize = m_contentPane.computeSize(m_contentPane.getParent().getSize().x, SWT.DEFAULT);
        final Point linksSize = m_linksLinksContentPane.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        final Point titleSize = m_titleSection.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        setMinWidth(Math.max(Math.max(titleSize.x, linksSize.x), MINIMUM_CONTENT_PANE_WIDTH));
        setMinHeight(minSize.y);
    }

    /**
     * This method will be call as the final invocation during the {@link #performSave()} method.
     */
    @SuppressWarnings("javadoc")
    protected abstract void completeSave();

    private void performSave() {
        m_inEditMode.set(false);

        // we must commit prior to updating display, else atoms may not longer have their UI elements
        //      available to query
        m_modelFacilitator.commitEdit();

        performPostEditModeTransitionActions();

        completeSave();
    }

    private void performDiscard() {
        m_inEditMode.set(false);

        // must restore state before updating display to have a display synced to the restored model
        m_modelFacilitator.restoreState();

        performPostEditModeTransitionActions();
    }

    private void performPostEditModeTransitionActions() {
        updateDisplay();

        configureFloatingHeaderBarButtons();

        m_editSaveButton = null;

        m_tagAddTextField = null;
        m_tagsAddButton = null;

        m_linksAddURLTextField = null;
        m_linksAddTitleTextField = null;
        m_linksAddTypeComboViewer = null;
        m_linksAddButton = null;
    }

    /**
     * Subclasses can invoke this to update the floating head bar's position and content.
     */
    protected void currentAssetNameHasChanged() {
        getDisplay().asyncExec(() -> {
            m_headerText = m_currentAssetName;
            m_assetNameHasChanged.set(true);
            if (!isDisposed()) {
                updateFloatingHeaderBar();
            }
        });
    }

    /**
     * This can be invoked by subclasses to update the buttons displayed in the floating header bar.
     */
    protected void configureFloatingHeaderBarButtons() {
        SWTUtilities.removeAllChildren(m_headerButtonPane);

        if (m_inEditMode.get()) {
            m_editSaveButton = new FlatButton(m_headerButtonPane, SWT.PUSH, SAVE_IMAGE, new Point(20, 20), true);
            GridData gd = (GridData)m_editSaveButton.getLayoutData();
            gd.verticalIndent += 3;
            m_editSaveButton.setLayoutData(gd);
            m_editSaveButton.addClickListener((source) -> {
                performSave();
            });
            m_editSaveButton.setHighlightAsCircle(true);

            final FlatButton fb = new FlatButton(m_headerButtonPane, SWT.PUSH, CANCEL_IMAGE, new Point(20, 20), true);
            gd = (GridData)fb.getLayoutData();
            gd.verticalIndent += 3;
            fb.setLayoutData(gd);
            fb.addClickListener((source) -> {
                performDiscard();
            });
            fb.setHighlightAsCircle(true);

            updateEditSaveButton();
        } else {
            final Label l = new Label(m_headerButtonPane, SWT.LEFT);
            l.setLayoutData(new GridData(20, 20));

            if (m_metadataCanBeEdited.get()) {
                final FlatButton fb = new FlatButton(m_headerButtonPane, SWT.PUSH, EDIT_IMAGE, new Point(20, 20), true);
                final GridData gd = (GridData)fb.getLayoutData();
                gd.verticalIndent += 3;
                fb.setLayoutData(gd);
                fb.addClickListener((source) -> {
                    m_inEditMode.set(true);

                    m_modelFacilitator.storeStateForEdit();

                    updateDisplay();

                    configureFloatingHeaderBarButtons();
                });
                fb.setHighlightAsCircle(true);
            } else {
                final Label l2 = new Label(m_headerButtonPane, SWT.LEFT);
                l2.setLayoutData(new GridData(20, 20));
            }
        }

        m_headerBar.layout(true, true);
    }

    private void updateEditSaveButton() {
        if (m_inEditMode.get()) {
            m_editSaveButton.setVisible(m_modelFacilitator.modelIsDirty());
            updateFloatingHeaderBar();
        }
    }

    private void updateFloatingHeaderBar() {
        if ((m_currentAssetName == null) || (m_currentAssetName.trim().length() == 0)) {
            return;
        }

        m_floatingHeaderPositioner.run();

        // the ol' "SWT occasionally hands us stale bounds at the moment of this mouse event inspired action so
        //      let's give it a moment to sort itself out..." - we cue up one that doesn't actually fire until
        //      the user releases a mouse down
        getDisplay().asyncExec(m_floatingHeaderPositioner);
    }

    private Composite[] createHorizontalSection(final String label, final String noDataLabel) {
        final int compositeCount = (noDataLabel != null) ? 3 : 2;
        final Composite[] sectionAndContentPane = new Composite[compositeCount];

        sectionAndContentPane[0] = new Composite(m_contentPane, SWT.NONE);
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        sectionAndContentPane[0].setLayoutData(gd);
        GridLayout gl = new GridLayout(compositeCount, false);
        gl.marginTop = 5;
        gl.marginBottom = 8;
        gl.marginWidth = 0;
        sectionAndContentPane[0].setLayout(gl);

        Label l = new Label(sectionAndContentPane[0], SWT.LEFT);
        l.setText(label);
        l.setFont(BOLD_CONTENT_FONT);
        l.setForeground(SECTION_LABEL_TEXT_COLOR);
        gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        l.setLayoutData(gd);

        int index = 1;
        if (noDataLabel != null) {
            sectionAndContentPane[index] = new Composite(sectionAndContentPane[0], SWT.NONE);
            gd = new GridData();
            gd.horizontalAlignment = SWT.LEFT;
            gd.horizontalIndent = 9;
            sectionAndContentPane[index].setLayoutData(gd);
            gl = new GridLayout(1, false);
            gl.marginHeight = 0;
            gl.marginWidth = 0;
            gl.marginBottom = 2;
            sectionAndContentPane[index].setLayout(gl);

            l = new Label(sectionAndContentPane[index], SWT.LEFT);
            l.setFont(ITALIC_CONTENT_FONT);
            l.setForeground(TEXT_COLOR);
            l.setText(noDataLabel);
            gd = new GridData();
            gd.horizontalAlignment = SWT.LEFT;
            gd.verticalAlignment = SWT.BOTTOM;
            gd.grabExcessHorizontalSpace = true;
            l.setLayoutData(gd);

            gd = (GridData)sectionAndContentPane[index].getLayoutData();
            gd.heightHint = l.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
            // re-set it again in case some platform does a clone on get
            sectionAndContentPane[index].setLayoutData(gd);

            index++;
        }

        sectionAndContentPane[index] = new Composite(sectionAndContentPane[0], SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalIndent = 9;
        sectionAndContentPane[index].setLayoutData(gd);
        gl = new GridLayout(1, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        sectionAndContentPane[index].setLayout(gl);

        return sectionAndContentPane;
    }

    private Composite[] createVerticalSection(final String label, final String noDataLabel) {
        final Composite[] sectionAndContentPane = new Composite[3];

        sectionAndContentPane[0] = new Composite(m_contentPane, SWT.NONE);
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        sectionAndContentPane[0].setLayoutData(gd);
        GridLayout gl = new GridLayout(1, false);
        gl.marginTop = 5;
        gl.marginBottom = 8;
        gl.marginWidth = 0;
        gl.verticalSpacing = 0;
        sectionAndContentPane[0].setLayout(gl);

        Label l = new Label(sectionAndContentPane[0], SWT.LEFT);
        l.setText(label);
        l.setFont(BOLD_CONTENT_FONT);
        l.setForeground(SECTION_LABEL_TEXT_COLOR);
        gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        l.setLayoutData(gd);

        sectionAndContentPane[1] = new Composite(sectionAndContentPane[0], SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        sectionAndContentPane[1].setLayoutData(gd);
        gl = new GridLayout(1, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        sectionAndContentPane[1].setLayout(gl);

        l = new Label(sectionAndContentPane[1], SWT.LEFT);
        l.setText(noDataLabel);
        l.setFont(ITALIC_CONTENT_FONT);
        l.setForeground(TEXT_COLOR);
        gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        l.setLayoutData(gd);

        sectionAndContentPane[2] = new Composite(sectionAndContentPane[0], SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        sectionAndContentPane[2].setLayoutData(gd);
        gl = new GridLayout(1, false);
        gl.marginTop = 5;
        gl.marginBottom = 0;
        sectionAndContentPane[2].setLayout(gl);

        return sectionAndContentPane;
    }

    private void createTagsAddUI() {
        m_tagAddTextField = new Text(m_tagsAddContentPane, SWT.BORDER);
        m_tagAddTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(final KeyEvent ke) {
                m_tagsAddButton.setEnabled(m_tagAddTextField.getText().length() > 0);

                if (ke.character == SWT.CR) {
                    processTagAdd();
                }
            }
        });
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        m_tagAddTextField.setLayoutData(gd);

        m_tagsAddButton = new Button(m_tagsAddContentPane, SWT.PUSH);
        m_tagsAddButton.setText(Messages.getString("AbstractMetaView.27")); //$NON-NLS-1$
        m_tagsAddButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent se) {
                processTagAdd();
            }
        });
        gd = new GridData();
        gd.horizontalAlignment = SWT.RIGHT;
        m_tagsAddButton.setLayoutData(gd);
        m_tagsAddButton.setEnabled(false);

        gd = (GridData)m_tagsAddContentPane.getLayoutData();
        final Point textFieldSize = m_tagAddTextField.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        final Point buttonSize = m_tagsAddButton.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        gd.heightHint = Math.max(textFieldSize.y, buttonSize.y) + 4;
        // re-set it again in case some platform does a clone on get
        m_tagsAddContentPane.setLayoutData(gd);
    }

    private void processTagAdd() {
        final String tagText = m_tagAddTextField.getText().trim();
        if (tagText.length() > 0) {
            final MetaInfoAtom tag = m_modelFacilitator.addTag(tagText);
            tag.populateContainerForEdit(m_tagsTagsContentPane);
        }
        m_tagAddTextField.setText(""); //$NON-NLS-1$
        m_tagsAddButton.setEnabled(false);

        m_tagAddTextField.setFocus();

        layout(true, true);

        updateMinimumSizes();
    }

    private void createLinksAddUI() {
        m_linksAddURLTextField = addLabelTextFieldCouplet(m_linksAddContentPane, "URL:", "e.g. https://www.knime.com"); //$NON-NLS-1$ //$NON-NLS-2$
        m_linksAddURLTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(final KeyEvent ke) {
                m_linksAddButton.setEnabled(m_linksAddURLTextField.getText().length() > 0);

                if ((ke.character == SWT.CR) && m_linksAddButton.isEnabled()) {
                    processLinkAdd();
                }
            }
        });
        m_linksAddTitleTextField = addLabelTextFieldCouplet(m_linksAddContentPane, Messages.getString("AbstractMetaView.31"), Messages.getString("AbstractMetaView.32")); //$NON-NLS-1$ //$NON-NLS-2$
        m_linksAddTitleTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(final KeyEvent ke) {
                if ((ke.character == SWT.CR) && m_linksAddButton.isEnabled()) {
                    processLinkAdd();
                }
            }
        });
        final Label l = new Label(m_linksAddContentPane, SWT.LEFT);
        l.setText(Messages.getString("AbstractMetaView.33")); //$NON-NLS-1$
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        l.setLayoutData(gd);
        m_linksAddTypeComboViewer = new ComboViewer(m_linksAddContentPane, SWT.READ_ONLY);
        gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        gd.grabExcessHorizontalSpace = true;
        m_linksAddTypeComboViewer.getCombo().setLayoutData(gd);
        m_linksAddTypeComboViewer.setContentProvider(ArrayContentProvider.getInstance());
        m_linksAddTypeComboViewer.setInput(LinkMetaInfoAtom.LEGACY_LINK_TYPES);
        m_linksAddTypeComboViewer.setLabelProvider(new LabelProvider() {
            /**
             * {@inheritDoc}
             */
            @Override
            @SuppressWarnings("unchecked")  // generic casting...
            public String getText(final Object o) {
                if (o instanceof Pair) {
                    return ((Pair<String, String>)o).getLeft();
                }

                return null;
            }
        });
        m_linksAddTypeComboViewer.getCombo().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(final KeyEvent ke) {
                if ((ke.character == SWT.CR) && m_linksAddButton.isEnabled()) {
                    processLinkAdd();
                }
            }
        });
        m_linksAddTypeComboViewer.getCombo().select(0);

        m_linksAddButton = new Button(m_linksAddContentPane, SWT.PUSH);
        m_linksAddButton.setText(Messages.getString("AbstractMetaView.34")); //$NON-NLS-1$
        m_linksAddButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent se) {
                processLinkAdd();
            }
        });
        gd = new GridData();
        gd.horizontalAlignment = SWT.RIGHT;
        gd.horizontalSpan = 2;
        m_linksAddButton.setLayoutData(gd);
        m_linksAddButton.setEnabled(false);
    }

    @SuppressWarnings("unchecked")  // generics casting...
    private void processLinkAdd() {
        final String url = m_linksAddURLTextField.getText();
        final String title = m_linksAddTitleTextField.getText();
        final StructuredSelection selection = (StructuredSelection)m_linksAddTypeComboViewer.getSelection();
        final Pair<String, String> selectedType = (Pair<String, String>)selection.getFirstElement();
        final String type = selectedType.getLeft();

        try {
            final MetaInfoAtom link = m_modelFacilitator.addLink(url, title, type);
            m_linksAddURLTextField.setText(""); //$NON-NLS-1$
            m_linksAddTitleTextField.setText(""); //$NON-NLS-1$
            m_linksAddButton.setEnabled(false);
            link.populateContainerForEdit(m_linksLinksContentPane);

            m_linksAddURLTextField.setFocus();

            layout(true, true);

            updateMinimumSizes();
        } catch (final MalformedURLException e) {
            MessageDialog.openError(PlatformUI.getWorkbench().getDisplay().getActiveShell(), Messages.getString("AbstractMetaView.37"), //$NON-NLS-1$
                Messages.getString("AbstractMetaView.38") + url + Messages.getString("AbstractMetaView.39")); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void modelCardinalityChanged(final boolean increased) {
        if (!increased) {
            layout(true, true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void modelDirtyStateChanged() {
        updateEditSaveButton();
    }


    private class FloatingHeaderBarPositioner implements Runnable {
        private final double m_fontMetricsCorrectionFactor;

        private FloatingHeaderBarPositioner() {
            final Optional<Object> o =
                PlatformSpecificUIisms.getDetail(PlatformSpecificUIisms.FONT_METRICS_CORRECTION_DETAIL);

            m_fontMetricsCorrectionFactor = o.isPresent() ? ((Double)o.get()).doubleValue() : 1.0;
        }

        @Override
        public void run() {
            if (isDisposed()) {
                return;
            }

            final Rectangle viewportBounds = getBounds();
            final ScrollBar verticalSB = getVerticalBar();
            final int sbWidth = (verticalSB.isVisible() ? verticalSB.getSize().x : 0);
            final int viewportWidth = viewportBounds.width - ((2 * getBorderWidth()) + sbWidth);
            final Point origin = getOrigin();

            if ((m_lastRenderedViewportWidth.getAndSet(viewportWidth) == viewportWidth)
                    && (m_lastRenderedViewportOriginX.getAndSet(origin.x) == origin.x)
                    && (m_lastRenderedViewportOriginY.getAndSet(origin.y) == origin.y)
                    && !m_assetNameHasChanged.getAndSet(false)) {
                return;
            }

            final Point neededButtonPaneSize = m_headerButtonPane.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            final int centerAlignedAvailableWidth
                = viewportWidth - (2 * (LEFT_INDENT_HEADER_SUB_PANES + HEADER_MARGIN_RIGHT + neededButtonPaneSize.x));
            if (centerAlignedAvailableWidth <= 0) {
                return;
            }

            final Point labelSize = m_headerLabelPlaceholder.getSize();
            final int barHeight = Math.max(neededButtonPaneSize.y, labelSize.y) + 4 + (2 * HEADER_VERTICAL_INSET);
            m_headerBar.setBounds(origin.x, origin.y, viewportWidth, barHeight);

            final int placeholderWidth = viewportWidth - (TOTAL_HEADER_PADDING + neededButtonPaneSize.x);
            m_headerLabelPlaceholder.setSize(placeholderWidth, labelSize.y);
            final GridData gd = (GridData)m_headerLabelPlaceholder.getLayoutData();
            gd.widthHint = placeholderWidth;
            m_headerLabelPlaceholder.setLayoutData(gd);

            final GC gc = new GC(m_headerBar.getDisplay());
            try {
                gc.setFont(m_headerLabelPlaceholder.getFont());
                Point fullStringSize = gc.textExtent(m_currentAssetName);
                final int stringWidth = (int)(fullStringSize.x * m_fontMetricsCorrectionFactor);

                if (stringWidth > centerAlignedAvailableWidth) {
                    // this could be made more precise by iterative size checks,
                    //      but let's try this first for performance
                    final double percentage = (centerAlignedAvailableWidth * 0.87) / fullStringSize.x;
                    final int charCount = (int)(m_currentAssetName.length() * percentage);
                    final String substring = m_currentAssetName.substring(0, charCount) + "..."; //$NON-NLS-1$

                    m_headerText = substring;
                    fullStringSize = gc.textExtent(m_headerText);
                } else {
                    m_headerText = m_currentAssetName;
                }
                m_headerDrawX.set((viewportWidth - (int)(fullStringSize.x * m_fontMetricsCorrectionFactor)) / 2);
            } finally {
                gc.dispose();
            }

            m_headerBar.layout(true, true);
            m_headerBar.redraw();
        }
    }
}
