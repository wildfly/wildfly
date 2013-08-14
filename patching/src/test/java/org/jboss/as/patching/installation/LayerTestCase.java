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
import static org.jboss.as.patching.IoUtils.mkdir;
import static org.jboss.as.patching.IoUtils.newFile;
import static org.jboss.as.patching.Constants.BASE;
import static org.jboss.as.patching.runner.PatchingAssert.assertDefinedModule;
import static org.jboss.as.patching.runner.PatchingAssert.assertDirDoesNotExist;
import static org.jboss.as.patching.runner.PatchingAssert.assertDirExists;
import static org.jboss.as.patching.runner.PatchingAssert.assertFileExists;
import static org.jboss.as.patching.runner.PatchingAssert.assertInstallationIsPatched;
import static org.jboss.as.patching.runner.PatchingAssert.assertPatchHasBeenApplied;
import static org.jboss.as.patching.runner.PatchingAssert.assertPatchHasBeenRolledBack;
import static org.jboss.as.patching.runner.TestUtils.createPatchXMLFile;
import static org.jboss.as.patching.runner.TestUtils.createZippedPatchFile;
import static org.jboss.as.patching.runner.TestUtils.randomString;
import static org.jboss.as.patching.runner.TestUtils.tree;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Properties;

import org.jboss.as.patching.Constants;
import org.jboss.as.patching.DirectoryStructure;
import org.jboss.as.patching.IoUtils;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.jboss.as.patching.runner.AbstractTaskTestCase;
import org.jboss.as.patching.runner.ContentModificationUtils;
import org.jboss.as.patching.runner.PatchingAssert;
import org.jboss.as.patching.tool.PatchingResult;
import org.jboss.as.patching.runner.TestUtils;
import org.junit.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class LayerTestCase extends AbstractTaskTestCase {

    @Test
    public void layerNotInLayersConf() throws Exception {
        String layerName = randomString();
        installLayer(env.getModuleRoot(), null, layerName);

        InstalledIdentity installedIdentity = loadInstalledIdentity();

        // if the layer name is not in layers.conf, it's not part of the installed identity
        List<Layer> layers = installedIdentity.getLayers();
        assertEquals(1, layers.size());
        assertEquals(BASE, layers.get(0).getName()); // only the base layer is installled
    }

    @Test
    public void installedLayer() throws Exception {
        String layerName = randomString();
        installLayer(env.getModuleRoot(), env.getInstalledImage().getLayersConf(), layerName);

        TestUtils.tree(env.getInstalledImage().getJbossHome());
        InstalledIdentity installedIdentity = loadInstalledIdentity();

        Identity identity = installedIdentity.getIdentity();
        assertEquals(productConfig.getProductName(), identity.getName());
        assertEquals(productConfig.resolveVersion(), identity.getVersion());

        List<Layer> layers = installedIdentity.getLayers();
        assertEquals(2, layers.size());
        Layer layer = layers.get(0);
        assertEquals(layerName, layer.getName());
        assertEquals(BASE, layers.get(1).getName()); // base layer is always appended

        PatchableTarget.TargetInfo targetInfo = layer.loadTargetInfo();
        assertEquals(BASE, targetInfo.getCumulativePatchID());
        assertTrue(targetInfo.getPatchIDs().isEmpty());
        DirectoryStructure directoryStructure = targetInfo.getDirectoryStructure();
        assertEquals(newFile(env.getModuleRoot(), "system", "layers", layerName), directoryStructure.getModuleRoot());
        assertNull(directoryStructure.getBundleRepositoryRoot());
    }

    @Test
    public void patchLayer() throws Exception {
        // add a layer
        String layerName = "mylayer";//randomString();
        installLayer(env.getModuleRoot(), env.getInstalledImage().getLayersConf(), layerName);

        InstalledIdentity installedIdentity = loadInstalledIdentity();

        // build a one-off patch for the layer with 1 added module
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
                .oneOffPatchElement(layerPatchId, layerName, false)
                    .addContentModification(moduleAdded)
                    .getParent()
                .addContentModification(fileAdded)
                .build();

        createPatchXMLFile(patchDir, patch);
        File zippedPatch = createZippedPatchFile(patchDir, patchID);

        // apply patch
        PatchingResult result = executePatch(zippedPatch);
        assertPatchHasBeenApplied(result, patch);
        InstalledIdentity patchedInstalledIdentity = loadInstalledIdentity();
        assertInstallationIsPatched(patch, patchedInstalledIdentity.getIdentity().loadTargetInfo());
        assertFileExists(env.getInstalledImage().getJbossHome(), "bin", fileAdded.getItem().getName());

        DirectoryStructure layerDirStructure = installedIdentity.getLayers().get(0).loadTargetInfo().getDirectoryStructure();
        File modulesPatchDir = layerDirStructure.getModulePatchDirectory(layerPatchId);
        assertDirExists(modulesPatchDir);
        assertDefinedModule(modulesPatchDir, moduleName, moduleAdded.getItem().getContentHash());
    }

    @Test
    public void patchAndRollbackLayer() throws Exception {
        // add a layer
        String layerName = randomString();
        installLayer(env.getModuleRoot(), env.getInstalledImage().getLayersConf(), layerName);

        InstalledIdentity installedIdentity = loadInstalledIdentity();

        PatchableTarget.TargetInfo identityInfo = installedIdentity.getIdentity().loadTargetInfo();
        assertEquals(BASE, identityInfo.getCumulativePatchID());
        assertTrue(identityInfo.getPatchIDs().isEmpty());

        // build a one-off patch for the layer with 1 added module
        // and 1 added file
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
                .oneOffPatchElement(layerPatchId, layerName, false)
                    .addContentModification(moduleAdded)
                    .getParent()
                .addContentModification(fileAdded)
                .build();

        createPatchXMLFile(patchDir, patch);
        File zippedPatch = createZippedPatchFile(patchDir, patchID);

        Identity identityBeforePatch = loadInstalledIdentity().getIdentity();

        // apply patch
        PatchingResult patchResult = executePatch(zippedPatch);
        assertPatchHasBeenApplied(patchResult, patch);
        // reload the installed identity
        InstalledIdentity patchedInstalledIdentity = InstalledIdentity.load(env.getInstalledImage().getJbossHome(), productConfig, env.getInstalledImage().getModulesDir());
        assertInstallationIsPatched(patch, patchedInstalledIdentity.getIdentity().loadTargetInfo());
        assertFileExists(env.getInstalledImage().getJbossHome(), "bin", fileAdded.getItem().getName());

        DirectoryStructure layerDirStructure = patchedInstalledIdentity.getLayers().get(0).loadTargetInfo().getDirectoryStructure();
        File modulesPatchDir = layerDirStructure.getModulePatchDirectory(layerPatchId);
        assertDirExists(modulesPatchDir);
        assertDefinedModule(modulesPatchDir, moduleName, moduleAdded.getItem().getContentHash());

        // rollback the patch
        PatchingResult rollbackResult = rollback(patchID);
        assertPatchHasBeenRolledBack(rollbackResult, identityBeforePatch);
        // reload the rolled back installed identity
        InstalledIdentity rolledBackInstalledIdentity = InstalledIdentity.load(env.getInstalledImage().getJbossHome(), productConfig, env.getInstalledImage().getModulesDir());
        PatchingAssert.assertFileDoesNotExist(env.getInstalledImage().getJbossHome(), "bin", fileAdded.getItem().getName());
        if (File.separatorChar != '\\') {
            assertDirDoesNotExist(rolledBackInstalledIdentity.getLayers().get(0).loadTargetInfo().getDirectoryStructure().getModulePatchDirectory(layerPatchId));
        }
    }

    private static void installLayer(File baseDir, File layerConf, String... layers) throws Exception {
        for (String layer : layers) {
            IoUtils.mkdir(baseDir, "system", "layers", layer);
        }
        if (layerConf != null) {
            Properties props = new Properties();
            StringBuilder str = new StringBuilder();
            for (int i = 0; i < layers.length; i++) {
                if (i > 0) {
                    str.append(',');
                }
                str.append(layers[i]);
            }
            props.put(Constants.LAYERS, str.toString());
            props.put(Constants.EXCLUDE_LAYER_BASE, "true");
            final FileOutputStream os = new FileOutputStream(layerConf);
            try {
                props.store(os, "");
            } finally {
                IoUtils.safeClose(os);
            }
        }
    }
}
