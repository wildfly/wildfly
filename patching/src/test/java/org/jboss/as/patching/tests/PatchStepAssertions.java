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

import java.io.File;
import java.io.IOException;

import org.jboss.as.patching.DirectoryStructure;
import org.jboss.as.patching.installation.InstallationManager;
import org.jboss.as.patching.installation.InstalledIdentity;
import org.jboss.as.patching.installation.PatchableTarget;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchElement;
import org.jboss.as.patching.metadata.PatchElementProvider;
import org.junit.Assert;

/**
 * @author Emanuel Muckenhuber
 */
abstract class PatchStepAssertions {

    protected abstract void before(File installation, Patch patch, InstallationManager manager) throws IOException;
    protected abstract void after(File installation, Patch patch, InstallationManager manager) throws IOException;

    protected static final PatchStepAssertions APPLY = new PatchStepAssertions() {
        @Override
        protected void before(final File installation, final Patch patch, final InstallationManager manager) throws IOException {
            assertNotApplied(patch, manager);
        }

        @Override
        protected void after(File installation, Patch patch, InstallationManager manager) throws IOException {
            assertApplied(patch, manager);
        }
    };
    protected static final PatchStepAssertions ROLLBACK = new PatchStepAssertions() {
        @Override
        protected void before(File installation, Patch patch, InstallationManager manager) throws IOException {
            assertApplied(patch, manager);
        }

        @Override
        protected void after(File installation, Patch patch, InstallationManager manager) throws IOException {
            assertNotApplied(patch, manager);
        }
    };

    static void assertApplied(final Patch patch, InstalledIdentity installedIdentity) throws IOException {
        final PatchableTarget.TargetInfo identity = installedIdentity.getIdentity().loadTargetInfo();
        assertIsApplied(patch.getIdentity().getPatchType(), patch.getPatchId(), installedIdentity.getIdentity().loadTargetInfo());
        assertExists(identity.getDirectoryStructure().getInstalledImage().getPatchHistoryDir(patch.getPatchId()));
        for (final PatchElement element : patch.getElements()) {
            final PatchElementProvider provider = element.getProvider();
            final PatchableTarget target = provider.isAddOn() ? installedIdentity.getAddOn(provider.getName()) : installedIdentity.getLayer(provider.getName());
            assertIsApplied(provider.getPatchType(), element.getId(), target.loadTargetInfo());
        }
    }

    static void assertNotApplied(final Patch patch, InstalledIdentity installedIdentity) throws IOException {
        final PatchableTarget.TargetInfo identity = installedIdentity.getIdentity().loadTargetInfo();
        assertNotApplied(patch.getIdentity().getPatchType(), patch.getPatchId(), identity);
        assertDoesNotExists(identity.getDirectoryStructure().getInstalledImage().getPatchHistoryDir(patch.getPatchId()));
        for (final PatchElement element : patch.getElements()) {
            final PatchElementProvider provider = element.getProvider();
            final PatchableTarget target = provider.isAddOn() ? installedIdentity.getAddOn(provider.getName()) : installedIdentity.getLayer(provider.getName());
            assertNotApplied(provider.getPatchType(), element.getId(), target.loadTargetInfo());
        }
    }

    static void assertNotApplied(final Patch.PatchType patchType, final String patchId, final PatchableTarget.TargetInfo targetInfo) {
        if (patchType == Patch.PatchType.UPGRADE) {
            Assert.assertNotEquals(patchId, targetInfo.getReleasePatchID());
        } else if (patchType == Patch.PatchType.CUMULATIVE) {
            Assert.assertNotEquals(patchId, targetInfo.getCumulativeID());
        } else {
            Assert.assertFalse(targetInfo.getPatchIDs().contains(patchId));
        }
        final DirectoryStructure structure = targetInfo.getDirectoryStructure();
        assertDoesNotExists(structure.getBundlesPatchDirectory(patchId));
        assertDoesNotExists(structure.getModulePatchDirectory(patchId));
    }

    static void assertIsApplied(final Patch.PatchType patchType, final String patchId, final PatchableTarget.TargetInfo targetInfo) {
        if (patchType == Patch.PatchType.UPGRADE) {
            Assert.assertEquals(patchId, targetInfo.getReleasePatchID());
            Assert.assertTrue(targetInfo.getPatchIDs().isEmpty());
        } else if (patchType == Patch.PatchType.CUMULATIVE) {
            Assert.assertEquals(patchId, targetInfo.getCumulativeID());
            Assert.assertTrue(targetInfo.getPatchIDs().isEmpty());
        } else {
            Assert.assertTrue(targetInfo.getPatchIDs().contains(patchId));
        }
        final DirectoryStructure structure = targetInfo.getDirectoryStructure();
        assertExists(structure.getBundlesPatchDirectory(patchId));
        assertExists(structure.getModulePatchDirectory(patchId));
    }

    static void assertExists(final File file) {
        if (file != null) {
            Assert.assertTrue(file.getAbsolutePath(), file.exists());
        }
    }

    static void assertDoesNotExists(final File file) {
        if (file != null) {
            Assert.assertFalse(file.getAbsolutePath(), file.exists());
        }
    }

}
