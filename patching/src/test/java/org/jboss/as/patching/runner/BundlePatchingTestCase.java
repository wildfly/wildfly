/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.patching.runner;

import static org.jboss.as.patching.HashUtils.hashFile;
import static org.jboss.as.patching.IoUtils.NO_CONTENT;
import static org.jboss.as.patching.metadata.ModificationType.ADD;
import static org.jboss.as.patching.metadata.ModificationType.MODIFY;
import static org.jboss.as.patching.metadata.ModificationType.REMOVE;
import static org.jboss.as.patching.runner.PatchingAssert.assertContains;
import static org.jboss.as.patching.runner.PatchingAssert.assertDefinedAbsentBundle;
import static org.jboss.as.patching.runner.PatchingAssert.assertDefinedBundle;
import static org.jboss.as.patching.runner.PatchingAssert.assertDirExists;
import static org.jboss.as.patching.runner.PatchingAssert.assertPatchHasBeenApplied;
import static org.jboss.as.patching.runner.TestUtils.createBundle;
import static org.jboss.as.patching.runner.TestUtils.createPatchXMLFile;
import static org.jboss.as.patching.runner.TestUtils.createZippedPatchFile;
import static org.jboss.as.patching.runner.TestUtils.getBundlePath;
import static org.jboss.as.patching.runner.TestUtils.mkdir;
import static org.jboss.as.patching.runner.TestUtils.randomString;

import java.io.File;
import java.util.Collections;

import org.jboss.as.patching.LocalPatchInfo;
import org.jboss.as.patching.PatchInfo;
import org.jboss.as.patching.metadata.BundleItem;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.junit.Test;

/**
 * @author Emanuel Muckenhuber
 */
public class BundlePatchingTestCase extends AbstractTaskTestCase {

    @Test
    public void testAddBundle() throws Exception {

        PatchInfo info = new LocalPatchInfo(randomString(), PatchInfo.BASE, Collections.<String>emptyList(), env);

        // build a one-off patch for the base installation
        // with 1 added module
        String patchID = randomString();
        File patchDir = mkdir(tempDir, patchID);
        String bundleName = randomString();
        File bundleDir = createBundle(patchDir, bundleName, true);
        byte[] newHash = hashFile(bundleDir);
        ContentModification bundleAdd = new ContentModification(new BundleItem(bundleName, null, newHash), NO_CONTENT, ADD);

        Patch patch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription(randomString())
                .setOneOffType(info.getVersion())
                .addContentModification(bundleAdd)
                .build();
        createPatchXMLFile(patchDir, patch);
        File zippedPatch = createZippedPatchFile(patchDir, patchID);

        PatchingResult result = executePatch(info, zippedPatch);

        assertPatchHasBeenApplied(result, patch);

        File bundlesPatchingDirectory = env.getBundlesPatchDirectory(patchID);
        assertDirExists(bundlesPatchingDirectory);
        assertContains(bundlesPatchingDirectory, getBundlePath(env, result.getPatchInfo()));
        assertDefinedBundle(getBundlePath(env, result.getPatchInfo()), bundleName, newHash);
    }

    @Test
    public void testModifyBundle() throws Exception {

        PatchInfo info = new LocalPatchInfo(randomString(), PatchInfo.BASE, Collections.<String>emptyList(), env);

        String bundleName = randomString();

        // create a bundle in the AS7 installation
        createBundle(env.getInstalledImage().getJbossHome(), bundleName, true);
        byte[] existingHash = hashFile(new File(env.getInstalledImage().getBundlesDir(), bundleName));

        // build a one-off patch for the base installation
        // with 1 modified module
        String patchID = randomString();
        File patchDir = mkdir(tempDir, patchID);
        File bundleDir = createBundle(patchDir, bundleName, true);
        byte[] newHash = hashFile(bundleDir);

        ContentModification bundleModify = new ContentModification(new BundleItem(bundleName, null, newHash), existingHash, MODIFY);

        Patch patch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription(randomString())
                .setOneOffType(info.getVersion())
                .addContentModification(bundleModify)
                .build();
        createPatchXMLFile(patchDir, patch);
        File zippedPatch = createZippedPatchFile(patchDir, patchID);

        PatchingResult result = executePatch(info, zippedPatch);

        assertPatchHasBeenApplied(result, patch);

        File bundlesPatchingDirectory = env.getBundlesPatchDirectory(patchID);
        assertDirExists(bundlesPatchingDirectory);
        assertContains(bundlesPatchingDirectory, getBundlePath(env, result.getPatchInfo()));
        assertDefinedBundle(getBundlePath(env, result.getPatchInfo()), bundleName, newHash);

    }

    @Test
    public void testRemoveBundle() throws Exception {

        PatchInfo info = new LocalPatchInfo(randomString(), PatchInfo.BASE, Collections.<String>emptyList(), env);

        // create a bundle in the AS7 installation
        String bundleName = randomString();
        File bundlesDir = env.getInstalledImage().getBundlesDir();
        createBundle(env.getInstalledImage().getJbossHome(), bundleName, true);
        byte[] existingHash = hashFile(new File(bundlesDir, bundleName));

        // build a one-off patch for the base installation
        // with 1 bundle removed
        ContentModification bundleRemoved = new ContentModification(new BundleItem(bundleName, null, NO_CONTENT), existingHash, REMOVE);

        Patch patch = PatchBuilder.create()
                .setPatchId(randomString())
                .setDescription(randomString())
                .setOneOffType(info.getVersion())
                .addContentModification(bundleRemoved)
                .build();

        // create the patch
        File patchDir = mkdir(tempDir, patch.getPatchId());
        createPatchXMLFile(patchDir, patch);
        File zippedPatch = createZippedPatchFile(patchDir, patch.getPatchId());

        PatchingResult result = executePatch(info, zippedPatch);

        assertPatchHasBeenApplied(result, patch);

        File bundlesPatchDirectory = env.getBundlesPatchDirectory(patch.getPatchId());
        assertDirExists(bundlesPatchDirectory);
        assertContains(bundlesPatchDirectory, getBundlePath(env, result.getPatchInfo()));
        assertDefinedAbsentBundle(getBundlePath(env, result.getPatchInfo()), bundleName);
    }


}
