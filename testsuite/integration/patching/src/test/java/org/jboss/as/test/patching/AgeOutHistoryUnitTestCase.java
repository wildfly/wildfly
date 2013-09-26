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
import static org.jboss.as.patching.IoUtils.newFile;
import static org.jboss.as.test.patching.PatchingTestUtil.CONTAINER;
import static org.jboss.as.test.patching.PatchingTestUtil.MODULES_PATH;
import static org.jboss.as.test.patching.PatchingTestUtil.createPatchXMLFile;
import static org.jboss.as.test.patching.PatchingTestUtil.createZippedPatchFile;
import static org.jboss.as.test.patching.PatchingTestUtil.randomString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
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
import org.junit.After;
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
public class AgeOutHistoryUnitTestCase extends AbstractPatchingTestCase {

    static final File ROOT = new File(PatchingTestUtil.AS_DISTRIBUTION);

    protected ProductConfig productConfig;
    protected ModelControllerClient client;

    @Before
    public void setup() throws Exception {
        productConfig = new ProductConfig(PatchingTestUtil.PRODUCT, PatchingTestUtil.AS_VERSION, "main");
    }

    @After
    public void tearDown() throws Exception {
        if(client != null) {
            try {
                client.close();
            } catch(Exception e) {}
        }
    }

    @Override
    protected void rollbackAllPatches() {
        final File home = new File(PatchingTestUtil.AS_DISTRIBUTION);
        PatchingTestUtil.resetInstallationState(home, PatchingTestUtil.BASE_MODULE_DIRECTORY);
    }

    @Test
    public void testUnpatched() throws Exception {
        controller.start(CONTAINER);
        try {
            client = createClient();
            assertUnpatched(client);
            ageoutHistory(client);
            assertUnpatched(client);
        } finally {
            controller.stop(CONTAINER);
        }
    }

    @Test
    public void testOneCP() throws Exception {

        Module module = new Module.Builder("module-test").
                miscFile(new ResourceItem("resource-test", ("module resource").getBytes())).
                build();
        File moduleDir = module.writeToDisk(new File(MODULES_PATH));

        byte[] targetHash = HashUtils.hashFile(moduleDir);
        targetHash = applyCP("cp1", targetHash);

        controller.start(CONTAINER);
        try {
            client = createClient();
            assertPatched(client, new String[]{"cp1"}, new boolean[]{true});
            ageoutHistory(client);
            assertPatched(client, new String[]{"cp1"}, new boolean[]{true});
        } finally {
            controller.stop(CONTAINER);
        }
    }

    @Test
    public void testOneOffsOnly() throws Exception {

        Module module = new Module.Builder("module-test").
                miscFile(new ResourceItem("resource-test", ("module resource").getBytes())).
                build();
        File moduleDir = module.writeToDisk(new File(MODULES_PATH));

        byte[] targetHash = HashUtils.hashFile(moduleDir);
        targetHash = applyOneOff("oneoff1", targetHash);
        targetHash = applyOneOff("oneoff2", targetHash);

        controller.start(CONTAINER);
        try {
            client = createClient();
            assertPatched(client, new String[]{"oneoff2", "oneoff1"}, new boolean[]{false, false});
            ageoutHistory(client);
            assertPatched(client, new String[]{"oneoff2", "oneoff1"}, new boolean[]{false, false});
        } finally {
            controller.stop(CONTAINER);
        }
    }

    @Test
    public void testCPWithOneOffs() throws Exception {

        Module module = new Module.Builder("module-test").
                miscFile(new ResourceItem("resource-test", ("module resource").getBytes())).
                build();
        File moduleDir = module.writeToDisk(new File(MODULES_PATH));

        byte[] targetHash = HashUtils.hashFile(moduleDir);
        targetHash = applyCP("cp1", targetHash);
        targetHash = applyOneOff("oneoff1", targetHash);
        targetHash = applyOneOff("oneoff2", targetHash);

        controller.start(CONTAINER);
        try {
            client = createClient();
            assertPatched(client, new String[]{"oneoff2", "oneoff1", "cp1"}, new boolean[]{false, false, true});
            ageoutHistory(client);
            assertPatched(client, new String[]{"oneoff2", "oneoff1", "cp1"}, new boolean[]{false, false, true});
        } finally {
            controller.stop(CONTAINER);
        }
    }

    @Test
    public void testCPBeforeCPWithOneOffs() throws Exception {

        Module module = new Module.Builder("module-test").
                miscFile(new ResourceItem("resource-test", ("module resource").getBytes())).
                build();
        File moduleDir = module.writeToDisk(new File(MODULES_PATH));

        byte[] targetHash = HashUtils.hashFile(moduleDir);
        targetHash = applyCP("cp0", targetHash);
        targetHash = applyCP("cp1", targetHash);
        targetHash = applyOneOff("oneoff1", targetHash);
        targetHash = applyOneOff("oneoff2", targetHash);

        controller.start(CONTAINER);
        try {
            client = createClient();
            assertPatched(client,
                    new String[]{"oneoff2", "oneoff1", "cp1", "cp0"},
                    new boolean[]{false, false, true, true});
            ageoutHistory(client);
            assertCleanedUp("cp0");
            assertPatched(client,
                    new String[]{"oneoff2", "oneoff1", "cp1", "cp0"},
                    new boolean[]{false, false, true, true});
        } finally {
            controller.stop(CONTAINER);
        }
    }

