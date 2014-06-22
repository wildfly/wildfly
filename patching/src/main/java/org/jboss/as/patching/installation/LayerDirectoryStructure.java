/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.patching.installation;

import static org.jboss.as.patching.Constants.OVERLAYS;

import java.io.File;

import org.jboss.as.patching.Constants;
import org.jboss.as.patching.DirectoryStructure;
import org.jboss.as.patching.logging.PatchLogger;

/**
 * @author Emanuel Muckenhuber
 */
abstract class LayerDirectoryStructure extends DirectoryStructure {

    abstract File getPatchesMetadata();

    protected File getPatchesMetadata(String name) {
        File root = getModuleRoot();
        if (root == null) {
            root = getBundleRepositoryRoot();
        }
        if (root == null) {
            throw PatchLogger.ROOT_LOGGER.installationInvalidLayerConfiguration(name);
        }
        return root;
    }

    @Override
    public File getInstallationInfo() {
        return new File(getPatchesMetadata(), Constants.INSTALLATION_METADATA);
    }

    @Override
    public File getBundlesPatchDirectory(final String patchId) {
        if (getBundleRepositoryRoot() == null) {
            return null;
        }
        // ${repo-root}/bundles/${layer.type+name}/.overlays/${patch.id}
        final File patches = new File(getBundleRepositoryRoot(), OVERLAYS);
        return new File(patches, patchId);
    }

    @Override
    public File getModulePatchDirectory(final String patchId) {
        if (getModuleRoot() == null) {
            return null;
        }
        // ${repo-root}/modules/${layer.type+name}/.overlays/${patch.id}
        final File patches = new File(getModuleRoot(), OVERLAYS);
        return new File(patches, patchId);
    }

    /**
     * Specific directory structure implementation for the identity.
     */
    abstract static class IdentityDirectoryStructure extends LayerDirectoryStructure {

        @Override
        public final File getBundleRepositoryRoot() {
            return null; // no bundle root associated with the identity
        }

        @Override
        public final File getModuleRoot() {
            return null; // no module root associated with the identity
        }

        @Override
        public File getInstallationInfo() {
            return new File(getPatchesMetadata(), Constants.IDENTITY_METADATA);
        }

        @Override
        protected File getPatchesMetadata() {
            return getInstalledImage().getInstallationMetadata();
        }

    }

}
