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

import static org.jboss.as.patching.Constants.BASE;
import static org.jboss.as.patching.HashUtils.hashFile;
import static org.jboss.as.patching.IoUtils.mkdir;
import static org.jboss.as.patching.runner.PatchingAssert.assertDefinedModule;
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

import java.io.File;

import org.jboss.as.patching.installation.Identity;
import org.jboss.as.patching.installation.InstalledIdentity;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.jboss.as.patching.tool.PatchingResult;
import org.junit.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012, Red Hat Inc
 */
public class OneOffPatchTestCase extends AbstractTaskTestCase {

    @Test
    public void testApplyOneOffPatch() throws Exception {
        // build a one-off patch for the base installation
        // with 1 added module
        String oneOffPatchID = randomString();
        String oneOffLayerPatchID = randomString();
        File oneOffPatchDir = mkdir(tempDir, oneOffPatchID);

        String moduleName = randomString();
        ContentModification moduleAdded = ContentModificationUtils.addModule(oneOffPatchDir, oneOffLayerPatchID, moduleName);

        Patch oneOffPatch = PatchBuilder.create()
                .setPatchId(oneOffPatchID)
                .setDescription(randomString())
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                .getParent()
                .oneOffPatchElement(oneOffLayerPatchID, BASE, false)
                    .addContentModification(moduleAdded)
                    .getParent()
                .build();

        createPatchXMLFile(oneOffPatchDir, oneOffPatch);
        File zippedPatch = createZippedPatchFile(oneOffPatchDir, oneOffPatchID);

        PatchingResult result = executePatch(zippedPatch);
        assertPatchHasBeenApplied(result, oneOffPatch);

        InstalledIdentity updatedInstalledIdentity = loadInstalledIdentity();
        File modulePatchDirectory = updatedInstalledIdentity.getLayers().get(0).loadTargetInfo().getDirectoryStructure().getModulePatchDirectory(oneOffLayerPatchID);
        assertDirExists(modulePatchDirectory);
        assertDefinedModule(modulePatchDirectory, moduleName, moduleAdded.getItem().getContentHash());
    }

    @Test
    public void testApplyOneOffPatchAndRollback() throws Exception {
        // create an existing file in the AS7 installation
        File binDir = mkdir(env.getInstalledImage().getJbossHome(), "bin");
        String fileName = "standalone.sh";
        File standaloneShellFile = touch(binDir, fileName);
        dump(standaloneShellFile, "original script to run standalone AS7");
        byte[] existingHash = hashFile(standaloneShellFile);

        // build a one-off patch for the base installation
        // with 1 added module
        // and 1 updated file
        String patchID = randomString();
        String layerPatchID = randomString();
        File patchDir = mkdir(tempDir, patchID);

        String moduleName = randomString();
        ContentModification moduleAdded = ContentModificationUtils.addModule(patchDir, layerPatchID, moduleName);

        ContentModification fileUpdated = ContentModificationUtils.modifyMisc(patchDir, patchID, "updated script", standaloneShellFile, "bin", "standalone.sh");

        Identity identityBeforePatch = loadInstalledIdentity().getIdentity();

        Patch patch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription(randomString())
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                .getParent()
                .oneOffPatchElement(layerPatchID, BASE, false)
                    .addContentModification(moduleAdded)
                    .getParent()
                .addContentModification(fileUpdated)
                .build();
        createPatchXMLFile(patchDir, patch);
        File zippedPatch = createZippedPatchFile(patchDir, patchID);

        PatchingResult result = executePatch(zippedPatch);
        assertPatchHasBeenApplied(result, patch);

        assertFileExists(standaloneShellFile);
        assertFileContent(fileUpdated.getItem().getContentHash(), standaloneShellFile);

        InstalledIdentity updatedInstalledIdentity = loadInstalledIdentity();
        File modulePatchDirectory = updatedInstalledIdentity.getLayers().get(0).loadTargetInfo().getDirectoryStructure().getModulePatchDirectory(layerPatchID);
        assertDirExists(modulePatchDirectory);
        assertDefinedModule(modulePatchDirectory, moduleName, moduleAdded.getItem().getContentHash());

        // rollback the patch based on the updated PatchInfo
        PatchingResult rollbackResult = rollback(patchID);
        assertPatchHasBeenRolledBack(rollbackResult, identityBeforePatch);

        assertFileExists(standaloneShellFile);
        assertFileContent(existingHash, standaloneShellFile);
    }

