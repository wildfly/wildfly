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

import static junit.framework.Assert.assertEquals;
import static org.jboss.as.patching.metadata.ModificationType.ADD;
import static org.jboss.as.patching.runner.PatchUtils.bytesToHexString;
import static org.jboss.as.patching.runner.PatchUtils.calculateHash;
import static org.jboss.as.patching.runner.PatchingAssert.assertFileContent;
import static org.jboss.as.patching.runner.PatchingAssert.assertFileExists;
import static org.jboss.as.patching.runner.PatchingAssert.assertPatchHasBeenApplied;
import static org.jboss.as.patching.runner.PatchingAssert.assertPatchHasBeenRolledBack;
import static org.jboss.as.patching.runner.PatchingTask.NO_CONTENT;
import static org.jboss.as.patching.runner.TestUtils.createModule;
import static org.jboss.as.patching.runner.TestUtils.createPatchXMLFile;
import static org.jboss.as.patching.runner.TestUtils.createZippedPatchFile;
import static org.jboss.as.patching.runner.TestUtils.dump;
import static org.jboss.as.patching.runner.TestUtils.mkdir;
import static org.jboss.as.patching.runner.TestUtils.randomString;
import static org.jboss.as.patching.runner.TestUtils.touch;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;

import org.jboss.as.patching.LocalPatchInfo;
import org.jboss.as.patching.PatchInfo;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.ModuleItem;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.Patch.PatchType;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012, Red Hat Inc
 */
public class ConfigurationBackupTestCase extends AbstractTaskTestCase {

    private LocalPatchInfo info;
    private byte[] originalAppClientHash;
    private byte[] originalStandaloneHash;
    private byte[] originalDomainHash;
    private File standaloneXmlFile;

    @Before
    public void setUp() throws Exception {
        // start from a base installation
        info = new LocalPatchInfo(randomString(), PatchInfo.BASE, Collections.<String>emptyList(), env);
        // with some files in the configuration directories
        File appClientXmlFile = touch(env.getInstalledImage().getAppClientDir(), "configuration", "appclient.xml");
        dump(appClientXmlFile, "<original content of appclient configuration>");
        originalAppClientHash = calculateHash(appClientXmlFile);
        standaloneXmlFile = touch(env.getInstalledImage().getStandaloneDir(), "configuration", "standalone.xml");
        dump(standaloneXmlFile, "<original content of standalone configuration>");
        originalStandaloneHash = calculateHash(standaloneXmlFile);
        File domainXmlFile = touch(env.getInstalledImage().getDomainDir(), "configuration", "domain.xml");
        dump(domainXmlFile, "<original content of domain configuration>");
        originalDomainHash = calculateHash(domainXmlFile);
    }

    @After
    public void tearDown() {
        info = null;
        originalAppClientHash = null;
        originalStandaloneHash = null;
        originalDomainHash = null;
        standaloneXmlFile = null;
    }

    @Test
    public void testCumulativePatch() throws Exception {

        // build a cumulative patch for the base installation
        // with 1 added module
        String patchID = randomString();
        File patchDir = mkdir(tempDir, patchID);
        String moduleName = randomString();
        File moduleDir = createModule(patchDir, moduleName);
        byte[] newHash = calculateHash(moduleDir);
        ContentModification moduleAdded = new ContentModification(new ModuleItem(moduleName, newHash), NO_CONTENT , ADD);

        Patch patch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription(randomString())
                .setCumulativeType(info.getVersion(), info.getVersion() + "-CP")
                .addContentModification(moduleAdded)
                .build();

        checkApplyPatchAndRollbackRestoresBackupConfiguration(patchDir, patch);
    }

    @Test
    public void testOneOffPatch() throws Exception {

        // build a cumulative patch for the base installation
        // with 1 added module
        String patchID = randomString();
        File patchDir = mkdir(tempDir, patchID);
        String moduleName = randomString();
        File moduleDir = createModule(patchDir, moduleName);
        byte[] newHash = calculateHash(moduleDir);
        ContentModification moduleAdded = new ContentModification(new ModuleItem(moduleName, newHash), NO_CONTENT , ADD);

        Patch patch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription(randomString())
                .setOneOffType(info.getVersion())
                .addContentModification(moduleAdded)
                .build();

        checkApplyPatchAndRollbackRestoresBackupConfiguration(patchDir, patch);
    }


    private void checkApplyPatchAndRollbackRestoresBackupConfiguration(File patchDir, Patch patch) throws Exception {
        createPatchXMLFile(patchDir, patch);
        File zippedPatch = createZippedPatchFile(patchDir, patch.getPatchId());

        PatchingResult result = executePatch(info, zippedPatch);

        assertPatchHasBeenApplied(result, patch);

        // check the AS7 config files have been backed up
        File backupAppclientXmlFile = assertFileExists(result.getPatchInfo().getEnvironment().getHistoryDir(patch.getPatchId()), "configuration", "appclient", "appclient.xml");
        assertFileContent(originalAppClientHash, backupAppclientXmlFile);
        File backupStandaloneXmlFile = assertFileExists(result.getPatchInfo().getEnvironment().getHistoryDir(patch.getPatchId()), "configuration", "standalone", "standalone.xml");
        assertFileContent(originalStandaloneHash, backupStandaloneXmlFile);
        File backupDomainXmlFile = assertFileExists(result.getPatchInfo().getEnvironment().getHistoryDir(patch.getPatchId()), "configuration", "domain", "domain.xml");
        assertFileContent(originalDomainHash, backupDomainXmlFile);

        // let's change the standalone.xml file
        dump(standaloneXmlFile, "<updated standalone configuration with changes from the added module>");
        byte[] updatedStandaloneXmlFile = calculateHash(standaloneXmlFile);

        PatchingResult rollbackResult = rollback(result.getPatchInfo(), patch.getPatchId());

        assertPatchHasBeenRolledBack(rollbackResult, patch, info);
        File rolledBackStandaloneXmlFile = assertFileExists(rollbackResult.getPatchInfo().getEnvironment().getInstalledImage().getStandaloneDir(), "configuration", "standalone.xml");
        assertEquals("updated content was " + bytesToHexString(updatedStandaloneXmlFile), bytesToHexString(originalStandaloneHash), bytesToHexString(calculateHash(rolledBackStandaloneXmlFile)));
    }

}
