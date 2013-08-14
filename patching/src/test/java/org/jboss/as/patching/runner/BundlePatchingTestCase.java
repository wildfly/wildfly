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

package org.jboss.as.patching.runner;

import static org.jboss.as.patching.Constants.LAYERS;
import static org.jboss.as.patching.Constants.SYSTEM;
import static org.jboss.as.patching.HashUtils.hashFile;
import static org.jboss.as.patching.IoUtils.mkdir;
import static org.jboss.as.patching.IoUtils.newFile;
import static org.jboss.as.patching.Constants.BASE;
import static org.jboss.as.patching.runner.PatchingAssert.assertDefinedAbsentBundle;
import static org.jboss.as.patching.runner.PatchingAssert.assertDefinedBundle;
import static org.jboss.as.patching.runner.PatchingAssert.assertDirExists;
import static org.jboss.as.patching.runner.PatchingAssert.assertPatchHasBeenApplied;
import static org.jboss.as.patching.runner.TestUtils.createBundle0;
import static org.jboss.as.patching.runner.TestUtils.createPatchXMLFile;
import static org.jboss.as.patching.runner.TestUtils.createZippedPatchFile;
import static org.jboss.as.patching.runner.TestUtils.randomString;

import java.io.File;

import org.jboss.as.patching.installation.InstalledIdentity;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.jboss.as.patching.tool.PatchingResult;
import org.junit.Test;

/**
 * @author Emanuel Muckenhuber
 */
public class BundlePatchingTestCase extends AbstractTaskTestCase {

    @Test
    public void testAddBundle() throws Exception {
        // build a one-off patch for the base layer
        // with 1 added bundle
        String patchID = "patchID";//randomString();
        File patchDir = mkdir(tempDir, patchID);
        String baseLayerPatchID = "baseLayerPatchID";//randomString();
        String bundleName = "bundleName";//randomString();
        ContentModification bundleAdded = ContentModificationUtils.addBundle(patchDir, baseLayerPatchID, bundleName);

        Patch patch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription(randomString())
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                .getParent()
                .oneOffPatchElement(baseLayerPatchID, BASE, false)
                   .addContentModification(bundleAdded)
                   .getParent()
                .build();
        createPatchXMLFile(patchDir, patch);
        File zippedPatch = createZippedPatchFile(patchDir, patchID);

        PatchingResult result = executePatch(zippedPatch);

        assertPatchHasBeenApplied(result, patch);

        InstalledIdentity updatedInstalledIdentity = loadInstalledIdentity();
        File bundlesPatchingDirectory = updatedInstalledIdentity.getLayers().get(0).loadTargetInfo().getDirectoryStructure().getBundlesPatchDirectory(baseLayerPatchID);
        assertDirExists(bundlesPatchingDirectory);
        assertDefinedBundle(bundlesPatchingDirectory, bundleName, bundleAdded.getItem().getContentHash());
    }

    @Test
    public void testUpdateBundle() throws Exception {
        String bundleName = randomString();

        // create a bundle in the AS7 installation
        File baseBundleDir = newFile(env.getInstalledImage().getBundlesDir(), SYSTEM, LAYERS, BASE);
        File bundleDir = createBundle0(baseBundleDir, bundleName, "bundle content");
        byte[] existingHash = hashFile(bundleDir);

        // build a one-off patch for the base installation
        // with 1 modified module
        String patchID = randomString();
        String baseLayerPatchID = randomString();
        File patchDir = mkdir(tempDir, patchID);
        ContentModification bundleUpdated = ContentModificationUtils.modifyBundle(patchDir, baseLayerPatchID, bundleDir, "updated bundle content");

        Patch patch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription(randomString())
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                .getParent()
                .oneOffPatchElement(baseLayerPatchID, BASE, false)
                   .addContentModification(bundleUpdated)
                   .getParent()
                .build();
        createPatchXMLFile(patchDir, patch);
        File zippedPatch = createZippedPatchFile(patchDir, patchID);

        PatchingResult result = executePatch(zippedPatch);
        assertPatchHasBeenApplied(result, patch);

        InstalledIdentity updatedInstalledIdentity = loadInstalledIdentity();
        File bundlesPatchingDirectory = updatedInstalledIdentity.getLayers().get(0).loadTargetInfo().getDirectoryStructure().getBundlesPatchDirectory(baseLayerPatchID);
        assertDirExists(bundlesPatchingDirectory);
        assertDefinedBundle(bundlesPatchingDirectory, bundleName, bundleUpdated.getItem().getContentHash());
    }

    @Test
    public void testRemoveBundle() throws Exception {

        // create a bundle in the AS7 installation
        String bundleName = randomString();
        File baseBundleDir = newFile(env.getInstalledImage().getBundlesDir(), SYSTEM, LAYERS, BASE);
        File bundleDir = createBundle0(baseBundleDir, bundleName, "bundle content");

        // build a one-off patch for the base installation
        // with 1 bundle removed
        ContentModification bundleRemoved = ContentModificationUtils.removeBundle(bundleDir);
        String baseLayerPatchID = randomString();

        Patch patch = PatchBuilder.create()
                .setPatchId(randomString())
                .setDescription(randomString())
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                .getParent()
                .oneOffPatchElement(baseLayerPatchID, BASE, false)
                    .addContentModification(bundleRemoved)
                    .getParent()
                .build();

        // create the patch
        File patchDir = mkdir(tempDir, patch.getPatchId());
        createPatchXMLFile(patchDir, patch);
        File zippedPatch = createZippedPatchFile(patchDir, patch.getPatchId());

        PatchingResult result = executePatch(zippedPatch);

        assertPatchHasBeenApplied(result, patch);

        InstalledIdentity updatedInstalledIdentity = loadInstalledIdentity();
        File bundlesPatchDirectory = updatedInstalledIdentity.getLayers().get(0).loadTargetInfo().getDirectoryStructure().getBundlesPatchDirectory(baseLayerPatchID);
        assertDirExists(bundlesPatchDirectory);
        assertDefinedAbsentBundle(bundlesPatchDirectory, bundleName);
    }


}
