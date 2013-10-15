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
import static org.jboss.as.patching.runner.TestUtils.createModule0;
import static org.jboss.as.patching.runner.TestUtils.dump;
import static org.jboss.as.patching.runner.TestUtils.randomString;
import static org.jboss.as.patching.runner.TestUtils.touch;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.jboss.as.patching.PatchingException;
import org.jboss.as.patching.installation.InstalledIdentity;
import org.jboss.as.patching.runner.PatchingAssert;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Emanuel Muckenhuber
 */
public class BasicHistoryUnitTestCase extends AbstractPatchingTest {

    static final String[] FILE_ONE = {"bin", "standalone.sh"};
    static final String[] FILE_TWO = {"bin", "standalone.conf"};
    static final String[] FILE_EXISTING = {"bin", "test"};

    @Test
    public void testBasicPatchHistory() throws IOException, PatchingException {

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
        cp1.setPatchId("CP1")
                .upgradeIdentity(PRODUCT_VERSION, PRODUCT_VERSION)
                .upgradeElement("base-CP1", "base", false)
                .addModuleWithRandomContent("org.jboss.test", moduleHash)
                .getParent()
                .addFileWithRandomContent(standaloneHash, FILE_ONE)
                .updateFileWithRandomContent(initialHash, existingHash, FILE_EXISTING);
        ;
        // Apply CP1
        apply(cp1);

        Assert.assertTrue(builder.hasFile(FILE_ONE));
        Assert.assertTrue(builder.hasFile(FILE_EXISTING));
        Assert.assertTrue(Arrays.equals(existingHash, hashFile(existing)));

        final PatchingTestStepBuilder oneOff1 = builder.createStepBuilder();
        oneOff1.setPatchId("oneOff1")
                .oneOffPatchIdentity(PRODUCT_VERSION)
                .oneOffPatchElement("base-oneOff1", "base", false)
                .updateModuleWithRandomContent("org.jboss.test", moduleHash, null)
                .getParent()
                .updateFileWithRandomContent(standaloneHash, null, FILE_ONE)
                .updateFileWithRandomContent(Arrays.copyOf(existingHash, existingHash.length), existingHash, FILE_EXISTING);
        ;
        // Apply oneOff1
        apply(oneOff1);

        Assert.assertTrue(builder.hasFile(FILE_ONE));
        Assert.assertTrue(builder.hasFile(FILE_EXISTING));
        Assert.assertTrue(Arrays.equals(existingHash, hashFile(existing)));

        final PatchingTestStepBuilder cp2 = builder.createStepBuilder();
        cp2.setPatchId("CP2")
                .upgradeIdentity(PRODUCT_VERSION, PRODUCT_VERSION)
                .upgradeElement("base-CP2", "base", false)
                .addModuleWithRandomContent("org.jboss.test", moduleHash)
                .getParent()
                .addFileWithRandomContent(standaloneHash, FILE_TWO)
                .updateFileWithRandomContent(Arrays.copyOf(existingHash, existingHash.length), existingHash, FILE_EXISTING);
        ;
        // Apply CP2
        apply(cp2);

        // Builds upon CP1 and has to invalidate one-off1
        Assert.assertTrue(builder.hasFile(FILE_TWO));
        Assert.assertTrue(builder.hasFile(FILE_ONE));
        Assert.assertTrue(builder.hasFile(FILE_EXISTING));
        Assert.assertTrue(Arrays.equals(existingHash, hashFile(existing)));

        rollback(cp2);
        rollback(oneOff1);
        rollback(cp1);
    }

    @Test
    public void testAutoResolveConflicts() throws Exception {

        final PatchingTestBuilder builder = createDefaultBuilder();

        // Create a file
        final File existing = builder.getFile(FILE_EXISTING);
        touch(existing);
        dump(existing, randomString());
        final byte[] existingHash = hashFile(existing);
        final byte[] initialHash = Arrays.copyOf(existingHash, existingHash.length);

        final byte[] moduleHash = new byte[20];

        final PatchingTestStepBuilder oo1 = builder.createStepBuilder();
        oo1.setPatchId("one-off-one")
                .oneOffPatchIdentity(PRODUCT_VERSION)
                .updateFileWithRandomContent(initialHash, existingHash, FILE_EXISTING)
                .oneOffPatchElement("base-one-off", "base", false)
                .addModuleWithRandomContent("test.module", moduleHash)
        ;
        // Apply OO1
        apply(oo1);

        final PatchingTestStepBuilder cp1 = builder.createStepBuilder();
        cp1.setPatchId("CP1")
                .upgradeIdentity(PRODUCT_VERSION, PRODUCT_VERSION)
                .updateFileWithRandomContent(initialHash, existingHash, FILE_EXISTING)
                .upgradeElement("base-cp1", "base", false)
                .addModuleWithRandomContent("test.module", moduleHash)
        ;
        // Apply CP1
        apply(cp1);
        rollback(cp1);
        rollback(oo1);
    }

