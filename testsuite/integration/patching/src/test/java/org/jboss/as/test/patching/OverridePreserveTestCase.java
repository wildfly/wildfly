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
import org.jboss.as.patching.IoUtils;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.jboss.as.version.ProductConfig;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static org.jboss.as.test.patching.PatchingTestUtil.CONTAINER;
import static org.jboss.as.test.patching.PatchingTestUtil.PRODUCT;
import static org.jboss.as.test.patching.PatchingTestUtil.AS_VERSION;
import static org.jboss.as.test.patching.PatchingTestUtil.FILE_SEPARATOR;
import static org.jboss.as.test.patching.PatchingTestUtil.assertPatchElements;
import static org.jboss.as.test.patching.PatchingTestUtil.baseModuleDir;
import static org.jboss.as.test.patching.PatchingTestUtil.createModule0;
import static org.jboss.as.test.patching.PatchingTestUtil.createPatchXMLFile;
import static org.jboss.as.test.patching.PatchingTestUtil.createZippedPatchFile;
import static org.jboss.as.test.patching.PatchingTestUtil.dump;
import static org.jboss.as.test.patching.PatchingTestUtil.randomString;
import static org.jboss.as.patching.Constants.BASE;
import static org.jboss.as.patching.Constants.LAYERS;
import static org.jboss.as.patching.Constants.SYSTEM;
import static org.jboss.as.patching.IoUtils.mkdir;
import static org.jboss.as.patching.IoUtils.newFile;

