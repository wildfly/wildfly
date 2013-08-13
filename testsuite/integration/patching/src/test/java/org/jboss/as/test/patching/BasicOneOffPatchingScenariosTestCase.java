/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.jboss.as.test.patching;

import java.io.File;
import java.util.List;

import com.google.common.base.Joiner;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.jboss.as.version.ProductConfig;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.patching.Constants.BASE;
import static org.jboss.as.patching.Constants.LAYERS;
import static org.jboss.as.patching.Constants.MODULES;
import static org.jboss.as.patching.Constants.SYSTEM;
import static org.jboss.as.patching.IoUtils.mkdir;
import static org.jboss.as.patching.IoUtils.newFile;
import static org.jboss.as.test.patching.PatchingTestUtil.AS_DISTRIBUTION;
import static org.jboss.as.test.patching.PatchingTestUtil.AS_VERSION;
import static org.jboss.as.test.patching.PatchingTestUtil.CONTAINER;
import static org.jboss.as.test.patching.PatchingTestUtil.FILE_SEPARATOR;
import static org.jboss.as.test.patching.PatchingTestUtil.PATCHES_PATH;
import static org.jboss.as.test.patching.PatchingTestUtil.PRODUCT;
import static org.jboss.as.test.patching.PatchingTestUtil.createModule0;
import static org.jboss.as.test.patching.PatchingTestUtil.createPatchXMLFile;
import static org.jboss.as.test.patching.PatchingTestUtil.createZippedPatchFile;
import static org.jboss.as.test.patching.PatchingTestUtil.randomString;
import static org.jboss.as.test.patching.PatchingTestUtil.readFile;


