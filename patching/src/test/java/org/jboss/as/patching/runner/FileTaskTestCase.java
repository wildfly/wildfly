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
import static org.jboss.as.patching.IoUtils.mkdir;
import static org.jboss.as.patching.runner.PatchingAssert.assertFileContent;
import static org.jboss.as.patching.runner.PatchingAssert.assertFileDoesNotExist;
import static org.jboss.as.patching.runner.PatchingAssert.assertFileExists;
import static org.jboss.as.patching.runner.PatchingAssert.assertPatchHasBeenApplied;
import static org.jboss.as.patching.runner.PatchingAssert.assertPatchHasBeenRolledBack;
import static org.jboss.as.patching.runner.TestUtils.createPatchXMLFile;
import static org.jboss.as.patching.runner.TestUtils.createZippedPatchFile;
import static org.jboss.as.patching.runner.TestUtils.dump;
import static org.jboss.as.patching.runner.TestUtils.randomString;
import static org.jboss.as.patching.runner.TestUtils.touch;
import static org.jboss.as.patching.runner.TestUtils.tree;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.jboss.as.patching.installation.Identity;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.jboss.as.patching.tool.PatchingResult;
import org.junit.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012, Red Hat Inc
 */
public class FileTaskTestCase extends AbstractTaskTestCase {

    @Test
    public void testAddFile() throws Exception {
        // build a one-off patch for the base installation
        // with 1 added file
        String patchID = randomString();
        File patchDir = mkdir(tempDir, patchID);

        ContentModification fileAdded = ContentModificationUtils.addMisc(patchDir, patchID, "new file resource", "bin", "my-new-standalone.sh");

        Patch patch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription(randomString())
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                   .getParent()
                .addContentModification(fileAdded)
                .build();

        assertFileDoesNotExist(env.getInstalledImage().getJbossHome(), "bin", fileAdded.getItem().getName());

        createPatchXMLFile(patchDir, patch);
        File zippedPatch = createZippedPatchFile(patchDir, patch.getPatchId());

        PatchingResult result = executePatch(zippedPatch);
        assertPatchHasBeenApplied(result, patch);

        assertFileExists(env.getInstalledImage().getJbossHome(), "bin", fileAdded.getItem().getName());
    }

    @Test
    public void testRemoveFile() throws Exception {

        // start from a base installation
        // with a file in it
        String fileName = "standalone.sh";
        File standaloneShellFile = touch(env.getInstalledImage().getJbossHome(), "bin", fileName );
        dump(standaloneShellFile, "original script to run standalone AS7");
        byte[] existingHash = hashFile(standaloneShellFile);

        // build a one-off patch for the base installation
        // with 1 removed file
        ContentModification fileRemoved = ContentModificationUtils.removeMisc(standaloneShellFile, "bin", fileName);

        Patch patch = PatchBuilder.create()
                .setPatchId(randomString())
                .setDescription(randomString())
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                   .getParent()
                .addContentModification(fileRemoved)
                .build();

        // create the patch
        File patchDir = mkdir(tempDir, patch.getPatchId());
        createPatchXMLFile(patchDir, patch);
        File zippedPatch = createZippedPatchFile(patchDir, patch.getPatchId());

        PatchingResult result = executePatch(zippedPatch);
        assertPatchHasBeenApplied(result, patch);

        /// file has been removed from the AS7 installation
        assertFileDoesNotExist(standaloneShellFile);
        // but it's been backed up
        assertFileExists(env.getInstalledImage().getPatchHistoryDir(patch.getPatchId()), "misc", "bin", fileName);
    }

    @Test
    public void testUpdateFile() throws Exception {

        // start from a base installation
        // with a file in it
        File binDir = mkdir(env.getInstalledImage().getJbossHome(), "bin");
        String fileName = "standalone.sh";
        File standaloneShellFile = touch(binDir, fileName);
        dump(standaloneShellFile, "original script to run standalone AS7");

        // build a one-off patch for the base installation
        // with 1 updated file
        String patchID = randomString();
        File patchDir = mkdir(tempDir, patchID);

        ContentModification fileModified = ContentModificationUtils.modifyMisc(patchDir, patchID, "updated script", standaloneShellFile, "bin", "standalone.sh");

        Patch patch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription(randomString())
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                   .getParent()
                .addContentModification(fileModified)
                .build();

        // create the patch
        createPatchXMLFile(patchDir, patch);
        File zippedPatch = createZippedPatchFile(patchDir, patch.getPatchId());

        PatchingResult result = executePatch(zippedPatch);
        assertPatchHasBeenApplied(result, patch);

        /// file has been updated in the AS7 installation
        // and it's the new one
        assertFileExists(standaloneShellFile);
        assertFileContent(fileModified.getItem().getContentHash(), standaloneShellFile);
        // the existing file has been backed up
        File backupFile = assertFileExists(env.getInstalledImage().getPatchHistoryDir(patchID), "misc", "bin", fileName);
        assertFileContent(fileModified.getTargetHash(), backupFile);
    }

    @Test
    public void testRemoveDirectoryAndRollback() throws Exception {
        // start from a base installation
        // Create a directory
        File test = mkdir(env.getInstalledImage().getJbossHome(), "test");
        String one = "one";
        String two = "two";
        File fileOne = touch(test, one);
        touch(fileOne); // touch one
        dump(fileOne, randomString());
        File fileTwo = touch(test, two);
        touch(fileTwo); // touch two
        dump(fileTwo, randomString());
        File subDirOne = mkdir(test, "sub");
        File subOne = touch(subDirOne, one);
        touch(subOne);
        dump(subOne, randomString());
        File subTwo = touch(subDirOne, two);
        touch(subTwo);
        dump(subTwo, randomString());
        byte[] existingHash = hashFile(test);

        // build a one-off patch for the base installation
        // with 1 removed directory

        String patchID = "patchID";//randomString();
        ContentModification dirRemoved = ContentModificationUtils.removeMisc(test);

        Patch patch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription(randomString())
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                   .getParent()
                .addContentModification(dirRemoved)
                .build();

        // create the patch
        File patchDir = mkdir(tempDir, patch.getPatchId());
        createPatchXMLFile(patchDir, patch);
        File zippedPatch = createZippedPatchFile(patchDir, patch.getPatchId());

        Identity identityBeforePatch = loadInstalledIdentity().getIdentity();

        //System.out.println("before patch");
        tree(env.getInstalledImage().getJbossHome());

        // Apply
        PatchingResult result = executePatch(zippedPatch);
        assertPatchHasBeenApplied(result, patch);
        assertFalse(test.exists());

        //System.out.println("after patch");
        tree(env.getInstalledImage().getJbossHome());

        // Rollback
        result = rollback(patch.getPatchId());
        assertPatchHasBeenRolledBack(result, identityBeforePatch);

        //System.out.println("after rollback");
        tree(env.getInstalledImage().getJbossHome());

        assertTrue(test.exists());
        assertTrue(fileOne.isFile());
        assertTrue(fileTwo.isFile());
        assertTrue(subOne.isFile());
        assertTrue(subTwo.isFile());
    }

}

