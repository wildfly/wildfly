/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.patching;

import java.io.File;

/**
 * The patching directory structure.
 *
 * ${JBOSS_HOME}
 * |
 * |-- bin
 * |-- bundles
 * |   |-- layers.conf
 * |   |-- system
 * |   |   |-- layers
 * |   |   |   | -- xyz
 * |   |   |   |    `-- .patches (overlay directory)
 * |   |   |   |        |-- patch-xyz-1
 * |   |   |   |        `-- patch-xyz-2
 * |   |   |   `-- vuw
 * |   |   |        `-- .patches (overlay directory)
 * |   |   |            `-- patch-vuw-1
 * |   |   |-- base
 * |   |   |   |-- .patches (overlay directory)
 * |   |   |   |   |-- patch-base-1
 * |   |   |   |   `-- patch-base-2
 * |   |   |   `-- org/jboss/as/osgi
 * |   |   `-- add-ons
 * |   |       `-- def
 * |   |           `-- .patches (overlay directory)
 * |   |               |-- patch-def-1
 * |   |               `-- patch-def-2
 * |   |
 * |   `-- my/own/bundle/path/thing
 * |
 * |-- docs
 * |-- modules
 * |   |-- layers.conf
 * |   |-- system
 * |   |   |-- layers
 * |   |   |   | -- xyz
 * |   |   |   |    `-- .patches (overlay directory)
 * |   |   |   |        |-- patch-xyz-1
 * |   |   |   |        `-- patch-xyz-2
 * |   |   |   `-- vuw
 * |   |   |        `-- .patches (overlay directory)
 * |   |   |            `-- patch-vuw-1
 * |   |   |-- base
 * |   |   |   |-- .patches (overlay directory)
 * |   |   |   |   |-- patch-base-1
 * |   |   |   |   `-- patch-base-2
 * |   |   |   |-- org/jboss/as/...
 * |   |   |   `-- org/jboss/as/server/main/module.xml
 * |   |   |
 * |   |   `-- add-ons
 * |   |       `-- def
 * |   |           `-- .patches (overlay directory)
 * |   |               |-- patch-def-1
 * |   |               `-- patch-def-2
 * |   |
 * |   `-- my/own/module/root/repo
 * |
 * |-- .installation (metdata directory)
 * |   |-- layers
 * |   |   |-- xyz
 * |   |   |   |-- installation (layer metadata)
 * |   |   |   |-- cumulative (links to given patchId)
 * |   |   |   |-- references
 * |   |   |   |   `-- patch-xyz-1 (list of one-off patches)
 * |   |   |   `-- history
 * |   |   |       |-- patch-xyz-1
 * |   |   |       |   |-- cumulative (previous cp)
 * |   |   |       |   |-- misc
 * |   |   |       |   `-- rollback.xml
 * |   |   |       `-- patch-xyz-2
 * |   |   |           |-- cumulative
 * |   |   |           |-- misc
 * |   |   |           `-- rollback.xml
 * |   |   |
 * |   |   `-- vuw
 * |   |       |-- installation
 * |   |       |-- cumulative (links to given patchId)
 * |   |       |-- references
 * |   |       |   `-- patch-vuw-1 (list of one-off patches)
 * |   |       `-- history
 * |   |           `-- patch-vuw-1
 * |   |               |-- cumulative (previous cp)
 * |   |               |-- misc
 * |   |               `-- rollback.xml
 * |   |
 * |   |-- base
 * |   |   |-- installation
 * |   |   |-- cumulative (links to given patchId)
 * |   |   |-- references
 * |   |   |   `-- patch-base-1 (list of one-off patches)
 * |   |   `-- history
 * |   |       |-- patch-base-1
 * |   |       |   |-- cumulative (previous cp)
 * |   |       |   |-- misc
 * |   |       |   `-- rollback.xml
 * |   |       `-- patch-base-2
 * |   |           |-- cumulative
 * |   |           |-- misc
 * |   |           `-- rollback.xml
 * |   |
 * |   `-- add-ons
 * |       `-- def
 * |           |-- installation
 * |           |-- cumulative (links to given patchId)
 * |           |-- references
 * |           |   `-- patch-def-1 (list of one-off patches)
 * |           `-- history
 * |               |-- patch-def-1
 * |               |   |-- cumulative (previous cp)
 * |               |   |-- misc
 * |               |   `-- rollback.xml
 * |               `-- patch-def-2
 * |                   |-- cumulative (previous cp)
 * |                   |-- misc
 * |                   `-- rollback.xml
 * |
 * `-- jboss-modules.jar
 *
 * @author Emanuel Muckenhuber
 */
