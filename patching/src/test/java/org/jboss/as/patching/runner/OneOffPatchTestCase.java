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
import static org.jboss.as.patching.metadata.ModificationType.ADD;
import static org.jboss.as.patching.metadata.ModificationType.MODIFY;
import static org.jboss.as.patching.runner.PatchingAssert.assertDefinedModule;
import static org.jboss.as.patching.runner.PatchingAssert.assertDirExists;
import static org.jboss.as.patching.runner.PatchingAssert.assertFileExists;
import static org.jboss.as.patching.runner.PatchingAssert.assertPatchHasBeenApplied;
import static org.jboss.as.patching.runner.PatchingAssert.assertPatchHasBeenRolledBack;
import static org.jboss.as.patching.runner.TestUtils.createModule;
import static org.jboss.as.patching.runner.TestUtils.createPatchXMLFile;
import static org.jboss.as.patching.runner.TestUtils.createZippedPatchFile;
import static org.jboss.as.patching.runner.TestUtils.dump;
import static org.jboss.as.patching.runner.TestUtils.getModulePath;
import static org.jboss.as.patching.runner.TestUtils.mkdir;
import static org.jboss.as.patching.runner.TestUtils.randomString;
import static org.jboss.as.patching.runner.TestUtils.touch;
import static org.jboss.as.patching.runner.TestUtils.tree;
import static org.junit.Assert.assertArrayEquals;

import java.io.File;
import java.util.Collections;

import org.jboss.as.patching.LocalPatchInfo;
import org.jboss.as.patching.PatchInfo;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.MiscContentItem;
import org.jboss.as.patching.metadata.ModuleItem;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.junit.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012, Red Hat Inc
 */
public class OneOffPatchTestCase extends AbstractTaskTestCase {

    @Test
    public void testApplyOneOffPatch() throws Exception {

        // start from a base installation
        PatchInfo info = new LocalPatchInfo(randomString(), PatchInfo.BASE, Collections.<String>emptyList(), env);

        // build a one-off patch for the base installation
        // with 1 added module
        String patchID = randomString();
        File patchDir = mkdir(tempDir, patchID);
        String moduleName = randomString();
        File moduleDir = createModule(patchDir, moduleName);
        byte[] newHash = hashFile(moduleDir);
        ContentModification moduleAdded = new ContentModification(new ModuleItem(moduleName, null, newHash), NO_CONTENT , ADD);

        Patch patch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription(randomString())
                .setOneOffType(info.getVersion())
                .addContentModification(moduleAdded)
                .build();
        createPatchXMLFile(patchDir, patch);
        File zippedPatch = createZippedPatchFile(patchDir, patchID);

        PatchingResult result = executePatch(info, zippedPatch);

        assertPatchHasBeenApplied(result, patch);
        tree(env.getInstalledImage().getJbossHome());
        assertDefinedModule(getModulePath(env, result.getPatchInfo()), moduleName, newHash);
    }

    @Test
    public void testApplyOneOffPatchAndRollback() throws Exception {

        // start from a base installation
        PatchInfo info = new LocalPatchInfo(randomString(), PatchInfo.BASE, Collections.<String>emptyList(), env);
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
        File patchDir = mkdir(tempDir, patchID);
        String moduleName = randomString();
        File moduleDir = createModule(patchDir, moduleName);
        byte[] newModuleHash = hashFile(moduleDir);
        ContentModification moduleAdded = new ContentModification(new ModuleItem(moduleName, null, newModuleHash), NO_CONTENT, ADD);
        File updatedFile = touch(patchDir, "misc", "bin", fileName);
        dump(updatedFile, "updated script");
        byte[] updatedHash = hashFile(updatedFile);
        ContentModification fileUpdated = new ContentModification(new MiscContentItem(fileName, new String[] { "bin" }, updatedHash), existingHash, MODIFY);

        Patch patch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription(randomString())
                .setOneOffType(info.getVersion())
                .addContentModification(moduleAdded)
                .addContentModification(fileUpdated)
                .build();
        createPatchXMLFile(patchDir, patch);
        File zippedPatch = createZippedPatchFile(patchDir, patchID);

        PatchingResult result = executePatch(info, zippedPatch);

        assertPatchHasBeenApplied(result, patch);
        assertFileExists(standaloneShellFile);
        assertArrayEquals(updatedHash, hashFile(standaloneShellFile));
        tree(env.getInstalledImage().getJbossHome());
        assertDirExists(env.getInstalledImage().getPatchHistoryDir(patchID));
        assertDefinedModule(getModulePath(env, result.getPatchInfo()), moduleName, newModuleHash);

        // rollback the patch based on the updated PatchInfo
        PatchingResult rollbackResult = rollback(result.getPatchInfo(), patchID);

        tree(env.getInstalledImage().getJbossHome());
        assertPatchHasBeenRolledBack(rollbackResult, patch, info);
        assertFileExists(standaloneShellFile);
        assertArrayEquals(existingHash, hashFile(standaloneShellFile));
    }

