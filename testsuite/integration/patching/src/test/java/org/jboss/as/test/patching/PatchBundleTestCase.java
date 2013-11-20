package org.jboss.as.test.patching;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.patching.Constants;
import org.jboss.as.patching.HashUtils;
import org.jboss.as.patching.metadata.BundledPatch;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.jboss.as.test.patching.util.module.Module;
import org.jboss.as.version.ProductConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.jboss.as.patching.IoUtils.mkdir;
import static org.jboss.as.patching.IoUtils.newFile;
import static org.jboss.as.test.patching.PatchingTestUtil.AS_DISTRIBUTION;
import static org.jboss.as.test.patching.PatchingTestUtil.AS_VERSION;
import static org.jboss.as.test.patching.PatchingTestUtil.CONTAINER;
import static org.jboss.as.test.patching.PatchingTestUtil.FILE_SEPARATOR;
import static org.jboss.as.test.patching.PatchingTestUtil.MODULES_PATH;
import static org.jboss.as.test.patching.PatchingTestUtil.PRODUCT;
import static org.jboss.as.test.patching.PatchingTestUtil.createPatchBundleXMLFile;
import static org.jboss.as.test.patching.PatchingTestUtil.createPatchXMLFile;
import static org.jboss.as.test.patching.PatchingTestUtil.createZippedPatchFile;
import static org.jboss.as.test.patching.PatchingTestUtil.randomString;

/**
 * @author Martin Simka
 */
@RunWith(Arquillian.class)
@RunAsClient
public class PatchBundleTestCase extends AbstractPatchingTestCase {


