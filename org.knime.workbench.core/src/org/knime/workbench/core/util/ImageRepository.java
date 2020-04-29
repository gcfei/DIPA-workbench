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
 *   20.06.2012 (meinl): created
 */
package org.knime.workbench.core.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.workbench.core.KNIMECorePlugin;

/**
 * Central repository for all KNIME-related images. It stores images for nodes,
 * categories, and images used in the GUI. All images are stored in a central
 * {@link ImageRegistry} so that they get disposed automatically when the
 * plug-in is deactivated (which it probably never will). </ p>
 * The methods distinguish between IconImages and Images. IconImages are returned in a 16x16px size. Images as they are.
 * IconImages may be scaled if the file content is of different size. Both images (icon and "normal") are zoomed if
 * system zoom requires (high dpi) - but only if the corresponding SystemProperty is set. Icons can be provided in
 * different sizes in different files with different names. The size must be appended (like foo.png -> foo_24x24.png and
 * foo_32x32.png). Images are also scaled. If they are provided in different sizes their file names must be appended
 * with @1.5x and @2x.
 *
 * <b>This class is experimental API, please do not use it for now.</b>
 *
 * @author Thorsten Meinl, University of Konstanz
 * @since 2.6
 */
public final class ImageRepository {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(ImageRepository.class);

    // appended to the key if the image is scaled to icon size
    private static final String ICONIFIED_KEY = "@:/icon"; //$NON-NLS-1$

    // appended to the key if the image is not scaled to sytem level zoom (highDPI)
    private static final String NOTSCALED_KEY = "@:/noscale"; //$NON-NLS-1$

    // used for the missing icon
    private static final String MISSING_ICON_KEY = "###MISSING_ICON###"; //$NON-NLS-1$

    /**
     * Enumeration for shared images.
     *
     * @author Thorsten Meinl, University of Konstanz
     * @since 2.6
     */
    public enum SharedImages {
        /** the KNIME triangle. */
        KNIME("icons/dipa.png"), //$NON-NLS-1$

        /** Add icon in the form of a plus sign. */
        AddPlus("icons/add_plus.png"), //$NON-NLS-1$
        /** edit icon when cursor moves over annotations - TODO this is unused. */
        AnnotationEditHover("icons/anno_edit.png"), //$NON-NLS-1$
        /** icon when cursor is over annotation top left corner in node edit mode, showing the ability to change mode */
        AnnotationEditModeHover("icons/anno_edit_pencil.png"), //$NON-NLS-1$
        /** Small icon for export wizards. */
        ExportSmall("icons/dipa_export16.png"), //$NON-NLS-1$
        /** Big icon for export wizards. */
        ExportBig("icons/dipa_export55.png"), //$NON-NLS-1$
        /** Big icon for import wizards. */
        ImportBig("icons/dipa_import55.png"), //$NON-NLS-1$
        /** Big icon for new KNIME flow. */
        NewKnimeBig("icons/new_dipa55.png"), //$NON-NLS-1$
        /** The default node icon. */
        DefaultNodeIcon(NodeFactory.getDefaultIcon()),
        /** The default metanode icon. */
        DefaultMetaNodeIcon("icons/meta_nodes/metanode_template.png"), //$NON-NLS-1$
        /** Disabled icon for metanodes. */
        MetanodeDisabled("icons/meta_nodes/metanode_template_disabled.png"), //$NON-NLS-1$
        /** Icon for a metanode in the node repository or navigator. */
        MetanodeRepository("icons/meta_nodes/metanode_template_repository.png"), //$NON-NLS-1$
        /** The default category icon. */
        DefaultCategoryIcon(NodeFactory.getDefaultIcon()),
        /** Icon with a lock. */
        Lock("icons/lockedstate.gif"), //$NON-NLS-1$
        /** Icon for canceling node or workflow execution. */
        CancelExecution("icons/actions/cancel.gif"), //$NON-NLS-1$
        /** Icon for configuring a node. */
        ConfigureNode("icons/actions/configure.gif"), //$NON-NLS-1$
        /** Icon for executing a node or workflow. */
        Execute("icons/actions/execute.gif"), //$NON-NLS-1$
        /** Icon for opening a node view. */
        OpenNodeView("icons/actions/openView.gif"), //$NON-NLS-1$
        /** Icon for reseting a node or workflow. */
        Reset("icons/actions/reset.gif"), //$NON-NLS-1$
        /** Icon for collapsing all levels in a tree. */
        CollapseAll("icons/collapseall.png"), //$NON-NLS-1$
        /** Icon for expanding all levels in a tree. */
        ExpandAll("icons/expandall.png"), //$NON-NLS-1$
        /** Icon for expanding one level in a tree. */
        Expand("icons/expand.png"), //$NON-NLS-1$
        /** Icon for synching a tree with another selection. */
        Synch("icons/sync.png"), //$NON-NLS-1$
        /** Icon for refreshing a component. */
        Refresh("icons/refresh.gif"), //$NON-NLS-1$
        /** Icon for a history view. */
        History("icons/history.png"), //$NON-NLS-1$
        /** Icon for delete. */
        Delete("icons/delete.png"), //$NON-NLS-1$
        /** Icon for a recycle bin. */
        RecycleBin("icons/recycle_bin.png"), //$NON-NLS-1$
        /** Icon for startup messages view. */
        StartupMessages("icons/startupMessages.png"), //$NON-NLS-1$
        /** Icon showing a magnifier glass. */
        Search("icons/search.gif"), //$NON-NLS-1$
        /** Icon showing a 'fuzzy' magnifier glass. */
        FuzzySearch("icons/fuzzy_search.png"), //$NON-NLS-1$

