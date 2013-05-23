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

import static org.jboss.as.patching.Constants.APP_CLIENT;
import static org.jboss.as.patching.Constants.BUNDLES;
import static org.jboss.as.patching.Constants.CUMULATIVE;
import static org.jboss.as.patching.Constants.DOMAIN;
import static org.jboss.as.patching.Constants.METADATA;
import static org.jboss.as.patching.Constants.MODULES;
import static org.jboss.as.patching.Constants.PATCHES;
import static org.jboss.as.patching.Constants.REFERENCES;
import static org.jboss.as.patching.Constants.STANDALONE;

import java.io.File;

import org.jboss.as.patching.installation.InstalledImage;

/**
 * The patching directory structure.
 *
 * <pre>
 * <code>
 * ${JBOSS_HOME}
 * |
 * |-- bin
 * |-- bundles
 * |   |-- layers.conf
 * |   |-- system
 * |   |   |-- layers
 * |   |   |   | -- xyz
 * |   |   |   |   `-- .patches (overlay directory)
 * |   |   |   |       |-- cumulative (cumulative link)
 * |   |   |   |       |-- references
 * |   |   |   |       |   `-- patch-xyz-1 (list of one-off patches)
 * |   |   |   |       |-- patch-xyz-1
 * |   |   |   |       `-- patch-xyz-2
 * |   |   |   |-- vuw
 * |   |   |   |   `-- .patches (overlay directory)
 * |   |   |   |       |-- cumulative (cumulative link)
 * |   |   |   |       |-- references
 * |   |   |   |       |   `-- patch-vuw-1 (list of one-off patches)
 * |   |   |   |       `-- patch-vuw-1
 * |   |   |   `-- base
 * |   |   |       |-- .patches (overlay directory)
 * |   |   |       |   |-- cumulative (cumulative link)
 * |   |   |       |   |-- references
 * |   |   |       |   |   `-- patch-base-1 (list of one-off patches)
 * |   |   |       |   |-- patch-base-1
 * |   |   |       |   `-- patch-base-2
 * |   |   |       `-- org/jboss/as/osgi
 * |   |   `-- add-ons
 * |   |       `-- def
 * |   |           `-- .patches (overlay directory)
 * |   |               |-- cumulative (cumulative link)
 * |   |               |-- references
 * |   |               |   `-- patch-def-1 (list of one-off patches)
 * |   |               |-- patch-def-1
 * |   |               `-- patch-def-2
 * |   |
 * |   `-- my/own/bundle/path/thing
 * |
 * |-- docs
 * |-- modules
 * |   |-- layers.conf  (vuw,xyz,base)
 * |   |-- system
 * |   |   |-- layers
 * |   |   |   | -- xyz
 * |   |   |   |    `-- .patches (overlay directory)
 * |   |   |   |       |-- cumulative (cumulative link)
 * |   |   |   |       |-- references
 * |   |   |   |       |   `-- patch-xyz-1 (list of one-off patches)
 * |   |   |   |       |-- patch-xyz-1
 * |   |   |   |       `-- patch-xyz-2
 * |   |   |   |-- vuw
 * |   |   |   |    `-- .patches   (overlay directory)
 * |   |   |   |       |-- cumulative (cumulative link)
 * |   |   |   |       |-- references
 * |   |   |   |       |   `-- patch-vuw-1 (list of one-off patches)
 * |   |   |   |        `-- patch-vuw-1
 * |   |   |   ` -- base
 * |   |   |        |-- .patches   (overlay directory)
 * |   |   |        |   |-- cumulative (links to given patchId)
 * |   |   |        |   |-- references
 * |   |   |        |   |   `-- patch-base-1 (list of one-off patches)
 * |   |   |        |   |-- patch-base-1
 * |   |   |        |   `-- patch-base-2
 * |   |   |        |-- org/jboss/as/...
 * |   |   |        `-- org/jboss/as/server/main/module.xml
 * |   |   |
 * |   |   `-- add-ons
 * |   |       `-- def
 * |   |           `-- .patches    (overlay directory)
 * |   |               |-- patch-def-1
 * |   |               `-- patch-def-2
 * |   |
 * |   `-- my/own/module/root/repo
 * |
 * |-- .installation               (metadata directory)
 * |   |-- cumulative              (patchId for the identity)
 * |   |-- references
 * |   |   `-- patch-identity-1    (list of one-off patches)
 * |   `-- patches                 (history dir)
 * |       `-- patch-identity-1
 * |           |-- patch.xml
 * |           |-- rollback.xml
 * |           |-- timestamp
 * |           |-- configuration   (configuration backup)
 * |           `-- misc            (misc backup)
 * |
 * `-- jboss-modules.jar
 * </code>
 * </pre>
 *
 * @author Emanuel Muckenhuber
 */
