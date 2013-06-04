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
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.MiscContentItem;
import org.jboss.as.patching.metadata.ModuleItem;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.junit.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012, Red Hat Inc
 */
public class CumulativePatchTestCase extends AbstractTaskTestCase {

    @Test
    public void testApplyCumulativePatch() throws Exception {

        // start from a base installation
        PatchInfo info = new LocalPatchInfo(randomString(), PatchInfo.BASE, Collections.<String>emptyList(), env);

        // build a CP patch for the base installation
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
                .setCumulativeType(info.getVersion(), info.getVersion() + "-CP")
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
    public void testApplyCumulativePatchAndRollback() throws Exception {

        // start from a base installation
        PatchInfo info = new LocalPatchInfo(randomString(), PatchInfo.BASE, Collections.<String>emptyList(), env);
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
                .setCumulativeType(info.getVersion(), info.getVersion() + "-CP")
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
    public void testApplyCumulativePatchThenOneOffPatch() throws Exception {

        // start from a base installation
        PatchInfo info = new LocalPatchInfo(randomString(), PatchInfo.BASE, Collections.<String>emptyList(), env);

        // build a CP patch for the base installation
        // with 1 added module
        String culumativePatchID = randomString();
        File cumulativePatchDir = mkdir(tempDir, culumativePatchID);
        String moduleName = randomString();
        File moduleDir = createModule(cumulativePatchDir, moduleName);
        byte[] newHash = hashFile(moduleDir);
        ContentModification moduleAdded = new ContentModification(new ModuleItem(moduleName, null, newHash), NO_CONTENT, ADD);

        Patch cumulativePatch = PatchBuilder.create()
                .setPatchId(culumativePatchID)
                .setDescription(randomString())
                .setCumulativeType(info.getVersion(), info.getVersion() + "-CP")
                .addContentModification(moduleAdded)
                .build();
        createPatchXMLFile(cumulativePatchDir, cumulativePatch);
        File zippedCumulativePatch = createZippedPatchFile(cumulativePatchDir, culumativePatchID);

        PatchingResult resultOfCumulativePatch = executePatch(info, zippedCumulativePatch);

        assertPatchHasBeenApplied(resultOfCumulativePatch, cumulativePatch);

        assertDefinedModule(getModulePath(env, resultOfCumulativePatch.getPatchInfo()), moduleName, newHash);

        // apply a one-off patch now
        String oneOffPatchID = randomString();
        File oneOffPatchDir = mkdir(tempDir, oneOffPatchID);

        File oneOffmoduleDir = createModule(oneOffPatchDir, moduleName, "update module resource");
        byte[] updatedHash = hashFile(oneOffmoduleDir);
        ContentModification moduleUpdated = new ContentModification(new ModuleItem(moduleName, null, updatedHash), newHash, MODIFY);

        Patch oneOffPatch = PatchBuilder.create()
                .setPatchId(oneOffPatchID)
                .setDescription(randomString())
                // one-off patch can be applied to CP
                .setOneOffType(cumulativePatch.getResultingVersion())
                .addContentModification(moduleUpdated)
                .build();
        createPatchXMLFile(oneOffPatchDir, oneOffPatch);
        File zippedOneOffPatch = createZippedPatchFile(oneOffPatchDir, oneOffPatchID);

        // use the updated PatchInfo for the result of applying the cumulative patch
        PatchingResult resultOfOneOffPatch = executePatch(resultOfCumulativePatch.getPatchInfo(), zippedOneOffPatch);

        assertPatchHasBeenApplied(resultOfOneOffPatch, oneOffPatch);

        assertDefinedModule(getModulePath(env, resultOfOneOffPatch.getPatchInfo()), moduleName, updatedHash);
    }

    @Test
    public void testApplyCumulativePatchThenOneOffPatchThenRollbackCumulativePatch() throws Exception {

        // start from a base installation
        PatchInfo info = new LocalPatchInfo(randomString(), PatchInfo.BASE, Collections.<String>emptyList(), env);
        // with a module in it
        String moduleName = randomString();
        // create an empty module in the AS7 installation
        createModule(env.getInstalledImage().getJbossHome(), moduleName);

        tree(env.getInstalledImage().getJbossHome());
        byte[] existingHash = hashFile(new File(env.getInstalledImage().getModulesDir(), moduleName));

        // build a CP patch for the base installation
        // with 1 updated module
        String culumativePatchID = randomString() + "-CP";
        File cumulativePatchDir = mkdir(tempDir, culumativePatchID);
        File moduleDir = createModule(cumulativePatchDir, moduleName, "this is a module update in a cumulative patch");
        byte[] updatedHashCP = hashFile(moduleDir);
        ContentModification moduleAdded = new ContentModification(new ModuleItem(moduleName, null, updatedHashCP), existingHash, MODIFY);

        Patch cumulativePatch = PatchBuilder.create()
                .setPatchId(culumativePatchID)
                .setDescription(randomString())
                .setCumulativeType(info.getVersion(), info.getVersion() + "-CP")
                .addContentModification(moduleAdded)
                .build();
        createPatchXMLFile(cumulativePatchDir, cumulativePatch);
        File zippedCumulativePatch = createZippedPatchFile(cumulativePatchDir, culumativePatchID);

        PatchingResult resultOfCumulativePatch = executePatch(info, zippedCumulativePatch);

        assertPatchHasBeenApplied(resultOfCumulativePatch, cumulativePatch);

        assertDefinedModule(getModulePath(env, resultOfCumulativePatch.getPatchInfo()), moduleName, updatedHashCP);

        // apply a one-off patch now
        String oneOffPatchID = randomString();
        File oneOffPatchDir = mkdir(tempDir, oneOffPatchID);

        File oneOffmoduleDir = createModule(oneOffPatchDir, moduleName, "update module resource");
        byte[] updatedHashOneOff = hashFile(oneOffmoduleDir);
        ContentModification moduleUpdated = new ContentModification(new ModuleItem(moduleName, null, updatedHashOneOff), updatedHashCP, MODIFY);

        Patch oneOffPatch = PatchBuilder.create()
                .setPatchId(oneOffPatchID + "-oneoff")
                .setDescription(randomString())
                // one-off patch can be applied to CP
                .setOneOffType(cumulativePatch.getResultingVersion())
                .addContentModification(moduleUpdated)
                .build();
        createPatchXMLFile(oneOffPatchDir, oneOffPatch);
        File zippedOneOffPatch = createZippedPatchFile(oneOffPatchDir, oneOffPatchID);

        // use the updated PatchInfo for the result of applying the cumulative patch
        PatchingResult resultOfOneOffPatch = executePatch(resultOfCumulativePatch.getPatchInfo(), zippedOneOffPatch);

        assertPatchHasBeenApplied(resultOfOneOffPatch, oneOffPatch);

        assertDefinedModule(getModulePath(env, resultOfOneOffPatch.getPatchInfo()), moduleName, updatedHashOneOff);

        // rollback the cumulative patch, this should also rollback the one-off patch
        PatchingResult resultOfCumulativePatchRollback = rollback(resultOfOneOffPatch.getPatchInfo(), culumativePatchID);

        tree(env.getInstalledImage().getJbossHome());
        assertPatchHasBeenRolledBack(resultOfCumulativePatchRollback, cumulativePatch, info);
        // assertNoResourcesForPatch(resultOfCumulativePatchRollback.getPatchInfo(), oneOffPatch);

        assertDefinedModule(getModulePath(env, resultOfCumulativePatchRollback.getPatchInfo()), moduleName, existingHash);
    }

    @Test
    public void testInvalidateOneOffPatches() throws Exception {

        // start from a base installation
        PatchInfo info = new LocalPatchInfo(randomString(), PatchInfo.BASE, Collections.<String>emptyList(), env);
        String moduleName = randomString();

        // build a one-off patch for the base installation
        String patchID = randomString();
        File patchDir = mkdir(tempDir, patchID);
        File oneModuleDir = createModule(patchDir, moduleName);
        byte[] oneModuleHash = hashFile(oneModuleDir);
        ContentModification oneModuleAdded = new ContentModification(new ModuleItem(moduleName, null, oneModuleHash), NO_CONTENT , ADD);

        Patch patch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription(randomString())
                .setOneOffType(info.getVersion())
                .addContentModification(oneModuleAdded)
                .build();
        createPatchXMLFile(patchDir, patch);
        File zippedPatch = createZippedPatchFile(patchDir, patchID);

        PatchingResult result = executePatch(info, zippedPatch);

        assertPatchHasBeenApplied(result, patch);
        tree(env.getInstalledImage().getJbossHome());
        assertDefinedModule(getModulePath(env, result.getPatchInfo()), moduleName, oneModuleHash);

        // build a CP patch for the base installation
        String culumativePatchID = randomString() + "-CP";
        File cumulativePatchDir = mkdir(tempDir, culumativePatchID);
        File moduleDir = createModule(cumulativePatchDir, moduleName, "this is a module update in a cumulative patch");
        byte[] updatedHashCP = hashFile(moduleDir);
        ContentModification moduleAdded = new ContentModification(new ModuleItem(moduleName, null, updatedHashCP), NO_CONTENT, ADD);

        Patch cumulativePatch = PatchBuilder.create()
                .setPatchId(culumativePatchID)
                .setDescription(randomString())
                .setCumulativeType(info.getVersion(), info.getVersion() + "-CP")
                .addContentModification(moduleAdded)
                .build();
        createPatchXMLFile(cumulativePatchDir, cumulativePatch);
        File zippedCumulativePatch = createZippedPatchFile(cumulativePatchDir, culumativePatchID);

        PatchingResult resultOfCumulativePatch = executePatch(info, zippedCumulativePatch);

        assertPatchHasBeenApplied(resultOfCumulativePatch, cumulativePatch);
        assertEquals(2, getModulePath(env, resultOfCumulativePatch.getPatchInfo()).length); // only CP and modules
        assertDefinedModule(getModulePath(env, resultOfCumulativePatch.getPatchInfo()), moduleName, updatedHashCP);

    }

}
