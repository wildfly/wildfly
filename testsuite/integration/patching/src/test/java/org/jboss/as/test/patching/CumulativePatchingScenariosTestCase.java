package org.jboss.as.test.patching;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;

import org.jboss.as.patching.HashUtils;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.jboss.as.version.ProductConfig;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

import static org.jboss.as.patching.Constants.BASE;
import static org.jboss.as.patching.IoUtils.mkdir;
import static org.jboss.as.test.patching.PatchingTestUtil.AS_VERSION;
import static org.jboss.as.test.patching.PatchingTestUtil.CONTAINER;
import static org.jboss.as.test.patching.PatchingTestUtil.FILE_SEPARATOR;
import static org.jboss.as.test.patching.PatchingTestUtil.MODULES_PATH;
import static org.jboss.as.test.patching.PatchingTestUtil.PRODUCT;
import static org.jboss.as.test.patching.PatchingTestUtil.createPatchXMLFile;
import static org.jboss.as.test.patching.PatchingTestUtil.createZippedPatchFile;
import static org.jboss.as.test.patching.PatchingTestUtil.randomString;

/**
 * @author Martin Simka
 */
@RunWith(Arquillian.class)
@RunAsClient
public class CumulativePatchingScenariosTestCase extends AbstractPatchingTestCase {

    private File createOneOffPatchAddingMiscFile(String patchID, String asVersion) throws Exception {
        File oneOffPatchDir = mkdir(tempDir, patchID);
        ContentModification miscFileAdded = ContentModificationUtils.addMisc(oneOffPatchDir, patchID,
                "test content", "awesomeDirectory", "awesomeFile");
        ProductConfig productConfig = new ProductConfig(PRODUCT, asVersion, "main");
        Patch oneOffPatch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription("A one-off patch adding a misc file.")
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                .getParent()
                .addContentModification(miscFileAdded)
                .build();
        createPatchXMLFile(oneOffPatchDir, oneOffPatch);
        return createZippedPatchFile(oneOffPatchDir, patchID);
    }

    private File createOneOffPatchAddingAModule(String patchID, String asVersion) throws Exception {
        String layerPatchID = randomString();
        File oneOffPatchDir = mkdir(tempDir, patchID);

        final String moduleName = "org.wildfly.awesomemodule";

        final ResourceItem resourceItem1 = new ResourceItem("testFile1", "content1".getBytes());
        final ResourceItem resourceItem2 = new ResourceItem("testFile2", "content2".getBytes());

        ContentModification moduleAdded = ContentModificationUtils.addModule(oneOffPatchDir, layerPatchID, moduleName, resourceItem1, resourceItem2);
        ProductConfig productConfig = new ProductConfig(PRODUCT, asVersion, "main");
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
        return createZippedPatchFile(oneOffPatchDir, patchID);
    }

    private File createCumulativePatch(String patchID, String asVersion, final String targetAsVersion) throws Exception {
        String layerPatchID = randomString();
        File cpPatchDir = mkdir(tempDir, patchID);

        final String moduleName = "patch.cumulative.awesomemodule";

        final ResourceItem resourceItem1 = new ResourceItem("testFile1", "content1".getBytes());
        final ResourceItem resourceItem2 = new ResourceItem("testFile2", "content2".getBytes());


        Asset newManifest = new Asset() {
            @Override
            public InputStream openStream() {
                String line = "JBossAS-Release-Version: " + targetAsVersion;
                return new ByteArrayInputStream(line.getBytes());
            }
        };
        JavaArchive versionModuleJar = ShrinkWrap.create(JavaArchive.class)
                .addPackage("org.jboss.as.version")
                .addAsManifestResource(newManifest, "MANIFEST.MF");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        versionModuleJar.as(ZipExporter.class).exportTo(baos);
        ResourceItem versionModuleResourceItem = new ResourceItem("as-version.jar", baos.toByteArray());
        final String versionModuleName = "org.jboss.as.version";
        final String originalVersionModulePath = MODULES_PATH + FILE_SEPARATOR + versionModuleName.replace(".", FILE_SEPARATOR) + FILE_SEPARATOR + "main";

        ContentModification moduleAdded = ContentModificationUtils.addModule(cpPatchDir, layerPatchID, moduleName, resourceItem1, resourceItem2);
        ContentModification versionModuleModified = ContentModificationUtils.modifyModule(cpPatchDir, layerPatchID, versionModuleName, HashUtils.hashFile(new File(originalVersionModulePath)), versionModuleResourceItem);
        ProductConfig productConfig = new ProductConfig(PRODUCT, asVersion, "main");
        Patch cpPatch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription("A cp patch.")
                .upgradeIdentity(productConfig.getProductName(), productConfig.getProductVersion(), targetAsVersion)
                .getParent()
                .upgradeElement(layerPatchID, "base", false)
                .addContentModification(moduleAdded)
                .addContentModification(versionModuleModified)
                .getParent()
                .build();
        createPatchXMLFile(cpPatchDir, cpPatch);
        return createZippedPatchFile(cpPatchDir, patchID);
    }

