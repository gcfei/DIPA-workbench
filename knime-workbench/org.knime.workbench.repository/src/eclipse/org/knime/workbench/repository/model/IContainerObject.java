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
 * Interface for repository objects that act as a container of other objects.
 *
 * @author Florian Georg, University of Konstanz
 */
public interface IContainerObject extends IRepositoryObject {
    /**
     * Constant indicating that the finder method should look at "infinite"
     * depth.
     */
    public static final int DEPTH_INFINITE = -1;

    /**
     * Returns wheter this container conatains cildren.
     *
     * @return <code>true</code>, if this container has children
     *
     */
    public boolean hasChildren();

    /**
     * Returns the children.
     *
     * @return Array containing the children
     */
    public IRepositoryObject[] getChildren();

    /**
     * Adds a child and returns whether it was successfully added nor not. A child cannot be added, e.g.,
     * if a sibling with the same name already exists.
     *
     * @param child The child to add
     * @return <code>true</code> if the child was added successfully, <code>false</code> otherwise
     */
    public boolean addChild(AbstractRepositoryObject child);

    /**
     * Adds a child after an existing object. If the specified existing object
     * does not exist or the child is already contained in this container, the
     * child is not added and <code>false</code> is returned.
     *
     * @param child the child to add
     * @param before the object that is before the new child
     * @return <code>true</code> if the child was added, <code>false</code> if
     *         the child has not been added
     */
    public boolean addChildAfter(AbstractRepositoryObject child,
            AbstractRepositoryObject before);

    /**
     * Adds a child before an existing object. If the specified existing object
     * does not exist or the child is already contained in this container, the
     * child is not added and <code>false</code> is returned.
     *
     * @param child the child to add
     * @param after the object that is after the new child
     * @return <code>true</code> if the child was added, <code>false</code> if
     *         the child has not been added
     */
    public boolean addChildBefore(AbstractRepositoryObject child,
            AbstractRepositoryObject after);

    /**
     * Removes a child, should throw an exception if invalid.
     *
     * @param child The child to remove
     */
    public void removeChild(AbstractRepositoryObject child);

    /**
     * Looks up a child, given by id.
     *
     * @param id The (level) id
     * @param recurse whether to dive into sub-containers
     *
     * @return The child, or <code>null</code>
     */
    public IRepositoryObject getChildByID(String id, boolean recurse);

    /**
     * Returns if the given object is a direct child of this object.
     *
     * @param child a potential child of this container
     * @return <code>true</code> if it is a child of this container,
     *         <code>false</code> otherwise
     */
    public boolean contains(IRepositoryObject child);

    /**
     * Returns whether this container object is locked or not. If it is locked, it only accepts
     * children from org.knime or com.knime plug-ins.
     *
     * @return <code>true</code> if this container is locked, <code>false</code> otherwise
     */
    public boolean isLocked();
}
