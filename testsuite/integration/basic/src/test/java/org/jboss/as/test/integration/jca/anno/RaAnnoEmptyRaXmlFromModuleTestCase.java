/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.anno;

import jakarta.annotation.Resource;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.jca.annorar.AnnoAdminObject;
import org.jboss.as.test.integration.jca.annorar.AnnoConnectionFactory;
import org.jboss.as.test.integration.jca.annorar.AnnoConnectionImpl;
import org.jboss.as.test.integration.jca.annorar.AnnoManagedConnectionFactory;
import org.jboss.as.test.integration.jca.moduledeployment.AbstractModuleDeploymentTestCaseSetup;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.jboss.as.controller.client.helpers.ClientConstants.ADD;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP_ADDR;
import static org.jboss.as.controller.client.helpers.ClientConstants.SUBSYSTEM;
import static org.jboss.as.controller.client.helpers.ClientConstants.VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Annotated RA with empty ra.xml and deployed as JBoss module.
 *
 * @author <a href="mailto:lgao@redhat.com">Lin Gao</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(RaAnnoEmptyRaXmlFromModuleTestCase.EmptyRaXMLWithAnnotationSetup.class)
public class RaAnnoEmptyRaXmlFromModuleTestCase {

    private static final String MODULE_NAME = "org.jboss.ironjacamar.ra16out";
    private static final String JNDI_CF = "java:/eis/raannocf";
    private static final String JNDI_AO = "java:/eis/ao/ra16annoao";
    private static final String RA_MODULE = "ramodule";
    private static final String CF_POOL = "ConnectionPool";
    private static final String AO_POOL = "AdminObjectPool";
    private static final PathAddress RA_ADDRESS = PathAddress.pathAddress(SUBSYSTEM, "resource-adapters")
            .append("resource-adapter", RA_MODULE);

    private static final String RA_EMPTY = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<connector xmlns=\"http://java.sun.com/xml/ns/javaee\"\n" +
            "           xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "           xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee\n" +
            "           http://java.sun.com/xml/ns/j2ee/connector_1_6.xsd\"\n" +
            "           version=\"1.6\" metadata-complete=\"false\">\n" +
            "\n" +
            "    <vendor-name>Red Hat Middleware LLC</vendor-name>\n" +
            "    <eis-type>Test RA</eis-type>\n" +
            "    <resourceadapter-version>0.1</resourceadapter-version>\n" +
            "    <resourceadapter />\n" +
            "</connector>";
    public static class EmptyRaXMLWithAnnotationSetup extends SnapshotRestoreSetupTask {

        private TestModule customModule;
        private Path metaPath;

        @Override
        public void doSetup(ManagementClient managementClient, String s) throws Exception {
            // create the ra module
            ClassLoader cl = RaAnnoEmptyRaXmlFromModuleTestCase.class.getClassLoader();
            URL moduleURL = cl.getResource(RaAnnoEmptyRaXmlFromModuleTestCase.class.getPackageName().replace(".", File.separator) + "/ramodule.xml");
            assertNotNull(moduleURL);
            customModule = new TestModule(MODULE_NAME, new File(moduleURL.toURI()));
            customModule.addResource("ra16out.jar").addPackage(AnnoConnectionFactory.class.getPackage());
            customModule.create();
            // create META-INF/ra.xml to the ra module
            try (InputStream input = new ByteArrayInputStream(RA_EMPTY.getBytes())) {
                metaPath = TestModule.getModulesDirectory(false)
                        .toPath()
                        .resolve(MODULE_NAME.replace('.', File.separatorChar))
                        .resolve("main")
                        .resolve("META-INF");
                if (Files.notExists(metaPath)) {
                    Files.createDirectories(metaPath);
                }
                Files.copy(input, metaPath.resolve("ra.xml"), StandardCopyOption.REPLACE_EXISTING);
            }
            // create activation in ra subsystem
            createRAActivation(managementClient.getControllerClient());

            ServerReload.executeReloadAndWaitForCompletion(managementClient, 50000);

        }

