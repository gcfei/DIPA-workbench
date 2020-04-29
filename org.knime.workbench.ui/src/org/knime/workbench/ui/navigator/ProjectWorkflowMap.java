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
package org.knime.workbench.ui.navigator;


import static org.knime.core.ui.wrapper.Wrapper.unwrapWFM;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IProject;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeMessageEvent;
import org.knime.core.node.workflow.NodeMessageListener;
import org.knime.core.node.workflow.NodePropertyChangedEvent;
import org.knime.core.node.workflow.NodePropertyChangedListener;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowEvent;
import org.knime.core.node.workflow.WorkflowListener;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.ui.wrapper.WorkflowManagerWrapper;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.core.util.ThreadUtils;

/**
 * This class represents a link between projects (file system representation of
 * workflows) and opened workflows (represented by a {@link WorkflowManager}).
 * The <code>WorkflowEditor</code> puts and removes the name of the opened
 * project together with the referring {@link WorkflowManager} instance. The
 * {@link KnimeResourceNavigator} uses this information to display opened
 * instances differently.
 *
 * @see KnimeResourceNavigator
 * @see KnimeResourceContentProvider
 * @see KnimeResourceLabelProvider
 * @see KnimeResourcePatternFilter
 *
 * @author Fabian Dill, University of Konstanz
 * @author Peter Ohl, KNIME AG, Zurich, Switzerland
 *
 * History: 2011-08-11 (ohl): Changing the key in the map.
 * Turns out some places (e.g. IResource.getLocationURI) create URIs without
 * trailing slash, others (e.g. File.toURI) with trailing slash. Removing the
 * slash and re-creating the URI fails for network drives under Windows (these
 * drives have two slashes at the beginning of their path causing a "file-URI
 * has an authority" error).
 *
 */
public final class ProjectWorkflowMap {

