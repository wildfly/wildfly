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
import static org.jboss.as.patching.HashUtils.hashFile;
import static org.jboss.as.patching.IoUtils.NO_CONTENT;
import static org.jboss.as.patching.IoUtils.mkdir;
import static org.jboss.as.patching.IoUtils.newFile;
import static org.jboss.as.patching.PatchInfo.BASE;
import static org.jboss.as.patching.metadata.ModificationType.ADD;
import static org.jboss.as.patching.runner.PatchingAssert.assertDefinedModule;
import static org.jboss.as.patching.runner.PatchingAssert.assertDirExists;
import static org.jboss.as.patching.runner.PatchingAssert.assertFileDoesNotExist;
import static org.jboss.as.patching.runner.PatchingAssert.assertFileExists;
import static org.jboss.as.patching.runner.PatchingAssert.assertInstallationIsPatched;
import static org.jboss.as.patching.runner.PatchingAssert.assertPatchHasBeenApplied;
import static org.jboss.as.patching.runner.PatchingAssert.assertPatchHasBeenRolledBack;
import static org.jboss.as.patching.runner.TestUtils.createModule;
import static org.jboss.as.patching.runner.TestUtils.createPatchXMLFile;
import static org.jboss.as.patching.runner.TestUtils.createZippedPatchFile;
import static org.jboss.as.patching.runner.TestUtils.dump;
import static org.jboss.as.patching.runner.TestUtils.randomString;
import static org.jboss.as.patching.runner.TestUtils.touch;
import static org.jboss.as.patching.runner.TestUtils.tree;

import java.io.File;
import java.util.Collection;