    /**
     * creates 4 CPs
     * creates bundle containing 4 CPs
     * applies bundle
     * rollbacks CP one by one
     * @throws Exception
     */
    @Test
    public void testApplyBundle() throws Exception {
        final String bundleName = "bundle_"  + randomString();
        File patchBundleDir = mkdir(tempDir, bundleName);

        final String cpPatchID = randomString();
        final String cpPatchID2 = randomString();
        final String cpPatchID3 = randomString();
        final String cpPatchID4 = randomString();
        final String eapWithCP = "EAP with cp patch";
        final String eapWithCP2 = "EAP with cp patch 2";
        final String eapWithCP3 = "EAP with cp patch 3";
        final String eapWithCP4 = "EAP with cp patch 4";

        List<BundledPatch.BundledPatchEntry> patches = new ArrayList<BundledPatch.BundledPatchEntry>();

        File cpZip = createCumulativePatchAddingARandomModule(cpPatchID, AS_VERSION, eapWithCP, patchBundleDir);
        patches.add(new BundledPatch.BundledPatchEntry(cpPatchID, cpZip.getName()));
        File cpZip2 = createNextCumulativePatchAddingRandomModule(cpPatchID2, eapWithCP, cpPatchID, eapWithCP2, patchBundleDir);
        patches.add(new BundledPatch.BundledPatchEntry(cpPatchID2, cpZip2.getName()));
        File cpZip3 = createNextCumulativePatchModyfyingJbossModules(cpPatchID3, eapWithCP2, cpPatchID2, eapWithCP3, patchBundleDir);
        patches.add(new BundledPatch.BundledPatchEntry(cpPatchID3, cpZip3.getName()));
        File cpZip4 = createNextCumulativePatchAddingRandomModule(cpPatchID4, eapWithCP3, cpPatchID3, eapWithCP4, patchBundleDir);
        patches.add(new BundledPatch.BundledPatchEntry(cpPatchID4, cpZip4.getName()));

        createPatchBundleXMLFile(patchBundleDir, patches);
        File patchBundleZip = createZippedPatchFile(patchBundleDir, bundleName);

        // apply bundle
        controller.start(CONTAINER);
          Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(patchBundleZip.getAbsolutePath()));
          Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // verify cp version and rollback cp4
        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + cpPatchID4 + " should be listed as installed",
                CliUtilsForPatching.getCumulativePatchId().equalsIgnoreCase(cpPatchID4));
        Assert.assertTrue("Rollback should be accepted", CliUtilsForPatching.rollbackCumulativePatch(true));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // verify cp version and rollback cp4
        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + cpPatchID3 + " should be listed as installed",
                CliUtilsForPatching.getCumulativePatchId().equalsIgnoreCase(cpPatchID3));
        Assert.assertTrue("Rollback should be accepted", CliUtilsForPatching.rollbackCumulativePatch(true));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // verify cp version and rollback cp2
        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + cpPatchID2 + " should be listed as installed",
                CliUtilsForPatching.getCumulativePatchId().equalsIgnoreCase(cpPatchID2));
        Assert.assertTrue("Rollback should be accepted", CliUtilsForPatching.rollbackCumulativePatch(true));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // verify cp version and rollback cp
        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + cpPatchID + " should be listed as installed",
                CliUtilsForPatching.getCumulativePatchId().equalsIgnoreCase(cpPatchID));
        Assert.assertTrue("Rollback should be accepted", CliUtilsForPatching.rollbackCumulativePatch(true));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // verify base
        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + Constants.BASE + " should be listed as installed",
                CliUtilsForPatching.getCumulativePatchId().equalsIgnoreCase(Constants.BASE));
        controller.stop(CONTAINER);
    }

    /**
     * creates 4 CPs
     * creates bundle containing 4 CPs
     * applies CP1
     * applies CP2
     * applies one-off
     * applies bundle
     * rollbacks CPs and one-off one by one
     * @throws Exception
     */
    @Test
    public void testApplyBundleSkipTwoCPs() throws Exception {
        final String bundleName = "bundle_"  + randomString();
        File patchBundleDir = mkdir(tempDir, bundleName);

        final String cpPatchID = randomString();
        final String cpPatchID2 = randomString();
        final String cpPatchID3 = randomString();
        final String cpPatchID4 = randomString();
        final String oneOffId = randomString();
        final String eapWithCP = "EAP with cp patch";
        final String eapWithCP2 = "EAP with cp patch 2";
        final String eapWithCP3 = "EAP with cp patch 3";
        final String eapWithCP4 = "EAP with cp patch 4";

        List<BundledPatch.BundledPatchEntry> patches = new ArrayList<BundledPatch.BundledPatchEntry>();

        File cpZip = createCumulativePatchAddingARandomModule(cpPatchID, AS_VERSION, eapWithCP, patchBundleDir);
        patches.add(new BundledPatch.BundledPatchEntry(cpPatchID, cpZip.getName()));
        File cpZip2 = createNextCumulativePatchAddingRandomModule(cpPatchID2, eapWithCP, cpPatchID, eapWithCP2, patchBundleDir);
        patches.add(new BundledPatch.BundledPatchEntry(cpPatchID2, cpZip2.getName()));
        File cpZip3 = createNextCumulativePatchModyfyingJbossModules(cpPatchID3, eapWithCP2, cpPatchID2, eapWithCP3, patchBundleDir);
        patches.add(new BundledPatch.BundledPatchEntry(cpPatchID3, cpZip3.getName()));
        File cpZip4 = createNextCumulativePatchAddingRandomModule(cpPatchID4, eapWithCP3, cpPatchID3, eapWithCP4, patchBundleDir);
        patches.add(new BundledPatch.BundledPatchEntry(cpPatchID4, cpZip4.getName()));
        File oneOffZip = createOneOffPatchAddingMiscFile(oneOffId, eapWithCP2);

        createPatchBundleXMLFile(patchBundleDir, patches);
        File patchBundleZip = createZippedPatchFile(patchBundleDir, bundleName);

        // apply cp1
        controller.start(CONTAINER);
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(cpZip.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // verify cp and apply cp2
        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + cpPatchID + " should be listed as installed",
                CliUtilsForPatching.getCumulativePatchId().equalsIgnoreCase(cpPatchID));
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(cpZip2.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // verify cp and apply oneoff
        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + cpPatchID2 + " should be listed as installed",
                CliUtilsForPatching.getCumulativePatchId().equalsIgnoreCase(cpPatchID2));
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(oneOffZip.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // verify cp and oneoff and apply bundle
        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + cpPatchID2 + " should be listed as installed",
                CliUtilsForPatching.getCumulativePatchId().equalsIgnoreCase(cpPatchID2));
        Assert.assertTrue("The patch " + oneOffId + " should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(oneOffId));
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(patchBundleZip.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode", CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // verify cp and rollback cp4
        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + cpPatchID4 + " should be listed as installed",
                CliUtilsForPatching.getCumulativePatchId().equalsIgnoreCase(cpPatchID4));
        Assert.assertTrue("Rollback should be accepted", CliUtilsForPatching.rollbackCumulativePatch(true));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // verify cp and rollback cp3
        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + cpPatchID3 + " should be listed as installed",
                CliUtilsForPatching.getCumulativePatchId().equalsIgnoreCase(cpPatchID3));
        Assert.assertTrue("Rollback should be accepted", CliUtilsForPatching.rollbackCumulativePatch(true));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // verify cp and rollback oneoff
        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + cpPatchID2 + " should be listed as installed",
                CliUtilsForPatching.getCumulativePatchId().equalsIgnoreCase(cpPatchID2));
        Assert.assertTrue("The patch " + oneOffId + " should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(oneOffId));
        Assert.assertTrue("Rollback should be accepted", CliUtilsForPatching.rollbackPatch(oneOffId));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // verify cp and rollback cp2
        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + cpPatchID2 + " should be listed as installed",
                CliUtilsForPatching.getCumulativePatchId().equalsIgnoreCase(cpPatchID2));
        Assert.assertFalse("The patch " + oneOffId + " should not be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(oneOffId));
        Assert.assertTrue("Rollback should be accepted", CliUtilsForPatching.rollbackCumulativePatch(true));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // verify cp and rollback cp
        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + cpPatchID + " should be listed as installed",
                CliUtilsForPatching.getCumulativePatchId().equalsIgnoreCase(cpPatchID));
        Assert.assertTrue("Rollback should be accepted", CliUtilsForPatching.rollbackCumulativePatch(true));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        // verify base
        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + Constants.BASE + " should be listed as installed",
                CliUtilsForPatching.getCumulativePatchId().equalsIgnoreCase(Constants.BASE));
        controller.stop(CONTAINER);
    }


    private File createCumulativePatchAddingARandomModule(String patchID, String asVersion, final String targetAsVersion, File targetDir) throws Exception {
        String layerPatchID = "layer" + patchID;
        File cpPatchDir = mkdir(tempDir, patchID);

        final String moduleName = "org.wildfly.test." + randomString();

        final ResourceItem resourceItem1 = new ResourceItem("testFile1", "content1".getBytes());
        final ResourceItem resourceItem2 = new ResourceItem("testFile2", "content2".getBytes());

        Module newModule = new Module.Builder(moduleName)
                .miscFile(resourceItem1)
                .miscFile(resourceItem2)
                .build();

        // Create the version module
        final String versionModuleName = ProductInfo.getVersionModule();
        final String slot = ProductInfo.getVersionModuleSlot();
        final String originalVersionModulePath = MODULES_PATH + FILE_SEPARATOR + versionModuleName.replace(".", FILE_SEPARATOR) + FILE_SEPARATOR + slot;
        final Module modifiedModule = PatchingTestUtil.createVersionModule(targetAsVersion);

        ContentModification moduleAdded = ContentModificationUtils.addModule(cpPatchDir, layerPatchID, newModule);
        ContentModification versionModuleModified = ContentModificationUtils.modifyModule(cpPatchDir, layerPatchID, HashUtils.hashFile(new File(originalVersionModulePath)), modifiedModule);

        Patch cpPatch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription("A cp patch.")
                .upgradeIdentity(PRODUCT, asVersion, targetAsVersion)
                .getParent()
                .upgradeElement(layerPatchID, "base", false)
                .addContentModification(moduleAdded)
                .addContentModification(versionModuleModified)
                .getParent()
                .build();
        createPatchXMLFile(cpPatchDir, cpPatch);
        return createZippedPatchFile(cpPatchDir, patchID, targetDir);
    }

    private File createNextCumulativePatchAddingRandomModule(String patchID, String asVersion, final String currentPatch, final String targetAsVersion, File targetDir) throws Exception {
        String layerPatchID = "layer" + patchID;
        File cpPatchDir = mkdir(tempDir, patchID);
        final String moduleName = "org.wildfly.test." + randomString();

        // Create the version module
        final String versionModuleName = ProductInfo.getVersionModule();
        final Module modifiedModule = PatchingTestUtil.createVersionModule(targetAsVersion);

        // Calculate the target hash of the currently active module
        final String currentLayerPatchID = "layer" + currentPatch;
        File originalVersionModulePath = new File(tempDir, currentPatch);
        originalVersionModulePath = new File(originalVersionModulePath, currentLayerPatchID);
        originalVersionModulePath = new File(originalVersionModulePath, Constants.MODULES);
        originalVersionModulePath = newFile(originalVersionModulePath, versionModuleName.split("\\."));
        originalVersionModulePath = new File(originalVersionModulePath, ProductInfo.getVersionModuleSlot());
        byte[] patchedAsVersionHash = HashUtils.hashFile(originalVersionModulePath);
        assert patchedAsVersionHash != null;

        final ResourceItem resourceItem1 = new ResourceItem("testFile1", "content1".getBytes());
        final ResourceItem resourceItem2 = new ResourceItem("testFile2", "content2".getBytes());

        Module newModule = new Module.Builder(moduleName)
                .miscFile(resourceItem1)
                .miscFile(resourceItem2)
                .build();

        ContentModification moduleAdded = ContentModificationUtils.addModule(cpPatchDir, layerPatchID, newModule);
        ContentModification versionModuleModified = ContentModificationUtils.modifyModule(cpPatchDir, layerPatchID, patchedAsVersionHash, modifiedModule);

        ProductConfig productConfig = new ProductConfig(PRODUCT, asVersion, "main");
        Patch cpPatch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription("A cp patch.")
                .upgradeIdentity(productConfig.getProductName(), productConfig.getProductVersion(), targetAsVersion)
                .getParent()
                .upgradeElement(layerPatchID, "base", false)
                .addContentModification(versionModuleModified)
                .addContentModification(moduleAdded)
                .getParent()
                .build();
        createPatchXMLFile(cpPatchDir, cpPatch);
        return createZippedPatchFile(cpPatchDir, patchID, targetDir);
    }

    private File createNextCumulativePatchModyfyingJbossModules(String patchID, String asVersion, final String currentPatch, final String targetAsVersion, File targetDir) throws Exception {
        String layerPatchID = "layer" + patchID;
        File cpPatchDir = mkdir(tempDir, patchID);

        // Also see if we can update jboss-modules
        final File installation = new File(AS_DISTRIBUTION);
        final File patchDir = new File(cpPatchDir, patchID);
        final ContentModification jbossModulesModification = PatchingTestUtil.updateModulesJar(installation, patchDir);

        // Create the version module
        final String versionModuleName = ProductInfo.getVersionModule();
        final Module modifiedModule = PatchingTestUtil.createVersionModule(targetAsVersion);

        // Calculate the target hash of the currently active module
        final String currentLayerPatchID = "layer" + currentPatch;
        File originalVersionModulePath = new File(tempDir, currentPatch);
        originalVersionModulePath = new File(originalVersionModulePath, currentLayerPatchID);
        originalVersionModulePath = new File(originalVersionModulePath, Constants.MODULES);
        originalVersionModulePath = newFile(originalVersionModulePath, versionModuleName.split("\\."));
        originalVersionModulePath = new File(originalVersionModulePath, ProductInfo.getVersionModuleSlot());
        byte[] patchedAsVersionHash = HashUtils.hashFile(originalVersionModulePath);
        assert patchedAsVersionHash != null;

        ContentModification versionModuleModified = ContentModificationUtils.modifyModule(cpPatchDir, layerPatchID, patchedAsVersionHash, modifiedModule);

        Patch cpPatch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription("A cp patch.")
                .upgradeIdentity(PRODUCT, asVersion, targetAsVersion)
                .getParent()
                .upgradeElement(layerPatchID, "base", false)
                .addContentModification(versionModuleModified)
                .getParent()
                .addContentModification(jbossModulesModification)
                .build();
        createPatchXMLFile(cpPatchDir, cpPatch);
        return createZippedPatchFile(cpPatchDir, patchID, targetDir);
    }

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

}
