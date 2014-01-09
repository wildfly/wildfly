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

package org.jboss.as.patching.tests;

import static org.jboss.as.patching.HashUtils.hashFile;
import static org.jboss.as.patching.runner.TestUtils.dump;
import static org.jboss.as.patching.runner.TestUtils.randomString;
import static org.jboss.as.patching.runner.TestUtils.touch;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.jboss.as.patching.PatchingException;
import org.jboss.as.patching.installation.Layer;
import org.jboss.as.patching.validation.PatchingGarbageLocator;
import org.junit.Test;


/**
 * @author Alexey Loubyansky
 *
 */
public class LocatingUnusedArtifactsUnitTestCase extends AbstractPatchingTest {

    static final String[] FILE_ONE = {"bin", "standalone.sh"};
    static final String[] FILE_TWO = {"bin", "standalone.conf"};
    static final String[] FILE_EXISTING = {"bin", "test"};
    private static final String CP_1_ID = "cp1";
    private static final String ONE_OFF_1_ID = "oneOff1";
    private static final String ONE_OFF_2_ID = "oneOff2";

    @Test
    public void testUnpatchedValidation() throws Exception {
        assertNoGarbage();
    }

    @Test
    public void testOneCPValidation() throws Exception {

        final PatchingTestBuilder builder = createDefaultBuilder();
        final byte[] standaloneHash = new byte[20];
        final byte[] moduleHash = new byte[20];

        // Create a file
        final File existing = builder.getFile(FILE_EXISTING);
        touch(existing);
        dump(existing, randomString());

        final byte[] existingHash = hashFile(existing);
        final byte[] initialHash = Arrays.copyOf(existingHash, existingHash.length);

        final PatchingTestStepBuilder cp1 = builder.createStepBuilder();
        final String cp1Id = "CP1";
        cp1.setPatchId(cp1Id)
                .upgradeIdentity(PRODUCT_VERSION, PRODUCT_VERSION)
                .upgradeElement("base-CP1", "base", false)
                .addModuleWithRandomContent("org.jboss.test", moduleHash)
                .getParent()
                .addFileWithRandomContent(standaloneHash, FILE_ONE)
                .updateFileWithRandomContent(initialHash, existingHash, FILE_EXISTING);
        ;
        // Apply CP1
        apply(cp1);

        assertNoGarbage();
    }

    @Test
    public void testOneOffAndCPValidation() throws Exception {

        final PatchingTestBuilder builder = createDefaultBuilder();
        final byte[] standaloneHash = new byte[20];
        final byte[] moduleHash = new byte[20];

        // Create a file
        final File existing = builder.getFile(FILE_EXISTING);
        touch(existing);
        dump(existing, randomString());

        final byte[] existingHash = hashFile(existing);
        final byte[] initialHash = Arrays.copyOf(existingHash, existingHash.length);

        final PatchingTestStepBuilder cp1 = builder.createStepBuilder();
        final String cp1Id = "CP1";
        cp1.setPatchId(cp1Id)
                .upgradeIdentity(PRODUCT_VERSION, PRODUCT_VERSION)
                .upgradeElement("base-CP1", "base", false)
                .addModuleWithRandomContent("org.jboss.test", moduleHash)
                .getParent()
                .addFileWithRandomContent(standaloneHash, FILE_ONE)
                .updateFileWithRandomContent(initialHash, existingHash, FILE_EXISTING);
        ;
        // Apply CP1
        apply(cp1);

        final PatchingTestStepBuilder oneOff1 = builder.createStepBuilder();
        final String oneOff1Id = "oneOff1";
        oneOff1.setPatchId(oneOff1Id)
                .oneOffPatchIdentity(PRODUCT_VERSION)
                .oneOffPatchElement("base-oneOff1", "base", false)
                .updateModuleWithRandomContent("org.jboss.test", moduleHash, null)
                .getParent()
                .updateFileWithRandomContent(standaloneHash, null, FILE_ONE)
                .updateFileWithRandomContent(Arrays.copyOf(existingHash, existingHash.length), existingHash, FILE_EXISTING);
        ;
        // Apply oneOff1
        apply(oneOff1);

        assertNoGarbage();
    }

    @Test
    public void testOneOffCPOneOffValidation() throws Exception {
        installOneOffCpOneOff();
        assertNoGarbage();
    }

    @Test
    public void testRemovedOneOffRollbackXml() throws Exception {
        installOneOffCpOneOff();
        removeRollbackXml(ONE_OFF_1_ID);
        assertNoGarbage();
    }

    @Test
    public void testRemovedCPRollbackXml() throws Exception {

        installOneOffCpOneOff();
        removeRollbackXml(CP_1_ID);

        final PatchingGarbageLocator garbageLocator = PatchingGarbageLocator.getIninitialized(updateInstallationManager());
        final List<File> inactiveHistory = garbageLocator.getInactiveHistory();
        assertEquals(1, inactiveHistory.size());
        assertEquals(getExpectedHistoryDir(ONE_OFF_1_ID), inactiveHistory.get(0).getAbsolutePath());

        final List<File> inactiveOverlays = garbageLocator.getInactiveOverlays();
        assertEquals(1, inactiveOverlays.size());
        assertEquals(getExpectedOverlayDir("base", ONE_OFF_1_ID), inactiveOverlays.get(0).getAbsolutePath());
    }

