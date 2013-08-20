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

package org.jboss.as.patching.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.jboss.as.patching.HashUtils.bytesToHexString;
import static org.jboss.as.patching.HashUtils.hashFile;
import static org.jboss.as.patching.IoUtils.mkdir;
import static org.jboss.as.patching.runner.PatchingAssert.assertDirDoesNotExist;
import static org.jboss.as.patching.runner.PatchingAssert.assertFileContent;
import static org.jboss.as.patching.runner.PatchingAssert.assertFileExists;
import static org.jboss.as.patching.runner.TestUtils.createInstalledImage;
import static org.jboss.as.patching.runner.TestUtils.createPatchXMLFile;
import static org.jboss.as.patching.runner.TestUtils.createZippedPatchFile;
import static org.jboss.as.patching.runner.TestUtils.dump;
import static org.jboss.as.patching.runner.TestUtils.randomString;
import static org.jboss.as.patching.runner.TestUtils.touch;

import java.io.File;
import java.io.IOException;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContextFactory;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.jboss.as.patching.runner.AbstractTaskTestCase;
import org.jboss.as.patching.runner.ContentModificationUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * @author Alexey Loubyansky
 *
 */
public class ResetConfigurationUnitTestCase extends AbstractTaskTestCase {

    private byte[] originalAppClientHash;
    private byte[] originalStandaloneHash;
    private byte[] originalDomainHash;
    private File appClientXmlFile;
    private File standaloneXmlFile;
    private File domainXmlFile;

    @Before
    public void setUp() throws Exception {
        // with some files in the configuration directories
        appClientXmlFile = touch(env.getInstalledImage().getAppClientDir(), "configuration", "appclient.xml");
        dump(appClientXmlFile, "<original content of appclient configuration>");
        originalAppClientHash = hashFile(appClientXmlFile);
        standaloneXmlFile = touch(env.getInstalledImage().getStandaloneDir(), "configuration", "standalone.xml");
        dump(standaloneXmlFile, "<original content of standalone configuration>");
        originalStandaloneHash = hashFile(standaloneXmlFile);
        domainXmlFile = touch(env.getInstalledImage().getDomainDir(), "configuration", "domain.xml");
        dump(domainXmlFile, "<original content of domain configuration>");
        originalDomainHash = hashFile(domainXmlFile);
    }

    @After
    public void tearDown() {
        super.tearDown();
        originalAppClientHash = null;
        originalStandaloneHash = null;
        originalDomainHash = null;
        standaloneXmlFile = null;
        appClientXmlFile = null;
        domainXmlFile = null;
    }