    /**
     * Applies one-off that adds a misc file
     * Applies one-off that adds a module
     * Applies CP that adds a module, should invalidate all previously installed one-off
     * does rollback of CP
     * does rollback of all one-offs
     *
     * @throws Exception
     */
    @Test
    public void testTwoOneOffsInvalidatedByCumulativePatch() throws Exception {
        String oneOffPatchID1 = randomString();
        String oneOffPatchID2 = randomString();
        String cpPatchID = randomString();
        File oneOffZip1 = createOneOffPatchAddingMiscFile(oneOffPatchID1, AS_VERSION);
        File oneOffZip2 = createOneOffPatchAddingAModule(oneOffPatchID2, AS_VERSION);
        File cpZip = createCumulativePatch(cpPatchID, AS_VERSION, "EAP with cp patch");

        // apply oneoffs
        controller.start(CONTAINER);
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(oneOffZip1.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + oneOffPatchID1 + " should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(oneOffPatchID1));
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(oneOffZip2.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + oneOffPatchID2 + " should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(oneOffPatchID2));

        // apply cumulative patch
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(cpZip.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + cpPatchID + " should be listed as installed",
                CliUtilsForPatching.getCumulativePatchId().equalsIgnoreCase(cpPatchID));
        Assert.assertFalse("The patch " + oneOffPatchID1 + " should NOT be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(oneOffPatchID1));
        Assert.assertFalse("The patch " + oneOffPatchID2 + " should NOT be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(oneOffPatchID2));

        // rollback cumulative patch
        Assert.assertTrue("Rollback should be accepted", CliUtilsForPatching.rollbackCumulativePatch(true));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertFalse("The patch " + cpPatchID + " should NOT be listed as installed",
                CliUtilsForPatching.getCumulativePatchId().equalsIgnoreCase(cpPatchID));
        Assert.assertTrue("The cumulative patch id should be " + BASE,
                CliUtilsForPatching.getCumulativePatchId().equalsIgnoreCase(BASE));
        Assert.assertTrue("The patch " + oneOffPatchID1 + " should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(oneOffPatchID1));
        Assert.assertTrue("The patch " + oneOffPatchID2 + " should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(oneOffPatchID2));

        //rollback oneoffs
        Assert.assertTrue("Rollback should be accepted", CliUtilsForPatching.rollbackPatch(oneOffPatchID2));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertFalse("The patch " + oneOffPatchID2 + " should NOT be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(oneOffPatchID2));
        Assert.assertTrue("Rollback should be accepted", CliUtilsForPatching.rollbackPatch(oneOffPatchID1));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertFalse("The patch " + oneOffPatchID1 + " should NOT be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(oneOffPatchID1));
        controller.stop(CONTAINER);
    }

    /**
     * Applies cumulative patch
     * Tries to apply one-off targeted to version without cp
     * Applies one-off targeted to new version with cp
     * Does rollback of one-off
     * Does rollback of CP
     * Applies one-off targeted to version without cp
     *
     * @throws Exception
     */
    @Test
    public void testApplyOneOffToWrongTargetVersion() throws Exception {
        final String cpAsVersion = "EAP with cp patch";
        String oneOffPatchForOldVersionWithoutCPID = randomString();
        String oneOffPatchForNewVersionWithCPID = randomString();
        String cpPatchID = randomString();
        File oneOffPatchForOldVersionWithoutCPZip = createOneOffPatchAddingMiscFile(oneOffPatchForOldVersionWithoutCPID, AS_VERSION);
        File oneOffPatchForNewVersionWithCPZip = createOneOffPatchAddingAModule(oneOffPatchForNewVersionWithCPID, cpAsVersion);
        File cpZip = createCumulativePatch(cpPatchID, AS_VERSION, cpAsVersion);

        // apply cumulative patch
        controller.start(CONTAINER);
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(cpZip.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + cpPatchID + " should be listed as installed",
                CliUtilsForPatching.getCumulativePatchId().equalsIgnoreCase(cpPatchID));

        // try to apply one-off targeted to old version
        Assert.assertFalse("Patch shouldn't be accepted", CliUtilsForPatching.applyPatch(oneOffPatchForOldVersionWithoutCPZip.getAbsolutePath()));

        // apply one-off targeted to version with CP
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(oneOffPatchForNewVersionWithCPZip.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + cpPatchID + " should be listed as installed",
                CliUtilsForPatching.getCumulativePatchId().equalsIgnoreCase(cpPatchID));
        Assert.assertTrue("The patch " + oneOffPatchForNewVersionWithCPID + " should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(oneOffPatchForNewVersionWithCPID));

        // try to rollback one-off
        Assert.assertFalse("Rollback shouldn't be accepted", CliUtilsForPatching.rollbackCumulativePatch(true));

        // rollback one-off for new version
        Assert.assertTrue("Rollback should be accepted", CliUtilsForPatching.rollbackPatch(oneOffPatchForNewVersionWithCPID));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertFalse("The patch " + oneOffPatchForNewVersionWithCPID + " shouldn't be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(oneOffPatchForNewVersionWithCPID));
        Assert.assertTrue("The patch " + cpPatchID + " should be listed as installed",
                CliUtilsForPatching.getCumulativePatchId().equalsIgnoreCase(cpPatchID));

        // rollback cp
        Assert.assertTrue("Rollback should be accepted", CliUtilsForPatching.rollbackCumulativePatch(true));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertFalse("The patch " + cpPatchID + " shouldn't be listed as installed",
                CliUtilsForPatching.getCumulativePatchId().equalsIgnoreCase(cpPatchID));

        // apply one-off patch for old version without cp
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(oneOffPatchForOldVersionWithoutCPZip.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + oneOffPatchForOldVersionWithoutCPZip + " should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(oneOffPatchForNewVersionWithCPID));
        controller.stop(CONTAINER);
    }


}
