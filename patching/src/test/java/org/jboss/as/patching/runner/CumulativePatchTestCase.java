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
import static org.jboss.as.patching.IoUtils.NO_CONTENT;
import static org.jboss.as.patching.IoUtils.newFile;
import static org.jboss.as.patching.PatchInfo.BASE;
import static org.jboss.as.patching.metadata.ModificationType.ADD;
import static org.jboss.as.patching.metadata.ModificationType.MODIFY;
import static org.jboss.as.patching.runner.PatchingAssert.assertDefinedModule;
import static org.jboss.as.patching.runner.PatchingAssert.assertDirDoesNotExist;
import static org.jboss.as.patching.runner.PatchingAssert.assertDirExists;
import static org.jboss.as.patching.runner.PatchingAssert.assertFileContent;
import static org.jboss.as.patching.runner.PatchingAssert.assertFileExists;
import static org.jboss.as.patching.runner.PatchingAssert.assertPatchHasBeenApplied;
import static org.jboss.as.patching.runner.PatchingAssert.assertPatchHasBeenRolledBack;
import static org.jboss.as.patching.runner.TestUtils.createModule;
import static org.jboss.as.patching.runner.TestUtils.createPatchXMLFile;
import static org.jboss.as.patching.runner.TestUtils.createZippedPatchFile;
import static org.jboss.as.patching.runner.TestUtils.dump;
import static org.jboss.as.patching.runner.TestUtils.getModulePath;
import static org.jboss.as.patching.IoUtils.mkdir;
import static org.jboss.as.patching.runner.TestUtils.randomString;
import static org.jboss.as.patching.runner.TestUtils.touch;
import static org.jboss.as.patching.runner.TestUtils.tree;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Collections;

import org.jboss.as.patching.LocalPatchInfo;
import org.jboss.as.patching.PatchInfo;
import org.jboss.as.patching.installation.Identity;
import org.jboss.as.patching.installation.InstallationManager;
import org.jboss.as.patching.installation.InstalledIdentity;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.MiscContentItem;
import org.jboss.as.patching.metadata.ModuleItem;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.jboss.as.patching.metadata.impl.IdentityImpl;
import org.jboss.as.patching.metadata.impl.PatchElementImpl;
import org.jboss.as.patching.metadata.impl.PatchElementProviderImpl;
import org.jboss.as.version.ProductConfig;
import org.junit.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012, Red Hat Inc
 */
public class CumulativePatchTestCase extends AbstractTaskTestCase {

