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

package org.jboss.as.boot;

import java.io.File;

/**
 * The patching directory structure.
 *
 * ${JBOSS_HOME}
 * |
 * |-- modules
 * |-- patches (overlay directory)
 * |   |-- patch01
 * |   |-- patch02
 * |   `-- .metadata
 * |       |-- cumulative (links to given patchId)
 * |       |-- references (list of one-off patches)
 * |       |   |-- patch01
 * |       |   `-- patch02
 * |       `-- history (rollback information for a patch)
 * |           |-- patch01
 * |           `-- patch02
 * |
 * `-- loader.jar (boot module-loader)
 *
 * @author Emanuel Muckenhuber
 */
public final class DirectoryStructure {

    static String CUMULATIVE = "cumulative";
    static String HISTORY = "history";
    static String METADATA = ".metadata";
    static String MODULES = "modules";
    static String PATCHES = "patches";
    static String REFERENCES = "references";

    public static interface InstalledImage {

        /**
         * Get the jboss home.
         *
         * @return the jboss home
         */
        File getJbossHome();

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

    }

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
     * Create the patch environment based on the default layout.
     *
     * @param jbossHome the $JBOSS_HOME
     * @return the patch environment
     */
    public static DirectoryStructure createDefault(final File jbossHome) {
        final File modules = new File(jbossHome, MODULES);
        final File patches = new File(jbossHome, PATCHES);
        return new DirectoryStructure(new InstalledImage() {

            @Override
            public File getJbossHome() {
                return jbossHome;
            }

            @Override
            public File getModulesDir() {
                return modules;
            }

            @Override
            public File getPatchesDir() {
                return patches;
            }
        });
    }

}