/**
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
@RunAsClient
public class BasicOneOffPatchingScenariosTestCase extends AbstractPatchingTestCase {

    private static final Logger logger = Logger.getLogger(BasicOneOffPatchingScenariosTestCase.class);

    /**
     * Prepare a one-off patch which adds a misc file. Apply it, check that the file was created.
     * Roll it back, check that the file was deleted.
     */
    @Test
    public void testOneOffPatchAddingAMiscFile() throws Exception {
        final String fileContent = "Hello World!";
        // prepare the patch
        String patchID = randomString();
        File oneOffPatchDir = mkdir(tempDir, patchID);
        ContentModification miscFileAdded = ContentModificationUtils.addMisc(oneOffPatchDir, patchID,
                fileContent, "awesomeDirectory", "awesomeFile");
        ProductConfig productConfig = new ProductConfig(PRODUCT, AS_VERSION, "main");
        Patch oneOffPatch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription("A one-off patch adding a misc file.")
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                .getParent()
                .addContentModification(miscFileAdded)
                .build();
        createPatchXMLFile(oneOffPatchDir, oneOffPatch);
        File zippedPatch = createZippedPatchFile(oneOffPatchDir, patchID);

        // apply the patch and check if server is in restart-required mode
        controller.start(CONTAINER);
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        String path = AS_DISTRIBUTION + FILE_SEPARATOR + Joiner.on(FILE_SEPARATOR).join(new String[]{"awesomeDirectory", "awesomeFile"});
        Assert.assertTrue("File " + path + " should exist", new File(path).exists());
        Assert.assertTrue("The patch " + patchID + " should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertEquals("Unexpected contents of misc file", fileContent, readFile(path));


        // rollback the patch and check if server is in restart-required mode
        Assert.assertTrue("Rollback should be accepted", CliUtilsForPatching.rollbackPatch(patchID));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertFalse("The patch " + patchID + " NOT should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertFalse("File + " + path + " should have been deleted", new File(path).exists());

        //reapply patch and check if server is in restart-required mode
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertTrue("File " + path + " should exist", new File(path).exists());
        Assert.assertTrue("The patch " + patchID + " should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertEquals("Unexpected contents of misc file", fileContent, readFile(path));
        controller.stop(CONTAINER);
    }

    /**
     * Prepare a one-off patch which adds multiple (2) misc files. Apply it, check that the files was created.
     * Roll it back, check that the files was deleted and apply it again to make sure re-applying works as expected
     */
    @Test
    public void testOneOffPatchAddingMultipleMiscFiles() throws Exception {
        // prepare the patch
        String patchID = randomString();
        File oneOffPatchDir = mkdir(tempDir, patchID);

        final String[] testFileSegments1 = new String[]{"testDir1", "testFile1.txt"};
        final String testFilePath1 = AS_DISTRIBUTION + FILE_SEPARATOR + Joiner.on(FILE_SEPARATOR).join(testFileSegments1);
        final String testContent1 = "test content1";

        final String[] testFileSegments2 = new String[]{"directory with spaces", "file with spaces"};
        final String testFilePath2 = AS_DISTRIBUTION + FILE_SEPARATOR + Joiner.on(FILE_SEPARATOR).join(testFileSegments2);
        final String testContent2 = "test content2";

        ContentModification miscFileAdded1 = ContentModificationUtils.addMisc(oneOffPatchDir, patchID,
                testContent1, testFileSegments1);
        ContentModification miscFileAdded2 = ContentModificationUtils.addMisc(oneOffPatchDir, patchID,
                testContent2, testFileSegments2);
        ProductConfig productConfig = new ProductConfig(PRODUCT, AS_VERSION, "main");
        Patch oneOffPatch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription("A one-off patch adding a misc file.")
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                .getParent()
                .addContentModification(miscFileAdded1)
                .addContentModification(miscFileAdded2)
                .build();
        createPatchXMLFile(oneOffPatchDir, oneOffPatch);
        File zippedPatch = createZippedPatchFile(oneOffPatchDir, patchID);

        // apply the patch and check if server is in restart-required mode
        controller.start(CONTAINER);
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // check if patch is installed, if files exists and check content of files
        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + patchID + " should be listed as installed", CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertTrue("File1 " + testFilePath1 + " should exist", new File(testFilePath1).exists());
        Assert.assertTrue("File2 " + testFilePath2 + " should exist", new File(testFilePath2).exists());
        String patchContent = readFile(testFilePath1);
        Assert.assertEquals("check content of file1 after applying patch", testContent1, patchContent);
        patchContent = readFile(testFilePath2);
        Assert.assertEquals("check content of file2 after applying patch", testContent2, patchContent);

        // rollback the patch and check if server is in restart-required mode
        Assert.assertTrue("Rollback should be accepted", CliUtilsForPatching.rollbackPatch(patchID));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // check if patch is uninstalled, if files don't exists
        controller.start(CONTAINER);
        Assert.assertFalse("The patch " + patchID + " NOT should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertFalse("File1 + " + testFilePath1 + " should have been deleted", new File(testFilePath1).exists());
        Assert.assertFalse("File2 + " + testFilePath1 + " should have been deleted", new File(testFilePath1).exists());

        // reapply the patch and check if server is in restart-required mode
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // check if patch is installed, if files exists and check content of files
        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + patchID + " should be listed as installed", CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertTrue("File1 " + testFilePath1 + " should exist", new File(testFilePath1).exists());
        Assert.assertTrue("File2 " + testFilePath2 + " should exist", new File(testFilePath2).exists());
        patchContent = readFile(testFilePath1);
        Assert.assertEquals("check content of file1 after applying patch", testContent1, patchContent);
        patchContent = readFile(testFilePath2);
        Assert.assertEquals("check content of file2 after applying patch", testContent2, patchContent);
        controller.stop(CONTAINER);
    }

    /**
     * Prepare a one-off patch which modifies a misc file. Apply it, check that the file was replaced.
     * Roll it back, check that the file was restored successfully.
     */
    @Test
    public void testOneOffPatchModifyingAMiscFile() throws Exception {
        // prepare the patch
        String patchID = randomString();
        File oneOffPatchDir = mkdir(tempDir, patchID);

        final String testFilePath = AS_DISTRIBUTION + FILE_SEPARATOR + "README.txt";
        final String testContent = "test content";
        final String originalContent = readFile(testFilePath);

        ContentModification miscFileModified = ContentModificationUtils.modifyMisc(oneOffPatchDir, patchID, testContent, new File(testFilePath), "README.txt");
        ProductConfig productConfig = new ProductConfig(PRODUCT, AS_VERSION, "consoleSlot");
        Patch oneOffPatch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription("A one-off patch modifying a misc file.")
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                .getParent()
                .addContentModification(miscFileModified)
                .build();
        createPatchXMLFile(oneOffPatchDir, oneOffPatch);
        File zippedPatch = createZippedPatchFile(oneOffPatchDir, patchID);


        // apply the patch and check if server is in restart-required mode
        controller.start(CONTAINER);
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        //check if patch is installed, check content of file
        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + patchID + " should be listed as installed", CliUtilsForPatching.getInstalledPatches().contains(patchID));
        String patchContent = readFile(testFilePath);
        Assert.assertEquals(testContent, patchContent);

        //rollback the patch and check if server is in restart-required mode
        Assert.assertTrue("Rollback should be accepted", CliUtilsForPatching.rollbackPatch(patchID));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        //check if patch is uninstalled, check content of file
        controller.start(CONTAINER);
        Assert.assertFalse("The patch " + patchID + " NOT should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        patchContent = readFile(testFilePath);
        Assert.assertEquals("check content of file after rollback", originalContent, patchContent);

        //reapply the patch and check if server is in restart-required mode
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        //check if patch is installed, check content of file
        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + patchID + " should be listed as installed", CliUtilsForPatching.getInstalledPatches().contains(patchID));
        patchContent = readFile(testFilePath);
        Assert.assertEquals("check content of file after reapplying", testContent, patchContent);
        controller.stop(CONTAINER);
    }

    /**
     * Prepare a one-off patch which modifies multiple (2) misc file. Apply it, check that the files was replaced.
     * Roll it back, check that the files was restored successfully and apply it again to make sure re-applying works as expected
     */
    @Test
    public void testOneOffPatchModifyingMultipleMiscFiles() throws Exception {
        // prepare the patch
        String patchID = randomString();
        File oneOffPatchDir = mkdir(tempDir, patchID);

        final String testFilePath1 = AS_DISTRIBUTION + FILE_SEPARATOR + "README.txt";
        final String testContent1 = "test content1";
        final String originalContent1 = readFile(testFilePath1);

        final String testFilePath2 = AS_DISTRIBUTION + FILE_SEPARATOR + "LICENSE.txt";
        final String testContent2 = "test content2";
        final String originalContent2 = readFile(testFilePath2);

        ContentModification miscFileModified1 = ContentModificationUtils.modifyMisc(oneOffPatchDir, patchID, testContent1, new File(testFilePath1), "README.txt");
        ContentModification miscFileModified2 = ContentModificationUtils.modifyMisc(oneOffPatchDir, patchID, testContent2, new File(testFilePath2), "LICENSE.txt");
        ProductConfig productConfig = new ProductConfig(PRODUCT, AS_VERSION, "consoleSlot");
        Patch oneOffPatch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription("A one-off patch modifying multiple misc files.")
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                .getParent()
                .addContentModification(miscFileModified1)
                .addContentModification(miscFileModified2)
                .build();
        createPatchXMLFile(oneOffPatchDir, oneOffPatch);
        File zippedPatch = createZippedPatchFile(oneOffPatchDir, patchID);


        // apply the patch and check if server is in restart-required mode
        controller.start(CONTAINER);
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        //check content
        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + patchID + " should be listed as installed", CliUtilsForPatching.getInstalledPatches().contains(patchID));
        String patchContent1 = readFile(testFilePath1);
        Assert.assertEquals("check content of applying patch1", testContent1, patchContent1);
        String patchContent2 = readFile(testFilePath2);
        Assert.assertEquals("check content of applying patch2", testContent2, patchContent2);

        //rollback the patch and check if server is in restart-required mode
        Assert.assertTrue("Rollback should be accepted", CliUtilsForPatching.rollbackPatch(patchID));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        //check content
        controller.start(CONTAINER);
        Assert.assertFalse("The patch " + patchID + " NOT should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        patchContent1 = readFile(testFilePath1);
        Assert.assertEquals("check content of file after rollback1", originalContent1, patchContent1);
        patchContent2 = readFile(testFilePath2);
        Assert.assertEquals("check content of file after rollback2", originalContent2, patchContent2);

        //reapply the patch and check if server is in restart-required mode
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        //check content
        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + patchID + " should be listed as installed", CliUtilsForPatching.getInstalledPatches().contains(patchID));
        patchContent1 = readFile(testFilePath1);
        Assert.assertEquals("check content of file after reapplying1", testContent1, patchContent1);
        patchContent2 = readFile(testFilePath2);
        Assert.assertEquals("check content of file after reapplying2", testContent2, patchContent2);
        controller.stop(CONTAINER);
    }

    /**
     * Prepare a one-off patch which removes a misc file. Apply it, check that the file was removed.
     * Roll it back, check that the file was restored and reapply patch
     */
    @Test
    public void testOneOffPatchDeletingAMiscFile() throws Exception {
        // prepare the patch
        String patchID = randomString();
        File oneOffPatchDir = mkdir(tempDir, patchID);

        final String testFilePath = AS_DISTRIBUTION + FILE_SEPARATOR + "README.txt";

        // store original content
        String originalContent = readFile(testFilePath);

        ContentModification miscFileRemoved = ContentModificationUtils.removeMisc(new File(testFilePath), "");
        ProductConfig productConfig = new ProductConfig(PRODUCT, AS_VERSION, "main");
        Patch oneOffPatch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription("A one-off patch removing a misc file.")
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                .getParent()
                .addContentModification(miscFileRemoved)
                .build();
        createPatchXMLFile(oneOffPatchDir, oneOffPatch);
        File zippedPatch = createZippedPatchFile(oneOffPatchDir, patchID);

        // apply the patch and check if server is in restart-required mode
        controller.start(CONTAINER);
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // check that patch is installed, file doesn't exist
        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + patchID + " should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertFalse("File " + testFilePath + " should have been deleted", new File(testFilePath).exists());

        // rollback the patch and check if server is in restart-required mode
        Assert.assertTrue("Rollback should be accepted", CliUtilsForPatching.rollbackPatch(patchID));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // check that the patch is uninstalled and file is restored
        controller.start(CONTAINER);
        Assert.assertFalse("The patch " + patchID + " NOT should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertTrue("File + " + testFilePath + " should be restored", new File(testFilePath).exists());
        Assert.assertEquals("Unexpected contents of misc file", originalContent, readFile(testFilePath));

        // reapply patch and check if server is in restart-required mode
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // check that patch is installed, file doesn't exist
        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + patchID + " should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertFalse("File " + testFilePath + " should have been deleted", new File(testFilePath).exists());
        controller.stop(CONTAINER);
    }

    /**
     * Prepare a one-off patch which deletes multiple (2) misc files. Apply it, check that the file was removed.
     * Roll it back, check that the file was restored and reapply patch
     */
    @Test
    public void testOneOffPatchDeletingMultipleMiscFiles() throws Exception {
        // prepare the patch
        String patchID = randomString();
        File oneOffPatchDir = mkdir(tempDir, patchID);

        final String testFilePath1 = AS_DISTRIBUTION + FILE_SEPARATOR + "README.txt";
        final String testFilePath2 = AS_DISTRIBUTION + FILE_SEPARATOR + "LICENSE.txt";

        // store original content
        String originalContent1 = readFile(testFilePath1);
        String originalContent2 = readFile(testFilePath2);

        ContentModification miscFileRemoved1 = ContentModificationUtils.removeMisc(new File(testFilePath1),
                "");
        ContentModification miscFileRemoved2 = ContentModificationUtils.removeMisc(new File(testFilePath2), "");
        ProductConfig productConfig = new ProductConfig(PRODUCT, AS_VERSION, "main");
        Patch oneOffPatch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription("A one-off patch removing a misc file.")
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                .getParent()
                .addContentModification(miscFileRemoved1)
                .addContentModification(miscFileRemoved2)
                .build();
        createPatchXMLFile(oneOffPatchDir, oneOffPatch);
        File zippedPatch = createZippedPatchFile(oneOffPatchDir, patchID);

        // apply the patch and check if server is in restart-required mode
        controller.start(CONTAINER);
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // check that patch is installed, files don't exist
        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + patchID + " should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertFalse("File " + testFilePath1 + " should have been deleted",
                new File(testFilePath1).exists());
        Assert.assertFalse("File " + testFilePath2 + " should have been deleted",
                new File(testFilePath2).exists());

        // rollback the patch and check if server is in restart-required mode
        Assert.assertTrue("Rollback should be accepted", CliUtilsForPatching.rollbackPatch(patchID));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // check that the patch is uninstalled and file is restored
        controller.start(CONTAINER);
        Assert.assertFalse("The patch " + patchID + " NOT should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertTrue("File + " + testFilePath1 + " should be restored", new File(testFilePath1).exists());
        Assert.assertTrue("File + " + testFilePath2 + " should be restored", new File(testFilePath2).exists());
        Assert.assertEquals("Unexpected contents of misc file", originalContent1, readFile(testFilePath1));
        Assert.assertEquals("Unexpected contents of misc file", originalContent2, readFile(testFilePath2));

        // reapply patch and check if server is in restart-required mode
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // check that patch is installed, file doesn't exist
        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + patchID + " should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertFalse("File " + testFilePath1 + " should have been deleted", new File(testFilePath1).exists());
        Assert.assertFalse("File " + testFilePath2 + " should have been deleted",
                new File(testFilePath2).exists());
        controller.stop(CONTAINER);
    }

    /**
     * Prepare a one-off patch which modifies misc file and deletes another misc file. Apply it, check that the files was replaced a deleted.
     * Roll it back, check that the files was restored, created and apply it again to make sure re-applying works as expected
     */
    @Test
    public void testOneOffPatchModifyingAMiscFileDeletingAnotherMiscFile() throws Exception {
        // prepare the patch
        String patchID = randomString();
        File oneOffPatchDir = mkdir(tempDir, patchID);

        final String testFilePath1 = AS_DISTRIBUTION + FILE_SEPARATOR + "README.txt";
        final String testContent1 = "test content1";
        final String originalContent1 = readFile(testFilePath1);

        final String testFilePathDeleted = AS_DISTRIBUTION + FILE_SEPARATOR + "LICENSE.txt";
        final String originalContentOfDeletedFile = readFile(testFilePathDeleted);

        ContentModification miscFileModified = ContentModificationUtils.modifyMisc(oneOffPatchDir, patchID, testContent1, new File(testFilePath1), "README.txt");
        ContentModification miscFileDeleted = ContentModificationUtils.removeMisc(new File(testFilePathDeleted), "");
        ProductConfig productConfig = new ProductConfig(PRODUCT, AS_VERSION, "main");
        Patch oneOffPatch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription("A one-off patch modifying one misc file and deleting another misc file.")
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                .getParent()
                .addContentModification(miscFileModified)
                .addContentModification(miscFileDeleted)
                .build();
        createPatchXMLFile(oneOffPatchDir, oneOffPatch);
        File zippedPatch = createZippedPatchFile(oneOffPatchDir, patchID);

        // apply the patch and check if server is in restart-required mode
        controller.start(CONTAINER);
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // check if patch is installed, if files are created, deleted and check content
        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + patchID + " should be listed as installed", CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertEquals("Unexpected contents of misc file", testContent1, readFile(testFilePath1));
        Assert.assertFalse("File " + testFilePathDeleted + " should have been deleted", new File(testFilePathDeleted).exists());

        // rollback the patch  and check if server is in restart-required mode
        Assert.assertTrue("Rollback should be accepted", CliUtilsForPatching.rollbackPatch(patchID));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // check if patch is uninstalled
        controller.start(CONTAINER);
        Assert.assertFalse("The patch " + patchID + " NOT should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertEquals("Unexpected contents of misc file", originalContent1, readFile(testFilePath1));
        Assert.assertTrue("File + " + testFilePathDeleted + " should exist", new File(testFilePathDeleted).exists());
        Assert.assertEquals("Unexpected contents of misc file", originalContentOfDeletedFile, readFile(testFilePathDeleted));

        // reapply the patch  and check if server is in restart-required mode
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // check if patch is installed, if files exists and check content of files
        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + patchID + " should be listed as installed", CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertEquals("Unexpected contents of misc file", testContent1, readFile(testFilePath1));
        Assert.assertFalse("File " + testFilePathDeleted + " should have been deleted", new File(testFilePathDeleted).exists());
        controller.stop(CONTAINER);
    }

    /**
     * Prepare a one-off patch which modifies multiple (2) misc file and deletes multiple (2) misc files. Apply it, check that the files was replaced, deleted.
     * Roll it back, check that the files was restored successfully and apply it again to make sure re-applying works as expected
     */
    @Test
    public void testOneOffPatchModifyingMultipleMiscFilesDeletingMultipleMiscFiles() throws Exception {
        // prepare the patch
        String patchID = randomString();
        File oneOffPatchDir = mkdir(tempDir, patchID);

        final String testFilePath1 = AS_DISTRIBUTION + FILE_SEPARATOR + "README.txt";
        final String testContent1 = "test content1";
        final String originalContent1 = readFile(testFilePath1);

        final String testFilePath2 = AS_DISTRIBUTION + FILE_SEPARATOR + "LICENSE.txt";
        final String testContent2 = "test content2";
        final String originalContent2 = readFile(testFilePath2);

        final String[] testFilePathSegments1 = new String[]{"welcome-content", "documentation.html"};
        final String testFilePathDeleted1 = AS_DISTRIBUTION + FILE_SEPARATOR + Joiner.on(FILE_SEPARATOR).join(testFilePathSegments1);
        final String originalContentOfDeletedFile1 = readFile(testFilePathDeleted1);
        final String[] testFilePathSegments2 = new String[]{"welcome-content", "index.html"};
        final String testFilePathDeleted2 = AS_DISTRIBUTION + FILE_SEPARATOR + Joiner.on(FILE_SEPARATOR).join(testFilePathSegments2);
        final String originalContentOfDeletedFile2 = readFile(testFilePathDeleted2);

        ContentModification miscFileModified1 = ContentModificationUtils.modifyMisc(oneOffPatchDir, patchID, testContent1, new File(testFilePath1), "README.txt");
        ContentModification miscFileModified2 = ContentModificationUtils.modifyMisc(oneOffPatchDir, patchID, testContent2, new File(testFilePath2), "LICENSE.txt");
        ContentModification miscFileDeleted1 = ContentModificationUtils.removeMisc(new File(testFilePathDeleted1), testFilePathSegments1);
        ContentModification miscFileDeleted2 = ContentModificationUtils.removeMisc(new File(testFilePathDeleted2), testFilePathSegments2);
        ProductConfig productConfig = new ProductConfig(PRODUCT, AS_VERSION, "consoleSlot");
        Patch oneOffPatch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription("A one-off patch modifying multiple misc files.")
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                .getParent()
                .addContentModification(miscFileModified1)
                .addContentModification(miscFileModified2)
                .addContentModification(miscFileDeleted1)
                .addContentModification(miscFileDeleted2)
                .build();
        createPatchXMLFile(oneOffPatchDir, oneOffPatch);
        File zippedPatch = createZippedPatchFile(oneOffPatchDir, patchID);


        // apply the patch and check if server is in restart-required mode
        controller.start(CONTAINER);
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        //check content
        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + patchID + " should be listed as installed", CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertEquals("Unexpected contents of misc file", testContent1, readFile(testFilePath1));
        Assert.assertEquals("Unexpected contents of misc file", testContent2, readFile(testFilePath2));
        Assert.assertFalse("File " + testFilePathDeleted1 + " should have been deleted", new File(testFilePathDeleted1).exists());
        Assert.assertFalse("File " + testFilePathDeleted2 + " should have been deleted", new File(testFilePathDeleted2).exists());

        //rollback the patch and check if server is in restart-required mode
        Assert.assertTrue("Rollback should be accepted", CliUtilsForPatching.rollbackPatch(patchID));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        //check content
        controller.start(CONTAINER);
        Assert.assertFalse("The patch " + patchID + " NOT should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertEquals("Unexpected contents of misc file", originalContent1, readFile(testFilePath1));
        Assert.assertEquals("Unexpected contents of misc file", originalContent2, readFile(testFilePath2));
        Assert.assertTrue("File + " + testFilePathDeleted1 + " should exist", new File(testFilePathDeleted1).exists());
        Assert.assertEquals("Unexpected contents of misc file", originalContentOfDeletedFile1, readFile(testFilePathDeleted1));
        Assert.assertTrue("File + " + testFilePathDeleted2 + " should exist", new File(testFilePathDeleted2).exists());
        Assert.assertEquals("Unexpected contents of misc file", originalContentOfDeletedFile2, readFile(testFilePathDeleted2));

        //reapply the patch and check if server is in restart-required mode
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        //check content
        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + patchID + " should be listed as installed", CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertEquals("Unexpected contents of misc file", testContent1, readFile(testFilePath1));
        Assert.assertEquals("Unexpected contents of misc file", testContent2, readFile(testFilePath2));
        Assert.assertFalse("File " + testFilePathDeleted1 + " should have been deleted", new File(testFilePathDeleted1).exists());
        Assert.assertFalse("File " + testFilePathDeleted2 + " should have been deleted", new File(testFilePathDeleted2).exists());
        controller.stop(CONTAINER);
    }

    /**
     * Prepare a one-off patch which adds misc file and deletes another misc file. Apply it, check that the files was created a deleted.
     * Roll it back, check that the files was deleted, created and apply it again to make sure re-applying works as expected
     */
    @Test
    public void testOneOffPatchAddingAMiscFileDeletingAnotherMiscFile() throws Exception {
        // prepare the patch
        String patchID = randomString();
        File oneOffPatchDir = mkdir(tempDir, patchID);

        final String[] testFileSegmentsAdded = new String[]{"testDir1", "testFile1.txt"};
        final String testFilePathAdded = AS_DISTRIBUTION + FILE_SEPARATOR + Joiner.on(FILE_SEPARATOR).join(testFileSegmentsAdded);
        final String testContentAdded = "test content1";

        final String testFilePathDeleted = AS_DISTRIBUTION + FILE_SEPARATOR + "LICENSE.txt";

        final String originalContentOfDeletedFile = readFile(testFilePathDeleted);

        ContentModification miscFileAdded = ContentModificationUtils.addMisc(oneOffPatchDir, patchID,
                testContentAdded, testFileSegmentsAdded);
        ContentModification miscFileDeleted = ContentModificationUtils.removeMisc(
                new File(testFilePathDeleted), "");
        ProductConfig productConfig = new ProductConfig(PRODUCT, AS_VERSION, "main");
        Patch oneOffPatch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription("A one-off patch adding one misc file and deleting another misc file.")
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                .getParent()
                .addContentModification(miscFileAdded)
                .addContentModification(miscFileDeleted)
                .build();
        createPatchXMLFile(oneOffPatchDir, oneOffPatch);
        File zippedPatch = createZippedPatchFile(oneOffPatchDir, patchID);

        // apply the patch and check if server is in restart-required mode
        controller.start(CONTAINER);
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // check if patch is installed, if files are created, deleted and check content
        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + patchID + " should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertTrue("File1 " + testFilePathAdded + " should exist", new File(testFilePathAdded).exists());
        Assert.assertFalse("File2 " + testFilePathDeleted + " should have been deleted", new File(testFilePathDeleted).exists());
        Assert.assertEquals("check content of file after applying patch", testContentAdded, readFile(testFilePathAdded));

        // rollback the patch and check if server is in restart-required mode
        Assert.assertTrue("Rollback should be accepted", CliUtilsForPatching.rollbackPatch(patchID));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // check if patch is uninstalled
        controller.start(CONTAINER);
        Assert.assertFalse("The patch " + patchID + " NOT should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertFalse("File1 + " + testFilePathAdded + " should have been deleted",
                new File(testFilePathAdded).exists());
        Assert.assertTrue("File2 + " + testFilePathDeleted + " should exist",
                new File(testFilePathDeleted).exists());
        Assert.assertEquals("check content of file after patch rollback", originalContentOfDeletedFile, readFile(testFilePathDeleted));

        // reapply the patch and check if server is in restart-required mode
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // check if patch is installed, if files exists and check content of files
        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + patchID + " should be listed as installed", CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertTrue("File1 " + testFilePathAdded + " should exist",
                new File(testFilePathAdded).exists());
        Assert.assertFalse("File2 " + testFilePathDeleted + " should have been deleted", new File(testFilePathDeleted).exists());
        Assert.assertEquals("check content of file after applying patch", testContentAdded, readFile(testFilePathAdded));
        controller.stop(CONTAINER);
    }

    /**
     * Prepare a one-off patch which adds multiple misc files and deletes multiple misc files. Apply it, check that the files was created a deleted.
     * Roll it back, check that the files was deleted, created and apply it again to make sure re-applying works as expected
     */
    @Test
    public void testOneOffPatchAddingMultipleMiscFilesDeletingMultipleMiscFiles() throws Exception {
        // prepare the patch
        String patchID = randomString();
        File oneOffPatchDir = mkdir(tempDir, patchID);

        final String[] testFileSegmentsAdded1 = new String[]{"testDir1", "testFile2.txt"};
        final String testFilePathAdded1 = AS_DISTRIBUTION + FILE_SEPARATOR + Joiner.on(FILE_SEPARATOR).join(testFileSegmentsAdded1);
        final String testContentAdded1 = "test content2";

        final String[] testFileSegmentsAdded2 = new String[]{"testFile2.txt"};
        final String testFilePathAdded2 = AS_DISTRIBUTION + FILE_SEPARATOR + Joiner.on(FILE_SEPARATOR).join(testFileSegmentsAdded2);
        final String testContentAdded2 = "test content2";

        final String testFilePathDeleted1 = AS_DISTRIBUTION + FILE_SEPARATOR + "README.txt";
        final String testFilePathDeleted2 = AS_DISTRIBUTION + FILE_SEPARATOR + "LICENSE.txt";

        final String originalContentOfDeletedFile1 = readFile(testFilePathDeleted1);
        final String originalContentOfDeletedFile2 = readFile(testFilePathDeleted2);

        ContentModification miscFileAdded1 = ContentModificationUtils.addMisc(oneOffPatchDir, patchID,
                testContentAdded1, testFileSegmentsAdded1);
        ContentModification miscFileAdded2 = ContentModificationUtils.addMisc(oneOffPatchDir, patchID,
                testContentAdded2, testFileSegmentsAdded2);
        ContentModification miscFileDeleted1 = ContentModificationUtils.removeMisc(new File(testFilePathDeleted1), "");
        ContentModification miscFileDeleted2 = ContentModificationUtils.removeMisc(new File(testFilePathDeleted2), "");
        ProductConfig productConfig = new ProductConfig(PRODUCT, AS_VERSION, "main");
        Patch oneOffPatch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription("A one-off patch adding one misc file and deleting another misc file.")
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                .getParent()
                .addContentModification(miscFileAdded1)
                .addContentModification(miscFileAdded2)
                .addContentModification(miscFileDeleted1)
                .addContentModification(miscFileDeleted2)
                .build();
        createPatchXMLFile(oneOffPatchDir, oneOffPatch);
        File zippedPatch = createZippedPatchFile(oneOffPatchDir, patchID);

        // apply the patch and check if server is in restart-required mode
        controller.start(CONTAINER);
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // check if patch is installed, if files are created, deleted and check content
        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + patchID + " should be listed as installed", CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertTrue("File1 " + testFilePathAdded1 + " should exist", new File(testFilePathAdded1).exists());
        Assert.assertTrue("File2 " + testFilePathAdded2 + " should exist", new File(testFilePathAdded2).exists());
        Assert.assertFalse("File3 " + testFilePathDeleted1 + " should have been deleted", new File(testFilePathDeleted1).exists());
        Assert.assertFalse("File4 " + testFilePathDeleted2 + " should have been deleted", new File(testFilePathDeleted2).exists());
        Assert.assertEquals("check content of file after applying patch", testContentAdded1, readFile(testFilePathAdded1));
        Assert.assertEquals("check content of file after applying patch", testContentAdded2, readFile(testFilePathAdded2));

        // rollback the patch and check if server is in restart-required mode
        Assert.assertTrue("Rollback should be accepted", CliUtilsForPatching.rollbackPatch(patchID));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // check if patch is uninstalled
        controller.start(CONTAINER);
        Assert.assertFalse("The patch " + patchID + " NOT should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertFalse("File1 + " + testFilePathAdded1 + " should have been deleted", new File(testFilePathAdded1).exists());
        Assert.assertFalse("File2 + " + testFilePathAdded2 + " should have been deleted", new File(testFilePathAdded2).exists());
        Assert.assertTrue("File3 + " + testFilePathDeleted1 + " should exist", new File(testFilePathDeleted1).exists());
        Assert.assertTrue("File4 + " + testFilePathDeleted2 + " should exist", new File(testFilePathDeleted2).exists());
        Assert.assertEquals("check content of file after patch rollback", originalContentOfDeletedFile1, readFile(testFilePathDeleted1));
        Assert.assertEquals("check content of file after patch rollback", originalContentOfDeletedFile2, readFile(testFilePathDeleted2));

        // reapply the patch and check if server is in restart-required mode
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // check if patch is installed, if files exists and check content of files
        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + patchID + " should be listed as installed", CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertTrue("File1 " + testFilePathAdded1 + " should exist", new File(testFilePathAdded1).exists());
        Assert.assertTrue("File2 " + testFilePathAdded2 + " should exist", new File(testFilePathAdded2).exists());
        Assert.assertFalse("File3 " + testFilePathDeleted1 + " should have been deleted", new File(testFilePathDeleted1).exists());
        Assert.assertFalse("File4 " + testFilePathDeleted2 + " should have been deleted", new File(testFilePathDeleted2).exists());
        Assert.assertEquals("check content of file after applying patch", testContentAdded1, readFile(testFilePathAdded1));
        Assert.assertEquals("check content of file after applying patch", testContentAdded2, readFile(testFilePathAdded2));
        controller.stop(CONTAINER);
    }

    /**
     * Prepare a one-off patch which adds a new module "org.wildfly.awesomemodule" to the base layer. Apply it, check that the module was installed
     * Roll it back, check that the files was deleted, created and apply it again to make sure re-applying works as expected
     *
     * @throws Exception
     */
    @Test
    public void testOneOffPatchAddingAModule() throws Exception {
        // prepare the patch
        String patchID = randomString();
        String layerPatchID = randomString();
        File oneOffPatchDir = mkdir(tempDir, patchID);

        final String moduleName = "org.wildfly.awesomemodule";
        final String modulePath = PATCHES_PATH + FILE_SEPARATOR + layerPatchID + FILE_SEPARATOR + moduleName.replace(".", FILE_SEPARATOR) + FILE_SEPARATOR + "main";

        final ResourceItem resourceItem1 = new ResourceItem("testFile1", "content1".getBytes());
        final ResourceItem resourceItem2 = new ResourceItem("testFile2", "content2".getBytes());

        ContentModification moduleAdded = ContentModificationUtils.addModule(oneOffPatchDir, layerPatchID, moduleName, resourceItem1, resourceItem2);
        ProductConfig productConfig = new ProductConfig(PRODUCT, AS_VERSION, "main");
        Patch oneOffPatch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription("A one-off patch adding a new module.")
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                .getParent()
                .oneOffPatchElement(layerPatchID, "base", false)
                .setDescription("New module for the base layer")
                .addContentModification(moduleAdded)
                .getParent()
                .build();
        createPatchXMLFile(oneOffPatchDir, oneOffPatch);
        File zippedPatch = createZippedPatchFile(oneOffPatchDir, patchID);

        // apply the patch and check if server is in restart-required mode
        controller.start(CONTAINER);
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // check if patch is listed as installed, files exists on correct place
        controller.start(CONTAINER);

        List<String> paths = CliUtilsForPatching.getResourceLoaderPathsForModule(moduleName, false);
        Assert.assertTrue("Module should be loaded from the .overlays directory but was: " + paths.get(0),
                paths.get(0).contains(".overlays"+File.separator+layerPatchID));

        Assert.assertTrue("The patch " + patchID + " should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertTrue("The file " + resourceItem1.getItemName() + " should exist", new File(modulePath + FILE_SEPARATOR + resourceItem1.getItemName()).exists());
        Assert.assertTrue("The file " + resourceItem2.getItemName() + " should exist", new File(modulePath + FILE_SEPARATOR + resourceItem2.getItemName()).exists());

        // rollback the patch and check if server is in restart-required mode
        Assert.assertTrue("Rollback should be accepted", CliUtilsForPatching.rollbackPatch(patchID));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // check if patch is not listed
        controller.start(CONTAINER);

        // check that module is not active
        try {
            CliUtilsForPatching.getResourceLoaderPathsForModule(moduleName, true);
            Assert.fail("Module " + moduleName + " should have been removed");
        } catch(RuntimeException expected) {
        }

        Assert.assertFalse("The patch " + patchID + " NOT should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertFalse("The file " + resourceItem1.getItemName() + "should have been deleted", new File(modulePath + FILE_SEPARATOR + resourceItem1.getItemName()).exists());
        Assert.assertFalse("The file " + resourceItem2.getItemName() + "should have been deleted",
                new File(modulePath + FILE_SEPARATOR + resourceItem2.getItemName()).exists());

        // reapply patch and check if server is in restart-required mode
        Assert.assertTrue("Patch should be accepted",
                CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // check if patch is listed as installed, files exists on correct place
        controller.start(CONTAINER);
        paths = CliUtilsForPatching.getResourceLoaderPathsForModule(moduleName, false);
        Assert.assertTrue("Module should be loaded from the .overlays directory but was: " + paths.get(0),
                paths.get(0).contains(".overlays"+File.separator+layerPatchID));
        Assert.assertTrue("The patch " + patchID + " should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertTrue("The file " + resourceItem1.getItemName() + " should exist", new File(modulePath + FILE_SEPARATOR + resourceItem1.getItemName()).exists());
        Assert.assertTrue("The file " + resourceItem2.getItemName() + " should exist", new File(modulePath + FILE_SEPARATOR + resourceItem2.getItemName()).exists());
        controller.stop(CONTAINER);
    }

    /**
     * Prepare a one-off patch which adds multiple (2) modules "org.wildfly.awesomemodule(#)" to the base layer. Apply it, check that the modules were installed
     * Roll it back, check that the modules were uninstalled  and apply it again to make sure re-applying works as expected
     *
     * @throws Exception
     */
    @Test
    public void testOneOffPatchAddingMultipleModules() throws Exception {
        // prepare the patch
        String patchID = randomString();
        String layerPatchID = randomString();
        File oneOffPatchDir = mkdir(tempDir, patchID);

        final String moduleName1 = "org.wildfly.awesomemodule1";
        final String modulePath1 = PATCHES_PATH + FILE_SEPARATOR + layerPatchID + FILE_SEPARATOR + moduleName1.replace(".", FILE_SEPARATOR) + FILE_SEPARATOR + "main";
        final String moduleName2 = "org.wildfly.awesomemodul2";
        final String modulePath2 = PATCHES_PATH + FILE_SEPARATOR + layerPatchID + FILE_SEPARATOR + moduleName2.replace(".", FILE_SEPARATOR) + FILE_SEPARATOR + "main";

        final ResourceItem resourceItem1 = new ResourceItem("testFile1", "content1".getBytes());
        final ResourceItem resourceItem2 = new ResourceItem("testFile2", "content2".getBytes());

        ContentModification moduleAdded1 = ContentModificationUtils.addModule(oneOffPatchDir, layerPatchID, moduleName1, resourceItem1, resourceItem2);
        ContentModification moduleAdded2 = ContentModificationUtils.addModule(oneOffPatchDir, layerPatchID, moduleName2, resourceItem1, resourceItem2);
        ProductConfig productConfig = new ProductConfig(PRODUCT, AS_VERSION, "main");
        Patch oneOffPatch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription("A one-off patch adding multiple modules.")
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                .getParent()
                .oneOffPatchElement(layerPatchID, "base", false)
                .setDescription("New modules for the base layer")
                .addContentModification(moduleAdded1)
                .addContentModification(moduleAdded2)
                .getParent()
                .build();
        createPatchXMLFile(oneOffPatchDir, oneOffPatch);
        File zippedPatch = createZippedPatchFile(oneOffPatchDir, patchID);

        // apply the patch and check if server is in restart-required mode
        controller.start(CONTAINER);
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // check if patch is listed as installed, files exists on correct place
        controller.start(CONTAINER);

        List<String> paths = CliUtilsForPatching.getResourceLoaderPathsForModule(moduleName1, false);
        Assert.assertTrue("Module should be loaded from the .overlays directory but was: " + paths.get(0),
                paths.get(0).contains(".overlays"+File.separator+layerPatchID));

        Assert.assertTrue("The patch " + patchID + " should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertTrue("The file " + resourceItem1.getItemName() + " should exist", new File(modulePath1 + FILE_SEPARATOR + resourceItem1.getItemName()).exists());
        Assert.assertTrue("The file " + resourceItem2.getItemName() + " should exist", new File(modulePath1 + FILE_SEPARATOR + resourceItem2.getItemName()).exists());
        Assert.assertTrue("The file " + resourceItem1.getItemName() + " should exist", new File(modulePath2 + FILE_SEPARATOR + resourceItem1.getItemName()).exists());
        Assert.assertTrue("The file " + resourceItem2.getItemName() + " should exist", new File(modulePath2 + FILE_SEPARATOR + resourceItem2.getItemName()).exists());


        // rollback the patch and check if server is in restart-required mode
        Assert.assertTrue("Rollback should be accepted", CliUtilsForPatching.rollbackPatch(patchID));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // check if patch is not listed
        controller.start(CONTAINER);

        // check that module1 is not active
        try {
            CliUtilsForPatching.getResourceLoaderPathsForModule(moduleName1, true);
            Assert.fail("Module " + moduleName1 + " should have been removed by the patch");
        } catch(RuntimeException expected) {
        }

        Assert.assertFalse("The patch " + patchID + " NOT should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertFalse("The file " + resourceItem1.getItemName() + "should have been deleted", new File(modulePath1 + FILE_SEPARATOR + resourceItem1.getItemName()).exists());
        Assert.assertFalse("The file " + resourceItem2.getItemName() + "should have been deleted", new File(modulePath1 + FILE_SEPARATOR + resourceItem2.getItemName()).exists());
        Assert.assertFalse("The file " + resourceItem1.getItemName() + "should have been deleted", new File(modulePath2 + FILE_SEPARATOR + resourceItem1.getItemName()).exists());
        Assert.assertFalse("The file " + resourceItem2.getItemName() + "should have been deleted", new File(modulePath2 + FILE_SEPARATOR + resourceItem2.getItemName()).exists());
        Assert.assertFalse("The directory " + modulePath1 + "should have been deleted", new File(modulePath1).exists());
        Assert.assertFalse("The directory " + modulePath2 + "should have been deleted",
                new File(modulePath2).exists());

        // reapply patch and check if server is in restart-required mode
        Assert.assertTrue("Patch should be accepted",
                CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // check if patch is listed as installed, files exists on correct place
        controller.start(CONTAINER);

        paths = CliUtilsForPatching.getResourceLoaderPathsForModule(moduleName1, false);
        Assert.assertTrue("Module should be loaded from the .overlays directory but was: " + paths.get(0),
                paths.get(0).contains(".overlays"+File.separator+layerPatchID));

        Assert.assertTrue("The patch " + patchID + " should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertTrue("The file " + resourceItem1.getItemName() + " should exist", new File(modulePath1 + FILE_SEPARATOR + resourceItem1.getItemName()).exists());
        Assert.assertTrue("The file " + resourceItem2.getItemName() + " should exist", new File(modulePath1 + FILE_SEPARATOR + resourceItem2.getItemName()).exists());
        Assert.assertTrue("The file " + resourceItem1.getItemName() + " should exist", new File(modulePath2 + FILE_SEPARATOR + resourceItem1.getItemName()).exists());
        Assert.assertTrue("The file " + resourceItem2.getItemName() + " should exist", new File(modulePath2 + FILE_SEPARATOR + resourceItem2.getItemName()).exists());
        controller.stop(CONTAINER);
    }

    /**
     * Create a new module in AS distribution,
     * create a patch which modifies it by adding a new text file into it.
     */
    @Test
    public void testModifyAModule() throws Exception {
        ProductConfig productConfig = new ProductConfig(PRODUCT, AS_VERSION, "main");
        String moduleName = randomString();

        // creates an empty module
        File baseModuleDir = newFile(new File(PatchingTestUtil.AS_DISTRIBUTION), "modules", SYSTEM, LAYERS, BASE);

        File moduleDir = createModule0(baseModuleDir, moduleName);

        logger.info("moduleDir = " + moduleDir.getAbsolutePath());

        // prepare the patch
        String patchID = randomString();
        String baseLayerPatchID = randomString();
        File patchDir = mkdir(tempDir, patchID);

        // create the patch with the updated module
        ContentModification moduleModified = ContentModificationUtils.modifyModule(patchDir, baseLayerPatchID, moduleDir,
                new ResourceItem("res1", "new resource in the module".getBytes()));

        Patch patch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription(randomString())
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                .getParent()
                .oneOffPatchElement(baseLayerPatchID, BASE, false)
                .addContentModification(moduleModified)
                .getParent()
                .build();
        createPatchXMLFile(patchDir, patch);
        File zippedPatch = createZippedPatchFile(patchDir, patchID);
        logger.info(zippedPatch.getAbsolutePath());

        // apply patch and check if server is in restart-required mode
        controller.start(CONTAINER);
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + patchID + " should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        String newFilePath = Joiner.on(FILE_SEPARATOR).join(
                new String[]{PATCHES_PATH, baseLayerPatchID, moduleName, "main", "res1"});
        Assert.assertTrue("File " + newFilePath + " should exist", new File(newFilePath).exists());

        // check that JBoss Modules picks up the module from the right path
        List<String> paths = CliUtilsForPatching.getResourceLoaderPathsForModule(moduleName, false);
        Assert.assertTrue("Module should be loaded from the .overlays directory but was: " + paths.get(0),
                paths.get(0).contains(".overlays"+File.separator+baseLayerPatchID));

        // rollback the patch and check if server is in restart-required mode
        Assert.assertTrue("Rollback should be accepted", CliUtilsForPatching.rollbackPatch(patchID));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertFalse("The patch " + patchID + " NOT should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertFalse("File " + newFilePath + " should not exist", new File(newFilePath).exists());

        // reapply patch and check if server is in restart-required mode
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + patchID + " should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertTrue("File " + newFilePath + " should exist", new File(newFilePath).exists());

        // check that JBoss Modules picks up the module from the right path
        paths = CliUtilsForPatching.getResourceLoaderPathsForModule(moduleName, false);
        Assert.assertTrue("Module should be loaded from the .overlays directory but was: " + paths.get(0),
                paths.get(0).contains(".overlays"+File.separator+baseLayerPatchID));
        controller.stop(CONTAINER);
    }

    /**
     * Prepare a one-off patch which removes module. Apply it, check that the module was removed
     * Roll it back, check that the module was restored apply it again to make sure re-applying works as expected
     *
     * @throws Exception
     */
    @Test
    public void testOneOffPatchRemovingAModule() throws Exception {
        // prepare the patch
        String patchID = randomString();
        String layerPatchID = randomString();
        File oneOffPatchDir = mkdir(tempDir, patchID);

        // creates an empty module
        final String moduleName = randomString();
        File baseModuleDir = newFile(new File(PatchingTestUtil.AS_DISTRIBUTION), MODULES, SYSTEM, LAYERS, BASE);
        File moduleDir = createModule0(baseModuleDir, moduleName);
        File patchModuleDir = new File(PATCHES_PATH + FILE_SEPARATOR + layerPatchID + FILE_SEPARATOR + moduleName);
        File moduleXml = new File(patchModuleDir, "main" + FILE_SEPARATOR + "module.xml");

        ContentModification moduleRemoved = ContentModificationUtils.removeModule(moduleDir);
        ProductConfig productConfig = new ProductConfig(PRODUCT, AS_VERSION, "main");
        Patch oneOffPatch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription("A one-off patch removing a module.")
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                .getParent()
                .oneOffPatchElement(layerPatchID, "base", false)
                .setDescription("Remove module")
                .addContentModification(moduleRemoved)
                .getParent()
                .build();
        createPatchXMLFile(oneOffPatchDir, oneOffPatch);
        File zippedPatch = createZippedPatchFile(oneOffPatchDir, patchID);

        // apply the patch and check if server is in restart-required mode
        controller.start(CONTAINER);
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // check if patch is listed as installed, files exists on correct place
        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + patchID + " should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertTrue("The file " + moduleXml.getName() + " should exist", moduleXml.exists());
        // check that the module is not active
        try {
            List<String> paths = CliUtilsForPatching.getResourceLoaderPathsForModule(moduleName, true);
            Assert.fail("Module " + moduleName + " should have been removed by the patch");
        } catch(RuntimeException expected) {
        }

        // rollback the patch and check if server is in restart-required mode
        Assert.assertTrue("Rollback should be accepted", CliUtilsForPatching.rollbackPatch(patchID));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // check if patch is not listed
        controller.start(CONTAINER);
        // check that the module exists
        CliUtilsForPatching.getResourceLoaderPathsForModule(moduleName, true);
        Assert.assertFalse("The patch " + patchID + " NOT should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertFalse("The file " + patchModuleDir + "should have been deleted", patchModuleDir.exists());

        // reapply patch and check if server is in restart-required mode
        Assert.assertTrue("Patch should be accepted",
                CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // check if patch is listed as installed, files exists on correct place
        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + patchID + " should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertTrue("The file " + moduleXml.getName() + " should exist", moduleXml.exists());
        controller.stop(CONTAINER);
    }

    /**
     * Prepare a one-off patch which removes multiple (2) modules. Apply it, check that the modules were removed
     * Roll it back, check that the modules were restored apply it again to make sure re-applying works as expected
     *
     * @throws Exception
     */
    @Test
    public void testOneOffPatchRemovingMultipleModules() throws Exception {
        // prepare the patch
        String patchID = randomString();
        String layerPatchID = randomString();
        File oneOffPatchDir = mkdir(tempDir, patchID);

        // creates an empty module
        final String moduleName1 = randomString();
        File baseModuleDir1 = newFile(new File(PatchingTestUtil.AS_DISTRIBUTION), MODULES, SYSTEM, LAYERS, BASE);
        File moduleDir1 = createModule0(baseModuleDir1, moduleName1);
        File patchModuleDir1 = new File(PATCHES_PATH + FILE_SEPARATOR + layerPatchID + FILE_SEPARATOR + moduleName1);
        File moduleXml1 = new File(patchModuleDir1, "main" + FILE_SEPARATOR + "module.xml");

        final String moduleName2 = randomString();
        File baseModuleDir2 = newFile(new File(PatchingTestUtil.AS_DISTRIBUTION), MODULES, SYSTEM, LAYERS, BASE);
        File moduleDir2 = createModule0(baseModuleDir2, moduleName2);
        File patchModuleDir2 = new File(PATCHES_PATH + FILE_SEPARATOR + layerPatchID + FILE_SEPARATOR + moduleName2);
        File moduleXml2 = new File(patchModuleDir2, "main" + FILE_SEPARATOR + "module.xml");

        ContentModification moduleRemoved1 = ContentModificationUtils.removeModule(moduleDir1);
        ContentModification moduleRemoved2 = ContentModificationUtils.removeModule(moduleDir2);
        ProductConfig productConfig = new ProductConfig(PRODUCT, AS_VERSION, "main");
        Patch oneOffPatch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription("A one-off patch removing multiple modules.")
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                .getParent()
                .oneOffPatchElement(layerPatchID, "base", false)
                .setDescription("Remove modules")
                .addContentModification(moduleRemoved1)
                .addContentModification(moduleRemoved2)
                .getParent()
                .build();
        createPatchXMLFile(oneOffPatchDir, oneOffPatch);
        File zippedPatch = createZippedPatchFile(oneOffPatchDir, patchID);

        // apply the patch and check if server is in restart-required mode
        controller.start(CONTAINER);
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // check if patch is listed as installed, files exists on correct place
        controller.start(CONTAINER);
        // TODO more checks that the module exists
        Assert.assertTrue("The patch " + patchID + " should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertTrue("The file " + moduleXml1.getName() + " should exist", moduleXml1.exists());
        Assert.assertTrue("The file " + moduleXml2.getName() + " should exist", moduleXml2.exists());

        // rollback the patch and check if server is in restart-required mode
        Assert.assertTrue("Rollback should be accepted", CliUtilsForPatching.rollbackPatch(patchID));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // check if patch is not listed
        controller.start(CONTAINER);
        // TODO mode checks that the module does not exist anymore
        Assert.assertFalse("The patch " + patchID + " NOT should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertFalse("The file " + patchModuleDir1 + "should have been deleted", patchModuleDir1.exists());
        Assert.assertFalse("The file " + patchModuleDir2 + "should have been deleted", patchModuleDir2.exists());

        // reapply patch and check if server is in restart-required mode
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // check if patch is listed as installed, files exists on correct place
        controller.start(CONTAINER);
        // TODO more checks that the module exists
        Assert.assertTrue("The patch " + patchID + " should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertTrue("The file " + moduleXml1.getName() + " should exist", moduleXml1.exists());
        Assert.assertTrue("The file " + moduleXml2.getName() + " should exist", moduleXml2.exists());
        controller.stop(CONTAINER);
    }

}
