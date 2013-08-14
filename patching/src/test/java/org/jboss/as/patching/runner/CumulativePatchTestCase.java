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

import static org.jboss.as.patching.HashUtils.hashFile;
import static org.jboss.as.patching.IoUtils.mkdir;
import static org.jboss.as.patching.IoUtils.newFile;
import static org.jboss.as.patching.Constants.BASE;
import static org.jboss.as.patching.runner.PatchingAssert.assertDefinedModule;
import static org.jboss.as.patching.runner.PatchingAssert.assertDirDoesNotExist;
import static org.jboss.as.patching.runner.PatchingAssert.assertDirExists;
import static org.jboss.as.patching.runner.PatchingAssert.assertFileContent;
import static org.jboss.as.patching.runner.PatchingAssert.assertFileExists;
import static org.jboss.as.patching.runner.PatchingAssert.assertPatchHasBeenApplied;
import static org.jboss.as.patching.runner.PatchingAssert.assertPatchHasBeenRolledBack;
import static org.jboss.as.patching.runner.TestUtils.createPatchXMLFile;
import static org.jboss.as.patching.runner.TestUtils.createZippedPatchFile;
import static org.jboss.as.patching.runner.TestUtils.dump;
import static org.jboss.as.patching.runner.TestUtils.randomString;
import static org.jboss.as.patching.runner.TestUtils.touch;
import static org.jboss.as.patching.runner.TestUtils.tree;

import java.io.File;

import org.jboss.as.patching.installation.Identity;
import org.jboss.as.patching.installation.InstalledIdentity;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.jboss.as.patching.tool.PatchingResult;
import org.jboss.as.version.ProductConfig;
import org.junit.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012, Red Hat Inc
 */
public class CumulativePatchTestCase extends AbstractTaskTestCase {

    @Test
    public void testApplyReleasePatch() throws Exception {
        // build a Release patch for the base installation
        // with 1 added module
        String patchID = randomString();
        String layerPatchID = randomString();
        File patchDir = mkdir(tempDir, patchID);
        String moduleName = randomString();
        ContentModification moduleAdded = ContentModificationUtils.addModule(patchDir, layerPatchID, moduleName);

        InstalledIdentity installedIdentity = loadInstalledIdentity();

        Patch patch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription(randomString())
                .upgradeIdentity(installedIdentity.getIdentity().getName(), installedIdentity.getIdentity().getVersion(), productConfig.getProductVersion() + "-Release1")
                .getParent()
                .upgradeElement(layerPatchID, BASE, false)
                    .addContentModification(moduleAdded)
                    .getParent()
                .build();

        createPatchXMLFile(patchDir, patch);
        File zippedPatch = createZippedPatchFile(patchDir, patchID);

        PatchingResult result = executePatch(zippedPatch);
        assertPatchHasBeenApplied(result, patch);

        InstalledIdentity updatedInstalledIdentity = loadInstalledIdentity();
        File modulePatchDirectory = updatedInstalledIdentity.getLayers().get(0).loadTargetInfo().getDirectoryStructure().getModulePatchDirectory(layerPatchID);
        assertDirExists(modulePatchDirectory);
        assertDefinedModule(modulePatchDirectory, moduleName, moduleAdded.getItem().getContentHash());
    }

