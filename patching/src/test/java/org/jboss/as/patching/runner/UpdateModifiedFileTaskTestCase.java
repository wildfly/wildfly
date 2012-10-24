package org.jboss.as.patching.runner;

import static org.jboss.as.patching.metadata.ModificationType.MODIFY;
import static org.jboss.as.patching.runner.PatchUtils.calculateHash;
import static org.jboss.as.patching.runner.PatchingAssert.assertFileExists;
import static org.jboss.as.patching.runner.PatchingAssert.assertPatchHasBeenApplied;
import static org.jboss.as.patching.runner.PatchingAssert.assertPatchHasNotBeenApplied;
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
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class UpdateModifiedFileTaskTestCase extends AbstractTaskTestCase {

    private PatchingTaskRunner runner;
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
        expectedModifiedHash = calculateHash(modifiedFile);
        // let's simulate that the file has been modified by the users by using a hash that is not the file checksum
        byte[] unmodifiedHash = randomString().getBytes();

        // build a one-off patch for the base installation
        // with 1 updated file
        String patchID = randomString();
        File patchDir = mkdir(tempDir, patchID);

        File updatedFile = touch(patchDir, "misc", "bin", fileName);
        dump(updatedFile, "updated script");
        updatedHash = calculateHash(updatedFile);
        fileUpdated = new ContentModification(new MiscContentItem(fileName, new String[] { "bin" }, updatedHash), unmodifiedHash, MODIFY);

        patch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription(randomString())
                .setOneOffType(info.getVersion())
                .addContentModification(fileUpdated)
                .build();

        // create the patch
        createPatchXMLFile(patchDir, patch);
        zippedPatch = createZippedPatchFile(patchDir, patch.getPatchId());

        runner = new PatchingTaskRunner(info, env);
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

        PatchingResult result = runner.executeDirect(new FileInputStream(zippedPatch), ContentVerificationPolicy.STRICT);


        assertPatchHasNotBeenApplied(result, patch, fileUpdated.getItem());

        /// file has not been modified in the AS7 installation
        assertFileExists(modifiedFile);
        assertArrayEquals(expectedModifiedHash, calculateHash(modifiedFile));
    }

    @Test
    public void testUpdateModifiedFileWithOVERRIDE_ALL() throws Exception {
        PatchingResult result = runner.executeDirect(new FileInputStream(zippedPatch), ContentVerificationPolicy.OVERRIDE_ALL);

        assertPatchHasBeenApplied(result, patch);

        /// file has been updated in the AS7 installation
        // and it's the new one
        assertFileExists(modifiedFile);
        assertArrayEquals(updatedHash, calculateHash(modifiedFile));
        // the existing file has been backed up
        File backupFile = assertFileExists(env.getHistoryDir(patch.getPatchId()), "misc", "bin", modifiedFile.getName());
        assertArrayEquals(expectedModifiedHash, calculateHash(backupFile));
    }

    @Test
    public void testUpdateModifiedFileWithPRESERVE_ALL() throws Exception {

        PatchingResult result = runner.executeDirect(new FileInputStream(zippedPatch), ContentVerificationPolicy.PRESERVE_ALL);

        assertPatchHasNotBeenApplied(result, patch, fileUpdated.getItem());

        /// file has not been modified in the AS7 installation
        assertFileExists(modifiedFile);
        assertArrayEquals(expectedModifiedHash, calculateHash(modifiedFile));
    }
}
