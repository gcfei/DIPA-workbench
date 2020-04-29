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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.graphics.Image;

/**
 * Implementation of a repository category. (usually contributed by extension
 * point)
 *
 * @author Florian Georg, University of Konstanz
 */
public class Category extends AbstractContainerObject {
    private String m_description;

    private String m_path;

    private Image m_icon;

    /**
     * Creates a new category that is a copy of the given category.
     *
     * @param copy the category that should be copied
     */
    protected Category(final Category copy) {
        super(copy);
        this.m_description = copy.m_description;
        this.m_path = copy.m_path;
        this.m_icon = copy.m_icon;
    }

    /**
     * Creates a new locked repository category with the given level-id and name.
     *
     * @param id the category's unique id, must not be <code>null</code>
     * @param name the category's display name, must not be <code>null</code>
     * @param contributingPlugin the id of the plug-in which contributed this object, must not be <code>null</code>
     */
    public Category(final String id, final String name, final String contributingPlugin) {
        super(id, name, contributingPlugin);
    }


    /**
     * Creates a new repository category with the given level-id and name.
     *
     * @param id the category's unique id, must not be <code>null</code>
     * @param name the category's display name, must not be <code>null</code>
     * @param contributingPlugin the id of the plug-in which contributed this container object, must not be
     *            <code>null</code>
     * @param locked <code>true</code> if this category is locked, <code>false</code> otherwise
     */
    public Category(final String id, final String name, final String contributingPlugin, final boolean locked) {
        super(id, name, contributingPlugin, locked);
    }

    /**
     * Returns a description for this category.
     *
     * @return a description or <code>null</code> if non is available
     */
    public String getDescription() {
        return m_description;
    }

    /**
     * Sets a description for this category.
     *
     * @param description a description
     */
    public void setDescription(final String description) {
        m_description = description;
    }

    /**
     * Returns the path from the root to this category.
     *
     * @return the absolute path
     */
    public String getPath() {
        return m_path;
    }

    /**
     * Sets the path from the root to this category.
     *
     * @param path the absolute path
     */
    public void setPath(final String path) {
        m_path = path;
    }

    /**
     * Returns the category's icon.
     *
     * @return an icon
     */
    public Image getIcon() {
        return m_icon;
    }

    /**
     * Sets the category's icon.
     *
     * @param icon an icon
     */
    public void setIcon(final Image icon) {
        m_icon = icon;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Id: " + getID() + " Name: " + getName() + " After-id: "
                + getAfterID();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IRepositoryObject deepCopy() {
        return new Category(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result =
                prime
                        * result
                        + ((m_description == null) ? 0 : m_description
                                .hashCode());
        result = prime * result + ((m_path == null) ? 0 : m_path.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Category other = (Category)obj;
        if (!Objects.equals(m_description, other.m_description)) {
            return false;
        }
        if (!Objects.equals(m_path, other.m_path)) {
            return false;
        }
        return true;
    }

    private static final Pattern NUMERICAL_END_PATTERN = Pattern.compile("(.*?)(\\d+)$");

    /**
     * Creates a unique name for a new category inside the given parent. The new name consists of the given name
     * and potentially a number added at the end.
     *
     * @param parent the parent category
     * @param name the desired new name
     * @return the given name, if it is unique, or a uniquified name
     */
    public static String createUniqueName(final IContainerObject parent, final String name) {
        Set<String> childNames = new HashSet<String>();
        for (IRepositoryObject o : parent.getChildren()) {
            if (o instanceof Category) {
                childNames.add(((Category) o).getName());
            }
        }

        String newName = name;
        String oldName = name;
        int index = 1;
        Matcher m = NUMERICAL_END_PATTERN.matcher(name);
        if (m.matches()) {
            index = Integer.parseInt(m.group(2)) + 1;
            oldName = m.group(1);
        }
        while (childNames.contains(newName)) {
            newName = oldName.trim() + " " + index++;
        }
        return newName;
    }
}