    @Test
    public void testApplyReleasePatchAndRollback() throws Exception {
        // start from a base installation
        // create an existing file in the AS7 installation
        File binDir = mkdir(env.getInstalledImage().getJbossHome(), "bin");
        String fileName = "standalone.sh";
        File standaloneShellFile = touch(binDir, fileName);
        dump(standaloneShellFile, "original script to run standalone AS7");
        byte[] existingHash = hashFile(standaloneShellFile);

        // build a Release patch for the base installation
        // with 1 added module
        // and 1 updated file
        String patchID = randomString();
        String layerPatchID = randomString();
        File patchDir = mkdir(tempDir, patchID);
        String moduleName = randomString();

        ContentModification moduleAdded = ContentModificationUtils.addModule(patchDir, layerPatchID, moduleName);
        ContentModification fileModified = ContentModificationUtils.modifyMisc(patchDir, patchID, "updated script", standaloneShellFile, "bin", "standalone.sh");

        InstalledIdentity installedIdentity = loadInstalledIdentity();

        Patch patch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription(randomString())
                .upgradeIdentity(installedIdentity.getIdentity().getName(), installedIdentity.getIdentity().getVersion(), productConfig.getProductVersion() + "-Release1")
                    .getParent()
                .upgradeElement(layerPatchID, BASE, false)
                    .addContentModification(moduleAdded)
                    .getParent()
                .addContentModification(fileModified)
                .build();
        createPatchXMLFile(patchDir, patch);
        File zippedPatch = createZippedPatchFile(patchDir, patchID);

        Identity identityBeforePatch = loadInstalledIdentity().getIdentity();

        PatchingResult result = executePatch(zippedPatch);
        assertPatchHasBeenApplied(result, patch);

        assertFileExists(standaloneShellFile);
        assertFileContent(fileModified.getItem().getContentHash(), standaloneShellFile);

        InstalledIdentity updatedInstalledIdentity = loadInstalledIdentity();
        File modulePatchDirectory = updatedInstalledIdentity.getLayers().get(0).loadTargetInfo().getDirectoryStructure().getModulePatchDirectory(layerPatchID);
        assertDirExists(modulePatchDirectory);
        assertDefinedModule(modulePatchDirectory, moduleName, moduleAdded.getItem().getContentHash());

        // rollback the patch based on the updated PatchInfo
        PatchingResult rollbackResult = rollback(patchID);

        tree(env.getInstalledImage().getJbossHome());
        assertPatchHasBeenRolledBack(rollbackResult, identityBeforePatch);
        assertFileExists(standaloneShellFile);
        assertFileContent(existingHash, standaloneShellFile);
    }

    @Test
    public void testApplyReleasePatchThenOneOffPatch() throws Exception {
        // build a Release patch for the base installation
        // with 1 added module
        String releasePatchID = randomString();
        String releaseLayerPatchID = randomString();
        File releasePatchDir = mkdir(tempDir, releasePatchID);
        String moduleName = randomString();

        ContentModification moduleAdded = ContentModificationUtils.addModule(releasePatchDir, releaseLayerPatchID, moduleName);

        InstalledIdentity installedIdentity = loadInstalledIdentity();
        final String resultingVersion = productConfig.getProductVersion() + "-Release1";
        Patch releasePatch = PatchBuilder.create()
                .setPatchId(releasePatchID)
                .setDescription(randomString())
                .upgradeIdentity(installedIdentity.getIdentity().getName(), installedIdentity.getIdentity().getVersion(), resultingVersion)
                .getParent()
                .upgradeElement(releaseLayerPatchID, BASE, false)
                    .addContentModification(moduleAdded)
                    .getParent()
                .build();

        createPatchXMLFile(releasePatchDir, releasePatch);
        File zippedReleasePatch = createZippedPatchFile(releasePatchDir, releasePatchID);

        PatchingResult resultOfReleasePatch = executePatch(zippedReleasePatch);
        assertPatchHasBeenApplied(resultOfReleasePatch, releasePatch);

        // FIXME when is the product version persisted when the release is applied?
        productConfig = new ProductConfig(productConfig.getProductName(), productConfig.getProductVersion() + "-Release1", productConfig.getConsoleSlot());

        InstalledIdentity updatedInstalledIdentity = loadInstalledIdentity();

        File modulePatchDirectory = updatedInstalledIdentity.getLayers().get(0).loadTargetInfo().getDirectoryStructure().getModulePatchDirectory(releaseLayerPatchID);
        assertDirExists(modulePatchDirectory);
        assertDefinedModule(modulePatchDirectory, moduleName, moduleAdded.getItem().getContentHash());

        // apply a one-off patch now
        String oneOffPatchID = randomString();
        String oneOffLayerPatchID = randomString();
        File oneOffPatchDir = mkdir(tempDir, oneOffPatchID);

        ContentModification moduleModified = ContentModificationUtils.modifyModule(oneOffPatchDir, oneOffLayerPatchID, newFile(modulePatchDirectory, moduleName), "new resource in the module");

        Patch oneOffPatch = PatchBuilder.create()
                .setPatchId(oneOffPatchID)
                .setDescription(randomString())
                // one-off patch can be applied to Release
                .oneOffPatchIdentity(productConfig.getProductName(), resultingVersion)
                .getParent()
                .oneOffPatchElement(oneOffLayerPatchID, BASE, false)
                    .addContentModification(moduleModified)
                    .getParent()
                .build();

        createPatchXMLFile(oneOffPatchDir, oneOffPatch);
        File zippedOneOffPatch = createZippedPatchFile(oneOffPatchDir, oneOffPatchID);

        PatchingResult resultOfOneOffPatch = executePatch(zippedOneOffPatch);
        assertPatchHasBeenApplied(resultOfOneOffPatch, oneOffPatch);

        InstalledIdentity installedIdentityAfterOneOffPatch = loadInstalledIdentity();
        modulePatchDirectory = installedIdentityAfterOneOffPatch.getLayers().get(0).loadTargetInfo().getDirectoryStructure().getModulePatchDirectory(oneOffLayerPatchID);
        assertDirExists(modulePatchDirectory);
        assertDefinedModule(modulePatchDirectory, moduleName, moduleModified.getItem().getContentHash());
    }