        /** Icon for a folder. */
        Folder("icons/folder.png"), //$NON-NLS-1$
        /** Icon for a file. */
        File("icons/any_file.png"), //$NON-NLS-1$
        /** Icon for another file. */
        File2("icons/file.png"), //$NON-NLS-1$
        /** Icon for a workflow node. */
        Node("icons/node.png"), //$NON-NLS-1$
        /** Icon for knime project (neutral). */
        Workflow("icons/project_basic.png"), //$NON-NLS-1$
        /** Icon for a metanode template. */
        MetaNodeTemplate("icons/meta_nodes/metanode_template_repository.png"), //$NON-NLS-1$
        /** Icon for a workflow group. */
        WorkflowGroup("icons/wf_set.png"), //$NON-NLS-1$
        /** Icon for a system folder. */
        SystemFolder("icons/system_folder.png"), //$NON-NLS-1$
        /** Icon for a system flow. */
        SystemFlow("icons/system_flow.png"), //$NON-NLS-1$


        /** Icons for diff view. */
        MetaNodeIcon("icons/metanode_icon.png"), //$NON-NLS-1$
        /** meta node with ports. */
        MetaNodeDetailed("icons/metanode_detailed.png"), //$NON-NLS-1$
        /** Icons for diff view. */
        NodeIcon("icons/node_icon.png"), //$NON-NLS-1$
        /** node with ports and status. */
        NodeIconDetailed("icons/node_detailed.png"), //$NON-NLS-1$
        /** Icons for diff view. */
        SubNodeIcon("icons/subnode_icon.png"), //$NON-NLS-1$
        /** subnode with ports. */
        SubNodeDetailed("icons/subnode_detailed.png"), //$NON-NLS-1$
        /** cross/delete icon for the search bar. */
        ButtonClear("icons/clear.png"), //$NON-NLS-1$
        /** filter icon. */
        FunnelIcon("icons/filter.png"), //$NON-NLS-1$
        /** hide equal nodes button. */
        ButtonHideEqualNodes("icons/hide_equals.png"), //$NON-NLS-1$
        /** show additional nodes only button. */
        ButtonShowAdditionalNodesOnly("icons/show_add_only.png"), //$NON-NLS-1$
        /** Icon for configured knime project. */
        WorkflowConfigured("icons/project_configured.png"), //$NON-NLS-1$
        /** Icon for executing knime project. */
        WorkflowExecuting("icons/project_executing.png"), //$NON-NLS-1$
        /** Icon for fully executed knime project. */
        WorkflowExecuted("icons/project_executed.png"), //$NON-NLS-1$
        /** Icon for knime project with errors. */
        WorkflowError("icons/project_error.png"), //$NON-NLS-1$
        /** Icon for a closed knime project. */
        WorkflowClosed("icons/project_basic.png"), //$NON-NLS-1$
        /** Icon for a knime project with unknown state. */
        WorkflowUnknown("icons/dipa_unknown.png"), //$NON-NLS-1$
        /** Icon for a knime project with a red unknown state. */
        WorkflowUnknownRed("icons/dipa_unknown_red.png"), //$NON-NLS-1$