public final class DirectoryStructure {

    public interface InstalledImage {

        /**
         * Get the jboss home.
         *
         * @return the jboss home
         */
        File getJbossHome();

        /**
         * Get the app-client directory.
         *
         * @return the app client dir
         */
        File getAppClientDir();

        /**
         * Get the bundles directory.
         *
         * @return the bundles directory
         */
        File getBundlesDir();

        /**
         * Get the domain directory.
         *
         * @return the domain dir
         */
        File getDomainDir();

        /**
         * Get the modules directory.
         *
         * @return the modules dir
         */
        File getModulesDir();

        /**
         * Get the patches directory.
         *
         * @return the patches directory
         */
        File getPatchesDir();

        /**
         * Get the standalone dir.
         *
         * @return the standalone dir
         */
        File getStandaloneDir();

    }

    // Directories
    public static String APP_CLIENT = "appclient";
    public static String CONFIGURATION = "configuration";
    public static String BUNDLES = "bundles";
    public static String DOMAIN = "domain";
    public static String HISTORY = "history";
    public static String METADATA = ".metadata";
    public static String MODULES = "modules";
    public static String PATCHES = "patches";
    public static String STANDALONE = "standalone";
    // Markers
    public static String CUMULATIVE = "cumulative";
    public static String REFERENCES = "references";

    private final InstalledImage image;

    DirectoryStructure(final InstalledImage image) {
        this.image = image;
    }

    /**
     * Get the installed image layout.
     *
     * @return the installed image
     */
    public InstalledImage getInstalledImage() {
        return image;
    }

    /**
     * Get the patches metadata directory.
     *
     * @return the patches metadata directory
     */
    public File getPatchesMetadata() {
        return new File(getInstalledImage().getPatchesDir(), METADATA);
    }

    /**
     * Get the cumulative patch symlink file.
     *
     * @return the cumulative patch id
     */
    public File getCumulativeLink() {
        return new File(getPatchesMetadata(), CUMULATIVE);
    }

    /**
     * Get the references file, containing all active patches for a given
     * cumulative patch release.
     *
     * @param cumulativeId the cumulative patch id
     * @return the cumulative references file
     */
    public File getCumulativeRefs(final String cumulativeId) {
        final File references = new File(getPatchesMetadata(), REFERENCES);
        return new File(references, cumulativeId);
    }

    /**
     * Get the history dir for a given patch id.
     *
     * @param patchId the patch id
     * @return the history dir
     */
    public File getHistoryDir(final String patchId) {
        final File history = new File(getPatchesMetadata(), HISTORY);
        return new File(history, patchId);
    }

    /**
     * Get the patch directory for a given patch-id.
     *
     * @param patchId the patch-id
     * @return the patch directory
     */
    public File getPatchDirectory(final String patchId) {
        return new File(getInstalledImage().getPatchesDir(), patchId);
    }

    /**
     * Get the bundles patch directory for a given patch-id.
     *
     * @param patchId the patch-id
     * @return the bundles patch directory
     */
    public File getBundlesPatchDirectory(final String patchId) {
        return new File(getPatchDirectory(patchId), BUNDLES);
    }

    /**
     * Get the modules patch directory for a given patch-id.
     *
     * @param patchId the patch-id
     * @return the modules patch directory
     */
    public File getModulePatchDirectory(final String patchId) {
        return new File(getPatchDirectory(patchId), MODULES);
    }

    /**
     * Create the patch environment based on the default layout.
     *
     * @param jbossHome the $JBOSS_HOME
     * @return the patch environment
     */
    public static DirectoryStructure createDefault(final File jbossHome) {
        final File appClient = new File(jbossHome, APP_CLIENT);
        final File bundles = new File(jbossHome, BUNDLES);
        final File domain = new File(jbossHome, DOMAIN);
        final File modules = new File(jbossHome, MODULES);
        final File patches = new File(jbossHome, PATCHES);
        final File standalone = new File(jbossHome, STANDALONE);
        return new DirectoryStructure(new InstalledImage() {

            @Override
            public File getJbossHome() {
                return jbossHome;
            }

            @Override
            public File getBundlesDir() {
                return bundles;
            }

            @Override
            public File getModulesDir() {
                return modules;
            }

            @Override
            public File getPatchesDir() {
                return patches;
            }

            @Override
            public File getAppClientDir() {
                return appClient;
            }

            @Override
            public File getDomainDir() {
                return domain;
            }

            @Override
            public File getStandaloneDir() {
                return standalone;
            }
        });
    }

}
