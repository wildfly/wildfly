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

import static org.jboss.as.patching.Constants.PATCHES;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.jboss.as.patching.Constants;
import org.jboss.as.patching.DirectoryStructure;
import org.jboss.as.patching.runner.PatchUtils;

/**
 * @author Emanuel Muckenhuber
 */
abstract class AbstractPatchableTarget extends DirectoryStructure implements Layer, AddOn {

    protected File getPatchesMetadata() {
        File root = getModuleRoot();
        if (root == null) {
            root = getBundleRepositoryRoot();
        }
        if (root == null) {
            throw new IllegalStateException("no associated module or bundle repository with this layer");
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
        // ${repo-root}/bundles/${layer.type+name}/patches/${patch.id}
        final File patches = new File(getBundleRepositoryRoot(), PATCHES);
        return new File(patches, patchId);
    }

    @Override
    public File getModulePatchDirectory(final String patchId) {
        if (getModuleRoot() == null) {
            return null;
        }
        // ${repo-root}/modules/${layer.type+name}/patches/${patch.id}
        final File patches = new File(getModuleRoot(), PATCHES);
        return new File(patches, patchId);
    }

    @Override
    public TargetInfo loadTargetInfo() throws IOException {
        final Properties properties = PatchUtils.loadProperties(getInstallationInfo());
        final String ref = PatchUtils.readRef(properties, Constants.CUMULATIVE);
        final List<String> patches = PatchUtils.readRefs(properties);
        return new TargetInfo() {

            @Override
            public String getCumulativeID() {
                return ref;
            }

            @Override
            public List<String> getPatchIDs() {
                return patches;
            }

            @Override
            public Properties getProperties() {
                return properties;
            }

            @Override
            public DirectoryStructure getDirectoryStructure() {
                return AbstractPatchableTarget.this;
            }

        };
    }

}
