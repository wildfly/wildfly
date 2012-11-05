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
import static org.jboss.as.patching.metadata.ModificationType.REMOVE;
import static org.jboss.as.patching.runner.PatchingAssert.assertContains;
import static org.jboss.as.patching.runner.PatchingAssert.assertDefinedAbsentModule;
import static org.jboss.as.patching.runner.PatchingAssert.assertDefinedModule;
import static org.jboss.as.patching.runner.PatchingAssert.assertDirExists;
import static org.jboss.as.patching.runner.PatchingAssert.assertPatchHasBeenApplied;
import static org.jboss.as.patching.runner.TestUtils.createModule;
import static org.jboss.as.patching.runner.TestUtils.createPatchXMLFile;
import static org.jboss.as.patching.runner.TestUtils.createZippedPatchFile;
import static org.jboss.as.patching.runner.TestUtils.mkdir;
import static org.jboss.as.patching.runner.TestUtils.randomString;
import static org.jboss.as.patching.runner.TestUtils.tree;

import java.io.File;
import java.util.Collections;

import org.jboss.as.patching.LocalPatchInfo;
import org.jboss.as.patching.PatchInfo;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.ModuleItem;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.junit.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012, Red Hat Inc
 */
public class ModuleTaskTestCase extends AbstractTaskTestCase{

    @Test
    public void testAddModule() throws Exception {

        // start from a base installation
        PatchInfo info = new LocalPatchInfo(randomString(), PatchInfo.BASE, Collections.<String>emptyList(), env);

        // build a one-off patch for the base installation
        // with 1 added module
        String patchID = randomString();
        File patchDir = mkdir(tempDir, patchID);
        String moduleName = randomString();
        File moduleDir = createModule(patchDir, moduleName);
        byte[] newHash = hashFile(moduleDir);
        ContentModification moduleAdded = new ContentModification(new ModuleItem(moduleName, newHash), NO_CONTENT, ADD);

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

        File modulesPatchDir = env.getModulePatchDirectory(patchID);
        assertDirExists(modulesPatchDir);
        tree(result.getPatchInfo().getEnvironment().getInstalledImage().getJbossHome());
        assertContains(modulesPatchDir, result.getPatchInfo().getModulePath());
        assertDefinedModule(result.getPatchInfo().getModulePath(), moduleName, newHash);
    }

    @Test
    public void testRemoveModule() throws Exception {

        String moduleName = randomString();

        // start from a base installation
        PatchInfo info = new LocalPatchInfo(randomString(), PatchInfo.BASE, Collections.<String>emptyList(), env);

        // create an empty module in the AS7 installation
        createModule(env.getInstalledImage().getJbossHome(), moduleName);
        byte[] existingHash = hashFile(new File(env.getInstalledImage().getModulesDir(), moduleName));

        // build a one-off patch for the base installation
        // with 1 module removed
        ContentModification moduleRemoved = new ContentModification(new ModuleItem(moduleName, NO_CONTENT), existingHash, REMOVE);

        Patch patch = PatchBuilder.create()
                .setPatchId(randomString())
                .setDescription(randomString())
                .setOneOffType(info.getVersion())
                .addContentModification(moduleRemoved)
                .build();

        // create the patch
        File patchDir = mkdir(tempDir, patch.getPatchId());
        createPatchXMLFile(patchDir, patch);
        File zippedPatch = createZippedPatchFile(patchDir, patch.getPatchId());

        PatchingResult result = executePatch(info, zippedPatch);

        assertPatchHasBeenApplied(result, patch);

        File modulesPatchDir = env.getModulePatchDirectory(patch.getPatchId());
        assertDirExists(modulesPatchDir);
        assertContains(modulesPatchDir, result.getPatchInfo().getModulePath());
        assertDefinedAbsentModule(result.getPatchInfo().getModulePath(), moduleName);
    }

    @Test
    public void testUpdateModule() throws Exception {

        String moduleName = randomString();

        // start from a base installation
        PatchInfo info = new LocalPatchInfo(randomString(), PatchInfo.BASE, Collections.<String>emptyList(), env);

        // create an empty module in the AS7 installation
        createModule(env.getInstalledImage().getJbossHome(), moduleName);

        tree(env.getInstalledImage().getJbossHome());
        byte[] existingHash = hashFile(new File(env.getInstalledImage().getModulesDir(), moduleName));

        // build a one-off patch for the base installation
        // with 1 module updated

        String patchID = randomString();
        File patchDir = mkdir(tempDir, patchID);

        // create the patch with the update module
        File moduleDir = createModule(patchDir, moduleName, "new resource in the module");
        byte[] updatedHash = hashFile(moduleDir);
        ContentModification moduleUpdated = new ContentModification(new ModuleItem(moduleName, updatedHash), existingHash, MODIFY);

        Patch patch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription(randomString())
                .setOneOffType(info.getVersion())
                .addContentModification(moduleUpdated)
                .build();
        createPatchXMLFile(patchDir, patch);
        File zippedPatch = createZippedPatchFile(patchDir, patch.getPatchId());

        PatchingResult result = executePatch(info, zippedPatch);

        assertPatchHasBeenApplied(result, patch);

        File modulesPatchDir = env.getModulePatchDirectory(patch.getPatchId());
        assertDirExists(modulesPatchDir);
        assertContains(modulesPatchDir, result.getPatchInfo().getModulePath());
        // check that the defined module is the updated one
        assertDefinedModule(result.getPatchInfo().getModulePath(), moduleName, updatedHash);
    }
}
