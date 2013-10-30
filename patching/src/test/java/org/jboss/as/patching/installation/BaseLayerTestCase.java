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
import static junit.framework.Assert.assertTrue;
import static org.jboss.as.patching.Constants.LAYERS;
import static org.jboss.as.patching.IoUtils.mkdir;
import static org.jboss.as.patching.IoUtils.newFile;
import static org.jboss.as.patching.Constants.BASE;
import static org.jboss.as.patching.runner.PatchingAssert.assertDefinedModule;
import static org.jboss.as.patching.runner.PatchingAssert.assertDirExists;
import static org.jboss.as.patching.runner.PatchingAssert.assertFileExists;
import static org.jboss.as.patching.runner.PatchingAssert.assertInstallationIsPatched;
import static org.jboss.as.patching.runner.PatchingAssert.assertPatchHasBeenApplied;
import static org.jboss.as.patching.runner.TestUtils.createPatchXMLFile;
import static org.jboss.as.patching.runner.TestUtils.createZippedPatchFile;
import static org.jboss.as.patching.runner.TestUtils.randomString;
import static org.jboss.as.patching.runner.TestUtils.tree;

import java.io.File;
import java.util.List;

import org.jboss.as.patching.DirectoryStructure;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.jboss.as.patching.runner.AbstractTaskTestCase;
import org.jboss.as.patching.runner.ContentModificationUtils;
import org.jboss.as.patching.tool.PatchingResult;
import org.junit.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class BaseLayerTestCase extends AbstractTaskTestCase {

    @Test
    public void baseLayerIsAlwaysInstalled() throws Exception {
        tree(env.getInstalledImage().getJbossHome());

        InstalledIdentity installedIdentity = loadInstalledIdentity();
        List<Layer> layers = installedIdentity.getLayers();
        assertEquals(1, layers.size());
        Layer layer = layers.get(0);
        assertEquals(BASE, layer.getName());

        PatchableTarget.TargetInfo targetInfo = layer.loadTargetInfo();
        assertEquals(BASE, targetInfo.getCumulativePatchID());
        assertTrue(targetInfo.getPatchIDs().isEmpty());
        DirectoryStructure directoryStructure = targetInfo.getDirectoryStructure();
        assertEquals(newFile(env.getBundleRepositoryRoot(), "system", LAYERS, BASE), directoryStructure.getBundleRepositoryRoot());
        assertEquals(newFile(env.getModuleRoot(), "system", LAYERS, BASE), directoryStructure.getModuleRoot());
    }

    @Test
    public void patchBase() throws Exception {
        InstalledIdentity installedIdentity = loadInstalledIdentity();

        // build a one-off patch for the base layer with 1 added module
        // and 1 add file
        String patchID = randomString();
        File patchDir = mkdir(tempDir, patchID);
        String layerPatchId = "mylayerPatchID";//randomString();
        String moduleName = randomString();
        ContentModification moduleAdded = ContentModificationUtils.addModule(patchDir, layerPatchId, moduleName);
        ContentModification fileAdded = ContentModificationUtils.addMisc(patchDir, patchID, "new file resource", "bin", "my-new-standalone.sh");

        Patch patch = PatchBuilder.create()
                .setPatchId(patchID)
                .oneOffPatchIdentity(installedIdentity.getIdentity().getName(), installedIdentity.getIdentity().getVersion())
                .getParent()
                .oneOffPatchElement(layerPatchId, BASE, false)
                   .addContentModification(moduleAdded)
                   .getParent()
                .addContentModification(fileAdded)
                .build();

        createPatchXMLFile(patchDir, patch);
        //System.out.println("patch =>>");
        File zippedPatch = createZippedPatchFile(patchDir, patchID);

        // apply patch
        PatchingResult result = executePatch(zippedPatch);
        assertPatchHasBeenApplied(result, patch);
        InstalledIdentity patchedInstalledIdentity = InstalledIdentity.load(env.getInstalledImage().getJbossHome(), productConfig, env.getInstalledImage().getModulesDir());
        assertInstallationIsPatched(patch, patchedInstalledIdentity.getIdentity().loadTargetInfo());
        assertFileExists(env.getInstalledImage().getJbossHome(), "bin", fileAdded.getItem().getName());

        //System.out.println("installation =>>");
        tree(env.getInstalledImage().getJbossHome());

        DirectoryStructure layerDirStructure = installedIdentity.getLayers().get(0).loadTargetInfo().getDirectoryStructure();
        File modulesPatchDir = layerDirStructure.getModulePatchDirectory(layerPatchId);
        assertDirExists(modulesPatchDir);
        assertDefinedModule(modulesPatchDir, moduleName, moduleAdded.getItem().getContentHash());
    }
}