/**
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
@RunAsClient
public class OverridePreserveTestCase {

    private static final Logger logger = Logger.getLogger(OverridePreserveTestCase.class);

    private final String file1 = PatchingTestUtil.AS_DISTRIBUTION + FILE_SEPARATOR + "README.txt";
    private final String file1patchedContent = "Patched content for README.txt";
    private String file1originalContent;
    private final String file1modifiedContent = "I manually edited README.txt and it now looks like this.";

    private final String file2 = PatchingTestUtil.AS_DISTRIBUTION + FILE_SEPARATOR + "LICENSE.txt";
    private final String file2patchedContent = "Patched content for LICENSE.txt";
    private String file2originalContent;
    private final String file2modifiedContent = "I manually edited LICENSE.txt and it now looks like this.";

    private File tempDir;

    @ArquillianResource
    private ContainerController controller;

    @Before
    public void setUp() throws Exception {
        file1originalContent = PatchingTestUtil.readFile(file1);
        file2originalContent = PatchingTestUtil.readFile(file2);
        tempDir = mkdir(new File(System.getProperty("java.io.tmpdir")), randomString());
        assertPatchElements(baseModuleDir, null);
    }

    @After
    public void cleanup() throws Exception {
        if(controller.isStarted(CONTAINER))
            controller.stop(CONTAINER);

        final boolean success = CliUtilsForPatching.rollbackAll();

        PatchingTestUtil.setFileContent(file1, file1originalContent);
        PatchingTestUtil.setFileContent(file2, file2originalContent);

        if (IoUtils.recursiveDelete(tempDir)) {
            tempDir.deleteOnExit();
        }
        if (!success) {
            // Reset installation state
            final File home = new File(PatchingTestUtil.AS_DISTRIBUTION);
            PatchingTestUtil.resetInstallationState(home, baseModuleDir);
            Assert.fail("failed to rollback all patches");
        }
    }

    /**
     * Prepare a patch that modifies two misc files. [LICENSE.txt, README.txt]
     * Modify these two files before installing the patch.
     * apply patch with --preserve=README.txt,OVERRIDE.txt
     * rollback patch with --preserve=README.txt --override=LICENSE.txt
     */
    @Test
    public void testPreserveMiscFiles() throws Exception {
        // prepare the patch
        String patchID = randomString();
        File oneOffPatchDir = mkdir(tempDir, patchID);

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

        // apply patch with --preserve=LICENSE,txt,README.txt
        Assert.assertTrue("Patch should be accepted",
                CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath(), String.format(CliUtilsForPatching.PRESERVE, "README.txt,LICENSE.txt")));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + patchID + " should be listed as installed", CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertEquals("Misc file should not be overwritten", file1modifiedContent, PatchingTestUtil.readFile(file1));
        Assert.assertEquals("Misc file should not be overwritten", file2modifiedContent, PatchingTestUtil.readFile(file2));

        // rollback patch
        Assert.assertTrue("Rollback should be accepted",
                CliUtilsForPatching.rollbackPatch(patchID,
                        String.format(CliUtilsForPatching.OVERRIDE, "LICENSE.txt"),
                        String.format(CliUtilsForPatching.PRESERVE, "README.txt")));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertFalse("The patch " + patchID + " NOT should be listed as installed" ,
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertEquals("Misc file should not be overridden", file1modifiedContent, PatchingTestUtil.readFile(file1));
        Assert.assertEquals("Misc file should not be overridden", file2modifiedContent, PatchingTestUtil.readFile(file2));
        controller.stop(CONTAINER);
    }

    /**
     * Prepare a patch that modifies two misc files. [LICENSE.txt, README.txt]
     * Modify these two files before installing the patch.
     * apply patch with --override=README.txt,OVERRIDE.txt
     * rollback patch with --preserve=README.txt --override=LICENSE.txt
     */
    @Test
    public void testOverrideMiscFiles() throws Exception {
        // prepare the patch
        String patchID = randomString();
        File oneOffPatchDir = mkdir(tempDir, patchID);

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

        // apply patch with --override=LICENSE,txt,README.txt
        Assert.assertTrue("Patch should be accepted",
                CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath(), String.format(CliUtilsForPatching.OVERRIDE, "README.txt,LICENSE.txt")));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + patchID + " should be listed as installed", CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertEquals("Misc file should be overwritten", file1patchedContent, PatchingTestUtil.readFile(file1));
        Assert.assertEquals("Misc file should be overwritten", file2patchedContent, PatchingTestUtil.readFile(file2));

        // rollback patch
        Assert.assertTrue("Rollback should be accepted",
                CliUtilsForPatching.rollbackPatch(patchID,
                        String.format(CliUtilsForPatching.OVERRIDE, "LICENSE.txt"),
                        String.format(CliUtilsForPatching.PRESERVE, "README.txt")));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertFalse("The patch " + patchID + " NOT should be listed as installed" ,
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertEquals("Misc file should not be overridden", file1patchedContent, PatchingTestUtil.readFile(file1));
        Assert.assertEquals("Misc file should be restored", file2modifiedContent, PatchingTestUtil.readFile(file2));
        controller.stop(CONTAINER);
    }

    /**
     * Prepare a patch that modifies two misc files. [LICENSE.txt, README.txt]
     * Modify these two files before installing the patch.
     * apply patch with --override-all
     * rollback patch with --preserve=README.txt --override=LICENSE.txt
     */
    @Test
    public void testOverrideAllMiscFiles() throws Exception {
        // prepare the patch
        String patchID = randomString();
        File oneOffPatchDir = mkdir(tempDir, patchID);

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

        // rollback patch
        Assert.assertTrue("Rollback should be accepted",
                CliUtilsForPatching.rollbackPatch(patchID,
                        String.format(CliUtilsForPatching.OVERRIDE, "LICENSE.txt"),
                        String.format(CliUtilsForPatching.PRESERVE, "README.txt")));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertFalse("The patch " + patchID + " NOT should be listed as installed" ,
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertEquals("Misc file should not be overridden", file1patchedContent, PatchingTestUtil.readFile(file1));
        Assert.assertEquals("Misc file should be restored", file2modifiedContent, PatchingTestUtil.readFile(file2));
        controller.stop(CONTAINER);
    }

    /**
     * Prepare a patch that modifies two misc files. [LICENSE.txt, README.txt]
     * Modify these two files before installing the patch.
     * apply patch with --override=LICENSE.xt --preserve=README.txt
     * rollback patch with --preserve=README.txt --override=LICENSE.txt
     */
    @Test
    public void testOverrideOnePreserveOneMiscFile() throws Exception {
        // prepare the patch
        String patchID = randomString();
        File oneOffPatchDir = mkdir(tempDir, patchID);

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

        // apply patch with --override=LICENSE,txt --preserve=README.txt
        Assert.assertTrue("Patch should be accepted",
                CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath(),
                        String.format(CliUtilsForPatching.OVERRIDE, "LICENSE.txt"),
                        String.format(CliUtilsForPatching.PRESERVE, "README.txt")));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + patchID + " should be listed as installed", CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertEquals("Misc file should not be overwritten", file1modifiedContent, PatchingTestUtil.readFile(file1));
        Assert.assertEquals("Misc file should be overwritten", file2patchedContent, PatchingTestUtil.readFile(file2));

        // rollback patch
        Assert.assertTrue("Rollback should be accepted",
                CliUtilsForPatching.rollbackPatch(patchID,
                        String.format(CliUtilsForPatching.OVERRIDE, "LICENSE.txt"),
                        String.format(CliUtilsForPatching.PRESERVE, "README.txt")));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertFalse("The patch " + patchID + " NOT should be listed as installed" ,
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        Assert.assertEquals("Misc file should not be overridden", file1modifiedContent, PatchingTestUtil.readFile(file1));
        Assert.assertEquals("Misc file should be restored", file2modifiedContent, PatchingTestUtil.readFile(file2));
        controller.stop(CONTAINER);
    }

    /**
     * Prepare a patch that modifies module.
     * Modify this module before installing the patch.
     * apply patch with --override-modules
     * rollback patch
     */
    @Test
    public void testOverrideModules() throws Exception {
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

        // modify module
        File fileModifyModule = new File(moduleDir.getAbsolutePath() + FILE_SEPARATOR + "main", "newFile");
        dump(fileModifyModule, "test content");

        // apply patch without --override-modules
        controller.start(CONTAINER);
        Assert.assertFalse("Server should reject patch installation in this case",
                CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath()));

        // apply patch with --override-modules
        Assert.assertTrue("Patch should be accepted",
                CliUtilsForPatching.applyPatch(zippedPatch.getAbsolutePath(), CliUtilsForPatching.OVERRIDE_MODULES));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + patchID + " should be listed as installed", CliUtilsForPatching.getInstalledPatches().contains(patchID));

        // rollback patch
        Assert.assertTrue("Rollback should be accepted",
                CliUtilsForPatching.rollbackPatch(patchID, CliUtilsForPatching.OVERRIDE_MODULES));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertFalse("The patch " + patchID + " NOT should be listed as installed" ,
                CliUtilsForPatching.getInstalledPatches().contains(patchID));
        controller.stop(CONTAINER);
    }


}
