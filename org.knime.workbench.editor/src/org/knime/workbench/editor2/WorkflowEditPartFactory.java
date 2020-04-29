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
 *   25.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.knime.core.node.workflow.Annotation;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeAnnotation;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeInPort;
import org.knime.core.node.workflow.NodeOutPort;
import org.knime.core.node.workflow.NodePort;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.WorkflowInPort;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowOutPort;
import org.knime.core.ui.node.workflow.ConnectionContainerUI;
import org.knime.core.ui.node.workflow.NodeInPortUI;
import org.knime.core.ui.node.workflow.NodeOutPortUI;
import org.knime.core.ui.node.workflow.NodePortUI;
import org.knime.core.ui.node.workflow.SingleNodeContainerUI;
import org.knime.core.ui.node.workflow.WorkflowInPortUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.ui.node.workflow.WorkflowOutPortUI;
import org.knime.workbench.descriptionview.DescriptionView;
import org.knime.workbench.editor2.directannotationedit.StyledTextEditor;
import org.knime.workbench.editor2.editparts.AnnotationEditPart;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.editor2.editparts.MetaNodeOutPortEditPart;
import org.knime.workbench.editor2.editparts.NodeAnnotationEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeInPortEditPart;
import org.knime.workbench.editor2.editparts.NodeOutPortEditPart;
import org.knime.workbench.editor2.editparts.SubworkflowEditPart;
import org.knime.workbench.editor2.editparts.WorkflowInPortBarEditPart;
import org.knime.workbench.editor2.editparts.WorkflowInPortEditPart;
import org.knime.workbench.editor2.editparts.WorkflowOutPortBarEditPart;
import org.knime.workbench.editor2.editparts.WorkflowOutPortEditPart;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;
import org.knime.workbench.editor2.model.WorkflowPortBar;

/**
 * This factory creates the GEF <code>EditPart</code>s instances (the controller objects) for given model objects;
 * it also serves as a part listener of the workbench and so potentially should be renamed to better represent this.
 *
 * @author Florian Georg, University of Konstanz
 * @author Fabian Dill, University of Konstanz
 */
public final class WorkflowEditPartFactory implements EditPartFactory, IPartListener2 {
    /*
     * we need this flag to determine between the "root" workflow manager and
     * all subsequent metanodes. This means that we implicitely assume that
     * the "root" workflow manager is the first object to come of type
     * WorkflowManager. This assumption is correct, since the top model is
     * passed to the factory and subsequently is asked for its model children.
     *
     * @see WorkflowGraphicalViewerCreator#createViewer
     * @see AbstractGraphicalViewer#sertEditPartFactory
     * @see WorkflowRootEditPart#getModelChildren
     */
    private boolean m_isTop = true;

    /**
     * Adds a part listener to activate the KNIME command context if the KNIME editor is visible.
     */
    public WorkflowEditPartFactory() {
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().addPartListener(this);
    }

