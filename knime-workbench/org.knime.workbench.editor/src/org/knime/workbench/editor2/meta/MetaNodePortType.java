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
 *
 */
package org.knime.workbench.editor2.meta;

import org.knime.core.node.port.PortType;

/** Represents a port type available in the metanode wizard. This class
 * is registered in the contributions to the metanode port type
 * extension point.
 *
 * <p>Derived class must provide a node-arg constructor, see
 * {@link #MetaNodePortType(PortType, String)} for details.
 *
 * @deprecated register your {@link PortType} at the extension point <tt>org.knime.core.PortType</tt> instead
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
@Deprecated
public abstract class MetaNodePortType {

    private final PortType m_type;
    private final String m_name;

    /** Constructors that sets final fields for port type and name.
     * Derived classes will have an no-arg constructor that calls
     * this constructor with the corresponding values, e.g.
     * <pre>
     * public FooMetaNodePortType() {
     *   super(FooPortObject.TYPE, "Foo");
     * }
     * </pre>
     * None of the arguments must be null.
     * @param type The associated port type.
     * @param name A user-friendly name, e.g. "Data", "Database", "PMML"
     * @throws NullPointerException If either argument is null.
     */
    protected MetaNodePortType(final PortType type,
            final String name) {
        if (name == null || type == null) {
            throw new IllegalArgumentException(Messages.MetaNodePortType_0);
        }
        m_type = type;
        m_name = name;
    }

    /** @return the type */
    public final PortType getType() {
        return m_type;
    }

    /** @return the name */
    public final String getName() {
        return m_name;
    }

    /** {@inheritDoc} */
    @Override
    public final String toString() {
        return m_name + " -- " + m_type; //$NON-NLS-1$
    }

    /** {@inheritDoc} */
    @Override
    public final boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof MetaNodePortType) {
            MetaNodePortType other = (MetaNodePortType)obj;
            return (other.getName().equals(getName()));
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public final int hashCode() {
        return m_name.hashCode();
    };
}