    @Test
    public void testRemovedLastOneOffRollbackXml() throws Exception {

        installOneOffCpOneOff();
        removeRollbackXml(ONE_OFF_2_ID);

        PatchingGarbageLocator garbageLocator = PatchingGarbageLocator.getIninitialized(updateInstallationManager());
        final List<File> inactiveHistory = garbageLocator.getInactiveHistory();

        List<String> inactivePaths = Arrays.asList(new String[] { getExpectedHistoryDir(ONE_OFF_1_ID) });
        assertEqualPaths(inactivePaths, inactiveHistory);

        final List<File> inactiveOverlays = garbageLocator.getInactiveOverlays();
        inactivePaths = Arrays.asList(new String[]{getExpectedOverlayDir("base", ONE_OFF_1_ID)});
        assertEqualPaths(inactivePaths, inactiveOverlays);

        // test cleaning
        garbageLocator.deleteInactiveContent();
        garbageLocator.reset();
        assertTrue(garbageLocator.getInactiveHistory().isEmpty());
        assertTrue(garbageLocator.getInactiveOverlays().isEmpty());
        assertTrue(new File(getExpectedOverlayDir("base", ONE_OFF_2_ID)).exists());
    }

    @Test
    public void testMultipleLayers() throws Exception {

        final PatchingTestBuilder builder = createDefaultBuilder("layer2", "layer1", "base");

        final byte[] standaloneHash = new byte[20];
        final byte[] moduleHash = new byte[20];

        final PatchingTestStepBuilder cp1 = builder.createStepBuilder();
        cp1.setPatchId("CP1")
                .upgradeIdentity(PRODUCT_VERSION, PRODUCT_VERSION)
                .upgradeElement("base-CP1", "base", false)
                .addModuleWithRandomContent("org.jboss.test", moduleHash)
                .getParent()
                .addFileWithRandomContent(standaloneHash, FILE_ONE);
        // Apply CP1
        apply(cp1);

        final PatchingTestStepBuilder cp2 = builder.createStepBuilder();
        cp2.setPatchId("CP2")
                .upgradeIdentity(PRODUCT_VERSION, PRODUCT_VERSION)
                .upgradeElement("layer2-CP2", "layer2", false)
                    .addModuleWithRandomContent("org.jboss.test", null)
                    .getParent()
                .upgradeElement("layer1-CP2", "layer1", false)
                    .addModuleWithRandomContent("org.jboss.test", null)
                    .getParent()
                .updateFileWithRandomContent(Arrays.copyOf(standaloneHash, standaloneHash.length), standaloneHash, FILE_ONE);
        // Apply CP2
        apply(cp2);

        final PatchingTestStepBuilder cp3 = builder.createStepBuilder();
        cp3.setPatchId("CP3")
                .upgradeIdentity(PRODUCT_VERSION, PRODUCT_VERSION)
                .removeFile(Arrays.copyOf(standaloneHash, standaloneHash.length), FILE_ONE);
        // Apply CP3
        apply(cp3);

        removeRollbackXml("CP3");

        PatchingGarbageLocator garbageLocator = PatchingGarbageLocator.getIninitialized(updateInstallationManager());
        final List<File> inactiveHistory = garbageLocator.getInactiveHistory();
        List<String> inactivePaths = Arrays.asList(new String[]{getExpectedHistoryDir("CP1"), getExpectedHistoryDir("CP2")});
        assertEqualPaths(inactivePaths, inactiveHistory);

        final List<File> inactiveOverlays = garbageLocator.getInactiveOverlays();
        inactivePaths = Arrays.asList(new String[]{getExpectedOverlayDir("layer2", "CP2"),
                getExpectedOverlayDir( "layer1", "CP2"), getExpectedOverlayDir("base", "CP1")});
        assertEqualPaths(inactivePaths, inactiveOverlays);

        // test cleaning
        garbageLocator.deleteInactiveContent();
        garbageLocator.reset();
        assertTrue(garbageLocator.getInactiveHistory().isEmpty());
        assertTrue(garbageLocator.getInactiveOverlays().isEmpty());

    }

    @Test
    public void testUnpatchedWitGarbage() throws Exception {

        createDefaultBuilder("layer2", "layer1", "base");

        final List<String> historyGarbage = Arrays.asList(new String[]{getExpectedHistoryDir("CP1"), getExpectedHistoryDir("CP2")});
        for(int i = 0; i < historyGarbage.size(); ++i) {
            new File(historyGarbage.get(i)).mkdirs();
        }

        final List<String> overlayGarbage = Arrays.asList(new String[]{getExpectedOverlayDir("layer2", "CP2"),
                getExpectedOverlayDir("layer1", "CP2"), getExpectedOverlayDir("base", "CP1")});
        for(int i = 0; i < overlayGarbage.size(); ++i) {
            new File(overlayGarbage.get(i)).mkdirs();
        }

        PatchingGarbageLocator garbageLocator = PatchingGarbageLocator.getIninitialized(loadInstallationManager());
        final List<File> inactiveHistory = garbageLocator.getInactiveHistory();
        assertEqualPaths(historyGarbage, inactiveHistory);

        final List<File> inactiveOverlays = garbageLocator.getInactiveOverlays();
        assertEqualPaths(overlayGarbage, inactiveOverlays);

        garbageLocator.deleteInactiveContent();
        garbageLocator.reset();
        assertTrue(garbageLocator.getInactiveHistory().isEmpty());
        assertTrue(garbageLocator.getInactiveOverlays().isEmpty());
    }

