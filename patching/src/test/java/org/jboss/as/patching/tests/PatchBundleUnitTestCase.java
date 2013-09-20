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

import static org.jboss.as.patching.IoUtils.safeClose;
import static org.jboss.as.patching.runner.TestUtils.dump;
import static org.jboss.as.patching.runner.TestUtils.randomString;
import static org.jboss.as.patching.runner.TestUtils.touch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.patching.PatchingException;
import org.jboss.as.patching.ZipUtils;
import org.jboss.as.patching.installation.InstallationManager;
import org.jboss.as.patching.metadata.BundledPatch;
import org.jboss.as.patching.metadata.PatchBundleXml;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.tool.ContentVerificationPolicy;
import org.jboss.as.patching.tool.PatchTool;
import org.jboss.as.patching.tool.PatchingResult;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Emanuel Muckenhuber
 */
public class PatchBundleUnitTestCase extends AbstractPatchingTest {

    static final String[] FILE_ONE = {"bin", "standalone.sh"};
    static final String[] FILE_TWO = {"bin", "standalone.conf"};
    static final String[] FILE_EXISTING = {"bin", "test"};

    @Test
    public void testMultiInstall() throws Exception {

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
        final PatchingTestStepBuilder cp2 = builder.createStepBuilder();
        cp2.setPatchId("CP2")
                .upgradeIdentity(PRODUCT_VERSION, PRODUCT_VERSION)
                .upgradeElement("layer-1-CP1", "layer-1", false)
                .addModuleWithRandomContent("org.jboss.test.two", moduleHashTwo)
        ;
        final PatchingTestStepBuilder cp3 = builder.createStepBuilder();
        cp3.setPatchId("CP3")
                .upgradeIdentity(PRODUCT_VERSION, PRODUCT_VERSION)
                .upgradeElement("layer-2-CP1", "layer-2", false)
                .removeModule("org.jboss.test.three", "main", moduleHashThree)
        ;
        final PatchingTestStepBuilder cp4 = builder.createStepBuilder();
        cp4.setPatchId("CP4")
                .upgradeIdentity(PRODUCT_VERSION, PRODUCT_VERSION)
                .upgradeElement("base-CP2", "base", false)
                .addModuleWithRandomContent("org.jboss.test.four", moduleHashFour)
        ;
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

        final File multiPatch = prepare(builder.getRoot(), cp1, cp2, cp3, cp4, cp5);
        // Create the patch tool and apply the patch
        InstallationManager mgr = loadInstallationManager();
        final PatchTool patchTool = PatchTool.Factory.create(mgr);
        final PatchingResult result = patchTool.applyPatch(multiPatch, ContentVerificationPolicy.STRICT);
        result.commit();
        try {
            PatchStepAssertions.APPLY.after(builder.getFile(JBOSS_INSTALLATION), cp5.build(), mgr);
        } catch (IOException e) {
            throw new PatchingException(e);
        }

        mgr = loadInstallationManager();

        PatchStepAssertions.assertModule("base-CP3", mgr.getLayer("base"), "org.jboss.test", "main");
        PatchStepAssertions.assertModule("base-CP3", mgr.getLayer("base"), "org.jboss.test.four", "main");
        PatchStepAssertions.assertModule("layer-1-CP2", mgr.getLayer("layer-1"), "org.jboss.test.two", "main");
        PatchStepAssertions.assertModule("layer-1-CP2", mgr.getLayer("layer-1"), "org.jboss.test.five", "main");
        PatchStepAssertions.assertModule("layer-2-CP2", mgr.getLayer("layer-2"), "org.jboss.test.three", "main");
        PatchStepAssertions.assertModule("layer-2-CP2", mgr.getLayer("layer-2"), "org.jboss.test.six", "main");

        rollback(cp5);

        mgr = loadInstallationManager();
        PatchStepAssertions.assertModule("base-CP2", mgr.getLayer("base"), "org.jboss.test", "main");
        PatchStepAssertions.assertModule("base-CP2", mgr.getLayer("base"), "org.jboss.test.four", "main");

        rollback(cp4);
        rollback(cp3);
        rollback(cp2);
        rollback(cp1);

    }