        /** Icon for the favorite nodes view. */
        FavoriteNodesFolder("icons/fav/folder_fav.png"), //$NON-NLS-1$
        /** Icon for the most frequently used nodes category. */
        FavoriteNodesFrequentlyUsed("icons/fav/folder_freq.png"), //$NON-NLS-1$
        /** Icon for the last used nodes category. */
        FavoriteNodesLastUsed("icons/fav/folder_last.png"), //$NON-NLS-1$
        /** Icon with a green OK check mark. */
        Ok("icons/ok.png"), //$NON-NLS-1$
        /** Icon for all kinds of warnings. */
        Warning("icons/warning.png"), //$NON-NLS-1$
        /** Icon for information messages. */
        Info("icons/info.gif"), //$NON-NLS-1$
        /** Icon for tooltip indication. */
        Info_Outline("icons/info_outline.png"), //$NON-NLS-1$
        /** Icon icon in a round blue button. */
        InfoButton("icons/info.png"), //$NON-NLS-1$
        /** Info icon in a little speech balloon. */
        InfoBalloon("icons/info_balloon.png"), //$NON-NLS-1$
        /** Icon for all kinds of errors. */
        Error("icons/error.png"), //$NON-NLS-1$
        /** busy cursor (hour glass). */
        Busy("icons/busy.png"), //$NON-NLS-1$
        /** a question mark icon. */
        Help("icons/help.png"), //$NON-NLS-1$
        /** ServerSpace Explorer Icon: server logo, 55px.*/
        ServerSpaceServerLogo("icons/server_space/server_logo_55.png"), //$NON-NLS-1$
        /** ServerSpace Explorer Icon: server root. */
        ServerSpaceIcon("icons/server_space/explorer_server.png"), //$NON-NLS-1$
        /** ServerSpace Explorer Icon: scheduled job. */
        ServerScheduledJob("icons/server_space/flow_scheduled.png"), //$NON-NLS-1$
        /** ServerSpace Explorer Icon: scheduled periodic job. */
        ServerScheduledPeriodicJob("icons/server_space/flow_periodic.png"), //$NON-NLS-1$
        /** ServerSpace Explorer Icon: system scheduled periodic job. */
        SystemScheduledPeriodicJob("icons/server_space/flow_periodic_system.png"), //$NON-NLS-1$
        /** ServerSpace Explorer Icon: configured job. */
        ServerJobConfigured("icons/server_space/running_job_confgd.png"), //$NON-NLS-1$
        /** ServerSpace Explorer Icon: executing job. */
        ServerJobExecuting("icons/server_space/running_job_execting.png"), //$NON-NLS-1$
        /** ServerSpace Explorer Icon: executed job. */
        ServerJobExecuted("icons/server_space/running_job_execed.png"), //$NON-NLS-1$
        /** ServerSpace Explorer Icon: idle job. */
        ServerJobIdle("icons/server_space/running_job_idle.png"), //$NON-NLS-1$
        /** ServerSpace Explorer Icon: unknown job status. */
        ServerJobUnknown("icons/server_space/running_job_unknown.png"), //$NON-NLS-1$
        /** ServerSpace Explorer Icon: running job. */
        ServerJob("icons/server_space/running_job.png"), //$NON-NLS-1$
        /** ServerSpace Explorer Icon: Dialog icon - group permissions. */
        ServerDialogGroupPermissions("icons/server_space/grp_permission_55.png"), //$NON-NLS-1$
        /** ServerSpace Explorer Icon: Dialog icon - Meta Info Edit. */
        ServerDialogEditMetaInfo("icons/server_space/meta_info_edit55.png"), //$NON-NLS-1$
        /** ServerSpace Explorer Icon: Dialog icon - Permissions. */
        ServerDialogPermissions("icons/server_space/permission.png"), //$NON-NLS-1$
        /** ServerSpace Explorer Icon: Dialog icon - upload workflow. */
        ServerDialogWorkflowUpload("icons/server_space/upload_wf55.png"), //$NON-NLS-1$
        /** ServerSpace Explorer Menu Icon: edit meta info. */
        ServerEditMetaInfo("icons/server_space/meta_info_edit.png"), //$NON-NLS-1$
        /** ServerSpace Explorer Menu Icon: show node messages. */
        ServerShowNodeMsg("icons/server_space/nodemsg.png"), //$NON-NLS-1$
        /** ServerSpace Explorer Menu Icon: show schedule info. */
        ServerShowScheduleInfo("icons/server_space/schedinfo.png"), //$NON-NLS-1$
        /** ServerSpace Explorer Menu Icon: upload workflow. */
        ServerUploadWorkflow("icons/server_space/upload_wf.png"), //$NON-NLS-1$
        /** ServerSpace Explorer Menu Icon: open in web portal. */
        ServerOpenInWebPortal("icons/server_space/open_in_web.png"), //$NON-NLS-1$
        /** ServerSpace Explorer Menu Icon: show API definition. */
        ServerShowAPI("icons/server_space/show_api.png"), //$NON-NLS-1$
        /** ServerSpace Explorer Menu Icon: edit meta info. */
        ServerPermissions("icons/server_space/key.png"), //$NON-NLS-1$
        /** ServerSpace Explorer Menu Icon: create a snapshot. */
        ServerSnapshot("icons/server_space/snapshot.png"), //$NON-NLS-1$
        /** ServerSpace Explorer Menu Icon: replace head with snapshot. */
        ServerReplaceHead("icons/server_space/replace_head.png"), //$NON-NLS-1$
        /** ServerSpace Explorer Menu Icon: download something from the server. */
        ServerDownload("icons/server_space/download.png"), //$NON-NLS-1$
        /** ServerSpace Explorer Icon: unknown item. */
        ServerUnknownItem("icons/server_space/unknown_item.png"), //$NON-NLS-1$
        /** ServerSpace Explorer Icon: idle job. */
        TeamSpaceIcon("icons/team_space/explorer_teamspace.png"), //$NON-NLS-1$
        /** LocalSpace Explorer Icon: server root. */
        LocalSpaceIcon("icons/workflow_projects.png"), //$NON-NLS-1$
        /** WorkflowDiff: action icon. */
        WorkflowDiffIcon("icons/diff.png"), //$NON-NLS-1$
        /** Copy URL action icon. */
        CopyURLIcon("icons/actions/url.png"), //$NON-NLS-1$

