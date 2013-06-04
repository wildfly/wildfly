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
package org.jboss.as.patching.runner;

import static org.jboss.as.patching.HashUtils.hashFile;
import static org.jboss.as.patching.metadata.ModificationType.MODIFY;
import static org.jboss.as.patching.runner.PatchingAssert.assertFileExists;
import static org.jboss.as.patching.runner.PatchingAssert.assertPatchHasBeenApplied;
import static org.jboss.as.patching.runner.PatchingAssert.assertPatchHasNotBeenApplied;
import static org.jboss.as.patching.runner.TestUtils.createPatchXMLFile;
import static org.jboss.as.patching.runner.TestUtils.createZippedPatchFile;
import static org.jboss.as.patching.runner.TestUtils.dump;
import static org.jboss.as.patching.IoUtils.mkdir;
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
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.jboss.as.patching.metadata.impl.IdentityImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class UpdateModifiedFileTaskTestCase extends AbstractTaskTestCase {

    private PatchingRunnerWrapper runner;
    private File zippedPatch;
    private Patch patch;
    private ContentModification fileUpdated;
    private File modifiedFile;
    private byte[] expectedModifiedHash;
    private byte[] updatedHash;


    @Before
    public void setUp() throws Exception{
        // start from a base installation
        PatchInfo info = new LocalPatchInfo(randomString(), PatchInfo.BASE, Collections.<String>emptyList(), env);
        // with a file in it
        File binDir = mkdir(env.getInstalledImage().getJbossHome(), "bin");
        String fileName = "standalone.sh";
        modifiedFile = touch(binDir, fileName);
        dump(modifiedFile, "modified script to run standalone AS7");
        expectedModifiedHash = hashFile(modifiedFile);
        // let's simulate that the file has been modified by the users by using a hash that is not the file checksum
        byte[] unmodifiedHash = randomString().getBytes();

        // build a one-off patch for the base installation
        // with 1 updated file
        String patchID = randomString();
        File patchDir = mkdir(tempDir, patchID);

        File updatedFile = touch(patchDir, "misc", "bin", fileName);
        dump(updatedFile, "updated script");
        updatedHash = hashFile(updatedFile);
        fileUpdated = new ContentModification(new MiscContentItem(fileName, new String[] { "bin" }, updatedHash), unmodifiedHash, MODIFY);

        PatchBuilder builder = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription(randomString())
                .setIdentity(new IdentityImpl("eap", info.getVersion()))
                .setNoUpgrade();

//        PatchElementImpl element = new PatchElementImpl("patch element 01");
//        builder.addElement(element);
//        element.setDescription("patch element 01 description");
//        element.setNoUpgrade();
//
//        PatchElementProviderImpl provider = new PatchElementProviderImpl("base", "4.5.6", false);
//        provider.require("patch element 02");
//        element.setProvider(provider);

        builder.addContentModification(fileUpdated);

        patch = builder.build();

        // create the patch
        createPatchXMLFile(patchDir, patch);
        zippedPatch = createZippedPatchFile(patchDir, patch.getPatchId());

        runner = PatchingRunnerWrapper.Factory.create(info, env);
    }

    @After
    public void tearDown() {
        runner = null;
        zippedPatch = null;
        patch = null;
        fileUpdated = null;
        modifiedFile = null;
        expectedModifiedHash = null;
         updatedHash = null;
    }

    @Test
    public void testUpdateModifiedFileWithSTRICT() throws Exception {
        try {
            runner.executeDirect(new FileInputStream(zippedPatch), ContentVerificationPolicy.STRICT);
        } catch (PatchingException e) {
            assertPatchHasNotBeenApplied(e, patch, fileUpdated.getItem(), env);

            /// file has not been modified in the AS7 installation
            assertFileExists(modifiedFile);
            assertArrayEquals(expectedModifiedHash, hashFile(modifiedFile));
        }
    }

    @Test
    public void testUpdateModifiedFileWithOVERRIDE_ALL() throws Exception {
        PatchingResult result = runner.executeDirect(new FileInputStream(zippedPatch), ContentVerificationPolicy.OVERRIDE_ALL);

        assertPatchHasBeenApplied(result, patch);

        /// file has been updated in the AS7 installation
        // and it's the new one
        assertFileExists(modifiedFile);
        assertArrayEquals(updatedHash, hashFile(modifiedFile));
        // the existing file has been backed up
        File backupFile = assertFileExists(env.getInstalledImage().getPatchHistoryDir(patch.getPatchId()), "misc", "bin", modifiedFile.getName());
        assertArrayEquals(expectedModifiedHash, hashFile(backupFile));
    }

    @Test
    public void testUpdateModifiedFileWithPRESERVE_ALL() throws Exception {
        try {
            runner.executeDirect(new FileInputStream(zippedPatch), ContentVerificationPolicy.PRESERVE_ALL);
        } catch (PatchingException e) {
            assertPatchHasNotBeenApplied(e, patch, fileUpdated.getItem(), env);

            /// file has not been modified in the AS7 installation
            assertFileExists(modifiedFile);
            assertArrayEquals(expectedModifiedHash, hashFile(modifiedFile));
        }
    }
}