    @Test
    public void testRevert() throws Exception {

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
        final PatchingTestStepBuilder cp2 = builder.createStepBuilder();
        cp2.setPatchId("CP2")
                .upgradeIdentity(PRODUCT_VERSION, PRODUCT_VERSION)
                .upgradeElement("layer-1-CP1", "layer-1", false)
                .addModuleWithRandomContent("org.jboss.test.two", moduleHashTwo)
        ;
        final PatchingTestStepBuilder cp3 = builder.createStepBuilder();
        cp3.setPatchId("CP3")
                .upgradeIdentity(PRODUCT_VERSION, PRODUCT_VERSION)
                .upgradeElement("layer-2-CP1", "layer-2", false)
                .removeModule("org.jboss.test.three", "main", moduleHashThree)
        ;
        final PatchingTestStepBuilder cp4 = builder.createStepBuilder();
        cp4.setPatchId("CP4")
                .upgradeIdentity(PRODUCT_VERSION, PRODUCT_VERSION)
                .upgradeElement("base-CP2", "base", false)
                .addModuleWithRandomContent("org.jboss.test.four", moduleHashFour)
        ;
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

        final File multiPatch = prepare(builder.getRoot(), cp1, cp2, cp3, cp4, cp5);
        // Create the patch tool and apply the patch
        InstallationManager mgr = loadInstallationManager();
        final PatchTool patchTool = PatchTool.Factory.create(mgr);
        final PatchingResult result = patchTool.applyPatch(multiPatch, ContentVerificationPolicy.STRICT);
        result.rollback();

        mgr = loadInstallationManager();
        checkNotApplied(builder.getRoot(), mgr, cp1, cp2, cp3, cp4, cp5);
    }

    @Test
    public void testConflicts() throws Exception {

        final PatchingTestBuilder builder = createDefaultBuilder();

        final byte[] standaloneHash = new byte[20];
        final byte[] configHash = new byte[20];
        final byte[] existingHash = new byte[20];

        // Create a file
        final File existing = builder.getFile(FILE_EXISTING);
        touch(existing);
        dump(existing, randomString());


        final PatchingTestStepBuilder cp1 = builder.createStepBuilder();
        cp1.setPatchId("cp1")
                .upgradeIdentity(PRODUCT_VERSION, PRODUCT_VERSION)
                .addFileWithRandomContent(standaloneHash, FILE_ONE);

        final PatchingTestStepBuilder cp2 = builder.createStepBuilder();
        cp2.setPatchId("cp2")
                .upgradeIdentity(PRODUCT_VERSION, PRODUCT_VERSION)
                .addFileWithRandomContent(configHash, FILE_TWO);

        final PatchingTestStepBuilder cp3 = builder.createStepBuilder();
        cp3.setPatchId("cp3")
                .upgradeIdentity(PRODUCT_VERSION, PRODUCT_VERSION)
                .addFileWithRandomContent(existingHash, FILE_EXISTING);

        final File multiPatch = prepare(builder.getRoot(), cp1, cp2, cp3);
        // Create the patch tool and apply the patch
        InstallationManager mgr = loadInstallationManager();
        final PatchTool patchTool = PatchTool.Factory.create(mgr);
        try {
            patchTool.applyPatch(multiPatch, ContentVerificationPolicy.STRICT);
            Assert.fail();
        } catch (PatchingException e) {
            //ok
        }

        Assert.assertFalse(builder.hasFile(FILE_ONE));
        Assert.assertFalse(builder.hasFile(FILE_TWO));

    }

    protected void checkNotApplied(final File root, final InstallationManager mgr, final PatchingTestStepBuilder... steps) throws Exception {
        for (final PatchingTestStepBuilder step : steps) {
            PatchStepAssertions.APPLY.before(root, step.build(),  mgr);
        }
    }

    /**
     * Prepare the multi patch bundle.
     *
     * @param root  the temp dir root
     * @param steps the individual steps
     * @return the prepared content
     * @throws PatchingException
     */
    protected File prepare(File root, PatchingTestStepBuilder... steps) throws PatchingException {

        final File tempDir = new File(root, randomString());
        tempDir.mkdir();
        final List<BundledPatch.BundledPatchEntry> entries = new ArrayList<BundledPatch.BundledPatchEntry>();
        for (final PatchingTestStepBuilder step : steps) {

            // Prepare the patches.
            final Patch patch = step.build();
            writePatch(step.getPatchDir(), patch);
            final String patchId = patch.getPatchId();
            final String path = patchId + ".zip";
            final File patchOutput = new File(tempDir, path);
            ZipUtils.zip(step.getPatchDir(), patchOutput);
            entries.add(new BundledPatch.BundledPatchEntry(patchId, path));
        }

        final File multiPatchXml = new File(tempDir, PatchBundleXml.MULTI_PATCH_XML);
        try {
            final OutputStream os = new FileOutputStream(multiPatchXml);
            try {
                PatchBundleXml.marshal(os, new BundledPatch() {
                    @Override
                    public List<BundledPatchEntry> getPatches() {
                        return entries;
                    }
                });
            } finally {
                safeClose(os);
            }
        } catch (Exception e) {
            throw new PatchingException(e);
        }

        final File result = new File(root, "multi-step-contents.zip");
        ZipUtils.zip(tempDir, result);
        return result;
    }

}