        /** ServerSpace Workflow Hub Icon. */
        WorkflowHub("icons/server_space/workflow_hub.png"), //$NON-NLS-1$
        /** ServerSpace Workflow Hub Icon yellow. */
        WorkflowHubYellow("icons/server_space/workflow_hub_yellow.png"), //$NON-NLS-1$
        /** Icon for hub spaces */
        HubSpace("icons/server_space/space.png"), //$NON-NLS-1$

        /** Configuration icon */
        WorkflowConfiguration("icons/server_space/workflow_configuration.png"), //$NON-NLS-1$
        /** Completely transparent icon */
        EMPTY_ICON("icons/empty.png"); //$NON-NLS-1$


        private final URL m_url;

        private SharedImages(final String path) {
            m_url = FileLocator.find(KNIMECorePlugin.getDefault().getBundle(), new Path(path), null);
            if (m_url == null) {
                LOGGER.coding(Messages.ImageRepository_0 + toString() + Messages.ImageRepository_1 + path);
            }
        }

        private SharedImages(final URL url) {
            m_url = url;
        }

        /**
         * Returns the URL to the image.
         *
         * @return a URL
         */
        public URL getUrl() {
            return m_url;
        }
    }

    /**
     * Flags to decorate KNIME ServerSpace icons. Icon file names with the corresponding combinations of suffixes must
     * exist!
     */
    public enum ImgDecorator {
        /** Decorator for messages. */
        Message("_msg"), //$NON-NLS-1$
        /** Decorator for outdated jobs (ignored right now). */
        Outdated("_out"), //$NON-NLS-1$
        /** Decorator for orphaned jobs (ignored right now). */
        Orphaned(""); //$NON-NLS-1$

        private final String m_suffix;

        private ImgDecorator(final String suffix) {
            m_suffix = suffix;
        }

