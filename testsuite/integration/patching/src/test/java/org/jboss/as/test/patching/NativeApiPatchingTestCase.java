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

package org.jboss.as.test.patching;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;

import com.google.common.base.Joiner;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.patching.IoUtils;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.jboss.as.version.ProductConfig;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.patching.IoUtils.mkdir;
import static org.jboss.as.test.patching.PatchingTestUtil.AS_DISTRIBUTION;
import static org.jboss.as.test.patching.PatchingTestUtil.AS_VERSION;
import static org.jboss.as.test.patching.PatchingTestUtil.CONTAINER;
import static org.jboss.as.test.patching.PatchingTestUtil.FILE_SEPARATOR;
import static org.jboss.as.test.patching.PatchingTestUtil.PRODUCT;
import static org.jboss.as.test.patching.PatchingTestUtil.assertPatchElements;
import static org.jboss.as.test.patching.PatchingTestUtil.baseModuleDir;
import static org.jboss.as.test.patching.PatchingTestUtil.createPatchXMLFile;
import static org.jboss.as.test.patching.PatchingTestUtil.createZippedPatchFile;
import static org.jboss.as.test.patching.PatchingTestUtil.randomString;
import static org.jboss.as.test.patching.PatchingTestUtil.readFile;

/**
 * Smoke test to cover patching through the native API.
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
@RunAsClient
public class NativeApiPatchingTestCase {

    @ArquillianResource
    private ContainerController controller;

    private File tempDir;

    @Before
    public void prepare() throws IOException {
        tempDir = mkdir(new File(System.getProperty("java.io.tmpdir")), randomString());
        assertPatchElements(baseModuleDir, null);
    }

    @After
    public void cleanup() throws Exception {
        if(controller.isStarted(CONTAINER))
            controller.stop(CONTAINER);
        CliUtilsForPatching.rollbackAll();

        if (IoUtils.recursiveDelete(tempDir)) {
            tempDir.deleteOnExit();
        }
    }

    /**
     * Apply a simple one-off patch through the native API.
     * Roll it back.
     * @throws Exception
     */
    @Test
    public void testApplyOneoff() throws Exception {
        ModelControllerClient client = getControllerClient();

        final String fileContent = "Hello World!";
        // prepare the patch
        String patchID = randomString();
        File oneOffPatchDir = mkdir(tempDir, patchID);
        String[] miscFileLocation = new String[] {"newPatchDirectory", "awesomeFile"};
        ContentModification miscFileAdded = ContentModificationUtils.addMisc(oneOffPatchDir, patchID,
                fileContent, miscFileLocation);
        ProductConfig productConfig = new ProductConfig(PRODUCT, AS_VERSION, "main");
        Patch oneOffPatch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription("A one-off patch adding a misc file.")
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                .getParent()
                .addContentModification(miscFileAdded)
                .build();
        createPatchXMLFile(oneOffPatchDir, oneOffPatch);
        File zippedPatch = createZippedPatchFile(oneOffPatchDir, patchID);

        controller.start(CONTAINER);
        Operation o = NativeApiUtilsForPatching.createPatchOperation(zippedPatch);

        System.out.println(o.getOperation().toJSONString(false));
        ModelNode ret = client.execute(o);
        System.out.println(ret.toJSONString(false));
        Assert.assertTrue(ret.get("outcome").asString().equalsIgnoreCase("success"));

        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        String path = AS_DISTRIBUTION + FILE_SEPARATOR + Joiner.on(FILE_SEPARATOR)
                .join(miscFileLocation);
        Assert.assertTrue("File " + path + " should exist", new File(path).exists());

        Assert.assertTrue("The patch " + patchID + " should be listed as installed",
                NativeApiUtilsForPatching.getInstalledPatches(client).contains(patchID));

        ModelNode itemForPatch = NativeApiUtilsForPatching.getHistoryItemForOneOffPatch(client, patchID);
        Assert.assertNotNull("The patch should appear in patching history", itemForPatch);

        Assert.assertEquals("Unexpected contents of misc file", fileContent, readFile(path));

        o = NativeApiUtilsForPatching.createRollbackOperation(patchID);
        System.out.println(o.getOperation().toJSONString(false));
        ret = client.execute(o);
        System.out.println(ret.toJSONString(false));
        Assert.assertTrue(ret.get("outcome").asString().equalsIgnoreCase("success"));

        controller.stop(CONTAINER);

        controller.start(CONTAINER);

        Assert.assertFalse("File + " + path + " should have been deleted", new File(path).exists());

        Assert.assertFalse("The patch " + patchID + " NOT should be listed as installed",
                NativeApiUtilsForPatching.getInstalledPatches(client).contains(patchID));

        IoUtils.recursiveDelete(tempDir);
        IoUtils.recursiveDelete(zippedPatch);
    }

    private ModelControllerClient getControllerClient() throws UnknownHostException {
        return ModelControllerClient.Factory.create(System.getProperty("node0", "127.0.0.1"), 9999);
    }

}