    /**
     * Applies a patch, modifies standalone, appclient and domain xml config and rolls back the patch
     * with --reset-configuration=false
     * The expected result is:
     * - the modified config files remain as-is;
     * - in each configuration dir a new restored-configuration dir is created with the original,
     *   unmodified configuration files.
     *
     *
     * @throws Exception
     */
    @Test
    public void testResetConfigurationFalse() throws Exception {

        final File binDir = createInstalledImage(env, "consoleSlot", productConfig.getProductName(), productConfig.getProductVersion());

        // build a one-off patch for the base installation
        // with 1 updated file
        String patchID = randomString();
        String patchElementId = randomString();
        File patchDir = mkdir(tempDir, patchID);

        // create a file for the conflict
        String fileNoConflictName = "file-no-conflict.txt";
        File noConflictFile = touch(binDir, fileNoConflictName);
        dump(noConflictFile, "original script to run standalone AS7");
        // patch the file
        ContentModification fileNoConflictModified = ContentModificationUtils.modifyMisc(patchDir, patchID, "updated script", noConflictFile, "bin", fileNoConflictName);

        //TestUtils.tree(env.getInstalledImage().getJbossHome());

        Patch patch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription(randomString())
                .upgradeIdentity(productConfig.getProductName(), productConfig.getProductVersion(), productConfig.getProductVersion() + "CP1")
                .getParent()
                .addContentModification(fileNoConflictModified)
                .upgradeElement(patchElementId, "base", false)
                .getParent()
                .build();

        // create the patch
        createPatchXMLFile(patchDir, patch, false);

        File zippedPatch = createZippedPatchFile(patchDir, patch.getPatchId());

        // apply the patch using the cli
        CommandContext ctx = CommandContextFactory.getInstance().newCommandContext();
        try {
            ctx.handle("patch apply " + zippedPatch.getAbsolutePath() + " --distribution=" + env.getInstalledImage().getJbossHome());
        } catch(Exception e) {
            ctx.terminateSession();
            throw e;
        }

        // check the config files have been backed up
        File backupAppclientXmlFile = assertFileExists(env.getInstalledImage().getPatchHistoryDir(patch.getPatchId()), "configuration", "appclient", "appclient.xml");
        assertFileContent(originalAppClientHash, backupAppclientXmlFile);
        File backupStandaloneXmlFile = assertFileExists(env.getInstalledImage().getPatchHistoryDir(patch.getPatchId()), "configuration", "standalone", "standalone.xml");
        assertFileContent(originalStandaloneHash, backupStandaloneXmlFile);
        File backupDomainXmlFile = assertFileExists(env.getInstalledImage().getPatchHistoryDir(patch.getPatchId()), "configuration", "domain", "domain.xml");
        assertFileContent(originalDomainHash, backupDomainXmlFile);

        // let's change the standalone.xml file
        dump(standaloneXmlFile, "<updated standalone configuration with changes from the added module>");
        byte[] updatedStandaloneXmlHash = hashFile(standaloneXmlFile);

        dump(appClientXmlFile, "<updated app client configuration with changes from the added module>");
        byte[] updatedAppClientXmlHash = hashFile(appClientXmlFile);

        dump(domainXmlFile, "<updated domain configuration with changes from the added module>");
        byte[] updatedDomainXmlHash = hashFile(domainXmlFile);

        try {
            ctx.handle("patch rollback --reset-configuration=false --distribution=" + env.getInstalledImage().getJbossHome());
        } finally {
            ctx.terminateSession();
        }

        //TestUtils.tree(env.getInstalledImage().getJbossHome());

        assertRestoredConfig(env.getInstalledImage().getStandaloneDir(), "standalone.xml", updatedStandaloneXmlHash, originalStandaloneHash);
        assertRestoredConfig(env.getInstalledImage().getAppClientDir(), "appclient.xml", updatedAppClientXmlHash, originalAppClientHash);
        assertRestoredConfig(env.getInstalledImage().getDomainDir(), "domain.xml", updatedDomainXmlHash, originalDomainHash);
    }

    protected void assertRestoredConfig(File baseDir, String xmlName, byte[] updatedHash, byte[] originalHash)
            throws IOException {
        File rolledBackXmlFile = assertFileExists(baseDir, "configuration", xmlName);
        assertEquals(bytesToHexString(updatedHash), bytesToHexString(hashFile(rolledBackXmlFile)));
        File restoredXmlFile = assertFileExists(baseDir, "configuration", "restored-configuration", xmlName);
        assertEquals(bytesToHexString(originalHash), bytesToHexString(hashFile(restoredXmlFile)));
    }