        private void createRAActivation(ModelControllerClient modelControllerClient) throws Exception {
            final ModelNode createRAOp = new ModelNode();
            createRAOp.get(OP).set(ADD);
            createRAOp.get(OP_ADDR).set(RA_ADDRESS.toModelNode());
            createRAOp.get("module").set(MODULE_NAME);
            createRAOp.get("transaction-support").set("NoTransaction");
            ManagementOperations.executeOperation(modelControllerClient, createRAOp);

            ModelNode createCDOp = new ModelNode();
            createCDOp.get(OP).set(ADD);
            createCDOp.get(OP_ADDR).set(RA_ADDRESS.append("connection-definitions", CF_POOL).toModelNode());
            createCDOp.get("class-name").set("org.jboss.as.test.integration.jca.annorar.AnnoManagedConnectionFactory");
            createCDOp.get("jndi-name").set(JNDI_CF);
            ManagementOperations.executeOperation(modelControllerClient, createCDOp);

            ModelNode createAOOp = new ModelNode();
            createAOOp.get(OP).set(ADD);
            createAOOp.get(OP_ADDR).set(RA_ADDRESS.append("admin-objects", AO_POOL).toModelNode());
            createAOOp.get("jndi-name").set(JNDI_AO);
            createAOOp.get("class-name").set("org.jboss.as.test.integration.jca.annorar.AnnoAdminObjectImpl");
            ManagementOperations.executeOperation(modelControllerClient, createAOOp);

            ModelNode cpFirstOp = new ModelNode();
            cpFirstOp.get(OP).set(ADD);
            cpFirstOp.get(OP_ADDR).set(RA_ADDRESS.append("connection-definitions", CF_POOL).append("config-properties", "first").toModelNode());
            cpFirstOp.get(VALUE).set(23);
            ManagementOperations.executeOperation(modelControllerClient, cpFirstOp);

            ModelNode aoSecondOp = new ModelNode();
            aoSecondOp.get(OP).set(ADD);
            aoSecondOp.get(OP_ADDR).set(RA_ADDRESS.append("admin-objects", AO_POOL).append("config-properties", "second").toModelNode());
            aoSecondOp.get(VALUE).set(true);
            ManagementOperations.executeOperation(modelControllerClient, aoSecondOp);

            // activate
            ModelNode raActivateOp = new ModelNode();
            raActivateOp.get(OP).set("activate");
            raActivateOp.get(OP_ADDR).set(RA_ADDRESS.toModelNode());
            ManagementOperations.executeOperation(modelControllerClient, raActivateOp);
        }

        @Override
        public void nonManagementCleanUp() throws Exception {
            // delete meta directory
            if (metaPath != null) {
                Files.deleteIfExists(metaPath);
            }
            // delete module
            if (customModule != null) {
                customModule.remove();
            }
        }
    }

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "dummy.jar")
                .addClasses(RaAnnoEmptyRaXmlFromModuleTestCase.class, AbstractModuleDeploymentTestCaseSetup.class)
                .addAsManifestResource(
                new StringAsset(
                        "Dependencies: wildflyee.api, org.jboss.as.controller-client, "
                                + "org.jboss.as.controller, "
                                + "org.jboss.as.connector, "
                                + "org.jboss.dmr, "
                                + "org.jboss.ironjacamar.ra16out, "
                                + "org.jboss.remoting\n"),
                "MANIFEST.MF");
    }

    @Resource(mappedName = JNDI_CF)
    private AnnoConnectionFactory connectionFactory;

    @Resource(mappedName = JNDI_AO)
    private AnnoAdminObject adminObject;

    @Test
    public void testGetConnection() throws Throwable {
        assertNotNull(connectionFactory);
        AnnoConnectionImpl connection = (AnnoConnectionImpl) connectionFactory.getConnection();
        assertNotNull(connection);
        AnnoManagedConnectionFactory mcf = connection.getMCF();
        assertNotNull(mcf);
        // first is updated during activation
        assertEquals((byte) 23, (byte) mcf.getFirst());
        // second uses default
        assertEquals((short) 0, (short) mcf.getSecond());
        connection.close();
    }

    @Test
    public void testAdminOjbect() {
        assertNotNull(adminObject);
        assertEquals((long) 12345, (long) adminObject.getFirst());
        assertEquals(true, adminObject.getSecond());
    }

}
