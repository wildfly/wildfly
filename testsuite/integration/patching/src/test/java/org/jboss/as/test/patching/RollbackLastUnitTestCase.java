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

package org.jboss.as.test.patching;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.patching.HashUtils;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.jboss.as.test.integration.management.util.CLITestUtil;
import org.jboss.as.test.patching.util.module.Module;
import org.jboss.as.version.ProductConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static org.jboss.as.patching.Constants.BASE;
import static org.jboss.as.patching.Constants.BUNDLES;
import static org.jboss.as.patching.Constants.LAYERS;
import static org.jboss.as.patching.Constants.MODULES;
import static org.jboss.as.patching.Constants.SYSTEM;
import static org.jboss.as.patching.IoUtils.mkdir;
import static org.jboss.as.patching.IoUtils.newFile;
import static org.jboss.as.test.patching.PatchingTestUtil.CONTAINER;
import static org.jboss.as.test.patching.PatchingTestUtil.MODULES_PATH;
import static org.jboss.as.test.patching.PatchingTestUtil.assertPatchElements;
import static org.jboss.as.test.patching.PatchingTestUtil.createBundle0;
import static org.jboss.as.test.patching.PatchingTestUtil.createPatchXMLFile;
import static org.jboss.as.test.patching.PatchingTestUtil.createZippedPatchFile;
import static org.jboss.as.test.patching.PatchingTestUtil.dump;
import static org.jboss.as.test.patching.PatchingTestUtil.randomString;
import static org.jboss.as.test.patching.PatchingTestUtil.touch;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;