    private static class MapWFKey {
        private final String m_key;
        private final URI m_uri;
        private MapWFKey(final URI workflow) {
            m_uri = workflow;
            m_key = addTrailingSlash(workflow.toString());
        }
        private String addTrailingSlash(final String path) {
          if (path.endsWith("/")) { //$NON-NLS-1$
              return path;
          } else {
              return path + "/"; //$NON-NLS-1$
          }
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof MapWFKey) {
                return ((MapWFKey)obj).m_key.equals(m_key);
            }
            return false;
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return m_key.hashCode();
        }
        /**
         * @return the uri
         */
        public URI getURI() {
            return m_uri;
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return m_key;
        }
    }

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            ProjectWorkflowMap.class);

    private ProjectWorkflowMap() {
        // Utility class
    }

    /**
     * A map which keeps track of the number of registered clients to the
     * referring workflow. Registration is done by URI - since this is
     * the key used in the project workflow map and the workflow manager
     * instance might be replaced. Registered clients in this map prevent the
     * workflow from being removed from the {@link #PROJECTS} map with
     * {@link #remove(URI)}, only if there are no registered clients for this
     * workflow, {@link #remove(URI)} will actually remove the workflow from
     * {@link #PROJECTS}.
     *
     */
    private static final Map<MapWFKey, Set<Object>>WORKFLOW_CLIENTS
        = new HashMap<MapWFKey, Set<Object>>();

    /*
     * Map with name of workflow path and referring workflow manager
     * instance. Maintained by WorkflowEditor, used by KnimeResourceNavigator.
     * (This map contains only open workflows.)
     */
    private static final Map<MapWFKey, NodeContainerUI> PROJECTS
        = new LinkedHashMap<MapWFKey, NodeContainerUI>() {

        @Override
        public NodeContainerUI put(final MapWFKey key, final NodeContainerUI value) {
            NodeContainerUI old = super.put(key, value);
            if (old != null) {
                LOGGER.debug(Messages.ProjectWorkflowMap_2 + key + Messages.ProjectWorkflowMap_3);
            }
            if (value != null) {
                LOGGER.debug(Messages.ProjectWorkflowMap_4 + key
                        + Messages.ProjectWorkflowMap_5 + size() + Messages.ProjectWorkflowMap_6);
            }
            return old;
        };

        @Override
        public NodeContainerUI remove(final Object key) {
            NodeContainerUI old = super.remove(key);
            if (old != null) {
                LOGGER.debug(Messages.ProjectWorkflowMap_7 + key
                        + Messages.ProjectWorkflowMap_8 + size() + Messages.ProjectWorkflowMap_9);
            }
            return old;
        };
    };

    /**
     *
     * @param workflow the path to the workflow which is used by the client
     * @param client any object which uses the workflow
     *
     * @see #unregisterClientFrom(URI, Object)
     */
    public static void registerClientTo(final URI workflow,
            final Object client) {
        if (workflow == null) {
            return;
        }
        MapWFKey wf = new MapWFKey(workflow);
        Set<Object> callers = WORKFLOW_CLIENTS.get(wf);
        if (callers == null) {
            callers = new HashSet<Object>();
        }
        callers.add(client);
        WORKFLOW_CLIENTS.put(wf, callers);
        LOGGER.debug(Messages.ProjectWorkflowMap_10 + client + Messages.ProjectWorkflowMap_11 + wf
                + ". " + callers.size() + Messages.ProjectWorkflowMap_13); //$NON-NLS-1$
    }

    /**
     *
     * @param workflow path to the workflow which is not used anymore by this
     * client (has no effect if the client was not yet registered for this
     * workflow path)
     * @param client the client which has registered before with the
     * {@link #registerClientTo(URI, Object)} method
     * @see #registerClientTo(URI, Object)
     */
    public static void unregisterClientFrom(final URI workflow,
            final Object client) {
        if (workflow == null) {
            return;
        }
        MapWFKey wf = new MapWFKey(workflow);
        if (!WORKFLOW_CLIENTS.containsKey(wf)) {
            return;
        }
        Set<Object> callers = WORKFLOW_CLIENTS.get(wf);
        callers.remove(client);
        if (callers.isEmpty()) {
            WORKFLOW_CLIENTS.remove(wf);
        } else {
            WORKFLOW_CLIENTS.put(wf, callers);
        }
        LOGGER.debug(Messages.ProjectWorkflowMap_14 + client + Messages.ProjectWorkflowMap_15 + wf
                + ". " + callers.size() + Messages.ProjectWorkflowMap_17); //$NON-NLS-1$
    }

    /*
     * All registered workflow listeners (KnimeResourceNavigator) which reflect
     * changes on opened workflows (display new nodes).
     */
    private static final Set<WorkflowListener>WF_LISTENERS
        = new LinkedHashSet<WorkflowListener>();

    /*
     * NodeStateChangeListeners (projects) to reflect states of projects
     * (idle, executing, executed). See KnimeResourceLabelProvider.
     */
    private static final Set<NodeStateChangeListener>NSC_LISTENERS
        = new LinkedHashSet<NodeStateChangeListener>();

    // forwards events to registered listeners
    private static final NodeStateChangeListener NSC_LISTENER
    = new NodeStateChangeListener() {

        @Override
        public void stateChanged(final NodeStateEvent state) {
            for (NodeStateChangeListener listener : NSC_LISTENERS) {
                listener.stateChanged(state);
            }
        }

    };

    private static final Set<NodePropertyChangedListener> NODE_PROP_LISTENERS =
        new LinkedHashSet<NodePropertyChangedListener>();

    // forwards events to registered listeners
    private static final NodePropertyChangedListener NODE_PROP_LISTENER =
        new NodePropertyChangedListener() {
        @Override
        public void nodePropertyChanged(final NodePropertyChangedEvent e) {
            for (NodePropertyChangedListener l : NODE_PROP_LISTENERS) {
                l.nodePropertyChanged(e);
            }
        }
    };

    private static final Set<NodeMessageListener> MSG_LISTENERS
        = new LinkedHashSet<NodeMessageListener>();

    private static final NodeMessageListener MSG_LISTENER
        = new NodeMessageListener() {

            @Override
            public void messageChanged(final NodeMessageEvent messageEvent) {
                for (NodeMessageListener l : MSG_LISTENERS) {
                    l.messageChanged(messageEvent);
                }
            }

    };

    private static final WorkflowListener WF_LISTENER = new WorkflowListener() {

        /**
         * Forwards events to registered listeners, if a meta node is added a
         * workflow listener is also added to the meta node in order to reflect
         * changes on the meta node's workflow.
         *
         * {@inheritDoc}
         */
        @Override
        public void workflowChanged(final WorkflowEvent event) {
            // add as listener
            if (event.getType().equals(WorkflowEvent.Type.NODE_ADDED)
                    && event.getNewValue() instanceof WorkflowManager) {
                WorkflowManager manager = (WorkflowManager)event.getNewValue();
                manager.addListener(WF_LISTENER);
                manager.addNodeStateChangeListener(NSC_LISTENER);
                for (NodeContainer cont : manager.getNodeContainers()) {
                    if (cont instanceof WorkflowManager) {
                        WorkflowManager wfm = (WorkflowManager)cont;
                        wfm.addListener(WF_LISTENER);
                        wfm.addNodeStateChangeListener(NSC_LISTENER);
                    }
                }
            }
            // inform registered listeners
            for (WorkflowListener listener : WF_LISTENERS) {
                listener.workflowChanged(event);
            }
            // unregister referring node
            if (event.getType().equals(WorkflowEvent.Type.NODE_REMOVED)
                    && event.getOldValue() instanceof WorkflowManager) {
                WorkflowManager wfm = (WorkflowManager)event.getOldValue();
                wfm.removeListener(WF_LISTENER);
                wfm.removeNodeStateChangeListener(NSC_LISTENER);
            }
        }

    };

    /**
     *
     * @param newPath the new path of the {@link IProject} after renaming
     * @param nc the {@link WorkflowManager} with a project new associated name
     * @param oldPath the old {@link IProject} path, under which the opened
     *  {@link WorkflowManager} is stored in the map
     */
    public static void replace(final URI newPath,
            final WorkflowManagerUI nc, final URI oldPath) {
        if (oldPath == null) {
            throw new IllegalArgumentException(Messages.ProjectWorkflowMap_18 + newPath + ")"); //$NON-NLS-2$
        }
        final MapWFKey oldKey = new MapWFKey(oldPath);
        NodeContainerUI removed = PROJECTS.remove(oldKey);
        if (removed == null) {
            throw new IllegalArgumentException(Messages.ProjectWorkflowMap_20 + oldPath);
        }
        Set<Object> clientList = WORKFLOW_CLIENTS.remove(oldKey);
        WF_LISTENER.workflowChanged(new WorkflowEvent(WorkflowEvent.Type.NODE_REMOVED, removed.getID(), Wrapper.unwrapNC(removed), null));
        putWorkflowUI(newPath, nc);
        if (clientList != null) {
            WORKFLOW_CLIENTS.put(new MapWFKey(newPath), clientList);
        }
        WF_LISTENER.workflowChanged(new WorkflowEvent(WorkflowEvent.Type.NODE_ADDED, nc.getID(), null, nc));
        NSC_LISTENER.stateChanged(new NodeStateEvent(nc.getID()));
    }

    /**
     * Removes the {@link WorkflowManager} from the map, typically when the
     *  referring editor is closed and the WorkflowEditor is disposed.
     * @param path URI of the directory under which the
     * {@link WorkflowManager} is stored in the map.
     */
    public static void remove(final URI path) {
        MapWFKey p = new MapWFKey(path);
        final WorkflowManagerUI manager = (WorkflowManagerUI)PROJECTS.get(p);
        // workflow is only in client map if there is at least one client
        if (manager != null && !WORKFLOW_CLIENTS.containsKey(p)) {
            Wrapper.unwrapWFMOptional(manager).ifPresent(wm -> NodeContext.pushContext(wm));
            try {
                PROJECTS.remove(p);
                if (Wrapper.wraps(manager, WorkflowManager.class)) {
                    WF_LISTENER.workflowChanged(new WorkflowEvent(WorkflowEvent.Type.NODE_REMOVED, manager.getID(),
                        Wrapper.unwrapWFM(manager), null));
                }
                manager.removeListener(WF_LISTENER);
                manager.removeNodeStateChangeListener(NSC_LISTENER);
                manager.removeNodeMessageListener(MSG_LISTENER);
                manager.removeNodePropertyChangedListener(NODE_PROP_LISTENER);
                try {
                    manager.shutdown();
                } catch (Throwable t) {
                    // at least we have tried it
                    LOGGER.error(Messages.ProjectWorkflowMap_21 + p, t);
                } finally {
                    // So far this only needs to be done for locally executed workflows,
                    // i.e. those represented by an ordinary WorkflowManager.
                    // For all other WorkflowManagerUI implementations this is not done.
                    if (Wrapper.wraps(manager, WorkflowManager.class)) {
                        NodeID nodeIDToRemove;
                        WorkflowManager wfm = unwrapWFM(manager);
                        if (wfm.getParent() == WorkflowManager.ROOT) {
                            nodeIDToRemove = wfm.getID();
                        } else {
                            nodeIDToRemove = ((SubNodeContainer)wfm.getDirectNCParent()).getID();
                        }
                        if (manager.getNodeContainerState().isExecutionInProgress()) {
                            ThreadUtils.threadWithContext(() -> {
                                final int timeout = 20;
                                LOGGER.debugWithFormat(
                                    Messages.ProjectWorkflowMap_22 + Messages.ProjectWorkflowMap_23 + " %d" + Messages.ProjectWorkflowMap_27,
                                    timeout);
                                try {
                                    manager.waitWhileInExecution(timeout, TimeUnit.SECONDS);
                                } catch (InterruptedException ie) {
                                    LOGGER.fatal(Messages.ProjectWorkflowMap_24, ie);
                                }
                                if (manager.getNodeContainerState().isExecutionInProgress()) {
                                    LOGGER.errorWithFormat(
                                        Messages.ProjectWorkflowMap_25 + " %d" + Messages.ProjectWorkflowMap_28, timeout);
                                } else {
                                    LOGGER.debug(Messages.ProjectWorkflowMap_26);
                                }
                                WorkflowManager.ROOT.removeProject(nodeIDToRemove);
                            }, "Removal workflow - " + manager.getNameWithID()).start(); //$NON-NLS-1$
                        } else {
                            WorkflowManager.ROOT.removeProject(nodeIDToRemove);
                        }
                    }
                }
            } finally {
                Wrapper.unwrapWFMOptional(manager).ifPresent(wm -> NodeContext.removeLastContext());
            }
        }
    }

    /**
     * Adds a {@link WorkflowManager} of an opened workflow with the URI of
     * the workflow directory to the map. Used by the WorkflowEditor.
     *
     * @param path URI of the directory containing the workflow.knime file
     * @param manager {@link WorkflowManager} in memory holding the workflow
     */
    public static void putWorkflow(final URI path,
            final WorkflowManager manager) {
        putWorkflowUI(path, WorkflowManagerWrapper.wrap(manager));
    }

    /**
     * Adds a {@link WorkflowManagerUI} of an opened workflow with the URI of
     * the workflow directory to the map. Used by the WorkflowEditor.
     *
     * @param path URI of the directory containing the workflow.knime file
     * @param manager {@link WorkflowManagerUI} in memory holding the workflow
     */
    public static void putWorkflowUI(final URI path,
            final WorkflowManagerUI manager) {
        MapWFKey p = new MapWFKey(path);
        // in case the manager is replaced
        // -> unregister listeners from the old one
        NodeContainerUI oldOne = PROJECTS.get(p);
        if (oldOne != null) {
            oldOne.removeNodeStateChangeListener(NSC_LISTENER);
            ((WorkflowManagerUI)oldOne).removeListener(WF_LISTENER);
            oldOne.removeNodeMessageListener(MSG_LISTENER);
            oldOne.removeNodePropertyChangedListener(NODE_PROP_LISTENER);
        }
        PROJECTS.put(p, manager);
        manager.addNodeStateChangeListener(NSC_LISTENER);
        manager.addListener(WF_LISTENER);
        manager.addNodeMessageListener(MSG_LISTENER);
        manager.addNodePropertyChangedListener(NODE_PROP_LISTENER);

        //so far, the WorkflowManagerUI doesn't allow any edit operations won't trigger any changed events
        //TODO - needs to be considered in the future! WorkflowEvent consumers then need to be able to work
        //with WorkflowManagerUI instances, too!
        if(Wrapper.wraps(manager, WorkflowManager.class)) {
            WF_LISTENER.workflowChanged(new WorkflowEvent(
                    WorkflowEvent.Type.NODE_ADDED, manager.getID(), null,
                    Wrapper.unwrapWFM(manager)));
        }
    }

    /**
     * Returns the {@link WorkflowManager} instance which is registered under
     * the workflow URI. Might be <code>null</code> if the
     * {@link WorkflowManager} was not registered with this URI, is closed OR
     * there is only a WorkflowManagerUI (see {@link #getWorkflowUI(URI)}).
     *
     * @see KnimeResourceContentProvider
     * @see KnimeResourceLabelProvider
     *
     * @param path URI of the workflow directory containing the workflow.knime
     * @return the corresponding {@link WorkflowManager} or <code>null</code>
     * if the workflow manager is not registered with the passed URI
     */
    public static NodeContainer getWorkflow(final URI path) {
        return Wrapper.unwrapNCOptional(getWorkflowUI(path)).orElse(null);
    }

    /**
     * Returns the {@link WorkflowManagerUI} instance which is registered under
     * the workflow URI. Might be <code>null</code> if the
     * {@link WorkflowManager} was not registered with this URI or
     * is closed.
     *
     * @see KnimeResourceContentProvider
     * @see KnimeResourceLabelProvider
     *
     * @param path URI of the workflow directory containing the workflow.knime
     * @return the corresponding {@link WorkflowManager} or <code>null</code>
     * if the workflow manager is not registered with the passed URI
     */
    public static NodeContainerUI getWorkflowUI(final URI path) {
        return PROJECTS.get(new MapWFKey(path));
    }

    /**
     * Finds the location of the workflow with the specified ID. If no workflow
     * is registered in the projects map with a matching ID, null is returned.
     *
     * @param workflowID id of the {@link WorkflowManager} for which the
     *            location of the workflow directory should be found
     * @return URI of the directory containing the corresponding workflow, or
     *         null, if the workflow is not registered (not opened).
     */
    public static URI findProjectFor(final NodeID workflowID) {
        for (Map.Entry<MapWFKey, NodeContainerUI> entry : PROJECTS.entrySet()) {
            if (entry.getValue().getID().equals(workflowID)) {
                return entry.getKey().getURI();
            }
        }
        return null;
    }

    /**
     * Adds a workflow listener, which gets informed on every workflow changed
     * event of meta nodes and projects.
     *
     * @param listener to be added
     */
    public static void addWorkflowListener(final WorkflowListener listener) {
        WF_LISTENERS.add(listener);
    }

    /**
     *
     * @param listener to be removed
     */
    public static void removeWorkflowListener(final WorkflowListener listener) {
        WF_LISTENERS.remove(listener);
    }

    /**
     *
     * @param listener listener to be informed about state changes of projects
     */
    public static void addStateListener(
            final NodeStateChangeListener listener) {
        NSC_LISTENERS.add(listener);
    }

    /**
     *
     * @param listener to be removed
     */
    public static void removeStateListener(
            final NodeStateChangeListener listener) {
        NSC_LISTENERS.remove(listener);
    }

    /**
     *
     * @param l listener to be informed about message changes
     */
    public static void addNodeMessageListener(final NodeMessageListener l) {
        MSG_LISTENERS.add(l);
    }

    /**
     *
     * @param l listener to be removed
     */
    public static void removeNodeMessageListener(final NodeMessageListener l) {
        MSG_LISTENERS.remove(l);
    }

    /**
     * @param l The listener to add.
     */
    public static void addNodePropertyChangedListener(
            final NodePropertyChangedListener l) {
        NODE_PROP_LISTENERS.add(l);
    }

    /**
     * @param l the listener to remove
     */
    public static void removeNodePropertyChangedListener(
            final NodePropertyChangedListener l) {
        NODE_PROP_LISTENERS.remove(l);
    }

}