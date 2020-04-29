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
 *   16.03.2005 (georg): created
 */
package org.knime.workbench.repository.model;


/**
 * Realizes a root node. This has no parent (<code>null</code>) and can't be
 * added as a child to other containers.
 *
 * @author Florian Georg, University of Konstanz
 */
public class Root extends AbstractContainerObject {
    /**
     * Constructor for a root.
     */
    public Root() {
        super("", "Root", "org.knime.base", false);
    }

    /**
     * Creates a copy of the given repository root.
     *
     * @param copy the object that should be copied
     */
    protected Root(final Root copy) {
        super(copy);
    }

    /**
     * This returns the '/' as ID for the root repository element.
     *
     * @see org.knime.workbench.repository.model.IRepositoryObject#getID()
     */
    @Override
    public String getID() {
        return "/";
    }

    /**
     * {@inheritDoc}
     *
     * @return always <code>null</code>
     */
    @Override
    public IContainerObject getParent() {
        return null;
    }

    /**
     * Throws a <code>UnsupportedOperationException</code>.
     *
     * {@inheritDoc}
     *
     */
    @Override
    public void setParent(final IContainerObject parent) {
        throw new UnsupportedOperationException(
                "Can't set parent of a root object");
    }

    /**
     * Locates a sub-container given by the supplied name.
     *
     * @param path The path that is made up of ID segments, separated by a slash
     *            "/"
     * @return The container, or <code>null</code> if not found
     */
    public IContainerObject findContainer(final String path) {
        String[] segments = path.split("/");
        IContainerObject parent = this;
        for (int i = 0; i < segments.length; i++) {
            IRepositoryObject obj = parent.getChildByID(segments[i], false);

            // not found ?
            if (obj == null) {
                return null;
            }
            // object is no container ? skip it
            if (!(obj instanceof IContainerObject)) {
                continue;
            }
            parent = (IContainerObject)obj;

        }

        assert parent != null;

        return parent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IRepositoryObject deepCopy() {
        return new Root(this);
    }
}
