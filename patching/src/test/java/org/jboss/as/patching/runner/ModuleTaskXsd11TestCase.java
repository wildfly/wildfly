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
import static org.jboss.as.patching.runner.TestUtils.getModulePath;
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
import org.jboss.as.patching.metadata.xsd1_1.PatchBuilder1_1;
import org.jboss.as.patching.metadata.xsd1_1.impl.IdentityImpl;
import org.jboss.as.patching.metadata.xsd1_1.impl.PatchElementImpl;
import org.jboss.as.patching.metadata.xsd1_1.impl.PatchElementProviderImpl;
import org.junit.Test;

/**
 * @author Alexey Loubyansky
 */
public class ModuleTaskXsd11TestCase extends AbstractTaskTestCase{

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
        ContentModification moduleAdded = new ContentModification(new ModuleItem(moduleName, ModuleItem.MAIN_SLOT, newHash), NO_CONTENT, ADD);

        PatchBuilder1_1 builder = PatchBuilder1_1.create()
                .setPatchId(patchID)
                .setDescription(randomString())
                .setIdentity(new IdentityImpl("eap", info.getVersion()))
                .setNoUpgrade();

        PatchElementImpl element = new PatchElementImpl("patch element 01");
        builder.addElement(element);
        element.setDescription("patch element 01 description");
        element.setNoUpgrade();

        PatchElementProviderImpl provider = new PatchElementProviderImpl("base", "4.5.6", false);
        provider.require("patch element 02");
        element.setProvider(provider);

        element.addContentModification(moduleAdded);

        Patch patch = builder.build();

        createPatchXMLFile(patchDir, patch);
        File zippedPatch = createZippedPatchFile(patchDir, patchID);

        PatchingResult result = executePatch(info, zippedPatch);

        assertPatchHasBeenApplied(result, patch);

        File modulesPatchDir = env.getModulePatchDirectory(patchID);
        assertDirExists(modulesPatchDir);
        tree(env.getInstalledImage().getJbossHome());
        assertContains(modulesPatchDir, getModulePath(env, result.getPatchInfo()));
        assertDefinedModule(getModulePath(env, result.getPatchInfo()), moduleName, newHash);
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
        ContentModification moduleRemoved = new ContentModification(new ModuleItem(moduleName, ModuleItem.MAIN_SLOT, NO_CONTENT), existingHash, REMOVE);

        PatchBuilder1_1 builder = PatchBuilder1_1.create()
                .setPatchId(randomString())
                .setDescription(randomString())
                .setIdentity(new IdentityImpl("eap", info.getVersion()))
                .setNoUpgrade();

        PatchElementImpl element = new PatchElementImpl("patch element 01");
        builder.addElement(element);
        element.setDescription("patch element 01 description");
        element.setNoUpgrade();

        PatchElementProviderImpl provider = new PatchElementProviderImpl("base", "4.5.6", false);
        provider.require("patch element 02");
        element.setProvider(provider);

        element.addContentModification(moduleRemoved);

        Patch patch = builder.build();

        // create the patch
        File patchDir = mkdir(tempDir, patch.getPatchId());
        createPatchXMLFile(patchDir, patch);

/*        final java.io.FileInputStream fis = new java.io.FileInputStream(new java.io.File(patchDir, "patch.xml"));
        try {
            byte[] content = new byte[fis.available()];
            fis.read(content, 0, content.length);
            System.out.println(new String(content));
        } finally {
            fis.close();
        }
*/
        File zippedPatch = createZippedPatchFile(patchDir, patch.getPatchId());

        PatchingResult result = executePatch(info, zippedPatch);

        assertPatchHasBeenApplied(result, patch);

        File modulesPatchDir = env.getModulePatchDirectory(patch.getPatchId());
        assertDirExists(modulesPatchDir);
        assertContains(modulesPatchDir, getModulePath(env, result.getPatchInfo()));
        assertDefinedAbsentModule(getModulePath(env, result.getPatchInfo()), moduleName);
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
        ContentModification moduleUpdated = new ContentModification(new ModuleItem(moduleName, ModuleItem.MAIN_SLOT, updatedHash), existingHash, MODIFY);

        PatchBuilder1_1 builder = PatchBuilder1_1.create()
                .setPatchId(patchID)
                .setDescription(randomString())
                .setIdentity(new IdentityImpl("eap", info.getVersion()))
                .setNoUpgrade();

        PatchElementImpl element = new PatchElementImpl("patch element 01");
        builder.addElement(element);
        element.setDescription("patch element 01 description");
        element.setNoUpgrade();

        PatchElementProviderImpl provider = new PatchElementProviderImpl("base", "4.5.6", false);
        provider.require("patch element 02");
        element.setProvider(provider);

        element.addContentModification(moduleUpdated);

        Patch patch = builder.build();

        createPatchXMLFile(patchDir, patch);
        File zippedPatch = createZippedPatchFile(patchDir, patch.getPatchId());

        PatchingResult result = executePatch(info, zippedPatch);

        assertPatchHasBeenApplied(result, patch);

        File modulesPatchDir = env.getModulePatchDirectory(patch.getPatchId());
        assertDirExists(modulesPatchDir);
        assertContains(modulesPatchDir, getModulePath(env, result.getPatchInfo()));
        // check that the defined module is the updated one
        assertDefinedModule(getModulePath(env, result.getPatchInfo()), moduleName, updatedHash);
    }
}