    @Test
    public void testApplyCumulativePatch() throws Exception {
        // build a CP patch for the base installation
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
                .setIdentity(new IdentityImpl(installedIdentity.getIdentity().getName(), installedIdentity.getIdentity().getVersion()))
                .setUpgrade(productConfig.getProductVersion() + "-CP1")
                .addElement(new PatchElementImpl(layerPatchID)
                        .setProvider(new PatchElementProviderImpl(BASE, "1.0.1", false))
                        .setNoUpgrade()
                        .addContentModification(moduleAdded))
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
    public void testApplyCumulativePatchAndRollback() throws Exception {
        // start from a base installation
        // create an existing file in the AS7 installation
        File binDir = mkdir(env.getInstalledImage().getJbossHome(), "bin");
        String fileName = "standalone.sh";
        File standaloneShellFile = touch(binDir, fileName);
        dump(standaloneShellFile, "original script to run standalone AS7");
        byte[] existingHash = hashFile(standaloneShellFile);

        // build a CP patch for the base installation
        // with 1 added module
        // and 1 updated file
        String patchID = randomString();
        String layerPatchID = randomString();
        File patchDir = mkdir(tempDir, patchID);
        String moduleName = randomString();

        ContentModification moduleAdded = ContentModificationUtils.addModule(patchDir, layerPatchID, moduleName);
        ContentModification fileModified = ContentModificationUtils.modifyMisc(patchDir, "updated script", standaloneShellFile, "bin", "standalone.sh");

        InstalledIdentity installedIdentity = loadInstalledIdentity();

        Patch patch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription(randomString())
                .setIdentity(new IdentityImpl(installedIdentity.getIdentity().getName(), installedIdentity.getIdentity().getVersion()))
                .setUpgrade(productConfig.getProductVersion() + "-CP1")
                .addElement(new PatchElementImpl(layerPatchID)
                        .setProvider(new PatchElementProviderImpl(BASE, "1.0.1", false))
                        .setNoUpgrade()
                        .addContentModification(moduleAdded))
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
    public void testApplyCumulativePatchThenOneOffPatch() throws Exception {
        // build a CP patch for the base installation
        // with 1 added module
        String cumulativePatchID = randomString();
        String cumulativeLayerPatchID = randomString();
        File cumulativePatchDir = mkdir(tempDir, cumulativePatchID);
        String moduleName = randomString();

        ContentModification moduleAdded = ContentModificationUtils.addModule(cumulativePatchDir, cumulativeLayerPatchID, moduleName);

        InstalledIdentity installedIdentity = loadInstalledIdentity();

        Patch cumulativePatch = PatchBuilder.create()
                .setPatchId(cumulativePatchID)
                .setDescription(randomString())
                .setIdentity(new IdentityImpl(installedIdentity.getIdentity().getName(), installedIdentity.getIdentity().getVersion()))
                .setUpgrade(installedIdentity.getIdentity().getVersion() + "-CP1")
                .addElement(new PatchElementImpl(cumulativeLayerPatchID)
                        .setProvider(new PatchElementProviderImpl(BASE, "1.0.1", false))
                        .setNoUpgrade()
                        .addContentModification(moduleAdded))
                .build();

        createPatchXMLFile(cumulativePatchDir, cumulativePatch);
        File zippedCumulativePatch = createZippedPatchFile(cumulativePatchDir, cumulativePatchID);

        PatchingResult resultOfCumulativePatch = executePatch(zippedCumulativePatch);
        assertPatchHasBeenApplied(resultOfCumulativePatch, cumulativePatch);

        // FIXME when is the product version persisted when the cumulative is applied?
        productConfig = new ProductConfig(productConfig.getProductName(), productConfig.getProductVersion() + "-CP1", productConfig.getConsoleSlot());

        InstalledIdentity updatedInstalledIdentity = loadInstalledIdentity();

        File modulePatchDirectory = updatedInstalledIdentity.getLayers().get(0).loadTargetInfo().getDirectoryStructure().getModulePatchDirectory(cumulativeLayerPatchID);
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
                // one-off patch can be applied to CP
                .setOneOffType(cumulativePatch.getResultingVersion())
                .addElement(new PatchElementImpl(oneOffLayerPatchID)
                        .setProvider(new PatchElementProviderImpl(BASE, "1.0.1", false))
                        .addContentModification(moduleModified))
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
    public void testApplyCumulativePatchThenOneOffPatchThenRollbackCumulativePatch() throws Exception {
        // build a CP patch for the base installation
        // with 1 added module
        String cumulativePatchID = randomString();
        String cumulativeLayerPatchID = randomString();
        File cumulativePatchDir = mkdir(tempDir, cumulativePatchID);
        String moduleName = randomString();

        ContentModification moduleAdded = ContentModificationUtils.addModule(cumulativePatchDir, cumulativeLayerPatchID, moduleName);

        InstalledIdentity identityBeforePatch = loadInstalledIdentity();

        Patch cumulativePatch = PatchBuilder.create()
                .setPatchId(cumulativePatchID)
                .setDescription(randomString())
                .setIdentity(new IdentityImpl(identityBeforePatch.getIdentity().getName(), identityBeforePatch.getIdentity().getVersion()))
                .setUpgrade(identityBeforePatch.getIdentity().getVersion() + "-CP1")
                .addElement(new PatchElementImpl(cumulativeLayerPatchID)
                        .setProvider(new PatchElementProviderImpl(BASE, "1.0.1", false))
                        .setNoUpgrade()
                        .addContentModification(moduleAdded))
                .build();
        createPatchXMLFile(cumulativePatchDir, cumulativePatch);
        File zippedCumulativePatch = createZippedPatchFile(cumulativePatchDir, cumulativePatchID);

        PatchingResult resultOfCumulativePatch = executePatch(zippedCumulativePatch);
        assertPatchHasBeenApplied(resultOfCumulativePatch, cumulativePatch);

        // FIXME when is the product version persisted when the cumulative is applied?
        productConfig = new ProductConfig(productConfig.getProductName(), productConfig.getProductVersion() + "-CP1", productConfig.getConsoleSlot());

        InstalledIdentity updatedInstalledIdentity = loadInstalledIdentity();
        File modulePatchDirectory = updatedInstalledIdentity.getLayers().get(0).loadTargetInfo().getDirectoryStructure().getModulePatchDirectory(cumulativeLayerPatchID);
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
                        // one-off patch can be applied to CP
                .setOneOffType(cumulativePatch.getResultingVersion())
                .addElement(new PatchElementImpl(oneOffLayerPatchID)
                        .setProvider(new PatchElementProviderImpl(BASE, "1.0.1", false))
                        .addContentModification(moduleModified))
                .build();

        createPatchXMLFile(oneOffPatchDir, oneOffPatch);
        File zippedOneOffPatch = createZippedPatchFile(oneOffPatchDir, oneOffPatchID);

        PatchingResult resultOfOneOffPatch = executePatch(zippedOneOffPatch);
        assertPatchHasBeenApplied(resultOfOneOffPatch, oneOffPatch);

        InstalledIdentity installedIdentityAfterOneOffPatch = loadInstalledIdentity();
        modulePatchDirectory = installedIdentityAfterOneOffPatch.getLayers().get(0).loadTargetInfo().getDirectoryStructure().getModulePatchDirectory(oneOffLayerPatchID);
        assertDirExists(modulePatchDirectory);
        assertDefinedModule(modulePatchDirectory, moduleName, moduleModified.getItem().getContentHash());

        // rollback the cumulative patch, this should also rollback the one-off patch
        PatchingResult resultOfCumulativePatchRollback = rollback(cumulativePatchID);

        tree(env.getInstalledImage().getJbossHome());
        assertPatchHasBeenRolledBack(resultOfCumulativePatchRollback, identityBeforePatch.getIdentity());

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
                        // one-off patch can be applied to CP
                .setOneOffType(productConfig.getProductVersion())
                .addElement(new PatchElementImpl(oneOffLayerPatchID)
                        .setProvider(new PatchElementProviderImpl(BASE, "1.0.1", false))
                        .addContentModification(moduleAdded))
                .build();

        createPatchXMLFile(oneOffPatchDir, oneOffPatch);
        File zippedOneOffPatch = createZippedPatchFile(oneOffPatchDir, oneOffPatchID);

        PatchingResult resultOfOneOffPatch = executePatch(zippedOneOffPatch);
        assertPatchHasBeenApplied(resultOfOneOffPatch, oneOffPatch);

        InstalledIdentity installedIdentityAfterOneOffPatch = loadInstalledIdentity();
        File modulePatchDirectory = installedIdentityAfterOneOffPatch.getLayers().get(0).loadTargetInfo().getDirectoryStructure().getModulePatchDirectory(oneOffLayerPatchID);
        assertDirExists(modulePatchDirectory);
        assertDefinedModule(modulePatchDirectory, moduleName, moduleAdded.getItem().getContentHash());

        // build a CP patch for the base installation
        String cumulativePatchID = "cumulativePatchID";// randomString() + "-CP";
        String cumulativeLayerPatchID = "cumulativeLayerPatchID";//randomString();
        File cumulativePatchDir = mkdir(tempDir, cumulativePatchID);

        ContentModification moduleAddedInCumulativePatch = ContentModificationUtils.addModule(cumulativePatchDir, cumulativeLayerPatchID, moduleName, "different content in the module");

        Patch cumulativePatch = PatchBuilder.create()
                .setPatchId(cumulativePatchID)
                .setDescription(randomString())
                .setIdentity(new IdentityImpl(identityBeforePatch.getIdentity().getName(), identityBeforePatch.getIdentity().getVersion()))
                .setUpgrade(identityBeforePatch.getIdentity().getVersion() + "-CP1")
                .addElement(new PatchElementImpl(cumulativeLayerPatchID)
                        .setProvider(new PatchElementProviderImpl(BASE, "1.0.1", false))
                        .setNoUpgrade()
                        .addContentModification(moduleAddedInCumulativePatch))
                .build();
        createPatchXMLFile(cumulativePatchDir, cumulativePatch);
        File zippedCumulativePatch = createZippedPatchFile(cumulativePatchDir, cumulativePatchID);

        PatchingResult resultOfCumulativePatch = executePatch(zippedCumulativePatch);
        assertPatchHasBeenApplied(resultOfCumulativePatch, cumulativePatch);

        tree(env.getInstalledImage().getJbossHome());
        modulePatchDirectory = installedIdentityAfterOneOffPatch.getLayers().get(0).loadTargetInfo().getDirectoryStructure().getModulePatchDirectory(cumulativeLayerPatchID);
        assertDirExists(modulePatchDirectory);
        assertDefinedModule(modulePatchDirectory, moduleName, moduleAddedInCumulativePatch.getItem().getContentHash());
    }

}
