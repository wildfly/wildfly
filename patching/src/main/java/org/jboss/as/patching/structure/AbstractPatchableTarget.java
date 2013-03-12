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

package org.jboss.as.patching.structure;

import org.jboss.as.patching.Constants;
import org.jboss.as.patching.PatchInfo;
import org.jboss.as.patching.runner.PatchUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Emanuel Muckenhuber
 */
abstract class AbstractPatchableTarget implements BaseLayer, Layer, AddOn {

    /**
     * Get the layer specific modules root: ${module-repo-root}/modules/system/${layer.type+name}
     *
     * @return the modules root
     */
    protected abstract File getModulesBase();

    /**
     * Get the layer specific bundles root: ${bundle-repo-root}/bundles/system/${layer.type+name}
     *
     * @return the bundles root
     */
    protected abstract File getBundlesBase();

    /**
     * Get the layer specific metadata directory: ${jboss.home}/patches/${layer.type+name}
     *
     * @return the metadata directory
     */
    protected abstract File getPatchesMetadataDir();

    protected File getModulesPatchesDir(String patchId) {
        // ${repo-root}/modules/${layer.type+name}/patches/${patch.id}
        final File patches = new File(getModulesBase(), Constants.PATCHES);
        return new File(patches, patchId);
    }

    protected File getBundlesPatchesDir(String patchId) {
        // ${repo-root}/bundles/${layer.type+name}/patches/${patch.id}
        final File patches = new File(getBundlesBase(), patchId);
        return new File(patches, patchId);
    }

    protected File getCumulativeLink() {
        return new File(getPatchesMetadataDir(), Constants.CUMULATIVE);
    }

    protected File getCumulativeRefs(final String cumulativeId) {
        final File references = new File(getPatchesMetadataDir(), Constants.REFERENCES);
        return new File(references, cumulativeId);
    }

    protected File getBundlesPatchDirectory(final String patchId) {
        return new File(getBundlesPatchesDir(patchId), Constants.BUNDLES);
    }

    protected File getModulePatchDirectory(final String patchId) {
        return new File(getModulesPatchesDir(patchId), Constants.MODULES);
    }

    @Override
    public PatchInfo loadPatchInfo() throws IOException {
        final String ref = PatchUtils.readRef(getCumulativeLink());
        final List<String> patches = PatchUtils.readRefs(getCumulativeRefs(ref));
        return new PatchInfo() {
            @Override
            public String getVersion() {
                return AbstractPatchableTarget.this.getVersion();
            }

            @Override
            public String getCumulativeID() {
                return ref;
            }

            @Override
            public List<String> getPatchIDs() {
                return patches;
            }

            @Override
            public File[] getPatchingPath() {
                // TODO remove
                throw new IllegalStateException();
            }

            @Override
            public File[] getModulePath() {
                final List<File> path = new ArrayList<File>();
                for (final String patch : patches) {
                    path.add(getModulePatchDirectory(patch));
                }
                if (!BASE.equals(ref)) {
                    path.add(getModulePatchDirectory(ref));
                }
                path.add(getModulesBase());
                return path.toArray(new File[path.size()]);
            }

            @Override
            public File[] getBundlePath() {
                final List<File> path = new ArrayList<File>();
                for (final String patch : patches) {
                    path.add(getBundlesPatchDirectory(patch));
                }
                if (!BASE.equals(ref)) {
                    path.add(getBundlesPatchDirectory(ref));
                }
                path.add(getBundlesBase());
                return path.toArray(new File[path.size()]);
            }
        };
    }

}