    @Test
    public void apply2OneOffPatchesAndRollbackTheFirstOne() throws Exception {
        // create two files in the AS7 installation
        File binDir = mkdir(env.getInstalledImage().getJbossHome(), "bin");
        String standaloneFileName = "standalone.sh";
        File standaloneShellFile = touch(binDir, standaloneFileName);
        dump(standaloneShellFile, "original script to run standalone AS7");
        byte[] existingHashForStandaloneShell = hashFile(standaloneShellFile);
        String domainFileName = "domain.sh";
        File domainShellFile = touch(binDir, domainFileName);
        dump(domainShellFile, "original script to run domain AS7");
        byte[] existingHashForDomainShell = hashFile(domainShellFile);

        // build a one-off patch for the base installation
        // with 1 added module
        // and 1 updated file
        String patchID = "patch-1-" + randomString();
        String layerPatchID = randomString();
        File patchDir = mkdir(tempDir, patchID);

        String moduleName = randomString();
        ContentModification moduleAdded = ContentModificationUtils.addModule(patchDir, layerPatchID, moduleName);

        ContentModification fileUpdated = ContentModificationUtils.modifyMisc(patchDir, patchID, "updated script", standaloneShellFile, "bin", "standalone.sh");

        Identity identityBeforePatch = loadInstalledIdentity().getIdentity();

        Patch patch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription(randomString())
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                .getParent()
                .oneOffPatchElement(layerPatchID, BASE, false)
                    .addContentModification(moduleAdded)
                    .getParent()
                .addContentModification(fileUpdated)
                .build();
        createPatchXMLFile(patchDir, patch);
        File zippedPatch = createZippedPatchFile(patchDir, patchID);

        PatchingResult result = executePatch(zippedPatch);
        assertPatchHasBeenApplied(result, patch);

        assertFileExists(standaloneShellFile);
        assertFileContent(fileUpdated.getItem().getContentHash(), standaloneShellFile);

        InstalledIdentity updatedInstalledIdentity = loadInstalledIdentity();
        File modulePatchDirectory = updatedInstalledIdentity.getLayers().get(0).loadTargetInfo().getDirectoryStructure().getModulePatchDirectory(layerPatchID);
        assertDirExists(modulePatchDirectory);
        assertDefinedModule(modulePatchDirectory, moduleName, moduleAdded.getItem().getContentHash());

        // build a 2nd one-off patch for the base installation
        // with 1 updated file
        String patchID_2 = "patch-2-" + randomString();
        File patchDir_2 = mkdir(tempDir, patchID_2);

        ContentModification otherFileUpdated = ContentModificationUtils.modifyMisc(patchDir_2, patchID_2, "updated domain script", domainShellFile, "bin", "domain.sh");

        Patch patch_2 = PatchBuilder.create()
                .setPatchId(patchID_2)
                .setDescription(randomString())
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                .getParent()
                .addContentModification(otherFileUpdated)
                .build();

        createPatchXMLFile(patchDir_2, patch_2);
        File zippedPatch_2 = createZippedPatchFile(patchDir_2, patchID_2);

        PatchingResult result_2 = executePatch(zippedPatch_2);
        assertPatchHasBeenApplied(result_2, patch_2);

        assertFileExists(domainShellFile);
        assertFileContent(otherFileUpdated.getItem().getContentHash(), domainShellFile);

        // rollback the *first* patch based on the updated PatchInfo
        PatchingResult rollbackResult = rollback(patchID, true);

        // both patches must be rolled back
        assertFileExists(standaloneShellFile);
        assertFileContent(existingHashForStandaloneShell, standaloneShellFile);
        assertFileExists(domainShellFile);
        assertFileContent(existingHashForDomainShell, domainShellFile);
    }
}