/**
 * @author Alexey Loubyansky
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RollbackLastUnitTestCase extends AbstractPatchingTestCase {

    protected ProductConfig productConfig;

    @Before
    public void setup() throws Exception {
        productConfig = new ProductConfig(PatchingTestUtil.PRODUCT, PatchingTestUtil.AS_VERSION, "main");
    }

    @Test
    public void testMain() throws Exception {

        // build a one-off patch for the base installation
        // with 1 updated file
        String patchID = randomString();
        String patchElementId = randomString();
        File patchDir = mkdir(tempDir, patchID);

        // create a module to be updated w/o a conflict
        File baseModuleDir = newFile(new File(PatchingTestUtil.AS_DISTRIBUTION), MODULES, SYSTEM, LAYERS, BASE);
        String moduleName = "module-test";
        Module module = new Module.Builder(moduleName)
                .miscFile(new ResourceItem("resource-test", "new resource in the module".getBytes()))
                .build();
        File moduleDir = module.writeToDisk(new File(MODULES_PATH));

        // create the patch with the updated module
        ContentModification moduleModified = ContentModificationUtils.modifyModule(patchDir, patchElementId, HashUtils.hashFile(moduleDir), module);

        // create a file for the conflict
        String fileName = "file-test.txt";
        File miscFile = touch(new File(PatchingTestUtil.AS_DISTRIBUTION, "bin"), fileName);
        dump(miscFile, "original script to run standalone AS7");
        byte[] originalFileHash = HashUtils.hashFile(miscFile);
        // patch the file
        ContentModification fileModified = ContentModificationUtils.modifyMisc(patchDir, patchID, "updated script", miscFile, "bin", fileName);

        // create a bundle to be updated w/o a conflict
        File baseBundleDir = newFile(new File(PatchingTestUtil.AS_DISTRIBUTION), BUNDLES, SYSTEM, LAYERS, BASE);
        String bundleName = "bundle-test";
        File bundleDir = createBundle0(baseBundleDir, bundleName, "bundle content");
        // patch the bundle
        ContentModification bundleModified = ContentModificationUtils.modifyBundle(patchDir, patchElementId, bundleDir, "updated bundle content");

        Patch patch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription(randomString())
                .upgradeIdentity(productConfig.getProductName(), productConfig.getProductVersion(), productConfig.getProductVersion() + "CP1")
                .getParent()
                .addContentModification(fileModified)
                .upgradeElement(patchElementId, "base", false)
                .addContentModification(moduleModified)
                .addContentModification(bundleModified)
                .getParent()
                .build();

        // create the patch
        createPatchXMLFile(patchDir, patch);

        File zippedPatch = createZippedPatchFile(patchDir, patch.getPatchId());

        // no patches applied
        assertPatchElements(baseModuleDir, null);
        assertPatchElements(baseBundleDir, null);

        controller.start(CONTAINER);

        // apply the patch using the cli
        CommandContext ctx = CLITestUtil.getCommandContext();
        try {
            ctx.connectController();
            ctx.handle("patch apply " + zippedPatch.getAbsolutePath() + " --distribution=" + PatchingTestUtil.AS_DISTRIBUTION);
        } catch (Exception e) {
            ctx.terminateSession();
            throw e;
        } finally {
            controller.stop(CONTAINER);
        }

        // first patch applied
        assertPatchElements(baseModuleDir, new String[]{patchElementId}, false);
        assertPatchElements(baseBundleDir, new String[]{patchElementId}, false);

        byte[] patch1FileHash = HashUtils.hashFile(miscFile);
        assertNotEqual(originalFileHash, patch1FileHash);

        // next patch
        final String patchID2 = randomString();
        final String patchElementId2 = randomString();

        final File patchedModule = newFile(baseModuleDir, ".overlays", patchElementId, moduleName);
        final File patchedBundle = newFile(baseBundleDir, ".overlays", patchElementId, bundleName);

        Module modifiedModule = new Module.Builder(moduleName)
                .miscFile(new ResourceItem("resource-test", "another module update".getBytes()))
                .build();

        ContentModification fileModified2 = ContentModificationUtils.modifyMisc(patchDir, patchID2, "another file update", miscFile, "bin", fileName);
        ContentModification moduleModified2 = ContentModificationUtils.modifyModule(patchDir, patchElementId2, HashUtils.hashFile(patchedModule), modifiedModule);
        ContentModification bundleModified2 = ContentModificationUtils.modifyBundle(patchDir, patchElementId2, patchedBundle, "another bundle update");

        Patch patch2 = PatchBuilder.create()
                .setPatchId(patchID2)
                .setDescription(randomString())
                .upgradeIdentity(productConfig.getProductName(), productConfig.getProductVersion(), productConfig.getProductName() + "CP2")
                .getParent()
                .addContentModification(fileModified2)
                .upgradeElement(patchElementId2, "base", false)
                .addContentModification(moduleModified2)
                .addContentModification(bundleModified2)
                .getParent()
                .build();

        createPatchXMLFile(patchDir, patch2);
        File zippedPatch2 = createZippedPatchFile(patchDir, patch2.getPatchId());

        controller.start(CONTAINER);
        try {
            ctx.handle("patch apply " + zippedPatch2.getAbsolutePath() + " --distribution=" + PatchingTestUtil.AS_DISTRIBUTION);
        } catch (Exception e) {
            e.printStackTrace();
            ctx.terminateSession();
            throw e;
        } finally {
            controller.stop(CONTAINER);
        }

        // both patches applied
        assertPatchElements(baseModuleDir, new String[]{patchElementId, patchElementId2}, false);
        assertPatchElements(baseBundleDir, new String[]{patchElementId, patchElementId2}, false);

        byte[] patch2FileHash = HashUtils.hashFile(miscFile);
        assertNotEqual(patch1FileHash, patch2FileHash);
        assertNotEqual(originalFileHash, patch2FileHash);

        controller.start(CONTAINER);
        try {
            ctx.handle("patch rollback --reset-configuration=false --distribution=" + PatchingTestUtil.AS_DISTRIBUTION);
        } catch (Exception e) {
            ctx.terminateSession();
            throw e;
        } finally {
            controller.stop(CONTAINER);
        }

        byte[] curFileHash = HashUtils.hashFile(miscFile);
        assertNotEqual(curFileHash, patch2FileHash);
        assertArrayEquals(curFileHash, patch1FileHash);
        assertNotEqual(curFileHash, originalFileHash);

        controller.start(CONTAINER);
        try {
            // only the first patch is present
            assertPatchElements(baseModuleDir, new String[]{patchElementId}, false);
            assertPatchElements(baseBundleDir, new String[]{patchElementId}, false);

            ctx.handle("patch rollback --reset-configuration=false --distribution=" + PatchingTestUtil.AS_DISTRIBUTION);
        } catch (Exception e) {
            ctx.terminateSession();
            throw e;
        } finally {
            ctx.terminateSession();
            controller.stop(CONTAINER);
        }

        try {
            controller.start(CONTAINER);

            // no patches present
            assertPatchElements(baseModuleDir, null, false);
            assertPatchElements(baseBundleDir, null, false);
        } finally {
            controller.stop(CONTAINER);
        }

        curFileHash = HashUtils.hashFile(miscFile);
        assertNotEqual(curFileHash, patch2FileHash);
        assertNotEqual(curFileHash, patch1FileHash);
        assertArrayEquals(curFileHash, originalFileHash);
    }


    private static void assertNotEqual(byte[] a1, byte[] a2) {
        if (a1.length != a2.length) {
            return;
        }
        for (int i = 0; i < a1.length; ++i) {
            if (a1[i] != a2[i]) {
                return;
            }
        }
        fail("arrays are equal");
    }
}
