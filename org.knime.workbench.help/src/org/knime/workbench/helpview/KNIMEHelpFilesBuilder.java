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
 *   30.11.2007 (Fabian Dill): created
 */
package org.knime.workbench.helpview;

import java.io.File;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.core.util.FileUtil;
import org.knime.core.util.PathUtils;
import org.knime.workbench.helpview.wizard.KNIMEHelpFilesWizard;
import org.knime.workbench.helpview.wizard.NodeDescriptionConverter;

/**
 * Scans for the node description XML files of all installed KNIME nodes, and
 * converts them into HTML files used by the Eclipse help system. This
 * application uses the same mechanism as the repository manager, i.e. all nodes
 * displayed in the node repository being processed.
 *
 * Important: in order to use this application you have to have write permission
 * on this package in the installation directory!
 *
 * @author Fabian Dill, University of Konstanz
 */
@SuppressWarnings("restriction")
public class KNIMEHelpFilesBuilder implements IApplication {

    private static final String PATTERN_ARG = "-pluginPattern";

    private static final String DESTINATION_ARG = "-destination";

    private static void printUsage() {
        System.err.println("Usage: KNIMEHelpFilesBuilder options");
        System.err.println("Allowed options are:");
        System.err.println("\t-pluginPattern pattern : "
                + "a regular expression for which plug-ins documenation"
                + "should be built; if missing, a wizard for selecting"
                + " some plug-ins is opened");
        System.err.println("\t-destination dir : directory where "
                + "the result should be written to; if missing, files"
                + "are directly written into the plug-in");
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Object start(final IApplicationContext context) throws Exception {
        System.setProperty("java.awt.headless", "true");
        Object o = context.getArguments().get("application.args");

        Pattern pluginPattern = null;
        File destinationDir = null;
        if ((o != null) && (o instanceof String[])) {
            String[] args = (String[])o;
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals(PATTERN_ARG)) {
                    pluginPattern = Pattern.compile(args[i + 1]);
                } else if (args[i].equals(DESTINATION_ARG)) {
                    destinationDir = new File(args[i + 1]);
                } else if (args[i].equals("-help")) {
                    printUsage();
                    return EXIT_OK;

                }
            }
        }

        if (pluginPattern != null) {
            NodeDescriptionConverter.instance().buildDocumentationFor(
                    pluginPattern, destinationDir);
        } else {
            Display display = Display.getDefault();
            KNIMEHelpFilesWizard wizard = new KNIMEHelpFilesWizard();
            WizardDialog dialog = new WizardDialog(SWTUtilities.getActiveShell(display), wizard);
            dialog.open();
            display.dispose();
        }

        if (destinationDir == null) {
            // Clean the plugin configuration, because otherwise the help files
            // will not be found

            Path osgiConfig =
                FileUtil.resolveToPath(Platform.getConfigurationLocation().getURL()).resolve("org.eclipse.osgi");
            PathUtils.deleteDirectoryIfExists(osgiConfig);
        }
        return EXIT_OK;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void stop() {
    }
}
