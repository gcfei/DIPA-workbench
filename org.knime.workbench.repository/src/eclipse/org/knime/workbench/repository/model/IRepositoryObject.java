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

import org.eclipse.core.runtime.IAdaptable;

/**
 * Base interface for objects in the repository.
 *
 * @author Florian Georg, University of Konstanz
 */
public interface IRepositoryObject extends IAdaptable {
    /**
     * Returns an ID for this object.The semantics may differ in the concrete
     * implementations.
     *
     * @return A (semantically) id for this object
     */
    public String getID();

    /**
     * Returns the parent object. May be <code>null</code> if this is a root
     * object, or detached from the model tree.
     *
     * @return The parent, or <code>null</code>
     */
    public IContainerObject getParent();

    /**
     * Moves this object to a new parent object.
     *
     * @param newParent The new parent.
     */
    public void move(IContainerObject newParent);

    /**
     * Creates a deep copy of this object. Deep means that all other
     * {@link IRepositoryObject} associated with this object are also
     * deep-copied. Other objects are not copied, they are shared afterwards
     * instead.
     *
     * @return a deep copy
     */
    public IRepositoryObject deepCopy();


    /**
     * Returns the id of the plug-in which contributed this container.
     *
     * @return a plug-in id or <code>null</code> if the plug-in is unknown
     */
    public String getContributingPlugin();

    /**
     * Return the name of this object.
     *
     * @return the object's name
     */
    public String getName();
}