    @Test
    public void testApplyReleasePatchThenOneOffPatchThenRollbackReleasePatch() throws Exception {
        // build a Release patch for the base installation
        // with 1 added module
        String releasePatchID = randomString();
        String releaseLayerPatchID = randomString();
        File releasePatchDir = mkdir(tempDir, releasePatchID);
        String moduleName = randomString();

        ContentModification moduleAdded = ContentModificationUtils.addModule(releasePatchDir, releaseLayerPatchID, moduleName);

        InstalledIdentity identityBeforePatch = loadInstalledIdentity();
        final String resultingVersion = identityBeforePatch.getIdentity().getVersion() + "-Release1";
        Patch releasePatch = PatchBuilder.create()
                .setPatchId(releasePatchID)
                .setDescription(randomString())
                .upgradeIdentity(identityBeforePatch.getIdentity().getName(), identityBeforePatch.getIdentity().getVersion(), resultingVersion)
                .getParent()
                .upgradeElement(releaseLayerPatchID, BASE, false)
                    .addContentModification(moduleAdded)
                    .getParent()
                .build();
        createPatchXMLFile(releasePatchDir, releasePatch);
        File zippedReleasePatch = createZippedPatchFile(releasePatchDir, releasePatchID);

        PatchingResult resultOfReleasePatch = executePatch(zippedReleasePatch);
        assertPatchHasBeenApplied(resultOfReleasePatch, releasePatch);

        // FIXME when is the product version persisted when the release is applied?
        productConfig = new ProductConfig(productConfig.getProductName(), productConfig.getProductVersion() + "-Release1", productConfig.getConsoleSlot());

        InstalledIdentity updatedInstalledIdentity = loadInstalledIdentity();
        File modulePatchDirectory = updatedInstalledIdentity.getLayers().get(0).loadTargetInfo().getDirectoryStructure().getModulePatchDirectory(releaseLayerPatchID);
        assertDirExists(modulePatchDirectory);
        assertDefinedModule(modulePatchDirectory, moduleName, moduleAdded.getItem().getContentHash());

        // apply a one-off patch now
        String oneOffPatchID = randomString();
        String oneOffLayerPatchID = randomString();
        File oneOffPatchDir = mkdir(tempDir, oneOffPatchID);

        ContentModification moduleModified = ContentModificationUtils.modifyModule(oneOffPatchDir, oneOffLayerPatchID, newFile(modulePatchDirectory, moduleName), "new resource in the module");

        Patch oneOffPatch = PatchBuilder.create()
                .setPatchId(oneOffPatchID)
                .setDescription(randomString())
                // one-off patch can be applied to Release
                .oneOffPatchIdentity(productConfig.getProductName(), resultingVersion)
                .getParent()
                .oneOffPatchElement(oneOffLayerPatchID, BASE, false)
                    .addContentModification(moduleModified)
                    .getParent()
                .build();

        createPatchXMLFile(oneOffPatchDir, oneOffPatch);
        File zippedOneOffPatch = createZippedPatchFile(oneOffPatchDir, oneOffPatchID);

        PatchingResult resultOfOneOffPatch = executePatch(zippedOneOffPatch);
        assertPatchHasBeenApplied(resultOfOneOffPatch, oneOffPatch);

        InstalledIdentity installedIdentityAfterOneOffPatch = loadInstalledIdentity();
        modulePatchDirectory = installedIdentityAfterOneOffPatch.getLayers().get(0).loadTargetInfo().getDirectoryStructure().getModulePatchDirectory(oneOffLayerPatchID);
        assertDirExists(modulePatchDirectory);
        assertDefinedModule(modulePatchDirectory, moduleName, moduleModified.getItem().getContentHash());

        // rollback the release patch, this should also rollback the one-off patch
        PatchingResult resultOfReleasePatchRollback = rollback(releasePatchID);

        tree(env.getInstalledImage().getJbossHome());
        assertPatchHasBeenRolledBack(resultOfReleasePatchRollback, identityBeforePatch.getIdentity());

        updatedInstalledIdentity = loadInstalledIdentity();
        File layerModuleRoot = updatedInstalledIdentity.getLayers().get(0).loadTargetInfo().getDirectoryStructure().getModuleRoot();
        assertDirDoesNotExist(newFile(layerModuleRoot, moduleName));
    }