    @Test
    public void testLayersBasics() throws Exception {

        final PatchingTestBuilder builder = createDefaultBuilder("layer2", "layer1", "base");

        final byte[] standaloneHash = new byte[20];
        final byte[] moduleHash = new byte[20];

        final PatchingTestStepBuilder cp1 = builder.createStepBuilder();
        cp1.setPatchId("CP1")
                .upgradeIdentity(PRODUCT_VERSION, PRODUCT_VERSION)
                .upgradeElement("base-CP1", "base", false)
                .addModuleWithRandomContent("org.jboss.test", moduleHash)
                .getParent()
                .addFileWithRandomContent(standaloneHash, FILE_ONE)
        ;
        // Apply CP1
        apply(cp1);

        Assert.assertTrue(builder.hasFile(FILE_ONE));

        final PatchingTestStepBuilder cp2 = builder.createStepBuilder();
        cp2.setPatchId("CP2")
                .upgradeIdentity(PRODUCT_VERSION, PRODUCT_VERSION)
                .upgradeElement("layer2-CP2", "layer2", false)
                    .addModuleWithRandomContent("org.jboss.test", null)
                    .getParent()
                .upgradeElement("layer1-CP2", "layer1", false)
                    .addModuleWithRandomContent("org.jboss.test", null)
                    .getParent()
                .updateFileWithRandomContent(Arrays.copyOf(standaloneHash, standaloneHash.length), standaloneHash, FILE_ONE)
        ;
        // Apply CP2
        apply(cp2);

        Assert.assertTrue(builder.hasFile(FILE_ONE));

        final PatchingTestStepBuilder cp3 = builder.createStepBuilder();
        cp3.setPatchId("CP3")
                .upgradeIdentity(PRODUCT_VERSION, PRODUCT_VERSION)
                .removeFile(Arrays.copyOf(standaloneHash, standaloneHash.length), FILE_ONE)
        ;
        // Apply CP3
        apply(cp3);

        Assert.assertFalse(builder.hasFile(FILE_ONE));

        // Rollback
        rollback(cp3);
        Assert.assertTrue(builder.hasFile(FILE_ONE));
        rollback(cp2);
        Assert.assertTrue(builder.hasFile(FILE_ONE));
        rollback(cp1);
        Assert.assertFalse(builder.hasFile(FILE_ONE));

    }

    @Test
    public void testRemoveModule() throws Exception {

        final PatchingTestBuilder testBuilder = createDefaultBuilder();

        final File root = testBuilder.getRoot();
        final File installation = new File(root, JBOSS_INSTALLATION);
        final File moduleRoot = new File(installation, "modules/system/layers/base");
        final File module0 = createModule0(moduleRoot, "test.module", randomString());

        final byte[] existing = hashFile(module0);

        final PatchingTestStepBuilder step1 = testBuilder.createStepBuilder();
        step1.setPatchId("step1")
                .oneOffPatchIdentity(PRODUCT_VERSION)
                .oneOffPatchElement("base-1", "base", false)
                .removeModule("test.module", "main", existing)
                ;

        apply(step1);
        rollback(step1);
    }

    @Test
    public void testBasicIncrementalPatch() throws Exception {

        final PatchingTestBuilder builder = createDefaultBuilder();

        final byte[] moduleHashOne = new byte[20];
        final byte[] moduleHashTwo = new byte[20];

        final PatchingTestStepBuilder cp1 = builder.createStepBuilder();
        cp1.setPatchId("CP1")
                .upgradeIdentity(PRODUCT_VERSION, PRODUCT_VERSION)
                .upgradeElement("base-CP1", "base", false)
                .addModuleWithRandomContent("org.jboss.test", moduleHashOne)
        ;
        // Apply CP1
        apply(cp1);

        final PatchingTestStepBuilder cp2 = builder.createStepBuilder();
        cp2.setPatchId("CP2")
                .upgradeIdentity(PRODUCT_VERSION, PRODUCT_VERSION)
                .upgradeElement("base-CP2", "base", false)
                .addModuleWithRandomContent("org.jboss.test.two", moduleHashTwo)
        ;
        // Apply CP2
        apply(cp2);

        InstalledIdentity identity = loadInstallationManager();
        PatchStepAssertions.assertModule("base-CP2", identity.getLayer("base"), "org.jboss.test", "main");
        PatchStepAssertions.assertModule("base-CP2", identity.getLayer("base"), "org.jboss.test.two", "main");

        final PatchingTestStepBuilder cp3 = builder.createStepBuilder();
        cp3.setPatchId("CP3")
                .upgradeIdentity(PRODUCT_VERSION, PRODUCT_VERSION)
                .upgradeElement("base-CP3", "base", false)
                .removeModule("org.jboss.test.two", "main", moduleHashTwo)
        ;
        // Apply CP2
        apply(cp3);

        identity = loadInstallationManager();
        PatchStepAssertions.assertModule("base-CP3", identity.getLayer("base"), "org.jboss.test", "main");
        PatchStepAssertions.assertModule("base-CP3", identity.getLayer("base"), "org.jboss.test.two", "main");

        rollback(cp3);

        identity = loadInstallationManager();
        PatchStepAssertions.assertModule("base-CP2", identity.getLayer("base"), "org.jboss.test", "main");
        PatchStepAssertions.assertModule("base-CP2", identity.getLayer("base"), "org.jboss.test.two", "main");

        rollback(cp2);

        identity = loadInstallationManager();
        PatchStepAssertions.assertModule("base-CP1", identity.getLayer("base"), "org.jboss.test", "main");

        rollback(cp1);
    }