    @Test
    public void apply2OneOffPatchesAndRollbackTheFirstOne() throws Exception {

        // start from a base installation
        PatchInfo info = new LocalPatchInfo(randomString(), PatchInfo.BASE, Collections.<String>emptyList(), env);
        // create an existing file in the AS7 installation
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
        File patchDir = mkdir(tempDir, patchID);
        String moduleName = randomString();
        File moduleDir = createModule(patchDir, moduleName);
        byte[] newModuleHash = hashFile(moduleDir);
        ContentModification moduleAdded = new ContentModification(new ModuleItem(moduleName, null, newModuleHash), NO_CONTENT, ADD);
        File updatedStandaloneShell = touch(patchDir, "misc", "bin", standaloneFileName);
        dump(updatedStandaloneShell, "updated script");
        byte[] updatedHashForStandaloneShell = hashFile(updatedStandaloneShell);
        ContentModification standaloneShellUpdated = new ContentModification(new MiscContentItem(standaloneFileName, new String[] { "bin" }, updatedHashForStandaloneShell), existingHashForStandaloneShell, MODIFY);

        Patch patch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription(randomString())
                .setOneOffType(info.getVersion())
                .addContentModification(moduleAdded)
                .addContentModification(standaloneShellUpdated)
                .build();
        createPatchXMLFile(patchDir, patch);
        File zippedPatch = createZippedPatchFile(patchDir, patchID);

        PatchingResult result = executePatch(info, zippedPatch);

        assertPatchHasBeenApplied(result, patch);
        assertFileExists(standaloneShellFile);
        assertArrayEquals(updatedHashForStandaloneShell, hashFile(standaloneShellFile));
        tree(env.getInstalledImage().getJbossHome());
        assertDirExists(env.getInstalledImage().getPatchHistoryDir(patchID));
        assertDefinedModule(getModulePath(env, result.getPatchInfo()), moduleName, newModuleHash);

        // build a 2nd one-off patch for the base installation
        // with 1 updated file
        String patchID_2 = "patch-2-" + randomString();
        File patchDir_2 = mkdir(tempDir, patchID_2);
        File updatedDomainShell = touch(patchDir_2, "misc", "bin", domainFileName);
        dump(updatedDomainShell, "updated script to run domain AS7");
        byte[] updatedHashForDomainShell = hashFile(updatedDomainShell);
        ContentModification domainShellUpdated = new ContentModification(new MiscContentItem(domainFileName, new String[] { "bin" }, updatedHashForDomainShell), existingHashForDomainShell, MODIFY);

        Patch patch_2 = PatchBuilder.create()
                .setPatchId(patchID_2)
                .setDescription(randomString())
                .setOneOffType(info.getVersion())
                .addContentModification(domainShellUpdated)
                .build();
        createPatchXMLFile(patchDir_2, patch_2);
        File zippedPatch_2 = createZippedPatchFile(patchDir_2, patchID_2);

        PatchingResult result_2 = executePatch(result.getPatchInfo(), zippedPatch_2);

        assertPatchHasBeenApplied(result_2, patch_2);
        assertFileExists(domainShellFile);
        assertArrayEquals(updatedHashForDomainShell, hashFile(domainShellFile));
        tree(env.getInstalledImage().getJbossHome());
        assertDirExists(env.getInstalledImage().getPatchHistoryDir(patchID_2));

        // rollback the *first* patch based on the updated PatchInfo
        PatchingResult rollbackResult = rollback(result_2.getPatchInfo(), patchID, true);

        // both patches must be rolled back
        tree(env.getInstalledImage().getJbossHome());
        assertPatchHasBeenRolledBack(rollbackResult, patch, info);
        assertPatchHasBeenRolledBack(rollbackResult, patch_2, info);
        assertFileExists(standaloneShellFile);
        assertArrayEquals(existingHashForStandaloneShell, hashFile(standaloneShellFile));
        assertFileExists(domainShellFile);
        assertArrayEquals(existingHashForDomainShell, hashFile(domainShellFile));
    }
}