    @Test
    public void testInvalidateOneOffPatches() throws Exception {
        // build a one-off patch for the base installation
        // with 1 added module
        String oneOffPatchID = "oneOffPatchID";//randomString();
        String oneOffLayerPatchID = "oneOffLayerPatchID";//randomString();
        File oneOffPatchDir = mkdir(tempDir, oneOffPatchID);
        String moduleName = "mymodule";//randomString();

        ContentModification moduleAdded = ContentModificationUtils.addModule(oneOffPatchDir, oneOffLayerPatchID, moduleName);

        InstalledIdentity identityBeforePatch = loadInstalledIdentity();

        Patch oneOffPatch = PatchBuilder.create()
                .setPatchId(oneOffPatchID)
                .setDescription(randomString())
                        // one-off patch can be applied to Release
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                .getParent()
                .oneOffPatchElement(oneOffLayerPatchID, BASE, false)
                    .addContentModification(moduleAdded)
                    .getParent()
                .build();

        createPatchXMLFile(oneOffPatchDir, oneOffPatch);
        File zippedOneOffPatch = createZippedPatchFile(oneOffPatchDir, oneOffPatchID);

        PatchingResult resultOfOneOffPatch = executePatch(zippedOneOffPatch);
        assertPatchHasBeenApplied(resultOfOneOffPatch, oneOffPatch);

        InstalledIdentity installedIdentityAfterOneOffPatch = loadInstalledIdentity();
        File modulePatchDirectory = installedIdentityAfterOneOffPatch.getLayers().get(0).loadTargetInfo().getDirectoryStructure().getModulePatchDirectory(oneOffLayerPatchID);
        assertDirExists(modulePatchDirectory);
        assertDefinedModule(modulePatchDirectory, moduleName, moduleAdded.getItem().getContentHash());

        // build a Release patch for the base installation
        String releasePatchID = "releasePatchID";// randomString() + "-Release";
        String releaseLayerPatchID = "releaseLayerPatchID";//randomString();
        File releasePatchDir = mkdir(tempDir, releasePatchID);

        ContentModification moduleAddedInReleasePatch = ContentModificationUtils.addModule(releasePatchDir, releaseLayerPatchID, moduleName, "different content in the module");

        final String resultingVersion = identityBeforePatch.getIdentity().getVersion() + "-Release1";
        Patch releasePatch = PatchBuilder.create()
                .setPatchId(releasePatchID)
                .setDescription(randomString())
                .upgradeIdentity(identityBeforePatch.getIdentity().getName(), identityBeforePatch.getIdentity().getVersion(), resultingVersion)
                .getParent()
                .upgradeElement(releaseLayerPatchID, BASE, false)
                    .addContentModification(moduleAddedInReleasePatch)
                    .getParent()
                .build();
        createPatchXMLFile(releasePatchDir, releasePatch);
        File zippedReleasePatch = createZippedPatchFile(releasePatchDir, releasePatchID);

        PatchingResult resultOfReleasePatch = executePatch(zippedReleasePatch);
        assertPatchHasBeenApplied(resultOfReleasePatch, releasePatch);

        tree(env.getInstalledImage().getJbossHome());
        modulePatchDirectory = installedIdentityAfterOneOffPatch.getLayers().get(0).loadTargetInfo().getDirectoryStructure().getModulePatchDirectory(releaseLayerPatchID);
        assertDirExists(modulePatchDirectory);
        assertDefinedModule(modulePatchDirectory, moduleName, moduleAddedInReleasePatch.getItem().getContentHash());
    }

}