    /**
     * Creates the referring edit parts for the following parts of the model.
     * <ul>
     * <li>{@link WorkflowManager}: either {@link WorkflowRootEditPart} or {@link NodeContainerEditPart} (depending on
     * the currently displayed level)</li>
     * <li>{@link SingleNodeContainer}: {@link NodeContainerEditPart}</li>
     * <li>{@link NodeInPort}: {@link NodeInPortEditPart}</li>
     * <li>{@link NodeOutPort}: {@link NodeOutPortEditPart}</li>
     * <li>{@link ConnectionContainer}: {@link ConnectionContainerEditPart}</li>
     * <li>{@link WorkflowInPort}: {@link WorkflowInPortEditPart}</li>
     * <li>{@link WorkflowOutPort}: {@link WorkflowOutPortEditPart}</li>
     * </ul>
     *
     * The {@link WorkflowRootEditPart} has its {@link NodeContainer}s and its {@link WorkflowInPort}s and
     * {@link WorkflowOutPort}s as model children. The {@link NodeContainerEditPart} has its {@link NodePort}s as its
     * children.
     *
     * @see WorkflowRootEditPart#getModelChildren()
     * @see NodeContainerEditPart#getModelChildren()
     *
     * @throws IllegalArgumentException if any other object is passed
     *
     *             {@inheritDoc}
     */
    @Override
    public EditPart createEditPart(final EditPart context, final Object model) {
        // instantiated here
        // correct type in the if statement
        // model at the end of method
        EditPart part = null;
        if (model instanceof WorkflowManagerUI) {
            // this is out "root" workflow manager
            if (m_isTop) {
                // all following objects of type WorkflowManager are treated as
                // metanodes and displayed as NodeContainers
                m_isTop = false;
                part = new WorkflowRootEditPart();
            } else {
                // we already have a "root" workflow manager
                // must be a metanode
                part = new SubworkflowEditPart();
            }
        } else if (model instanceof NodeAnnotation) {
            /* IMPORTANT: first test NodeAnnotation then Annotation (as the
             * first derives from the latter! */
            part = new NodeAnnotationEditPart();
        } else if (model instanceof Annotation) {
            /* IMPORTANT: first test NodeAnnotation then Annotation (as the
             * first derives from the latter! */
            /* workflow annotations hang off the workflow manager */
            part = new AnnotationEditPart();
        } else if (model instanceof WorkflowPortBar) {
            WorkflowPortBar bar = (WorkflowPortBar)model;
            if (bar.isInPortBar()) {
                part = new WorkflowInPortBarEditPart();
            } else {
                part = new WorkflowOutPortBarEditPart();
            }
        } else if (model instanceof SingleNodeContainerUI) {
            // SingleNodeContainer -> NodeContainerEditPart
            part = new NodeContainerEditPart();

            // we have to test for WorkflowInPort first because it's a
            // subclass of NodeInPort (same holds for WorkflowOutPort and
            // NodeOutPort)
        } else if (model instanceof WorkflowInPortUI && context instanceof WorkflowInPortBarEditPart) {
            // WorkflowInPort and context WorkflowRootEditPart ->
            // WorkflowInPortEditPart
            /*
             * if the context is a WorkflowRootEditPart it indicates that the
             * WorkflowInPort is a model child of the WorkflowRootEditPart, i.e.
             * we look at it as a workflow in port. If the context is a
             * NodeContainerEditPart the WorkflowInPort is a model child of a
             * NodeContainerEditPart and we look at it as a node in port.
             */
            WorkflowInPortUI inport = (WorkflowInPortUI)model;
            part = new WorkflowInPortEditPart(inport.getPortType(), inport.getPortIndex());
        } else if (model instanceof WorkflowOutPortUI && context instanceof WorkflowOutPortBarEditPart) {
            // WorkflowOutPort and context WorkflowRootEditPart ->
            // WorkflowOutPortEditPart
            /*
             * if the context is a WorkflowRootEditPart it indicates that the
             * WorkflowOutPort is a model child of the WorkflowRootEditPart,
             * i.e. we look at it as a workflow out port. If the context is a
             * NodeContainerEditPart the WorkflowOutPort is a model child of a
             * NodeContainerEditPart and we look at it as a node out port.
             */

            // TODO: return SubWorkFlowOutPortEditPart
            WorkflowOutPortUI outport = (WorkflowOutPortUI)model;

            part = new WorkflowOutPortEditPart(outport.getPortType(), outport.getPortIndex());
        } else if (model instanceof WorkflowOutPortUI) {
            // TODO: return SubWorkFlowOutPortEditPart
            WorkflowOutPortUI outport = (WorkflowOutPortUI)model;
            part = new MetaNodeOutPortEditPart(outport.getPortType(), outport.getPortIndex());

        } else if (model instanceof NodeInPortUI) {
            // NodeInPort -> NodeInPortEditPart
            NodePortUI port = (NodeInPortUI)model;
            part = new NodeInPortEditPart(port.getPortType(), port.getPortIndex());
        } else if (model instanceof NodeOutPortUI) {
            // NodeOutPort -> NodeOutPortEditPart
            NodePortUI port = (NodeOutPortUI)model;
            part = new NodeOutPortEditPart(port.getPortType(), port.getPortIndex());
        } else if (model instanceof ConnectionContainerUI) {
            // ConnectionContainer -> ConnectionContainerEditPart
            part = new ConnectionContainerEditPart();
        } else {
            throw new IllegalArgumentException("unknown model obj: " + model);
        }
        // associate the model with the part (= the controller)
        part.setModel(model);
        return part;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("restriction")
    @Override
    public void partActivated(final IWorkbenchPartReference partRef) {
        if (DescriptionView.ID.equals(partRef.getId())) {
            return;
        }

        final IWorkbenchWindow iww = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

        if (iww != null) {
            final IWorkbenchPage page = iww.getActivePage();

            if (page != null) {
                final IViewReference descriptionViewReference = page.findViewReference(DescriptionView.ID);

                if (descriptionViewReference != null) {
                    final DescriptionView descriptionView = (DescriptionView)descriptionViewReference.getView(false);

                    if (descriptionView != null) {
                        descriptionView.changeViewDueToPartActivation(
                            org.eclipse.ui.internal.browser.WebBrowserEditor.WEB_BROWSER_EDITOR_ID
                                .equals(partRef.getId()));
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void partBroughtToTop(final IWorkbenchPartReference partRef) {
        //NOOP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void partClosed(final IWorkbenchPartReference partRef) {
        //NOOP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void partDeactivated(final IWorkbenchPartReference partRef) {
        if (WorkflowEditor.ID.equals(partRef.getId())) {
            StyledTextEditor.relinquishFocusOfCurrentEditorIfInView();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void partOpened(final IWorkbenchPartReference partRef) {
        //NOOP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void partHidden(final IWorkbenchPartReference partRef) {
        if (WorkflowEditor.ID.equals(partRef.getId()) && m_activateContext != null) {
            m_activateContext.getContextService().deactivateContext(m_activateContext);
            m_activateContext = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void partInputChanged(final IWorkbenchPartReference partRef) {
        //NOOP
    }

    private IContextActivation m_activateContext;

    /**
     * {@inheritDoc}
     */
    @Override
    public void partVisible(final IWorkbenchPartReference partRef) {
        if (WorkflowEditor.ID.equals(partRef.getId()) && m_activateContext == null) {
            final IContextService contextService =
                PlatformUI.getWorkbench().getService(IContextService.class);
            m_activateContext = contextService.activateContext("org.knime.workbench.editor.context");
        }
    }
}
