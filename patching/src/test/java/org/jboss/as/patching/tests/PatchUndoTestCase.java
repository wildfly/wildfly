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

import static org.jboss.as.patching.runner.TestUtils.randomString;
import static org.jboss.as.patching.runner.TestUtils.tree;

import java.io.File;

import org.jboss.as.patching.HashUtils;
import org.jboss.as.patching.IoUtils;
import org.jboss.as.patching.PatchingException;
import org.jboss.as.patching.installation.InstalledIdentity;
import org.jboss.as.patching.installation.PatchableTarget;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.MiscContentItem;
import org.jboss.as.patching.metadata.ModificationType;
import org.jboss.as.patching.metadata.ModuleItem;
import org.jboss.as.patching.runner.ContentModificationUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test that undo actions properly handle failures.
 *
 * @author Emanuel Muckenhuber
 */
public class PatchUndoTestCase extends AbstractPatchingTest {

    private static final String[] MODULES_BASE = new String[]{ "modules", "system", "layers", "base", ".overlays" };

    private static final byte[] WRONG_HASH = HashUtils.hexStringToByteArray("ffaf3edf942c0f6fb8754f75d60722bfb6a6a503");

    @Test
    public void testWrongModuleContent() throws Exception {

        final PatchingTestBuilder builder = createDefaultBuilder();

        // Add some random content
        ContentModificationUtils.addModule(builder.getRoot(), "base-patch-001", "test.module", randomString());
        // Override the hash with a wrong one
        final ModuleItem item = new ModuleItem("test.module", "main", WRONG_HASH);
        final ContentModification wrongModification = new ContentModification(item, IoUtils.NO_CONTENT, ModificationType.ADD);

        final PatchingTestStepBuilder step1 = builder.createStepBuilder();
        step1.oneOffPatchIdentity(PRODUCT_VERSION)
                .setPatchId("oo1")
                .oneOffPatchElement("base-patch-001", "base", false)
                .addContentModification(wrongModification)
                .getParent()
                .addFileWithRandomContent(null, "test", "content");
        //
        try {
            apply(step1);
            Assert.fail("should have failed");
        } catch (PatchingException e) {
            Assert.assertFalse(builder.hasFile("test", "content"));
            final InstalledIdentity identity = loadInstallationManager();
            final PatchableTarget base = identity.getLayer("base");
            Assert.assertFalse(base.getDirectoryStructure().getModulePatchDirectory("base-patch-001").exists());
            Assert.assertFalse(identity.getInstalledImage().getPatchHistoryDir("oo1").exists());
        }

    }

    @Test
    public void testWrongMiscContent() throws Exception {

        final PatchingTestBuilder builder = createDefaultBuilder();

        ContentModificationUtils.addMisc(builder.getRoot(), "oo2", "test-content", "wrong-content");
        final MiscContentItem item = new MiscContentItem("wrong-content", new String[0], WRONG_HASH);
        final ContentModification wrongModification = new ContentModification(item, IoUtils.NO_CONTENT, ModificationType.ADD);

        final PatchingTestStepBuilder step1 = builder.createStepBuilder();
        step1.oneOffPatchIdentity(PRODUCT_VERSION)
                .setPatchId("oo2")
                .oneOffPatchElement("base-patch-002", "base", false)
                .addModuleWithRandomContent("other.test", null)
                .getParent()
                .addFileWithRandomContent(null, "test", "content")
                .addContentModification(wrongModification)
                ;
        //
        try {
            apply(step1);
            Assert.fail("should have failed");
        } catch (PatchingException e) {
            Assert.assertFalse(builder.hasFile("test", "content"));
            Assert.assertFalse(builder.hasFile("wrong-content"));
            final InstalledIdentity identity = loadInstallationManager();
            final PatchableTarget base = identity.getLayer("base");
            Assert.assertFalse(base.getDirectoryStructure().getModulePatchDirectory("base-patch-002").exists());
            Assert.assertFalse(identity.getInstalledImage().getPatchHistoryDir("oo2").exists());
        }
    }


    @Test
    public void testInvalidPatch() throws Exception {
        final PatchingTestBuilder builder = createDefaultBuilder();

        ContentModificationUtils.addMisc(builder.getRoot(), "oo2", "test-content", "wrong-content");
        final MiscContentItem item = new MiscContentItem("wrong-content", new String[0], WRONG_HASH);
        final ContentModification wrongModification = new ContentModification(item, IoUtils.NO_CONTENT, ModificationType.ADD);

        final PatchingTestStepBuilder step1 = builder.createStepBuilder();
        step1.oneOffPatchIdentity(PRODUCT_VERSION)
                .setPatchId("oo2")
                .oneOffPatchElement("base-patch-002", "base", false)
                .addModuleWithRandomContent("other.test", null)
                .getParent()
                .addFileWithRandomContent(null, "test", "content")
        ;
        apply(step1);
        Assert.assertTrue(builder.hasFile("test", "content"));

        //
        final PatchingTestStepBuilder step2 = builder.createStepBuilder();
        step2.upgradeIdentity(PRODUCT_VERSION, PRODUCT_VERSION)
                .setPatchId("cp2")
                .upgradeElement("base-patch-cp2", "base", false)
                .getParent()
                .addContentModification(wrongModification)
        ;

        try {
            apply(step2);
            Assert.fail("should have failed");
        } catch (PatchingException e) {
            Assert.assertTrue(builder.hasFile("test", "content"));
        }

    }

    @Test
    public void testModificationCompletion() throws Exception {

        final PatchingTestBuilder builder = createDefaultBuilder();

        final PatchingTestStepBuilder step1 = builder.createStepBuilder();
        step1.oneOffPatchIdentity(PRODUCT_VERSION)
                .setPatchId("patch1")
                .oneOffPatchElement("base-patch-001", "base", false)
                .addModuleWithRandomContent("test.module", null)
                .getParent()
                .addFileWithRandomContent(null, "test", "file")
        ;

        final File overlays = builder.getFile(MODULES_BASE);
        // Make the modification.complete() fail
        final File file = new File(overlays, ".overlays");
        Assert.assertTrue(file.mkdirs());

        try {
            apply(step1);
            tree(builder.getRoot());
            Assert.fail();
        } catch (Exception e) {
            // Ok
            Assert.assertFalse(builder.hasFile("test", "file"));
            Assert.assertFalse(new File(overlays, "base-patch-001").exists());

        }

    }


}