    @Test
    public void testIncrementalLayersPatch() throws Exception {

        final PatchingTestBuilder builder = createDefaultBuilder("layer-2", "layer-1", "base");

        final byte[] moduleHashOne = new byte[20];
        final byte[] moduleHashTwo = new byte[20];
        final byte[] moduleHashThree = new byte[20];
        final byte[] moduleHashFour = new byte[20];
        final byte[] moduleHashFive = new byte[20];
        final byte[] moduleHashSix = new byte[20];

        final PatchingTestStepBuilder cp1 = builder.createStepBuilder();
        cp1.setPatchId("CP1")
                .upgradeIdentity(PRODUCT_VERSION, PRODUCT_VERSION)
                .upgradeElement("base-CP1", "base", false)
                .addModuleWithRandomContent("org.jboss.test", moduleHashOne)
        ;
        // Apply CP1
        apply(cp1);

        final PatchingTestStepBuilder cp2 = builder.createStepBuilder();
        cp2.setPatchId("CP2")
                .upgradeIdentity(PRODUCT_VERSION, PRODUCT_VERSION)
                .upgradeElement("layer-1-CP1", "layer-1", false)
                .addModuleWithRandomContent("org.jboss.test.two", moduleHashTwo)
        ;
        // Apply CP2
        apply(cp2);

        final PatchingTestStepBuilder cp3 = builder.createStepBuilder();
        cp3.setPatchId("CP3")
                .upgradeIdentity(PRODUCT_VERSION, PRODUCT_VERSION)
                .upgradeElement("layer-2-CP1", "layer-2", false)
                .removeModule("org.jboss.test.three", "main", moduleHashThree)
        ;
        // Apply CP2
        apply(cp3);

        final PatchingTestStepBuilder cp4 = builder.createStepBuilder();
        cp4.setPatchId("CP4")
                .upgradeIdentity(PRODUCT_VERSION, PRODUCT_VERSION)
                .upgradeElement("base-CP2", "base", false)
                .addModuleWithRandomContent("org.jboss.test.four", moduleHashFour)
        ;
        // Apply CP4
        apply(cp4);

        InstalledIdentity identity = loadInstallationManager();
        PatchStepAssertions.assertModule("base-CP2", identity.getLayer("base"), "org.jboss.test", "main");
        PatchStepAssertions.assertModule("base-CP2", identity.getLayer("base"), "org.jboss.test.four", "main");

        final PatchingTestStepBuilder cp5 = builder.createStepBuilder();
        cp5.setPatchId("CP5")
                .upgradeIdentity(PRODUCT_VERSION, PRODUCT_VERSION)
                .upgradeElement("base-CP3", "base", false)
                .updateModuleWithRandomContent("org.jboss.test.four", moduleHashFour, null)
                .getParent()
                .upgradeElement("layer-1-CP2", "layer-1", false)
                .addModuleWithRandomContent("org.jboss.test.five", moduleHashFive)
                .getParent()
                .upgradeElement("layer-2-CP2", "layer-2", false)
                .addModuleWithRandomContent("org.jboss.test.six", moduleHashSix)
        ;
        // Apply CP5
        apply(cp5);

        identity = loadInstallationManager();
        PatchStepAssertions.assertModule("base-CP3", identity.getLayer("base"), "org.jboss.test", "main");
        PatchStepAssertions.assertModule("base-CP3", identity.getLayer("base"), "org.jboss.test.four", "main");
        PatchStepAssertions.assertModule("layer-1-CP2", identity.getLayer("layer-1"), "org.jboss.test.two", "main");
        PatchStepAssertions.assertModule("layer-1-CP2", identity.getLayer("layer-1"), "org.jboss.test.five", "main");
        PatchStepAssertions.assertModule("layer-2-CP2", identity.getLayer("layer-2"), "org.jboss.test.three", "main");
        PatchStepAssertions.assertModule("layer-2-CP2", identity.getLayer("layer-2"), "org.jboss.test.six", "main");

        rollback(cp5);
        rollback(cp4);
        rollback(cp3);
        rollback(cp2);
        rollback(cp1);
    }


}