        /**
         * Returns the image filename suffix for this image decorator.
         *
         * @return a suffix
         */
        String getSuffix() {
            return m_suffix;
        }
    }

    /**
     *
     */
    private ImageRepository() {
        // don't instantiate. Utility class.
    }

    /**
     * Returns a shared image descriptor.
     *
     * @param image the image
     * @return an image
     */
    public static ImageDescriptor getImageDescriptor(final SharedImages image) {
        return getImageDescriptor(image.getUrl());
    }

    public static ImageDescriptor getImageDescriptor(final URL url) {
        final String key = url.toString();
        // make sure the image is in the registry
        getImage(url);
        return KNIMECorePlugin.getDefault().getImageRegistry().getDescriptor(key);
    }

    /**
     * @see #getImage(URL).
     * @param image
     * @return an image.
     */
    public static Image getImage(final SharedImages image) {
        return getImage(image.getUrl());
    }

    public static ImageDescriptor getImageDescriptor(final String pluginID, final String path) {
        URL url = FileLocator.find(Platform.getBundle(pluginID), new Path(path), null);
        if (url == null) {
            return null;
        }
        return getImageDescriptor(url);
    }

    public static Image getImage(final String pluginID, final String path) {
        URL url = FileLocator.find(Platform.getBundle(pluginID), new Path(path), null);
        if (url == null) {
            return null;
        }
        return getImage(url);
    }

    /**
     * Returns an image from the specified location. If the system zoom factor (for high dpi) is set, it returns a
     * larger version (1.5 or 2 times). If a file with the corresponding name extension is provided it will take
     * the image from that file (without scaling). Eclipse naming convention is: 100% zoom file "basename.png", 150%
     * file: "basename@1.5x.png", 200% file: "basename@2x.png". If these extra files don't exist the base file is
     * scaled.
     *
     * @param resourceURL to the icon image
     * @return an image
     */
     public static Image getImage(final URL resourceURL) {
         if (resourceURL == null) {
             return null;
         }
        final String key = resourceURL.toString();
        Image img = KNIMECorePlugin.getDefault().getImageRegistry().get(key);
        if (img != null) {
            return img;
        }
        try {
            // the KNIME image provider ensures correct size of the image
            img = new Image(Display.getDefault(), new KNIMEImageProvider(resourceURL));
        } catch (IOException e) {
            LOGGER.coding(Messages.ImageRepository_2 + resourceURL.toString() + ": " + e.getMessage(), e); //$NON-NLS-2$
            return getMissingIcon();
        }
        KNIMECorePlugin.getDefault().getImageRegistry().put(key, img);
        return img;
     }

    /**
     * Returns a shared image.
     *
     * @param image the image
     * @return an image
     */
    public static Image getUnscaledImage(final SharedImages image) {
        return getUnscaledImage(image.getUrl());
    }

    /**
     * @see #getUnscaledImage(SharedImages)
     *
     * @param image the image
     * @return an image descriptor
     */
    public static ImageDescriptor getUnscaledImageDescriptor(final SharedImages image) {
        final String key = image.getUrl().toString() + NOTSCALED_KEY;
        // make sure the image is in the registry
        getUnscaledImage(image.getUrl());
        return KNIMECorePlugin.getDefault().getImageRegistry().getDescriptor(key);
    }

    /**
     * Returns an image from the specified location. If the system zoom factor (for high dpi) is set, it still returns
     * an image of original 100% size. Eclipse naming convention is for different zoom factors is totally ignored!
     *
     * @param resourceURL to the icon image
     * @return an image in its original size (no high DPI system zoom factor scaling)
     */
     public static Image getUnscaledImage(final URL resourceURL) {
         if (resourceURL == null) {
             return null;
         }
        final String key = resourceURL.toString() + NOTSCALED_KEY;
        Image img = KNIMECorePlugin.getDefault().getImageRegistry().get(key);
        if (img != null) {
            return img;
        }
        try {
            // the KNIME image provider ensures correct size of the icon
            img = new Image(Display.getDefault(), new KNIMENonscalingImageProvider(resourceURL));
        } catch (IOException e) {
            LOGGER.coding(Messages.ImageRepository_3 + resourceURL.toString() + ": " + e.getMessage(), e); //$NON-NLS-2$
            return getMissingIcon();
        }
        KNIMECorePlugin.getDefault().getImageRegistry().put(key, img);
        return img;
     }

