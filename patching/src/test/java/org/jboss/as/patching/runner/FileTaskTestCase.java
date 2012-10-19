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

import static org.jboss.as.patching.metadata.ModificationType.ADD;
import static org.jboss.as.patching.metadata.ModificationType.MODIFY;
import static org.jboss.as.patching.metadata.ModificationType.REMOVE;
import static org.jboss.as.patching.runner.PatchUtils.calculateHash;
import static org.jboss.as.patching.runner.PatchingAssert.assertFileDoesNotExist;
import static org.jboss.as.patching.runner.PatchingAssert.assertFileExists;
import static org.jboss.as.patching.runner.PatchingAssert.assertPatchHasBeenApplied;
import static org.jboss.as.patching.runner.PatchingTask.NO_CONTENT;
import static org.jboss.as.patching.runner.TestUtils.createPatchXMLFile;
import static org.jboss.as.patching.runner.TestUtils.createZippedPatchFile;
import static org.jboss.as.patching.runner.TestUtils.dump;
import static org.jboss.as.patching.runner.TestUtils.mkdir;
import static org.jboss.as.patching.runner.TestUtils.randomString;
import static org.jboss.as.patching.runner.TestUtils.touch;
import static org.junit.Assert.assertArrayEquals;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;

import org.jboss.as.patching.LocalPatchInfo;
import org.jboss.as.patching.PatchInfo;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.MiscContentItem;
import org.jboss.as.patching.metadata.ModificationType;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.Patch.PatchType;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012, Red Hat Inc
 */
public class FileTaskTestCase extends AbstractTaskTestCase {

    @Test
    public void testAddFile() throws Exception {

        // start from a base installation
        PatchInfo info = new LocalPatchInfo(randomString(), PatchInfo.BASE, Collections.<String>emptyList(), env);

        // build a one-off patch for the base installation
        // with 1 added file
        String patchID = randomString();
        File patchDir = mkdir(tempDir, patchID);
        String fileName =  "my-new-standalone.sh";
        File newFile = touch(patchDir, "misc", "bin", fileName);
        dump(newFile, "new file resource");
        byte[] newHash = calculateHash(newFile);
        ContentModification fileAdded = new ContentModification(new MiscContentItem(fileName, new String[] { "bin" }, newHash), NO_CONTENT, ADD);

        Patch patch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription(randomString())
                .setPatchType(PatchType.ONE_OFF)
                .addAppliesTo(info.getVersion())
                .addContentModification(fileAdded)
                .build();

        createPatchXMLFile(patchDir, patch);
        File zippedPatch = createZippedPatchFile(patchDir, patch.getPatchId());

        PatchingTaskRunner runner = new PatchingTaskRunner(info, env);
        PatchingResult result = runner.executeDirect(new FileInputStream(zippedPatch), ContentVerificationPolicy.STRICT);

        assertPatchHasBeenApplied(result, patch);

        assertFileExists(env.getInstalledImage().getJbossHome(), "bin", fileName);
    }

    @Test
    public void testRemoveFile() throws Exception {

        // start from a base installation
        PatchInfo info = new LocalPatchInfo(randomString(), PatchInfo.BASE, Collections.<String>emptyList(), env);
        // with a file in it
        String fileName = "standalone.sh";
        File standaloneShellFile = touch(env.getInstalledImage().getJbossHome(), "bin", fileName );
        dump(standaloneShellFile, "original script to run standalone AS7");
        byte[] existingHash = calculateHash(standaloneShellFile);

        // build a one-off patch for the base installation
        // with 1 removed file
        ContentModification fileRemoved = new ContentModification(new MiscContentItem(fileName, new String[] { "bin" }, NO_CONTENT), existingHash, REMOVE);

        Patch patch = PatchBuilder.create()
                .setPatchId(randomString())
                .setDescription(randomString())
                .setPatchType(PatchType.ONE_OFF)
                .addAppliesTo(info.getVersion())
                .addContentModification(fileRemoved)
                .build();

        // create the patch
        File patchDir = mkdir(tempDir, patch.getPatchId());
        createPatchXMLFile(patchDir, patch);
        File zippedPatch = createZippedPatchFile(patchDir, patch.getPatchId());

        PatchingTaskRunner runner = new PatchingTaskRunner(info, env);
        PatchingResult result = runner.executeDirect(new FileInputStream(zippedPatch), ContentVerificationPolicy.STRICT);

        assertPatchHasBeenApplied(result, patch);

        /// file has been removed from the AS7 installation
        assertFileDoesNotExist(standaloneShellFile);
        // but it's been backed up
        assertFileExists(env.getHistoryDir(patch.getPatchId()), "misc", "bin", fileName);
    }

    @Test
    public void testUpdateFile() throws Exception {

        // start from a base installation
        PatchInfo info = new LocalPatchInfo(randomString(), PatchInfo.BASE, Collections.<String>emptyList(), env);
        // with a file in it
        File binDir = mkdir(env.getInstalledImage().getJbossHome(), "bin");
        String fileName = "standalone.sh";
        File standaloneShellFile = touch(binDir, fileName);
        dump(standaloneShellFile, "original script to run standalone AS7");
        byte[] existingHash = calculateHash(standaloneShellFile);

        // build a one-off patch for the base installation
        // with 1 updated file
        String patchID = randomString();
        File patchDir = mkdir(tempDir, patchID);

        File updatedFile = touch(patchDir, "misc", "bin", fileName);
        dump(updatedFile, "updated script");
        byte[] updatedHash = calculateHash(updatedFile);
        ContentModification fileUpdated = new ContentModification(new MiscContentItem(fileName, new String[] { "bin" }, updatedHash), existingHash, MODIFY);

        Patch patch = PatchBuilder.create()
                .setPatchId(randomString())
                .setDescription(randomString())
                .setPatchType(PatchType.ONE_OFF)
                .addAppliesTo(info.getVersion())
                .addContentModification(fileUpdated)
                .build();

        // create the patch
        createPatchXMLFile(patchDir, patch);
        File zippedPatch = createZippedPatchFile(patchDir, patch.getPatchId());

        PatchingTaskRunner runner = new PatchingTaskRunner(info, env);
        PatchingResult result = runner.executeDirect(new FileInputStream(zippedPatch), ContentVerificationPolicy.STRICT);

        assertPatchHasBeenApplied(result, patch);

        /// file has been updated in the AS7 installation
        // and it's the new one
        assertFileExists(standaloneShellFile);
        assertArrayEquals(updatedHash, calculateHash(standaloneShellFile));
        // the existing file has been backed up
        File backupFile = assertFileExists(env.getHistoryDir(patch.getPatchId()), "misc", "bin", fileName);
        assertArrayEquals(existingHash, calculateHash(backupFile));        
    }
}
