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

package org.jboss.as.test.patching;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.jboss.as.version.ProductConfig;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static org.jboss.as.patching.IoUtils.mkdir;
import static org.jboss.as.test.patching.PatchingTestUtil.AS_DISTRIBUTION;
import static org.jboss.as.test.patching.PatchingTestUtil.AS_VERSION;
import static org.jboss.as.test.patching.PatchingTestUtil.CONTAINER;
import static org.jboss.as.test.patching.PatchingTestUtil.FILE_SEPARATOR;
import static org.jboss.as.test.patching.PatchingTestUtil.PRODUCT;
import static org.jboss.as.test.patching.PatchingTestUtil.createPatchXMLFile;
import static org.jboss.as.test.patching.PatchingTestUtil.createZippedPatchFile;
import static org.jboss.as.test.patching.PatchingTestUtil.randomString;
import static org.jboss.as.test.patching.PatchingTestUtil.readFile;
import static org.jboss.as.test.patching.PatchingTestUtil.setFileContent;

/**
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
@RunAsClient
public class NonStandardSituationsTestCase extends AbstractPatchingTestCase {

    private static final Logger logger = Logger.getLogger(NonStandardSituationsTestCase.class);


    /**
     * bug: https://issues.jboss.org/browse/WFLY-1803
     *
     * Prepare a patch which modifies a file, but the file will not be writable for the patching process.
     * Check that the patch rolls back completely and doesn't leave itself half-applied.
     */
    @Test
    public void testWritePermissionDenied() throws Exception {
        // prepare the patch
        String patchID = randomString();
        File oneOffPatchDir = mkdir(tempDir, patchID);

        final String testFile1Name = "f1_" + randomString();
        final String testFilePath1 = AS_DISTRIBUTION + FILE_SEPARATOR + testFile1Name;
        final String testContent1 = "test content1";
        final String originalContent1 = "original content1";
        setFileContent(testFilePath1, originalContent1);

        final String testFile2Name = "f2_" + randomString();
        final String testFilePath2 = AS_DISTRIBUTION + FILE_SEPARATOR + testFile2Name;
        final String testContent2 = "test content2";
        final String originalContent2 = "original content2";
        setFileContent(testFilePath2, originalContent2);

        ContentModification miscFileModified1 = ContentModificationUtils.modifyMisc(oneOffPatchDir, patchID, testContent1, new File(testFilePath1), testFile1Name);
        ContentModification miscFileModified2 = ContentModificationUtils.modifyMisc(oneOffPatchDir, patchID, testContent2, new File(testFilePath2), testFile2Name);
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
        logger.info(zippedPatch.getAbsolutePath());

        if(!new File(testFilePath2).setWritable(false)) {
            logger.warn("Unable to revoke write permissions on file " + testFilePath2);
            Assume.assumeFalse(true); // skip this test
        }

        controller.start(CONTAINER);
        boolean success = CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath());
        new File(testFilePath2).setWritable(true);
        Assert.assertFalse("Patch should not be applied successfully", success);
        Assert.assertFalse("The patch should not be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        controller.stop(CONTAINER);

        // both files should have their original content
        String actualContent1 = readFile(testFilePath1);
        String actualContent2 = readFile(testFilePath2);

        Assert.assertEquals(originalContent1, actualContent1);
        Assert.assertEquals(originalContent2, actualContent2);
    }


}
