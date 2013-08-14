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

package org.jboss.as.patching.installation;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.jboss.as.patching.Constants.ADD_ONS;
import static org.jboss.as.patching.IoUtils.mkdir;
import static org.jboss.as.patching.IoUtils.newFile;
import static org.jboss.as.patching.Constants.BASE;
import static org.jboss.as.patching.runner.PatchingAssert.assertDefinedModule;
import static org.jboss.as.patching.runner.PatchingAssert.assertDirExists;
import static org.jboss.as.patching.runner.PatchingAssert.assertFileDoesNotExist;
import static org.jboss.as.patching.runner.PatchingAssert.assertFileExists;
import static org.jboss.as.patching.runner.PatchingAssert.assertInstallationIsPatched;
import static org.jboss.as.patching.runner.PatchingAssert.assertPatchHasBeenApplied;
import static org.jboss.as.patching.runner.PatchingAssert.assertPatchHasBeenRolledBack;
import static org.jboss.as.patching.runner.TestUtils.createPatchXMLFile;
import static org.jboss.as.patching.runner.TestUtils.createZippedPatchFile;
import static org.jboss.as.patching.runner.TestUtils.randomString;
import static org.jboss.as.patching.runner.TestUtils.tree;

import java.io.File;
import java.util.Collection;

import org.jboss.as.patching.DirectoryStructure;
import org.jboss.as.patching.IoUtils;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.jboss.as.patching.runner.AbstractTaskTestCase;
import org.jboss.as.patching.runner.ContentModificationUtils;
import org.jboss.as.patching.tool.PatchingResult;
import org.jboss.as.patching.runner.TestUtils;
import org.junit.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class AddOnTestCase extends AbstractTaskTestCase {

    @Test
    public void installedAddOn() throws Exception {
        String addOnName = randomString();
        installAddOn(env.getModuleRoot(), addOnName);

        TestUtils.tree(env.getInstalledImage().getJbossHome());

        InstalledIdentity installedIdentity = loadInstalledIdentity();

        Collection<AddOn> addOns = installedIdentity.getAddOns();
        assertEquals(1, addOns.size());
        AddOn addOn = addOns.iterator().next();
        assertEquals(addOnName, addOn.getName());

        PatchableTarget.TargetInfo targetInfo = addOn.loadTargetInfo();
        assertEquals(BASE, targetInfo.getCumulativePatchID());
        assertTrue(targetInfo.getPatchIDs().isEmpty());
        DirectoryStructure directoryStructure = targetInfo.getDirectoryStructure();
        assertEquals(newFile(env.getModuleRoot(), "system", ADD_ONS, addOnName), directoryStructure.getModuleRoot());
        assertNull(directoryStructure.getBundleRepositoryRoot());
    }

    @Test
    public void patchAddOn() throws Exception {
        // start from a base installation
        // add an add-on
        String addOnName = randomString();
        installAddOn(env.getModuleRoot(), addOnName);

        InstalledIdentity installedIdentity = loadInstalledIdentity();

        // build a one-off patch for the add-on with 1 added module
        // and 1 add file
        String patchID = randomString();
        File patchDir = mkdir(tempDir, patchID);
        String addOnPatchID = randomString();
        String moduleName = randomString();
        ContentModification moduleAdded = ContentModificationUtils.addModule(patchDir, addOnPatchID, moduleName);
        ContentModification fileAdded = ContentModificationUtils.addMisc(patchDir, patchID, "new file resource", "bin", "my-new-standalone.sh");

        Patch patch = PatchBuilder.create()
                .setPatchId(patchID)
                .oneOffPatchIdentity(installedIdentity.getIdentity().getName(), installedIdentity.getIdentity().getVersion())
                .getParent()
                .oneOffPatchElement(addOnPatchID, addOnName, true)
                    .addContentModification(moduleAdded)
                    .getParent()
                .addContentModification(fileAdded)
                .build();

        createPatchXMLFile(patchDir, patch);
        File zippedPatch = createZippedPatchFile(patchDir, patchID);

        // apply patch
        PatchingResult result = executePatch(zippedPatch);
        assertPatchHasBeenApplied(result, patch);
        InstalledIdentity patchedInstalledIdentity = InstalledIdentity.load(env.getInstalledImage().getJbossHome(), productConfig, env.getInstalledImage().getModulesDir());
        assertInstallationIsPatched(patch, patchedInstalledIdentity.getIdentity().loadTargetInfo());
        assertFileExists(env.getInstalledImage().getJbossHome(), "bin", fileAdded.getItem().getName());

        DirectoryStructure addOnStructure = installedIdentity.getAddOns().iterator().next().loadTargetInfo().getDirectoryStructure();
        File modulesPatchDir = addOnStructure.getModulePatchDirectory(addOnPatchID);
        assertDirExists(modulesPatchDir);
        assertDefinedModule(modulesPatchDir, moduleName, moduleAdded.getItem().getContentHash());
    }

    @Test
    public void patchAndRollbackAddOn() throws Exception {
        // start from a base installation
        // add an add-on
        String addOnName = randomString();
        installAddOn(env.getModuleRoot(), addOnName);

        InstalledIdentity installedIdentity = loadInstalledIdentity();

        PatchableTarget.TargetInfo identityInfo = installedIdentity.getIdentity().loadTargetInfo();
        assertEquals(BASE, identityInfo.getCumulativePatchID());
        assertTrue(identityInfo.getPatchIDs().isEmpty());

        // build a one-off patch for the add-on with 1 added module
        // and 1 added file
        String patchID = randomString();
        File patchDir = mkdir(tempDir, patchID);
        String addOnPatchID = randomString();
        String moduleName = randomString();
        ContentModification moduleAdded = ContentModificationUtils.addModule(patchDir, addOnPatchID, moduleName);
        ContentModification fileAdded = ContentModificationUtils.addMisc(patchDir, patchID, "new file resource", "bin", "my-new-standalone.sh");

        Patch patch = PatchBuilder.create()
                .setPatchId(patchID)
                .oneOffPatchIdentity(installedIdentity.getIdentity().getName(), installedIdentity.getIdentity().getVersion())
                .getParent()
                .oneOffPatchElement(addOnPatchID, addOnName, true)
                    .addContentModification(moduleAdded)
                    .getParent()
                .addContentModification(fileAdded)
                .build();

        createPatchXMLFile(patchDir, patch);
        File zippedPatch = createZippedPatchFile(patchDir, patchID);

        // apply patch
        PatchingResult patchResult = executePatch(zippedPatch);
        assertPatchHasBeenApplied(patchResult, patch);
        // reload the installed identity
        InstalledIdentity patchedInstalledIdentity = InstalledIdentity.load(env.getInstalledImage().getJbossHome(), productConfig, env.getInstalledImage().getModulesDir());
        assertInstallationIsPatched(patch, patchedInstalledIdentity.getIdentity().loadTargetInfo());
        assertFileExists(env.getInstalledImage().getJbossHome(), "bin", fileAdded.getItem().getName());

        DirectoryStructure layerDirStructure = patchedInstalledIdentity.getAddOns().iterator().next().loadTargetInfo().getDirectoryStructure();
        File modulesPatchDir = layerDirStructure.getModulePatchDirectory(addOnPatchID);
        assertDirExists(modulesPatchDir);
        assertDefinedModule(modulesPatchDir, moduleName, moduleAdded.getItem().getContentHash());

        // rollback the patch
        PatchingResult rollbackResult = rollback(patchID);
        assertPatchHasBeenRolledBack(rollbackResult, patch, identityInfo);
        assertFileDoesNotExist(env.getInstalledImage().getJbossHome(), "bin", "my-new-standalone.sh");

    }

    private static void installAddOn(File baseDir, String... addOns) throws Exception {
        for (String addOn : addOns) {
            IoUtils.mkdir(baseDir, "system", ADD_ONS, addOn);
        }
    }
}