     /**
      * @see #getUnscaledImage(URL)
      * @param pluginID
      * @param path
      * @return
      */
     public static Image getUnscaledImage(final String pluginID, final String path) {
         if (path == null) {
             LOGGER.error(Messages.ImageRepository_4 + pluginID + ")"); //$NON-NLS-2$
             return getMissingIcon();
         }
         URL url = FileLocator.find(Platform.getBundle(pluginID), new Path(path), null);
         if (url == null) {
             LOGGER.coding(Messages.ImageRepository_5 + pluginID + Messages.ImageRepository_6 + path);
             return getMissingIcon();
         }
         return getUnscaledImage(url);
     }

     /**
      * Returns a 16x16px version of the provided image. Ignores the system zoom level for highDPI images.
      *
      * <b>NOTE:</b> even though this method says mot-par-mot that it is returning an Unscaled image, it is
      *     indeed scaling the image via <code>KNIMENonscalingIconProvider</code>
      *
      * @param image to the icon image; if this is null, null will be returned
      * @return a potentially scaled image
      */
     public static Image getUnscaledIconImage(final SharedImages image) {
         return (image != null) ? getUnscaledIconImage(image.getUrl()) : null;
     }

     /**
      * Returns a 16x16px version of the provided image. Ignores the system zoom level for highDPI images.
      *
      * <b>NOTE:</b> even though this method says mot-par-mot that it is returning an Unscaled image, it is
      *     indeed scaling the image via <code>KNIMENonscalingIconProvider</code>
      *
      * @param resourceURL to the icon image
      * @return a potentially scaled image
      */
      public static Image getUnscaledIconImage(final URL resourceURL) {
          if (resourceURL == null) {
              return null;
          }
         final String key = resourceURL.toString() + ICONIFIED_KEY + NOTSCALED_KEY;
         Image img = KNIMECorePlugin.getDefault().getImageRegistry().get(key);
         if (img != null) {
             return img;
         }
         try {
             // the KNIME image provider ensures correct size of the icon
             img = new Image(Display.getDefault(), new KNIMENonscalingIconProvider(resourceURL));
         } catch (IOException e) {
             LOGGER.coding(Messages.ImageRepository_7 + e.getMessage(), e);
             return getIconImage(SharedImages.DefaultNodeIcon);
         }
         KNIMECorePlugin.getDefault().getImageRegistry().put(key, img);
         return img;
      }


     /**
      * Returns the 16x16 version of the specified image with the passed decorators.<p>
      * The current implementation requires each icon that may be requested with decorators to be present in the
      * corresponding icons directory (the decorators are not added programmatically!) with the corresponding name.
      *
      * @param image the image to return
      * @param decorators the decorators to add to the image
      * @return the image with the specified decorators
      */
     public static Image getIconImage(final SharedImages image, final ImgDecorator... decorators) {
         URL imgURL = image.getUrl();
         if (imgURL != null && decorators != null && decorators.length > 0) {
             String suffix = getRequestedDecoratorSuffixes(decorators);
             // in the path we have to insert the suffix in front of the extension (.jpg, .png, etc.)
             String path = imgURL.getPath();
             int dotIdx = path.lastIndexOf('.');
             if (dotIdx <= 0) {
                 path += suffix;
             } else {
                 path = path.substring(0, dotIdx) + suffix + path.substring(dotIdx);
             }

             try {
                 if (imgURL.getQuery() != null) {
                     path += '?' + imgURL.getQuery();
                 }
                 imgURL = new URL(imgURL.getProtocol(), imgURL.getHost(), imgURL.getPort(), path);
             } catch (MalformedURLException e) {
                 LOGGER.coding(Messages.ImageRepository_8 + e.getMessage(), e);
                 return null;
             }
         }
         return getIconImage(imgURL);
     }

     // Computes an ordered list of image name suffixes for the requested decorators
     private static String getRequestedDecoratorSuffixes(final ImgDecorator... decorators) {
         StringBuilder suffix = new StringBuilder();
         // add the decorator suffixes in the order they are defined
         if (decorators != null) {
             for (ImgDecorator dec : ImgDecorator.values()) {
                 for (ImgDecorator flag : decorators) {
                     if (flag.equals(dec)) {
                         suffix.append(flag.getSuffix());
                         break; // add each decorator only once
                     }
                 }
             }
         }
         return suffix.toString();
     }

