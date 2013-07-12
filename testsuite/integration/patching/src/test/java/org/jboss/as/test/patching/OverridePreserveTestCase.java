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

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.jboss.as.version.ProductConfig;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static org.jboss.as.patching.IoUtils.mkdir;
import static org.jboss.as.test.patching.PatchingTestUtil.*;

/**
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
@RunAsClient
public class OverridePreserveTestCase {

    @ArquillianResource
    private ContainerController controller;

    @After
    public void cleanup() throws Exception {
        if(controller.isStarted(CONTAINER))
            controller.stop(CONTAINER);
        CliUtilsForPatching.rollbackAll();
    }

    /**
     * Prepare a patch that modifies two misc files. [LICENSE.txt, README.txt]
     * Modify these two files before installing the patch.
     * Instruct the patching mechanism to override the files.
     * Roll back the patch, instructing the mechanism to override the files.
     * Expected result: the files' contents should be the same as after modification (before patch installation)
     * Install the patch again.
     * Roll it back again, instructing to override one LICENSE.txt and preserve README.txt.
     * Expected result: LICENSE.txt will be preserved and README.txt will be overridden.
     * Roll back the patch, instructing to override LICENSE.txt.
     * Expected result: LICENSE.txt should have the original contents
     */
    @Test
    public void testMiscFiles() throws Exception {
        // prepare the patch
        File tempDir = mkdir(new File(System.getProperty("java.io.tmpdir")), randomString());
        String patchID = randomString();
        File oneOffPatchDir = mkdir(tempDir, patchID);

        final String file1 = PatchingTestUtil.AS_DISTRIBUTION + "/README.txt";
        final String file1patchedContent = "Patched content for README.txt";
        final String file1originalContent = PatchingTestUtil.readFile(file1);
        final String file1modifiedContent = "I manually edited README.txt and it now looks like this.";

        final String file2 = PatchingTestUtil.AS_DISTRIBUTION + "/LICENSE.txt";
        final String file2patchedContent = "Patched content for LICENSE.txt";
        final String file2originalContent = PatchingTestUtil.readFile(file2);
        final String file2modifiedContent = "I manually edited LICENSE.txt and it now looks like this.";


        ContentModification file1Modified = ContentModificationUtils.modifyMisc(oneOffPatchDir, patchID, file1patchedContent, new File(file1), "README.txt");
        ContentModification file2Modified = ContentModificationUtils.modifyMisc(oneOffPatchDir, patchID, file2patchedContent, new File(file2), "LICENSE.txt");

        ProductConfig productConfig = new ProductConfig(PRODUCT, AS_VERSION, "main");
        Patch oneOffPatch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription("A one-off patch modifying two misc files.")
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                    .getParent()
                .addContentModification(file1Modified)
                .addContentModification(file2Modified)
                .build();
        PatchingTestUtil.createPatchXMLFile(oneOffPatchDir, oneOffPatch);
        File zippedPatch = PatchingTestUtil.createZippedPatchFile(oneOffPatchDir, patchID);

        // modify files
        PatchingTestUtil.setFileContent(file1, file1modifiedContent);
        PatchingTestUtil.setFileContent(file2, file2modifiedContent);

        // apply the patch without override/preserve
        controller.start(CONTAINER);
        Assert.assertFalse("Server should reject patch installation in this case",
                CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath()));
        Assert.assertEquals("Misc file should not be overwritten", file1modifiedContent, PatchingTestUtil.readFile(file1));
        Assert.assertEquals("Misc file should not be overwritten", file2modifiedContent, PatchingTestUtil.readFile(file2));

        // apply patch with --override-all
        Assert.assertTrue("Patch should be accepted",
                CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath(), CliUtilsForPatching.OVERRIDE_ALL));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + patchID + " should be listed as installed", CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertEquals("Misc file should be overwritten", file1patchedContent, PatchingTestUtil.readFile(file1));
        Assert.assertEquals("Misc file should be overwritten", file2patchedContent, PatchingTestUtil.readFile(file2));

        // modify README.txt
        PatchingTestUtil.setFileContent(file1, file1modifiedContent);

        // rollback patch with --override=README.txt and --preserve=LICENSE.txt
        Assert.assertTrue("Rollback should be accepted",
                CliUtilsForPatching.rollbackPatch(patchID, String.format(CliUtilsForPatching.PRESERVE, "LICENSE.txt"),
                        String.format(CliUtilsForPatching.OVERRIDE, "README.txt")));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertFalse("The patch " + patchID + " NOT should be listed as installed", CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertEquals("README.txt should be reverted", file1modifiedContent, PatchingTestUtil.readFile(file1));
        Assert.assertEquals("LICENSE.txt should NOT be reverted", file2patchedContent, PatchingTestUtil.readFile(file2));

        // modify files
        PatchingTestUtil.setFileContent(file1, file1modifiedContent);
        PatchingTestUtil.setFileContent(file2, file2modifiedContent);

        // apply it with --override=LICENSE.txt --preserve=README.txt
        Assert.assertTrue("Patch should be accepted",
                CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath(),
                        String.format(CliUtilsForPatching.OVERRIDE, "LICENSE.txt"),
                        String.format(CliUtilsForPatching.PRESERVE, "README.txt")));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + patchID + " should be listed as installed", CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertEquals("README.txt should NOT be overwritten", file1modifiedContent, PatchingTestUtil.readFile(file1));
        Assert.assertEquals("LICENSE.txt should be overwritten", file2patchedContent, PatchingTestUtil.readFile(file2));

        // rollback
        Assert.assertTrue("Rollback should be accepted", CliUtilsForPatching.rollbackPatch(patchID, String.format(CliUtilsForPatching.OVERRIDE, "LICENSE.txt")));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertFalse("The patch " + patchID + " NOT should be listed as installed", CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertEquals("README.txt should NOT be changed", file1modifiedContent, PatchingTestUtil.readFile(file1));
        Assert.assertEquals("LICENSE.txt should be rolled back", file2modifiedContent, PatchingTestUtil.readFile(file2));

        // modify files
        PatchingTestUtil.setFileContent(file1, file1modifiedContent);
        PatchingTestUtil.setFileContent(file2, file2modifiedContent);

        // apply it with --preserve=LICENSE.txt,README.txt
        Assert.assertTrue("Patch should be accepted",
                CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath(),
                        String.format(CliUtilsForPatching.PRESERVE, "README.txt,LICENSE.txt")));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + patchID + " should be listed as installed", CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertEquals("README.txt should NOT be changed", file1modifiedContent, PatchingTestUtil.readFile(file1));
        Assert.assertEquals("LICENSE.txt should NOT be changed", file2modifiedContent, PatchingTestUtil.readFile(file2));
        controller.stop(CONTAINER);

        //final rollback is performed by @After

        // clean up
        PatchingTestUtil.setFileContent(file1, file1originalContent);
        PatchingTestUtil.setFileContent(file2, file2originalContent);
    }

}
