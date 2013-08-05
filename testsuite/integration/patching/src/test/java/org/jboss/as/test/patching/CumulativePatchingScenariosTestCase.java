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
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

import static org.jboss.as.patching.Constants.BASE;
import static org.jboss.as.patching.IoUtils.mkdir;
import static org.jboss.as.patching.IoUtils.newFile;
import static org.jboss.as.test.patching.PatchingTestUtil.AS_DISTRIBUTION;
import static org.jboss.as.test.patching.PatchingTestUtil.AS_VERSION;
import static org.jboss.as.test.patching.PatchingTestUtil.CONTAINER;
import static org.jboss.as.test.patching.PatchingTestUtil.FILE_SEPARATOR;
import static org.jboss.as.test.patching.PatchingTestUtil.MODULES_PATH;
import static org.jboss.as.test.patching.PatchingTestUtil.PRODUCT;
import static org.jboss.as.test.patching.PatchingTestUtil.createPatchXMLFile;
import static org.jboss.as.test.patching.PatchingTestUtil.createZippedPatchFile;
import static org.jboss.as.test.patching.PatchingTestUtil.randomString;
import static org.jboss.as.test.patching.PatchingTestUtil.readFile;

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

    private File createInvalidCumulativePatch(String patchID, String asVersion, final String targetAsVersion) throws Exception {
        String layerPatchID = randomString();
        File cpPatchDir = mkdir(tempDir, patchID);

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

        // create broken patch - replaced layerPatchID with patchID
        ContentModification versionModuleModified = ContentModificationUtils.modifyModule(cpPatchDir, patchID, versionModuleName, HashUtils.hashFile(new File(originalVersionModulePath)), versionModuleResourceItem);
        ProductConfig productConfig = new ProductConfig(PRODUCT, asVersion, "main");
        Patch cpPatch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription("A cp patch.")
                .upgradeIdentity(productConfig.getProductName(), productConfig.getProductVersion(), targetAsVersion)
                .getParent()
                .upgradeElement(layerPatchID, "base", false)
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
     * Applies one-off that adds a misc file
     * Applies one-off that adds a module
     * tries to apply a broken CP
     * patch shouldn't be accepted and one-offs should stay consistent
     * @throws Exception
     */
    @Test
    public void testOneOffInvalidCumulativePatch() throws Exception {
        String oneOffPatchID1 = randomString();
        String oneOffPatchID2 = randomString();
        String cpPatchID = randomString();
        File oneOffZip1 = createOneOffPatchAddingMiscFile(oneOffPatchID1, AS_VERSION);
        File oneOffZip2 = createOneOffPatchAddingAModule(oneOffPatchID2, AS_VERSION);
        File cpZip = createInvalidCumulativePatch(cpPatchID, AS_VERSION, "EAP with cp patch");

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
        Assert.assertFalse("Patch shouldn't be accepted", CliUtilsForPatching.applyPatch(cpZip.getAbsolutePath()));
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + oneOffPatchID1 + " should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(oneOffPatchID1));
        Assert.assertTrue("The patch " + oneOffPatchID2 + " should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(oneOffPatchID2));

        File miscFile = newFile(new File(AS_DISTRIBUTION), "awesomeDirectory", "awesomeFile");
        Assert.assertTrue("File " + miscFile.getAbsolutePath() + " should exist.", miscFile.exists());
        Assert.assertEquals("Unexpected content of file: " + miscFile.getAbsolutePath(), "test content", readFile(miscFile.getAbsolutePath()));
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

    /**
     * Applies CP
     * changes standalone/configuration/standalone.xml
     * changes domain/configuration/domain.xml
     * changes appclient/configuration/appclient.xml
     * does rollback of CP with --reset-configuration=true
     * Applies CP
     * changes standalone/configuration/standalone.xml
     * changes domain/configuration/domain.xml
     * changes appclient/configuration/appclient.xml
     * does rollback of CP with --reset-configuration=false
     * @throws Exception
     */
    @Test
    public void testCumulativePatchRollbackRestoreConfiguration() throws Exception {
        final String cpAsVersion = "EAP with cp patch";
        String cpPatchID = randomString();
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
        controller.stop(CONTAINER);

        // save original content of files in standalone/configuration, domain/configuration, appclient/configuration
        final String standaloneXmlPath = AS_DISTRIBUTION + FILE_SEPARATOR + "standalone" + FILE_SEPARATOR + "configuration" + FILE_SEPARATOR + "standalone.xml";
        final String standaloneConfOrigContent = readFile(standaloneXmlPath);

        final String domainXmlPath = AS_DISTRIBUTION + FILE_SEPARATOR + "domain" + FILE_SEPARATOR + "configuration" + FILE_SEPARATOR + "domain.xml";
        final String domainConfOrigContent = readFile(domainXmlPath);

        final String appClientXmlPath = AS_DISTRIBUTION + FILE_SEPARATOR + "appclient" + FILE_SEPARATOR + "configuration" + FILE_SEPARATOR + "appclient.xml";
        final String appClientConfOrigContent = readFile(appClientXmlPath);

        changeDatasource(standaloneXmlPath);
        changeDatasource(domainXmlPath);
        changeDatasource(appClientXmlPath);

        controller.start(CONTAINER);
        // rollback with reset-configuration=true
        Assert.assertTrue("Rollback should be accepted", CliUtilsForPatching.rollbackCumulativePatch(true));
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertEquals("File should be restored", standaloneConfOrigContent, readFile(standaloneXmlPath));
        Assert.assertEquals("File should be restored", domainConfOrigContent, readFile(domainXmlPath));
        Assert.assertEquals("File should be restored", appClientConfOrigContent, readFile(appClientXmlPath));

        // apply cumulative patch
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(cpZip.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + cpPatchID + " should be listed as installed",
                CliUtilsForPatching.getCumulativePatchId().equalsIgnoreCase(cpPatchID));
        controller.stop(CONTAINER);

        changeDatasource(standaloneXmlPath);
        changeDatasource(domainXmlPath);
        changeDatasource(appClientXmlPath);

        controller.start(CONTAINER);
        // rollback with reset-configuration=false
        Assert.assertTrue("Rollback should be accepted", CliUtilsForPatching.rollbackCumulativePatch(false));
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertNotEquals("File shouldn't be restored", standaloneConfOrigContent, readFile(standaloneXmlPath));
        Assert.assertNotEquals("File shouldn't be restored", domainConfOrigContent, readFile(domainXmlPath));
        Assert.assertNotEquals("File shouldn't be restored", appClientConfOrigContent, readFile(appClientXmlPath));
        controller.stop(CONTAINER);


    }

    private void changeDatasource(String filePath) throws Exception {
        // modify xml
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document = documentBuilder.parse(filePath);

        NodeList nodeList = document.getElementsByTagName("datasource");

        if(nodeList.getLength() < 1) {
            throw new IllegalStateException("unexpected count of datasources");
        }

        Node node = nodeList.item(0);
        Node attributeNode = node.getAttributes().getNamedItem("jndi-name");
        attributeNode.setNodeValue("java:jboss/datasources/changedDS");

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource domSource = new DOMSource(document);
        StreamResult streamResult = new StreamResult(new File(filePath));
        transformer.transform(domSource, streamResult);
    }


}