import org.jboss.as.patching.DirectoryStructure;
import org.jboss.as.patching.IoUtils;
import org.jboss.as.patching.LocalPatchInfo;
import org.jboss.as.patching.PatchInfo;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.MiscContentItem;
import org.jboss.as.patching.metadata.ModuleItem;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.jboss.as.patching.metadata.impl.IdentityImpl;
import org.jboss.as.patching.metadata.impl.PatchElementImpl;
import org.jboss.as.patching.metadata.impl.PatchElementProviderImpl;
import org.jboss.as.patching.runner.AbstractTaskTestCase;
import org.jboss.as.patching.runner.PatchingResult;
import org.jboss.as.patching.runner.TestUtils;
import org.jboss.as.version.ProductConfig;
import org.junit.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class AddOnTestCase extends AbstractTaskTestCase {

    @Test
    public void installedAddOn() throws Exception {
        String addOnName = randomString();
        installAddOn(env.getModuleRoot(), addOnName);

        ProductConfig productConfig = new ProductConfig("product", "version", "consoleSlot");

        TestUtils.tree(env.getInstalledImage().getJbossHome());

        InstalledIdentity installedIdentity = InstalledIdentity.load(env.getInstalledImage().getJbossHome(), productConfig, env.getInstalledImage().getModulesDir());

        Identity identity = installedIdentity.getIdentity();
        assertEquals(productConfig.getProductName(), identity.getName());
        assertEquals(productConfig.resolveVersion(), identity.getVersion());

        Collection<AddOn> addOns = installedIdentity.getAddOns();
        assertEquals(1, addOns.size());
        AddOn addOn = addOns.iterator().next();
        assertEquals(addOnName, addOn.getName());

        PatchableTarget.TargetInfo targetInfo = addOn.loadTargetInfo();
        assertEquals(BASE, targetInfo.getCumulativeID());
        assertTrue(targetInfo.getPatchIDs().isEmpty());
        DirectoryStructure directoryStructure = targetInfo.getDirectoryStructure();
        assertEquals(newFile(env.getModuleRoot(), "system", ADD_ONS, addOnName), directoryStructure.getModuleRoot());
        assertNull(directoryStructure.getBundleRepositoryRoot());
    }

    @Test
    public void patchAddOn() throws Exception {
        // start from a base installation
        ProductConfig productConfig = new ProductConfig("product", "version", "consoleSlot");

        // add an add-on
        String addOnName = randomString();
        installAddOn(env.getModuleRoot(), addOnName);

        InstalledIdentity installedIdentity = InstalledIdentity.load(env.getInstalledImage().getJbossHome(), productConfig, env.getInstalledImage().getModulesDir());

        System.out.println("installation =>>");
        tree(env.getInstalledImage().getJbossHome());

        // build a one-off patch for the add-on with 1 added module
        // and 1 add file
        String patchID = randomString();
        File patchDir = mkdir(tempDir, patchID);
        String addOnPatchID = randomString();
        String moduleName = randomString();
        File patchedAddOnModuleRoot = newFile(patchDir, addOnPatchID);
        File moduleDir = createModule(patchedAddOnModuleRoot, moduleName);
        byte[] newHash = hashFile(moduleDir);
        ContentModification moduleAdded = new ContentModification(new ModuleItem(moduleName, ModuleItem.MAIN_SLOT, newHash), NO_CONTENT, ADD);

        String fileName =  "my-new-standalone.sh";
        File newFile = touch(patchDir, addOnPatchID, "misc", "bin", fileName);
        dump(newFile, "new file resource");
        byte[] newFileHash = hashFile(newFile);
        ContentModification fileAdded = new ContentModification(new MiscContentItem(fileName, new String[] { "bin" }, newFileHash), NO_CONTENT, ADD);

        PatchElementImpl addOnPatch = new PatchElementImpl(addOnPatchID);
        addOnPatch.addContentModification(moduleAdded);
        addOnPatch.addContentModification(fileAdded);
        addOnPatch.setProvider(new PatchElementProviderImpl(addOnName, "1.0.1", true));
        addOnPatch.setNoUpgrade();
        Patch patch = PatchBuilder.create()
                .setPatchId(patchID)
                .setOneOffType(productConfig.getProductVersion())
                .setIdentity(new IdentityImpl(installedIdentity.getIdentity().getName(), installedIdentity.getIdentity().getVersion()))
                .addElement(addOnPatch)
                .build();

        createPatchXMLFile(patchDir, patch);
        System.out.println("patch =>>");
        File zippedPatch = createZippedPatchFile(patchDir, patchID);

        // apply patch
        PatchingResult result = executePatch(zippedPatch, installedIdentity, env.getInstalledImage());
        assertPatchHasBeenApplied(result, patch);
        InstalledIdentity patchedInstalledIdentity = InstalledIdentity.load(env.getInstalledImage().getJbossHome(), productConfig, env.getInstalledImage().getModulesDir());
        assertInstallationIsPatched(patch, patchedInstalledIdentity.getIdentity().loadTargetInfo());
        assertFileExists(env.getInstalledImage().getJbossHome(), "bin", fileName);

        System.out.println("installation =>>");
        tree(env.getInstalledImage().getJbossHome());

        DirectoryStructure addOnStructure = installedIdentity.getAddOns().iterator().next().loadTargetInfo().getDirectoryStructure();
        File modulesPatchDir = addOnStructure.getModulePatchDirectory(addOnPatchID);
        assertDirExists(modulesPatchDir);
        assertDefinedModule(modulesPatchDir, moduleName, newHash);
    }

    @Test
    public void patchAndRollbackAddOn() throws Exception {
        // start from a base installation
        ProductConfig productConfig = new ProductConfig("product", "version", "consoleSlot");

        // add an add-on
        String addOnName = randomString();
        installAddOn(env.getModuleRoot(), addOnName);

        InstalledIdentity installedIdentity = InstalledIdentity.load(env.getInstalledImage().getJbossHome(), productConfig, env.getInstalledImage().getModulesDir());
        PatchInfo originalPatchInfo = LocalPatchInfo.load(productConfig, env);
        assertEquals(BASE, originalPatchInfo.getCumulativeID());
        assertTrue(originalPatchInfo.getPatchIDs().isEmpty());

        System.out.println("installation =>>");
        tree(env.getInstalledImage().getJbossHome());

        // build a one-off patch for the add-on with 1 added module
        // and 1 added file
        String patchID = randomString();
        File patchDir = mkdir(tempDir, patchID);
        String addOnPatchID = randomString();
        String moduleName = randomString();
        File patchedAddOnModuleRoot = newFile(patchDir, addOnPatchID);
        File moduleDir = createModule(patchedAddOnModuleRoot, moduleName);
        byte[] newHash = hashFile(moduleDir);
        ContentModification moduleAdded = new ContentModification(new ModuleItem(moduleName, ModuleItem.MAIN_SLOT, newHash), NO_CONTENT, ADD);

        String fileName =  "my-new-standalone.sh";
        File newFile = touch(patchDir, addOnPatchID, "misc", "bin", fileName);
        dump(newFile, "new file resource");
        byte[] newFileHash = hashFile(newFile);
        ContentModification fileAdded = new ContentModification(new MiscContentItem(fileName, new String[] { "bin" }, newFileHash), NO_CONTENT, ADD);

        PatchElementImpl addOnPatch = new PatchElementImpl(addOnPatchID);
        addOnPatch.addContentModification(moduleAdded);
        addOnPatch.addContentModification(fileAdded);
        addOnPatch.setProvider(new PatchElementProviderImpl(addOnName, "1.0.1", true));
        addOnPatch.setNoUpgrade();
        Patch patch = PatchBuilder.create()
                .setPatchId(patchID)
                .setOneOffType(productConfig.getProductVersion())
                .setIdentity(new IdentityImpl(installedIdentity.getIdentity().getName(), installedIdentity.getIdentity().getVersion()))
                .addElement(addOnPatch)
                .build();

        createPatchXMLFile(patchDir, patch);
        System.out.println("patch =>>");
        File zippedPatch = createZippedPatchFile(patchDir, patchID);

        // apply patch
        PatchingResult patchResult = executePatch(zippedPatch, installedIdentity, env.getInstalledImage());
        assertPatchHasBeenApplied(patchResult, patch);
        // reload the installed identity
        InstalledIdentity patchedInstalledIdentity = InstalledIdentity.load(env.getInstalledImage().getJbossHome(), productConfig, env.getInstalledImage().getModulesDir());
        assertInstallationIsPatched(patch, patchedInstalledIdentity.getIdentity().loadTargetInfo());
        assertFileExists(env.getInstalledImage().getJbossHome(), "bin", fileName);

        System.out.println("installation =>>");
        tree(env.getInstalledImage().getJbossHome());

        DirectoryStructure layerDirStructure = patchedInstalledIdentity.getAddOns().iterator().next().loadTargetInfo().getDirectoryStructure();
        File modulesPatchDir = layerDirStructure.getModulePatchDirectory(addOnPatchID);
        assertDirExists(modulesPatchDir);
        assertDefinedModule(modulesPatchDir, moduleName, newHash);

        // rollback the patch
        PatchingResult rollbackResult = rollback(patchID, patchedInstalledIdentity, env.getInstalledImage());
        assertPatchHasBeenRolledBack(rollbackResult, patch, originalPatchInfo);

        assertFileDoesNotExist(env.getInstalledImage().getJbossHome(), "bin", fileName);

    }

    private static void installAddOn(File baseDir, String... addOns) throws Exception {
        for (String addOn : addOns) {
            IoUtils.mkdir(baseDir, "system", ADD_ONS, addOn);
        }
    }
}
