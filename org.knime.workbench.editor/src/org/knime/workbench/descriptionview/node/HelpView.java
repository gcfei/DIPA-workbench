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
 *   30.09.2005 (ohl): created
 */
package org.knime.workbench.descriptionview.node;

import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.workbench.descriptionview.BrowserProvider;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.repository.model.Category;
import org.knime.workbench.repository.model.MetaNodeTemplate;
import org.knime.workbench.repository.model.NodeTemplate;
import org.knime.workbench.repository.util.DynamicNodeDescriptionCreator;
import org.knime.workbench.repository.util.NodeFactoryHTMLCreator;

/**
 * View displaying the description of the selected nodes. The description is provided by the node's factory.
 *
 * @author ohl, University of Konstanz
 */
public class HelpView extends Composite {
    private final BrowserProvider m_browserProvider;

    /**
     * @param parent
     */
    public HelpView(final Composite parent) {
        super(parent, SWT.NONE);

        setLayout(new FillLayout());

        m_browserProvider = new BrowserProvider(this, false);

        pack();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setFocus() {
        return m_browserProvider.setFocus();
    }

    /**
     * The method updating the content of the browser. Depending on the type of the selected part(s) it will retrieve
     * the node(s) description and set it in the browser.
     *
     * @param selection
     */
    public void selectionChanged(final IStructuredSelection selection) {
        // we display the full description only if a single node is selected
        final boolean useSingleLine;
        if ((selection.size() > 1) || (selection.getFirstElement() instanceof Category)) {
            useSingleLine = true;
        } else {
            useSingleLine = false;
        }

        // construct the html page to display
        final StringBuilder content = new StringBuilder();
        if (useSingleLine) {
            // add the prefix to make it a html page
            content.append("<html><head>");
            content.append("<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\"></meta>");
            // include stylesheet
            content.append("<style>");
            content.append(NodeFactoryHTMLCreator.instance.getCss());
            content.append("</style>");
            content.append("</head><body><dl>");
        }
        // "Keep a list of already displayed objects (this works as long as the selected items come in an ordered way)
        //  ordered with item containing other selected items coming before the items contained. For the tree view
        //  in the repository this is the case." - presumably Ohl
        final HashSet<String> ids = new HashSet<String>();
        for (final Iterator<?> selIt = selection.iterator(); selIt.hasNext();) {
            final Object sel = selIt.next();
            if (sel instanceof Category) {
                // its a category in the node repository, display a list of
                // contained nodes
                final Category cat = (Category)sel;
                if (!ids.contains(cat.getID())) {
                    ids.add(cat.getID());
                    DynamicNodeDescriptionCreator.instance().addDescription(cat, content, ids);
                }
            } else if (sel instanceof NodeTemplate) {
                // its a node selected in the repository
                final NodeTemplate templ = (NodeTemplate)sel;
                if (!ids.contains(templ.getID())) {
                    ids.add(templ.getID());
                    DynamicNodeDescriptionCreator.instance().addDescription(templ, useSingleLine, content);
                }
            } else if (sel instanceof NodeContainerEditPart) {
                // if multiple nodes in the editor are selected we should
                // not show description for the same node (if used multiple
                // times) twice. We store the node name in the set.
                final NodeContainerUI nc = ((NodeContainerEditPart)sel).getNodeContainer();
                if (!ids.contains(nc.getName())) {
                    ids.add(nc.getName());
                    DynamicNodeDescriptionCreator.instance().addDescription(nc, useSingleLine, content);
                }
            } else if (sel instanceof MetaNodeTemplate) {
                // TODO: add support for MetaNodeTemplates and get the
                // description out of them
                final NodeContainerUI manager = ((MetaNodeTemplate)sel).getManager();
                DynamicNodeDescriptionCreator.instance().addDescription(manager, useSingleLine, content);
            }
        }
        if (useSingleLine) {
            // finish the html
            content.append("</dl></body></html>");
        }

        m_browserProvider.updateBrowserContent(content.toString());
    }
}