    @Test
    public void testCPWithOneOffsBeforeCPWithOneOffs() throws Exception {

        Module module = new Module.Builder("module-test").
                miscFile(new ResourceItem("resource-test", ("module resource").getBytes())).
                build();
        File moduleDir = module.writeToDisk(new File(MODULES_PATH));

        byte[] targetHash = HashUtils.hashFile(moduleDir);
        targetHash = applyOneOff("oneoff1", targetHash);
        targetHash = applyOneOff("oneoff2", targetHash);
        targetHash = applyCP("cp1", targetHash);
        targetHash = applyOneOff("oneoff3", targetHash);
        targetHash = applyOneOff("oneoff4", targetHash);
        targetHash = applyCP("cp2", targetHash);
        targetHash = applyOneOff("oneoff5", targetHash);
        targetHash = applyOneOff("oneoff6", targetHash);

        controller.start(CONTAINER);
        try {
            client = createClient();
            assertPatched(client,
                    new String[]{"oneoff6", "oneoff5", "cp2", "oneoff4", "oneoff3", "cp1", "oneoff2", "oneoff1"},
                    new boolean[]{false, false, true, false, false, true, false, false});
            ageoutHistory(client);
            assertCleanedUp("oneoff1", "oneoff2", "cp1", "oneoff3", "oneoff4");
            assertPatched(client,
                    new String[]{"oneoff6", "oneoff5", "cp2", "oneoff4", "oneoff3", "cp1", "oneoff2", "oneoff1"},
                    new boolean[]{false, false, true, false, false, true, false, false});
        } finally {
            controller.stop(CONTAINER);
        }
    }

    protected void assertPatched(ModelControllerClient client, String[] ids, boolean[] isCumulative) throws UnknownHostException, IOException {
        final ModelNode response = readHistory(client);
        assertTrue(response.has("outcome"));
        assertEquals("success", response.get("outcome").asString());
        assertTrue(response.has("result"));
        final List<ModelNode> list = response.get("result").asList();
        if(ids == null || ids.length == 0) {
            assertTrue(list.isEmpty());
        } else {
            assertEquals(list.toString(), ids.length, list.size());
            for(int i = 0; i < ids.length; ++i) {
                final ModelNode patch = list.get(i);
                assertEquals(ids[i], patch.get("patch-id").asString());
                final String type = isCumulative[i] ? Patch.PatchType.CUMULATIVE.getName() : Patch.PatchType.ONE_OFF.getName();
                assertEquals(type, patch.get("type").asString());
            }
        }
    }

    protected void assertUnpatched(ModelControllerClient client) throws UnknownHostException, IOException {
        final ModelNode response = readHistory(client);
        assertTrue(response.has("outcome"));
        assertEquals("success", response.get("outcome").asString());
        assertTrue(response.has("result"));
        final List<ModelNode> list = response.get("result").asList();
        assertTrue(list.isEmpty());
    }

    protected void ageoutHistory(ModelControllerClient client) throws UnknownHostException, IOException {
        final ModelNode op = new ModelNode();
        op.get("address").add("core-service", "patching");
        op.get("operation").set("ageout-history");
        final ModelNode response = client.execute(op);
        assertTrue(response.has("outcome"));
        assertEquals("success", response.get("outcome").asString());
    }

    protected ModelControllerClient createClient() throws UnknownHostException {
        return ModelControllerClient.Factory.create(TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort());
    }

    protected ModelNode readHistory(ModelControllerClient client) throws UnknownHostException, IOException {
        final ModelNode op = new ModelNode();
        op.get("address").add("core-service", "patching");
        op.get("operation").set("show-history");
        return client.execute(op);
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

    protected void assertCleanedUp(final String... patches) {

        final File base = newFile(PatchingTestUtil.BASE_MODULE_DIRECTORY, "system", "layers", "base");
        final File overlays = new File(base, ".overlays");
        for (final String patch : patches) {
            final File overlay = new File(overlays, patch);
            Assert.assertFalse(overlay.exists());
        }

        final File installation = newFile(ROOT, ".installation", "patches");
        for (final String patch : patches) {
            final File history = new File(installation, patch);
            Assert.assertTrue(history.exists());
            Assert.assertTrue(newFile(history, "patch.xml").exists());
            Assert.assertTrue(newFile(history, "rollback.xml").exists());
            Assert.assertEquals(2, history.list().length);
        }

    }

}
