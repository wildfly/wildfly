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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.jboss.as.patching.PatchingException;
import org.jboss.as.patching.installation.InstallationManager;
import org.jboss.as.patching.installation.Layer;
import org.jboss.as.patching.metadata.Patch.PatchType;
import org.jboss.as.patching.validation.ArtifactStateHandler;
import org.jboss.as.patching.validation.PatchingHistoryTree;
import org.jboss.as.patching.validation.PatchingHistoryTree.Builder;
import org.jboss.as.patching.validation.Context;
import org.jboss.as.patching.validation.ErrorHandler;
import org.jboss.as.patching.validation.PatchArtifact;
import org.jboss.as.patching.validation.PatchElementProviderArtifact;
import org.jboss.as.patching.validation.PatchHistoryDir;
import org.jboss.as.patching.validation.PatchingGarbageLocator;
import org.jboss.as.patching.validation.PatchingHistoryRoot;
import org.jboss.as.patching.validation.PatchingHistoryTree.TreeIterator;
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
    public void testTreeHandleAll() throws Exception {

        installOneOffCpOneOff();
        final Context ctx = getContext();

        final List<String> historyDirs = new ArrayList<String>();
        final List<String> moduleDirs = new ArrayList<String>();
        final List<String> bundleDirs = new ArrayList<String>();
        PatchingHistoryTree activeDirsHandler = Builder.getInstance()
                .addHandler(PatchHistoryDir.getInstance(), new ArtifactStateHandler<PatchHistoryDir.State>(){
                    @Override
                    public void handle(Context ctx, PatchHistoryDir.State state) {
                        historyDirs.add(state.getDirectory().getName());
                    }})
                .addHandler(PatchElementProviderArtifact.getInstance(), new ArtifactStateHandler<PatchElementProviderArtifact.State>() {
                    @Override
                    public void handle(Context ctx, PatchElementProviderArtifact.State state) {
                        if(state.getBundlesDir() != null) {
                            bundleDirs.add(state.getBundlesDir().getName());
                        }
                        if(state.getModulesDir() != null) {
                            moduleDirs.add(state.getModulesDir().getName());
                        }
                    }})
                .build();
        activeDirsHandler.handleAll(ctx);

        assertEquals(Arrays.asList(new String[]{"oneOff2", "cp1", "oneOff1"}), historyDirs);
        assertEquals(Arrays.asList(new String[]{"base-oneOff2", "base-cp1", "base-oneOff1"}), moduleDirs);
        assertTrue(bundleDirs.isEmpty());
    }

    @Test
    public void testTreeIterator() throws Exception {

        installOneOffCpOneOff();
        final Context ctx = getContext();

        final List<String> historyDirs = new ArrayList<String>();
        final List<String> moduleDirs = new ArrayList<String>();
        final List<String> bundleDirs = new ArrayList<String>();
        PatchingHistoryTree activeDirsHandler = Builder.getInstance()
                .addHandler(PatchHistoryDir.getInstance(), new ArtifactStateHandler<PatchHistoryDir.State>(){
                    @Override
                    public void handle(Context ctx, PatchHistoryDir.State state) {
                        historyDirs.add(state.getDirectory().getName());
                    }})
                .addHandler(PatchElementProviderArtifact.getInstance(), new ArtifactStateHandler<PatchElementProviderArtifact.State>() {
                    @Override
                    public void handle(Context ctx, PatchElementProviderArtifact.State state) {
                        if(state.getBundlesDir() != null) {
                            bundleDirs.add(state.getBundlesDir().getName());
                        }
                        if(state.getModulesDir() != null) {
                            moduleDirs.add(state.getModulesDir().getName());
                        }
                    }})
                .build();

        final TreeIterator tree = activeDirsHandler.treeIterator(ctx);

        assertTrue(tree.hasNext());
        assertTrue(historyDirs.isEmpty());
        assertTrue(moduleDirs.isEmpty());
        assertTrue(bundleDirs.isEmpty());

        tree.handleNext();
        assertTrue(historyDirs.contains("oneOff2"));
        assertTrue(moduleDirs.isEmpty());
        assertTrue(bundleDirs.isEmpty());

        assertTrue(tree.hasNext());
        tree.handleNext();
        assertEquals(1, historyDirs.size());
        assertTrue(moduleDirs.contains("base-oneOff2"));
        assertTrue(bundleDirs.isEmpty());

        assertTrue(tree.hasNext());
        tree.handleNext();
        assertTrue(historyDirs.contains("cp1"));
        assertEquals(1, moduleDirs.size());
        assertTrue(bundleDirs.isEmpty());

        assertTrue(tree.hasNext());
        tree.handleNext();
        assertEquals(2, historyDirs.size());
        assertTrue(moduleDirs.contains("base-cp1"));
        assertTrue(bundleDirs.isEmpty());

        assertTrue(tree.hasNext());
        tree.handleNext();
        assertTrue(historyDirs.contains("oneOff1"));
        assertEquals(2, moduleDirs.size());
        assertTrue(bundleDirs.isEmpty());

        assertTrue(tree.hasNext());
        tree.handleNext();
        assertEquals(3, historyDirs.size());
        assertTrue(moduleDirs.contains("base-oneOff1"));
        assertTrue(bundleDirs.isEmpty());

        assertFalse(tree.hasNext());
    }

    @Test
    public void testHistoryDirStateIterator() throws Exception {

        installOneOffCpOneOff();
        final Context ctx = getContext();

        final Iterator<PatchHistoryDir.State> historyDir = PatchingHistoryTree.stateIterator(PatchHistoryDir.getInstance(), ctx);

        assertTrue(historyDir.hasNext());
        PatchHistoryDir.State hdir = historyDir.next();
        assertNotNull(hdir);
        File dir = hdir.getDirectory();
        assertNotNull(dir);
        assertEquals("oneOff2", dir.getName());

        assertTrue(historyDir.hasNext());
        hdir = historyDir.next();
        assertNotNull(hdir);
        dir = hdir.getDirectory();
        assertNotNull(dir);
        assertEquals("cp1", dir.getName());

        assertTrue(historyDir.hasNext());
        hdir = historyDir.next();
        assertNotNull(hdir);
        dir = hdir.getDirectory();
        assertNotNull(dir);
        assertEquals("oneOff1", dir.getName());

        assertFalse(historyDir.hasNext());
    }

    @Test
    public void testUnpatchedValidation() throws Exception {

        final Context ctx = getContext();
        PatchingHistoryRoot.State history = PatchingHistoryRoot.getInstance().getState(ctx);
        final PatchArtifact.State patch = history.getLastAppliedPatch(ctx);
        assertNull(patch);
        assertNoGarbage(ctx);
    }

    @Test
    public void testOneCPValidattion() throws Exception {

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

        final Context ctx = getContext();
        PatchingHistoryRoot.State history = PatchingHistoryRoot.getInstance().getState(ctx);
        final PatchArtifact.State patch = history.getLastAppliedPatch(ctx);
        assertPatch(ctx, patch, cp1Id, PatchType.CUMULATIVE);

        assertNoPrevious(ctx, patch);
        assertNoGarbage(ctx);
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

        final Context ctx = getContext();

        PatchingHistoryRoot.State history = PatchingHistoryRoot.getInstance().getState(ctx);
        PatchArtifact.State patch = history.getLastAppliedPatch(ctx);
        assertPatch(ctx, patch, oneOff1Id, PatchType.ONE_OFF);

        assertTrue(patch.hasPrevious(ctx));
        patch = patch.getPrevious(ctx);
        assertPatch(ctx, patch, cp1Id, PatchType.CUMULATIVE);

        assertNoPrevious(ctx, patch);
        assertNoGarbage(ctx);
    }

    @Test
    public void testOneOffCPOneOffValidation() throws Exception {

        installOneOffCpOneOff();

        final Context ctx = getContext();

        PatchingHistoryRoot.State history = PatchingHistoryRoot.getInstance().getState(ctx);
        PatchArtifact.State patch = history.getLastAppliedPatch(ctx);

        assertPatch(ctx, patch, ONE_OFF_2_ID, PatchType.ONE_OFF);

        assertTrue(patch.hasPrevious(ctx));
        patch = patch.getPrevious(ctx);
        assertPatch(ctx, patch, CP_1_ID, PatchType.CUMULATIVE);

        assertTrue(patch.hasPrevious(ctx));
        patch = patch.getPrevious(ctx);
        assertPatch(ctx, patch, ONE_OFF_1_ID, PatchType.ONE_OFF);

        assertNoPrevious(ctx, patch);
        assertNoGarbage(ctx);
    }

    @Test
    public void testRemovedOneOffRollbackXml() throws Exception {

        installOneOffCpOneOff();

        final Context ctx = getContext(false);
        removeRollbackXml(ctx, ONE_OFF_1_ID);

        PatchingHistoryRoot.State history = PatchingHistoryRoot.getInstance().getState(ctx);
        PatchArtifact.State patch = history.getLastAppliedPatch(ctx);
        assertPatch(ctx, patch, ONE_OFF_2_ID, PatchType.ONE_OFF);

        assertTrue(patch.hasPrevious(ctx));
        patch = patch.getPrevious(ctx);
        assertPatch(ctx, patch, CP_1_ID, PatchType.CUMULATIVE);

        assertTrue(patch.hasPrevious(ctx));
        patch = patch.getPrevious(ctx);
        assertNotNull(patch);
        assertEquals(ONE_OFF_1_ID, patch.getPatchId());
        assertEquals(PatchType.ONE_OFF, patch.getType());
        assertHistoryExists(getExpectedHistoryDir(ctx, ONE_OFF_1_ID), ctx, patch, true, true, false);

        assertNoPrevious(ctx, patch);
        assertNoGarbage(ctx);
    }

    @Test
    public void testRemovedCPRollbackXml() throws Exception {

        installOneOffCpOneOff();

        final Context ctx = getContext(false);
        removeRollbackXml(ctx, CP_1_ID);

        PatchingHistoryRoot.State history = PatchingHistoryRoot.getInstance().getState(ctx);
        PatchArtifact.State patch = history.getLastAppliedPatch(ctx);
        assertPatch(ctx, patch, ONE_OFF_2_ID, PatchType.ONE_OFF);

        assertTrue(patch.hasPrevious(ctx));
        patch = patch.getPrevious(ctx);
        assertNotNull(patch);
        assertEquals(CP_1_ID, patch.getPatchId());
        assertEquals(PatchType.CUMULATIVE, patch.getType());
        assertHistoryExists(getExpectedHistoryDir(ctx, CP_1_ID), ctx, patch, true, true, false);

        assertNoPrevious(ctx, patch);

        final PatchingGarbageLocator garbageLocator = new PatchingGarbageLocator(ctx.getInstallationManager());
        final List<File> inactiveHistory = garbageLocator.getInactiveHistory();
        assertEquals(1, inactiveHistory.size());
        assertEquals(getExpectedHistoryDir(ctx, ONE_OFF_1_ID), inactiveHistory.get(0).getAbsolutePath());

        final List<File> inactiveOverlays = garbageLocator.getInactiveOverlays();
        assertEquals(1, inactiveOverlays.size());
        assertEquals(getExpectedOverlayDir(ctx, "base", ONE_OFF_1_ID), inactiveOverlays.get(0).getAbsolutePath());
    }

    @Test
    public void testRemovedLastOneOffRollbackXml() throws Exception {

        installOneOffCpOneOff();

        final Context ctx = getContext(false);
        removeRollbackXml(ctx, ONE_OFF_2_ID);

        PatchingHistoryRoot.State history = PatchingHistoryRoot.getInstance().getState(ctx);
        PatchArtifact.State patch = history.getLastAppliedPatch(ctx);
        assertNotNull(patch);
        assertEquals(ONE_OFF_2_ID, patch.getPatchId());
        assertEquals(PatchType.ONE_OFF, patch.getType());
        assertHistoryExists(getExpectedHistoryDir(ctx, ONE_OFF_2_ID), ctx, patch, true, true, false);

        assertNoPrevious(ctx, patch);

        PatchingGarbageLocator garbageLocator = new PatchingGarbageLocator(ctx.getInstallationManager());
        final List<File> inactiveHistory = garbageLocator.getInactiveHistory();

        List<String> inactivePaths = Arrays.asList(new String[]{getExpectedHistoryDir(ctx, CP_1_ID), getExpectedHistoryDir(ctx, ONE_OFF_1_ID)});
        assertEqualPaths(inactivePaths, inactiveHistory);

        final List<File> inactiveOverlays = garbageLocator.getInactiveOverlays();
        inactivePaths = Arrays.asList(new String[]{getExpectedOverlayDir(ctx, "base", CP_1_ID), getExpectedOverlayDir(ctx, "base", ONE_OFF_1_ID)});
        assertEqualPaths(inactivePaths, inactiveOverlays);

        // test cleaning
        garbageLocator.deleteInactiveContent();
        garbageLocator.reset();
        assertTrue(garbageLocator.getInactiveHistory().isEmpty());
        assertTrue(garbageLocator.getInactiveOverlays().isEmpty());

        // validate active patching content
        history = PatchingHistoryRoot.getInstance().getState(ctx);
        patch = history.getLastAppliedPatch(ctx);
        assertNotNull(patch);
        assertEquals(ONE_OFF_2_ID, patch.getPatchId());
        assertEquals(PatchType.ONE_OFF, patch.getType());
        assertHistoryExists(getExpectedHistoryDir(ctx, ONE_OFF_2_ID), ctx, patch, true, true, false);
        assertNoPrevious(ctx, patch);
        assertTrue(new File(getExpectedOverlayDir(ctx, "base", ONE_OFF_2_ID)).exists());
    }

    @Test
    public void testMaltipleLayers() throws Exception {

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

        final Context ctx = getContext(false);
        removeRollbackXml(ctx, "CP3");

        PatchingGarbageLocator garbageLocator = new PatchingGarbageLocator(ctx.getInstallationManager());
        final List<File> inactiveHistory = garbageLocator.getInactiveHistory();
        List<String> inactivePaths = Arrays.asList(new String[]{getExpectedHistoryDir(ctx, "CP1"), getExpectedHistoryDir(ctx, "CP2")});
        assertEqualPaths(inactivePaths, inactiveHistory);

        final List<File> inactiveOverlays = garbageLocator.getInactiveOverlays();
        inactivePaths = Arrays.asList(new String[]{getExpectedOverlayDir(ctx, "layer2", "CP2"),
                getExpectedOverlayDir(ctx, "layer1", "CP2"), getExpectedOverlayDir(ctx, "base", "CP1")});
        assertEqualPaths(inactivePaths, inactiveOverlays);

        // test cleaning
        garbageLocator.deleteInactiveContent();
        garbageLocator.reset();
        assertTrue(garbageLocator.getInactiveHistory().isEmpty());
        assertTrue(garbageLocator.getInactiveOverlays().isEmpty());

        PatchingHistoryRoot.State history = PatchingHistoryRoot.getInstance().getState(ctx);
        PatchArtifact.State patch = history.getLastAppliedPatch(ctx);
        assertNotNull(patch);
        assertEquals("CP3", patch.getPatchId());
        assertEquals(PatchType.CUMULATIVE, patch.getType());
        assertHistoryExists(getExpectedHistoryDir(ctx, "CP3"), ctx, patch, true, true, false);
        assertNoPrevious(ctx, patch);
    }

    @Test
    public void testUnpatchedWitGarbage() throws Exception {

        createDefaultBuilder("layer2", "layer1", "base");

        final Context ctx = getContext(false);
        final List<String> historyGarbage = Arrays.asList(new String[]{getExpectedHistoryDir(ctx, "CP1"), getExpectedHistoryDir(ctx, "CP2")});
        for(int i = 0; i < historyGarbage.size(); ++i) {
            new File(historyGarbage.get(i)).mkdirs();
        }

        final List<String> overlayGarbage = Arrays.asList(new String[]{getExpectedOverlayDir(ctx, "layer2", "CP2"),
                getExpectedOverlayDir(ctx, "layer1", "CP2"), getExpectedOverlayDir(ctx, "base", "CP1")});
        for(int i = 0; i < overlayGarbage.size(); ++i) {
            new File(overlayGarbage.get(i)).mkdirs();
        }

        PatchingGarbageLocator garbageLocator = new PatchingGarbageLocator(loadInstallationManager());
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

    protected void removeRollbackXml(final Context ctx, String patchId) {
        final File oneOff1History = ctx.getInstallationManager().getInstalledImage().getPatchHistoryDir(patchId);
        assertTrue(oneOff1History.exists());
        final File oneOff1RollbackXml = new File(oneOff1History, "rollback.xml");
        assertTrue(oneOff1RollbackXml.exists());
        assertTrue(oneOff1RollbackXml.delete());
        assertFalse(oneOff1RollbackXml.exists());
    }

    protected String getExpectedOverlayDir(final Context ctx, String layerName, final String patchId) {
        final Layer layer = ctx.getInstallationManager().getLayer(layerName);
        if(layer == null) {
            fail("No layer " + layerName);
        }
        return layer.getDirectoryStructure().getModulePatchDirectory(layerName + "-" + patchId).getAbsolutePath();
    }

    protected String getExpectedHistoryDir(final Context ctx, final String patchId) {
        return ctx.getInstallationManager().getInstalledImage().getPatchesDir().getAbsolutePath()
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

    protected void assertNoGarbage(final Context ctx) {
        final PatchingGarbageLocator garbageLocator = new PatchingGarbageLocator(ctx.getInstallationManager());
        List<File> inactiveHistory = garbageLocator.getInactiveHistory();
        assertTrue(inactiveHistory.toString(), inactiveHistory.isEmpty());
        assertTrue(garbageLocator.getInactiveOverlays().toString(), garbageLocator.getInactiveOverlays().isEmpty());
    }

    protected void assertNoPrevious(final Context ctx, PatchArtifact.State patch) {
        assertFalse(patch.hasPrevious(ctx));
        assertNull(patch.getPrevious(ctx));
    }

    protected void assertPatch(Context ctx, PatchArtifact.State patch, String patchId, PatchType patchType) {
        assertNotNull(patch);
        assertEquals(patchId, patch.getPatchId());
        assertEquals(patchType, patch.getType());

        assertHistoryExists(getExpectedHistoryDir(ctx, patchId), ctx, patch, true, true, true);
    }

    protected void assertHistoryExists(String expectedHistoryPath, Context ctx, PatchArtifact.State patch,
            boolean dirExists, boolean patchXmlExists, boolean rollbackXmlExists) {
        final PatchHistoryDir.State patchHistoryDir = patch.getHistoryDir(ctx);
        assertNotNull(patchHistoryDir);
        assertEquals(expectedHistoryPath, patchHistoryDir.getDirectory().getAbsolutePath());
        assertEquals(dirExists, patchHistoryDir.getDirectory().exists());
        assertEquals(patchXmlExists, patchHistoryDir.getPatchXml(ctx).getFile().exists());
        assertEquals(rollbackXmlExists, patchHistoryDir.getRollbackXml(ctx).getFile().exists());
    }

    private Context getContext() throws IOException {
        return getContext(true);
    }

    private Context getContext(final boolean failOnError) throws IOException {
        final InstallationManager manager = loadInstallationManager();
        return new Context() {

            @Override
            public InstallationManager getInstallationManager() {
                return manager;
            }

            @Override
            public ErrorHandler getErrorHandler() {
                return new ErrorHandler(){
                    @Override
                    public void error(String msg) {
                        if(failOnError) {
                            fail(msg);
                        }
                    }

                    @Override
                    public void error(String msg, Throwable t) {
                        if(failOnError) {
                            t.printStackTrace();
                            fail(msg);
                        }
                    }};
            }};
    }
}