    /**
     * @see #getIconImage. Returns the image wrapped in a descriptor.
     * @param icon
     * @return the descriptor wrapping the iconified image
     */
    public static ImageDescriptor getIconDescriptor(final SharedImages icon) {
        final String key = icon.getUrl().toString() + ICONIFIED_KEY;
        // make sure the image is in the repository!
        getIconImage(icon.getUrl());
        return KNIMECorePlugin.getDefault().getImageRegistry().getDescriptor(key);
    }
     /**
     * Returns a 16x16px version of a node icon. If no icon is specified by the node factory the default node icon is
     * returned. If the icon specified by the factory does not exist, the Eclipse default missing icon is returned. If the system
     * zoom factor (for high dpi) is set, it returns a 24x24px or 32x32px image accordingly. If a file with the corresponding name
     * extension is provided it will take the icon from that file (_24x24 or _32x32).
     *
     * @param nodeFactory a node factory
     * @return a potentially scaled image
     */
    public static Image getIconImage(final NodeFactory<? extends NodeModel> nodeFactory) {
        URL iconURL = nodeFactory.getIcon();
        if (iconURL == null) {
            return getIconImage(SharedImages.DefaultNodeIcon);
        }
        Image img = getIconImage(iconURL);
        return img;
    }

    /**
     * Returns a 16x16px version of the provided image. If the system zoom factor (for high dpi) is set, it returns a
     * 24x24px or 32x32px image accordingly. If a file with the corresponding name extension is provided it will take
     * the icon from that file (_24x24 or _32x32).
     *
     * @param resourceURL to the icon image
     * @return a potentially scaled image
     */
     public static Image getIconImage(final URL resourceURL) {
         if (resourceURL == null) {
             return null;
         }
        final String key = resourceURL.toString() + ICONIFIED_KEY;
        Image img = KNIMECorePlugin.getDefault().getImageRegistry().get(key);
        if (img != null) {
            return img;
        }
        try {
            // the KNIME image provider ensures correct size of the icon
            img = new Image(Display.getDefault(), new KNIMEIconImageProvider(resourceURL));
        } catch (IOException e) {
            LOGGER.coding(Messages.ImageRepository_9 + e.getMessage(), e);
//            return getIconImage(SharedImages.DefaultNodeIcon);
            return null;
        }
        KNIMECorePlugin.getDefault().getImageRegistry().put(key, img);
        return img;
     }

     public static ImageDescriptor getIconDescriptor(final URL resourceURL) {
         if (resourceURL == null) {
             return null;
         }
         final String key = resourceURL.toString() + ICONIFIED_KEY;
         // make sure the image is in the registry
         getIconImage(resourceURL);
         return KNIMECorePlugin.getDefault().getImageRegistry().getDescriptor(key);
     }

    /**
     * Returns an image for an "external" image. The image is given by the
     * plug-in an the path relative to the plug-in root.
     *
     * @param pluginID the plug-in's id
     * @param path the path of the image
     * @return an image or <code>null</code> if the image does not exist
     */
    public static Image getIconImage(final String pluginID, final String path) {
        if (path == null) {
            LOGGER.error(Messages.ImageRepository_10 + pluginID + ")"); //$NON-NLS-2$
            return getMissingIcon();
        }
        URL url = FileLocator.find(Platform.getBundle(pluginID), new Path(path), null);
        if (url == null) {
            return null;
        }
        return getIconImage(url);
    }

    public static ImageDescriptor getIconDescriptor(final String pluginID, final String path) {
        URL url = FileLocator.find(Platform.getBundle(pluginID), new Path(path), null);
        if (url == null) {
            return null;
        }
        return getIconDescriptor(url);
    }

    /**
     * @return a red 16x16 icon
     */
    public static Image getMissingIcon() {
        Image img = KNIMECorePlugin.getDefault().getImageRegistry().get(MISSING_ICON_KEY);
        if (img != null) {
            return img;
        }
        img = new Image(Display.getDefault(), KNIMEImageProvider.MISSING_IMAGE_DATA);
        KNIMECorePlugin.getDefault().getImageRegistry().put(MISSING_ICON_KEY, img);
        return img;
    }
}
