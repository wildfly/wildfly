/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.jboss.as.test.patching;

import static org.jboss.as.patching.IoUtils.mkdir;
import static org.jboss.as.test.patching.PatchingTestUtil.AS_VERSION;
import static org.jboss.as.test.patching.PatchingTestUtil.CONTAINER;
import static org.jboss.as.test.patching.PatchingTestUtil.PRODUCT;
import static org.jboss.as.test.patching.PatchingTestUtil.randomString;

import java.io.File;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.jboss.as.version.ProductConfig;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
@RunAsClient
public class OneOffPatchTestCase {

    @ArquillianResource
    private ContainerController controller;

    @After
    public void cleanup() {
        if(controller.isStarted(CONTAINER))
            controller.stop(CONTAINER);
    }

    /**
     * Prepare a one-off patch which adds a misc file. Apply it, check that the file was created.
     * Roll it back, check that the file was deleted.
     */
    @Test
    public void testOneOffPatchAddingAMiscFile() throws Exception {
        final String fileContent = "Hello World!";
        // prepare the patch
        File tempDir = mkdir(new File(System.getProperty("java.io.tmpdir")), randomString());
        String patchID = randomString();
        File oneOffPatchDir = mkdir(tempDir, patchID);
        ContentModification miscFileAdded = ContentModificationUtils.addMisc(oneOffPatchDir, patchID,
          fileContent, "awesomeDirectory", "awesomeFile");
        ProductConfig productConfig = new ProductConfig(PRODUCT, AS_VERSION, "main");
        Patch oneOffPatch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription("A one-off patch adding a misc file.")
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                    .getParent()
                .addContentModification(miscFileAdded)
                .build();
        PatchingTestUtil.createPatchXMLFile(oneOffPatchDir, oneOffPatch);
        File zippedPatch = PatchingTestUtil.createZippedPatchFile(oneOffPatchDir, patchID);

        // apply the patch
        controller.start(CONTAINER);
        CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath());
        controller.stop(CONTAINER);
        controller.start(CONTAINER);
        String path = PatchingTestUtil.AS_DISTRIBUTION+"/awesomeDirectory/awesomeFile";
        Assert.assertTrue("File " + path + " should exist", new File(path).exists());
        Assert.assertTrue("The patch " + patchID + " should be listed as installed" ,
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertEquals("Unexpected contents of misc file", fileContent, PatchingTestUtil.readFile(path));
        controller.stop(CONTAINER);
        controller.start(CONTAINER);

        // rollback the patch
        CliUtilsForPatching.rollbackPatch(patchID);
        controller.stop(CONTAINER);
        controller.start(CONTAINER);
        Assert.assertFalse("File + " + path + " should have been deleted", new File(path).exists());
        Assert.assertFalse("The patch " + patchID + " NOT should be listed as installed" ,
                CliUtilsForPatching.getInstalledPatches().contains(patchID));

        controller.stop(CONTAINER);
    }

    /**
     * Prepare a one-off patch which modifies a misc file. Apply it, check that the file was replaced.
     * Roll it back, check that the file was restored successfully.
     */
    @Test
    public void testOneOffPatchModifyingAMiscFile() throws Exception {
        // prepare the patch
        File tempDir = mkdir(new File(System.getProperty("java.io.tmpdir")), randomString());
        String patchID = randomString();
        File oneOffPatchDir = mkdir(tempDir, patchID);

        final String testFilePath = PatchingTestUtil.AS_DISTRIBUTION + "/README.txt";
        final String testContent = "test content";
        final String originalContent = PatchingTestUtil.readFile(testFilePath);

        ContentModification miscFileModified = ContentModificationUtils.modifyMisc(oneOffPatchDir, patchID, testContent, new File(testFilePath), "README.txt");
        ProductConfig productConfig = new ProductConfig(PRODUCT, AS_VERSION, "consoleSlot");
        Patch oneOffPatch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription("A one-off patch modifying a misc file.")
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                .getParent()
                .addContentModification(miscFileModified)
                .build();
        PatchingTestUtil.createPatchXMLFile(oneOffPatchDir, oneOffPatch);
        File zippedPatch = PatchingTestUtil.createZippedPatchFile(oneOffPatchDir, patchID);


        // apply the patch
        controller.start(CONTAINER);
        CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath());
        controller.stop(CONTAINER);