    protected void assertEqualPaths(List<String> expected, final List<File> actual) {
        assertEquals(expected.size(), actual.size());
        for(int i = 0; i < expected.size(); ++i) {
            assertTrue(expected.contains(actual.get(i).getAbsolutePath()));
        }
    }

    protected void removeRollbackXml(String patchId) throws IOException {
        final File oneOff1History = updateInstallationManager().getInstalledImage().getPatchHistoryDir(patchId);
        assertTrue(oneOff1History.exists());
        final File oneOff1RollbackXml = new File(oneOff1History, "rollback.xml");
        assertTrue(oneOff1RollbackXml.exists());
        assertTrue(oneOff1RollbackXml.delete());
        assertFalse(oneOff1RollbackXml.exists());
    }

    protected String getExpectedOverlayDir(String layerName, final String patchId) throws IOException {
        final Layer layer = updateInstallationManager().getLayer(layerName);
        if(layer == null) {
            fail("No layer " + layerName);
        }
        return layer.getDirectoryStructure().getModulePatchDirectory(layerName + "-" + patchId).getAbsolutePath();
    }

    protected String getExpectedHistoryDir(final String patchId) throws IOException {
        return updateInstallationManager().getInstalledImage().getPatchesDir().getAbsolutePath()
                + File.separator + patchId;
    }

    protected void installOneOffCpOneOff()
            throws IOException, PatchingException {
        final PatchingTestBuilder builder = createDefaultBuilder();
        final byte[] standaloneHash = new byte[20];
        final byte[] moduleHash = new byte[20];

        // Create a file
        final File existing = builder.getFile(FILE_EXISTING);
        touch(existing);
        dump(existing, randomString());

        final byte[] existingHash = hashFile(existing);
        final byte[] initialHash = Arrays.copyOf(existingHash, existingHash.length);

        final PatchingTestStepBuilder oneOff1 = builder.createStepBuilder();
        oneOff1.setPatchId(ONE_OFF_1_ID)
                .oneOffPatchIdentity(PRODUCT_VERSION)
                .oneOffPatchElement("base-" + ONE_OFF_1_ID, "base", false)
                .addModuleWithRandomContent("org.jboss.test", moduleHash)
                .getParent()
                .addFileWithRandomContent(standaloneHash, FILE_ONE)
                .updateFileWithRandomContent(Arrays.copyOf(existingHash, existingHash.length), existingHash, FILE_EXISTING);
        ;
        // Apply oneOff1
        apply(oneOff1);

        final PatchingTestStepBuilder cp1 = builder.createStepBuilder();
        cp1.setPatchId(CP_1_ID)
                .upgradeIdentity(PRODUCT_VERSION, PRODUCT_VERSION)
                .upgradeElement("base-" + CP_1_ID, "base", false)
                .updateModuleWithRandomContent("org.jboss.test", Arrays.copyOf(moduleHash, moduleHash.length), moduleHash)
                .getParent()
                .updateFileWithRandomContent(Arrays.copyOf(standaloneHash, standaloneHash.length), standaloneHash, FILE_ONE)
                .updateFileWithRandomContent(initialHash, existingHash, FILE_EXISTING);
        ;
        // Apply CP1
        apply(cp1);

        final PatchingTestStepBuilder oneOff2 = builder.createStepBuilder();
        oneOff2.setPatchId(ONE_OFF_2_ID)
                .oneOffPatchIdentity(PRODUCT_VERSION)
                .oneOffPatchElement("base-" + ONE_OFF_2_ID, "base", false)
                .updateModuleWithRandomContent("org.jboss.test", moduleHash, null)
                .getParent()
                .updateFileWithRandomContent(standaloneHash, null, FILE_ONE)
                .updateFileWithRandomContent(Arrays.copyOf(existingHash, existingHash.length), existingHash, FILE_EXISTING);
        ;
        // Apply oneOff1
        apply(oneOff2);
    }

    protected void assertNoGarbage() throws Exception {
        final PatchingGarbageLocator garbageLocator = PatchingGarbageLocator.getIninitialized(loadInstallationManager());
        List<File> inactiveHistory = garbageLocator.getInactiveHistory();
        assertTrue(inactiveHistory.toString(), inactiveHistory.isEmpty());
        assertTrue(garbageLocator.getInactiveOverlays().toString(), garbageLocator.getInactiveOverlays().isEmpty());
    }

}