public abstract class DirectoryStructure {

    /**
     * Get the installed image layout.
     *
     * @return the installed image
     */
    public abstract InstalledImage getInstalledImage();

    /**
     * Get the cumulative patch symlink file.
     *
     * @return the cumulative patch id
     */
    public abstract File getCumulativeLink();

    /**
     * Get the references file, containing all active patches for a given
     * cumulative patch release.
     *
     * @param cumulativeId the cumulative patch id
     * @return the cumulative references file
     */
    public abstract File getCumulativeRefs(final String cumulativeId);

    /**
     * Get the bundles repository root.
     *
     * @return the bundle base directory
     */
    public abstract File getBundleRepositoryRoot();

    /**
     * Get the bundles patch directory for a given patch-id.
     *
     * @param patchId the patch-id
     * @return the bundles patch directory
     */
    public abstract File getBundlesPatchDirectory(final String patchId);

    /**
     * Get the module root.
     *
     * @return the module root
     */
    public abstract File getModuleRoot();

    /**
     * Get the modules patch directory for a given patch-id.
     *
     * @param patchId the patch-id
     * @return the modules patch directory
     */
    public abstract File getModulePatchDirectory(final String patchId);

    /**
     * Create the legacy patch environment based on the default layout.
     *
     * @param jbossHome the $JBOSS_HOME
     * @return the patch environment
     * @deprecated see {@linkplain org.jboss.as.patching.installation.InstallationManager}
     */
    @Deprecated
    public static DirectoryStructure createLegacy(final File jbossHome) {
        final File appClient = new File(jbossHome, APP_CLIENT);
        final File bundles = new File(jbossHome, BUNDLES);
        final File domain = new File(jbossHome, DOMAIN);
        final File modules = new File(jbossHome, MODULES);
        final File installation = new File(jbossHome, Constants.INSTALLATION);
        final File patches = new File(modules, PATCHES);
        final File standalone = new File(jbossHome, STANDALONE);
        return new LegacyDirectoryStructure(new InstalledImage() {

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
            public File getInstallationMetadata() {
                return installation;
            }

            @Override
            public File getLayersConf() {
                return new File(getModulesDir(), Constants.LAYERS_CONF);
            }

            @Override
            public File getPatchesDir() {
                return patches;
            }

            @Override
            public File getPatchHistoryDir(String patchId) {
                return new File(getInstallationMetadata(), patchId);
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

    static class LegacyDirectoryStructure extends DirectoryStructure {
        private final InstalledImage image;
        LegacyDirectoryStructure(final InstalledImage image) {
            this.image = image;
        }

        @Override
        public InstalledImage getInstalledImage() {
            return image;
        }

        public File getPatchesMetadata() {
            return new File(getInstalledImage().getPatchesDir(), METADATA);
        }

        @Override
        public File getCumulativeLink() {
            return new File(getPatchesMetadata(), CUMULATIVE);
        }

        @Override
        public File getCumulativeRefs(final String cumulativeId) {
            final File references = new File(getPatchesMetadata(), REFERENCES);
            return new File(references, cumulativeId);
        }

        public File getPatchDirectory(final String patchId) {
            return new File(getInstalledImage().getPatchesDir(), patchId);
        }

        @Override
        public File getBundlesPatchDirectory(final String patchId) {
            return new File(getPatchDirectory(patchId), BUNDLES);
        }

        @Override
        public File getModulePatchDirectory(final String patchId) {
            return new File(getPatchDirectory(patchId), MODULES);
        }

        @Override
        public File getBundleRepositoryRoot() {
            return getInstalledImage().getBundlesDir();
        }

        @Override
        public File getModuleRoot() {
            return getInstalledImage().getModulesDir();
        }
    }

}