    /**
     * Applies a patch, modifies standalone, appclient and domain xml config and rolls back the patch
     * with --reset-configuration=true
     * The expected result is:
     * - the modified config files are replaced with the original configuration files;
     * - the restored-configuration dir is not created in any of the configuration dirs
     * .
     * @throws Exception
     */
    @Test
    public void testResetConfigurationTrue() throws Exception {

        final File binDir = createInstalledImage(env, "consoleSlot", productConfig.getProductName(), productConfig.getProductVersion());

        // build a one-off patch for the base installation
        // with 1 updated file
        String patchID = randomString();
        String patchElementId = randomString();
        File patchDir = mkdir(tempDir, patchID);

        // create a file for the conflict
        String fileNoConflictName = "file-no-conflict.txt";
        File noConflictFile = touch(binDir, fileNoConflictName);
        dump(noConflictFile, "original script to run standalone AS7");
        // patch the file
        ContentModification fileNoConflictModified = ContentModificationUtils.modifyMisc(patchDir, patchID, "updated script", noConflictFile, "bin", fileNoConflictName);

        //TestUtils.tree(env.getInstalledImage().getJbossHome());

        Patch patch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription(randomString())
                .upgradeIdentity(productConfig.getProductName(), productConfig.getProductVersion(), productConfig.getProductVersion() + "CP1")
                .getParent()
                .addContentModification(fileNoConflictModified)
                .upgradeElement(patchElementId, "base", false)
                .getParent()
                .build();

        // create the patch
        createPatchXMLFile(patchDir, patch, false);

        File zippedPatch = createZippedPatchFile(patchDir, patch.getPatchId());

        // apply the patch using the cli
        CommandContext ctx = CommandContextFactory.getInstance().newCommandContext();
        try {
            ctx.handle("patch apply " + zippedPatch.getAbsolutePath() + " --distribution=" + env.getInstalledImage().getJbossHome());
        } catch(Exception e) {
            ctx.terminateSession();
            throw e;
        }

        // check the config files have been backed up
        File backupAppclientXmlFile = assertFileExists(env.getInstalledImage().getPatchHistoryDir(patch.getPatchId()), "configuration", "appclient", "appclient.xml");
        assertFileContent(originalAppClientHash, backupAppclientXmlFile);
        File backupStandaloneXmlFile = assertFileExists(env.getInstalledImage().getPatchHistoryDir(patch.getPatchId()), "configuration", "standalone", "standalone.xml");
        assertFileContent(originalStandaloneHash, backupStandaloneXmlFile);
        File backupDomainXmlFile = assertFileExists(env.getInstalledImage().getPatchHistoryDir(patch.getPatchId()), "configuration", "domain", "domain.xml");
        assertFileContent(originalDomainHash, backupDomainXmlFile);

        // let's change the standalone.xml file
        dump(standaloneXmlFile, "<updated standalone configuration with changes from the added module>");
        byte[] updatedStandaloneXmlFile = hashFile(standaloneXmlFile);
        assertNotEquals(bytesToHexString(originalStandaloneHash), bytesToHexString(updatedStandaloneXmlFile));

        dump(appClientXmlFile, "<updated app client configuration with changes from the added module>");
        byte[] updatedAppClientXmlHash = hashFile(appClientXmlFile);
        assertNotEquals(bytesToHexString(originalAppClientHash), bytesToHexString(updatedAppClientXmlHash));

        dump(domainXmlFile, "<updated domain configuration with changes from the added module>");
        byte[] updatedDomainXmlHash = hashFile(domainXmlFile);
        assertNotEquals(bytesToHexString(originalDomainHash), bytesToHexString(updatedDomainXmlHash));

        try {
            ctx.handle("patch rollback --reset-configuration=true --distribution=" + env.getInstalledImage().getJbossHome());
        } finally {
            ctx.terminateSession();
        }

        //TestUtils.tree(env.getInstalledImage().getJbossHome());

        File rolledBackStandaloneXmlFile = assertFileExists(env.getInstalledImage().getStandaloneDir(), "configuration", "standalone.xml");
        assertEquals(bytesToHexString(originalStandaloneHash), bytesToHexString(hashFile(rolledBackStandaloneXmlFile)));

        File rolledBackAppClientXmlFile = assertFileExists(env.getInstalledImage().getAppClientDir(), "configuration", "appclient.xml");
        assertEquals(bytesToHexString(originalAppClientHash), bytesToHexString(hashFile(rolledBackAppClientXmlFile)));

        File rolledBackDomainXmlFile = assertFileExists(env.getInstalledImage().getDomainDir(), "configuration", "domain.xml");
        assertEquals(bytesToHexString(originalDomainHash), bytesToHexString(hashFile(rolledBackDomainXmlFile)));

        assertDirDoesNotExist(env.getInstalledImage().getStandaloneDir(), "configuration", "restored-configuration");
        assertDirDoesNotExist(env.getInstalledImage().getAppClientDir(), "configuration", "restored-configuration");
        assertDirDoesNotExist(env.getInstalledImage().getDomainDir(), "configuration", "restored-configuration");
    }
}
