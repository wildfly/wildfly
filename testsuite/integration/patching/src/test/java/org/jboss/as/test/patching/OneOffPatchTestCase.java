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
import org.junit.Assert;
import org.junit.Ignore;
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

    /**
     * Prepare a one-off patch which adds a misc file. Apply it, check that the file was created.
     * Roll it back, check that the file was deleted.
     */
    @Test @Ignore
    public void testOneOffPatchAddingAMiscFile() throws Exception {
        // prepare the patch
        File tempDir = mkdir(new File(System.getProperty("java.io.tmpdir")), randomString());
        String patchID = randomString();
        File oneOffPatchDir = mkdir(tempDir, patchID);
        ContentModification miscFileAdded = ContentModificationUtils.addMisc(oneOffPatchDir, patchID, "content", "awesomeDirectory", "awesomeFile");
        ProductConfig productConfig = new ProductConfig(PRODUCT, AS_VERSION, "consoleSlot");
        Patch oneOffPatch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription("A one-off patch adding a misc file.")
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                    .getParent()
                .addContentModification(miscFileAdded)
                .build();
        PatchingTestUtil.createPatchXMLFile(oneOffPatchDir, oneOffPatch);
        File zippedPatch = PatchingTestUtil.createZippedPatchFile(oneOffPatchDir, patchID);

        System.out.println("ZIPFILE: " + zippedPatch.getAbsolutePath());


        // apply the patch
        controller.start(CONTAINER);
        CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath());
        controller.stop(CONTAINER);
        controller.start(CONTAINER);
        String path = PatchingTestUtil.AS_DISTRIBUTION+"/awesomeDirectory/awesomeFile";
        Assert.assertTrue("File " + path + " should exist", new File(path).exists());
        // TODO check the content of the file, not just its existence
        controller.stop(CONTAINER);
        controller.start(CONTAINER);

        // rollback the patch
        CliUtilsForPatching.rollbackPatch(patchID);
        controller.stop(CONTAINER);
        controller.start(CONTAINER);
        Assert.assertFalse("File + " + path + " should have been deleted", new File(path).exists());

        controller.stop(CONTAINER);
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
        ProductConfig productConfig = new ProductConfig(PRODUCT, AS_VERSION, "consoleSlot");
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

        System.out.println("ZIPFILE: " + zippedPatch.getAbsolutePath());

        // apply the patch
        controller.start(CONTAINER);
        CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath());
        controller.stop(CONTAINER);
        controller.start(CONTAINER);

        // TODO check that the module exists

        controller.stop(CONTAINER);
        controller.start(CONTAINER);

        // rollback the patch
        CliUtilsForPatching.rollbackPatch(patchID);
        controller.stop(CONTAINER);
        controller.start(CONTAINER);

        // TODO check that the module exists

        controller.stop(CONTAINER);

    }
}
