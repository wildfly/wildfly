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

import static org.jboss.as.patching.IoUtils.mkdir;
import static org.jboss.as.test.patching.PatchingTestUtil.CONTAINER;
import static org.jboss.as.test.patching.PatchingTestUtil.MODULES_PATH;
import static org.jboss.as.test.patching.PatchingTestUtil.createPatchXMLFile;
import static org.jboss.as.test.patching.PatchingTestUtil.createZippedPatchFile;
import static org.jboss.as.test.patching.PatchingTestUtil.randomString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.patching.HashUtils;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.jboss.as.test.patching.util.module.Module;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.version.ProductConfig;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Alexey Loubyansky
 *
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ShowHistoryUnitTestCase extends AbstractPatchingTestCase {

    protected ProductConfig productConfig;
    private String[] patchIds;
    private boolean[] patchTypes;

    @Before
    public void setup() throws Exception {
        productConfig = new ProductConfig(PatchingTestUtil.PRODUCT, PatchingTestUtil.AS_VERSION, "main");
        patchIds = new String[8];
        patchTypes = new boolean[patchIds.length];
        for(int i = 0; i < patchIds.length; ++i) {
            patchIds[i] = randomString();
            patchTypes[i] = (i % 3 == 0);
        }
    }

    @Test
    public void testUnpatched() throws Exception {
        controller.start(CONTAINER);
        ModelControllerClient client = null;
        final ModelNode response;
        try {
            client = ModelControllerClient.Factory.create(TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort());
            ModelNode op = new ModelNode();
            op.get("address").add("core-service", "patching");
            op.get("operation").set("show-history");
            response = client.execute(op);
        } finally {
            if(client != null) {
                client.close();
            }
            controller.stop(CONTAINER);
        }

        assertTrue(response.has("outcome"));
        assertEquals("success", response.get("outcome").asString());
        assertTrue(response.has("result"));
        final List<ModelNode> list = response.get("result").asList();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testCP() throws Exception {
        Module module = new Module.Builder("module-test").
                miscFile(new ResourceItem("resource-test", ("module resource").getBytes())).
                build();
        File moduleDir = module.writeToDisk(new File(MODULES_PATH));

        byte[] targetHash = HashUtils.hashFile(moduleDir);
        targetHash = applyCP("cp1", targetHash);

        controller.start(CONTAINER);
        ModelControllerClient client = null;
        final ModelNode response;
        try {
            client = ModelControllerClient.Factory.create(TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort());
            ModelNode op = new ModelNode();
            op.get("address").add("core-service", "patching");
            op.get("operation").set("show-history");
            response = client.execute(op);
        } finally {
            if(client != null) {
                client.close();
            }
            controller.stop(CONTAINER);
        }

        assertTrue(response.has("outcome"));
        assertEquals("success", response.get("outcome").asString());
        assertTrue(response.has("result"));
        final List<ModelNode> list = response.get("result").asList();
        assertEquals(1, list.size());
        final ModelNode entry = list.get(0);
        assertEquals("cp1", entry.get("patch-id").asString());
        assertEquals("cumulative", entry.get("type").asString());
    }

    @Test
    public void testOneOff() throws Exception {
        Module module = new Module.Builder("module-test").
                miscFile(new ResourceItem("resource-test", ("module resource").getBytes())).
                build();
        File moduleDir = module.writeToDisk(new File(MODULES_PATH));

        byte[] targetHash = HashUtils.hashFile(moduleDir);
        targetHash = applyOneOff("oneoff1", targetHash);

        controller.start(CONTAINER);
        ModelControllerClient client = null;
        final ModelNode response;
        try {
            client = ModelControllerClient.Factory.create(TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort());
            ModelNode op = new ModelNode();
            op.get("address").add("core-service", "patching");
            op.get("operation").set("show-history");
            response = client.execute(op);
        } finally {
            if(client != null) {
                client.close();
            }
            controller.stop(CONTAINER);
        }

        assertTrue(response.has("outcome"));
        assertEquals("success", response.get("outcome").asString());
        assertTrue(response.has("result"));
        final List<ModelNode> list = response.get("result").asList();
        assertEquals(1, list.size());
        final ModelNode entry = list.get(0);
        assertEquals("oneoff1", entry.get("patch-id").asString());
        assertEquals("one-off", entry.get("type").asString());
    }

    @Test
    public void testOneOffAndCP() throws Exception {
        Module module = new Module.Builder("module-test").
                miscFile(new ResourceItem("resource-test", ("module resource").getBytes())).
                build();
        File moduleDir = module.writeToDisk(new File(MODULES_PATH));

        byte[] targetHash = HashUtils.hashFile(moduleDir);
        targetHash = applyOneOff("oneoff1", targetHash);
        targetHash = applyCP("cp1", targetHash);

        controller.start(CONTAINER);
        ModelControllerClient client = null;
        final ModelNode response;
        try {
            client = ModelControllerClient.Factory.create(TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort());
            ModelNode op = new ModelNode();
            op.get("address").add("core-service", "patching");
            op.get("operation").set("show-history");
            response = client.execute(op);
        } finally {
            if(client != null) {
                client.close();
            }
            controller.stop(CONTAINER);
        }

        assertTrue(response.has("outcome"));
        assertEquals("success", response.get("outcome").asString());
        assertTrue(response.has("result"));
        final List<ModelNode> list = response.get("result").asList();
        assertEquals(2, list.size());
        ModelNode entry = list.get(0);
        assertEquals("cp1", entry.get("patch-id").asString());
        assertEquals("cumulative", entry.get("type").asString());
        entry = list.get(1);
        assertEquals("oneoff1", entry.get("patch-id").asString());
        assertEquals("one-off", entry.get("type").asString());
    }

    @Test
    public void testMain() throws Exception {

        // create a module
        Module module = new Module.Builder("module-test").
                miscFile(new ResourceItem("resource-test", ("module resource").getBytes())).
                build();
        File moduleDir = module.writeToDisk(new File(MODULES_PATH));

        byte[] targetHash = HashUtils.hashFile(moduleDir);
        for(int i = 0; i < patchIds.length; ++i) {
            if(patchTypes[i]) {
                targetHash = applyCP(patchIds[i], targetHash);
            } else {
                targetHash = applyOneOff(patchIds[i], targetHash);
            }
        }

        controller.start(CONTAINER);
        ModelControllerClient client = null;
        final ModelNode response;
        try {
            client = ModelControllerClient.Factory.create(TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort());
            ModelNode op = new ModelNode();
            op.get("address").add("core-service", "patching");
            op.get("operation").set("show-history");
            response = client.execute(op);
        } finally {
            if(client != null) {
                client.close();
            }
            controller.stop(CONTAINER);
        }

        assertTrue(response.has("outcome"));
        assertEquals("success", response.get("outcome").asString());
        assertTrue(response.has("result"));
        final List<ModelNode> list = response.get("result").asList();
        assertEquals(patchIds.length, list.size());
        for(int i = 0; i < patchIds.length; ++i) {
            final ModelNode info = list.get(i);
            assertEquals(patchIds[patchIds.length - 1 - i], info.get("patch-id").asString());
            assertTrue(info.has("type"));
            final String type = patchTypes[patchTypes.length - 1 - i] ? "cumulative" : "one-off";
            assertEquals(type, info.get("type").asString());
            assertTrue(info.has("applied-at"));
        }
    }

    protected byte[] applyOneOff(String patchId, byte[] targetHash) throws IOException, Exception {
        return applyPatch(patchId, targetHash, false);
    }

    protected byte[] applyCP(String patchId, byte[] targetHash) throws Exception {
        return applyPatch(patchId, targetHash, true);
    }

    protected byte[] applyPatch(String patchID, byte[] targetHash, boolean cp) throws Exception {
        String moduleName = "module-test";
        String patchElementId = randomString();
        File patchDir = mkdir(tempDir, patchID);

        Module module = new Module.Builder(moduleName).
                miscFile(new ResourceItem("resource-test", ("resource patch " + patchID).getBytes())).
                build();

        // create the patch with the updated module
        ContentModification moduleModified = ContentModificationUtils.modifyModule(patchDir, patchElementId, targetHash, module);

        PatchBuilder patchBuilder = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription(randomString());
        if(cp) {
            patchBuilder = patchBuilder.
                    upgradeIdentity(productConfig.getProductName(), productConfig.getProductVersion(), productConfig.getProductVersion() + "_CP" + patchID).
                    getParent().
                    upgradeElement(patchElementId, "base", false).
                    addContentModification(moduleModified).
                    getParent();
        } else {
            patchBuilder = patchBuilder.
                    oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion()).
                    getParent().
                    oneOffPatchElement(patchElementId, "base", false).
                    addContentModification(moduleModified).
                    getParent();
        }
        Patch patch = patchBuilder.build();

        // create the patch
        createPatchXMLFile(patchDir, patch);

        File zippedPatch = createZippedPatchFile(patchDir, patch.getPatchId());

        // apply the patch and check if server is in restart-required mode
        controller.start(CONTAINER);
        try {
            Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath()));
            Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        } finally {
            controller.stop(CONTAINER);
        }
        return moduleModified.getItem().getContentHash();
    }
}