        //check content
        String patchContent = PatchingTestUtil.readFile(testFilePath);
        Assert.assertEquals(testContent, patchContent);

        //rollback the patch
        controller.start(CONTAINER);
        CliUtilsForPatching.rollbackPatch(patchID);
        controller.stop(CONTAINER);

        //check content
        patchContent =  PatchingTestUtil.readFile(testFilePath);
        Assert.assertEquals(originalContent, patchContent);
    }

    /**
     * adds a new module "org.wildfly.awesomemodule" to the base layer
     * @throws Exception
     */
    @Test
    public void testOneOffPatchAddingAModule() throws Exception {
        // prepare the patch
        File tempDir = mkdir(new File(System.getProperty("java.io.tmpdir")), randomString());
        String patchID = randomString();
        String layerPatchID  = randomString();
        File oneOffPatchDir = mkdir(tempDir, patchID);
        ContentModification moduleAdded = ContentModificationUtils.addModule(oneOffPatchDir, layerPatchID, "org.wildfly.awesomemodule", "content1", "content2");
        ProductConfig productConfig = new ProductConfig(PRODUCT, AS_VERSION, "main");
        Patch oneOffPatch = PatchBuilder.create()
            .setPatchId(patchID)
            .setDescription("A one-off patch adding a new module.")
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                .getParent()
            .oneOffPatchElement(layerPatchID, "base", false)
                .setDescription("New module for the base layer")
                .addContentModification(moduleAdded)
                .getParent()
           .build();
        PatchingTestUtil.createPatchXMLFile(oneOffPatchDir, oneOffPatch);
        File zippedPatch = PatchingTestUtil.createZippedPatchFile(oneOffPatchDir, patchID);

        // apply the patch
        controller.start(CONTAINER);
        CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath());
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);
        controller.start(CONTAINER);

        // TODO more checks that the module exists
        Assert.assertTrue("The patch " + patchID + " should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(patchID));


        controller.stop(CONTAINER);
        controller.start(CONTAINER);

        // rollback the patch
        CliUtilsForPatching.rollbackPatch(patchID);
        controller.stop(CONTAINER);
        controller.start(CONTAINER);

        // TODO mode checks that the module does not exist anymore
        Assert.assertFalse("The patch " + patchID + " NOT should be listed as installed" ,
                CliUtilsForPatching.getInstalledPatches().contains(patchID));

        controller.stop(CONTAINER);
    }

    /**
     * adds a new module "org.wildfly.awesomemodule" to the base layer
     * rollback it and apply the same patch again to make sure re-applying works as expected
     * @throws Exception
     */
    @Test
    public void testOneOffPatchAddingAModuleRepeatedly() throws Exception {
        // prepare the patch
        File tempDir = mkdir(new File(System.getProperty("java.io.tmpdir")), randomString());
        String patchID = randomString();
        String layerPatchID  = randomString();
        File oneOffPatchDir = mkdir(tempDir, patchID);
        ContentModification moduleAdded = ContentModificationUtils.addModule(oneOffPatchDir, layerPatchID, "org.wildfly.awesomemodule", "content1", "content2");
        ProductConfig productConfig = new ProductConfig(PRODUCT, AS_VERSION, "main");
        Patch oneOffPatch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription("A one-off patch adding a new module.")
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                .getParent()
                .oneOffPatchElement(layerPatchID, "base", false)
                .setDescription("New module for the base layer")
                .addContentModification(moduleAdded)
                .getParent()
                .build();
        PatchingTestUtil.createPatchXMLFile(oneOffPatchDir, oneOffPatch);
        File zippedPatch = PatchingTestUtil.createZippedPatchFile(oneOffPatchDir, patchID);

        // apply the patch, roll it back, then apply again
        controller.start(CONTAINER);
        CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath());
        controller.stop(CONTAINER);
        controller.start(CONTAINER);
        CliUtilsForPatching.rollbackPatch(patchID);
        controller.stop(CONTAINER);
        controller.start(CONTAINER);
        CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath());
        controller.stop(CONTAINER);
        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + patchID + " should be listed as installed" ,
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        // and finally roll it back to clean up
        CliUtilsForPatching.rollbackPatch(patchID);
        controller.stop(CONTAINER);
    }

}
