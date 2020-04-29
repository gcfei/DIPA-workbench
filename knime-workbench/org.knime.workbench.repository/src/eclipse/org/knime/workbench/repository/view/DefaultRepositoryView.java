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
 *   29.05.2012 (meinl): created
 */
package org.knime.workbench.repository.view;

import java.io.File;

import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.repository.model.CustomRepositoryManager;
import org.knime.workbench.repository.model.Root;

/**
 * The standard node repository view. It shows all available nodes, except if a
 * custom node repository definition file has been placed into the KNIME
 * installation directory (see {@link #DEFINITION_FILE}). In this case the
 * repository is transformed according to the definition file. There is no way
 * for the use to get access the repository any more!
 *
 * @author Thorsten Meinl, University of Konstanz
 * @since 2.6
 */
public class DefaultRepositoryView extends AbstractRepositoryView {

    static final String ID = "org.knime.workbench.repository.view.RepositoryView";

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(DefaultRepositoryView.class);

    /**
     * Name of the custom node repository definition file ({@value} ). The file
     * must be placed directly into the KNIME installation directory.
     * @since 2.6
     */
    public static final String DEFINITION_FILE = "customNodeRepository.xml";

    private CustomRepositoryManager m_manager;

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final IViewSite site) throws PartInitException {
        super.init(site);
        m_manager = null;
        Location loc = Platform.getInstallLocation();
        if (loc == null) {
            LOGGER.error("Cannot detected KNIME installation directory");
            return;
        } else if (!loc.getURL().getProtocol().equals("file")) {
            LOGGER.error("KNIME installation directory is not local");
            return;
        }
        File instDir = new File(loc.getURL().getPath());
        File customDefinition = new File(instDir, DEFINITION_FILE);
        if (customDefinition.exists()) {
            try {
                m_manager = new CustomRepositoryManager(customDefinition);
                setPartName(m_manager.getCustomName());
            } catch (Exception ex) {
                throw new PartInitException(
                        "Could not load custom repository content", ex);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Root transformRepository(final Root originalRoot) {
        if (m_manager != null) {
            return m_manager.transformRepository(originalRoot);
        } else {
            return originalRoot;
        }
    }
}
