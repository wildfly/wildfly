/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.web.test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.web.Constants.ACCESS_LOG;
import static org.jboss.as.web.Constants.CONFIGURATION;
import static org.jboss.as.web.Constants.CONNECTOR;
import static org.jboss.as.web.Constants.DIRECTORY;
import static org.jboss.as.web.Constants.SETTING;
import static org.jboss.as.web.Constants.SSL;
import static org.jboss.as.web.Constants.SSO;
import static org.jboss.as.web.Constants.VIRTUAL_SERVER;
import static org.jboss.as.web.WebExtension.SUBSYSTEM_NAME;

import java.io.IOException;

import junit.framework.Assert;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.OperationTransformer.TransformedOperation;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.as.web.Constants;
import org.jboss.as.web.WebExtension;
import org.jboss.as.web.WebMessages;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 *
 * @author Jean-Frederic Clere
 * @author Kabir Khan
 */
public class WebSubsystemTestCase extends AbstractSubsystemBaseTest {

    public WebSubsystemTestCase() {
        super(WebExtension.SUBSYSTEM_NAME, new WebExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem.xml");

    }
    @Override
    protected String getSubsystemXml(String configId) throws IOException {
        return readResource(configId);
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.MANAGEMENT;
    }

    @Override
    protected void compareXml(String configId, String original, String marshalled) throws Exception {
        super.compareXml(configId, original, marshalled, true);
    }

    @Test
    public void testAliases() throws Exception {
        KernelServices services = super.installInController(readResource("subsystem.xml"));
        ModelNode noAliasModel = services.readWholeModel();
        ModelNode aliasModel = services.readWholeModel(true);

        testSSLAlias(services, noAliasModel, aliasModel);
        testSSOAlias(services, noAliasModel, aliasModel);
        testAccessLogAlias(services, noAliasModel, aliasModel);
    }

    @Test
    public void testTransformation_1_1_0_JBPAPP_9134() throws Exception {
        String subsystemXml = readResource("subsystem.xml");
        ModelVersion modelVersion = ModelVersion.create(1, 1, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(null)
                .setSubsystemXml(subsystemXml);

        //This legacy subsystem references classes in the removed org.jboss.as.controller.alias package,
        //which is why we need to include the jboss-as-controller artifact.
        builder.createLegacyKernelServicesBuilder(null, modelVersion)
            .addMavenResourceURL("org.jboss.as:jboss-as-web:7.1.2.Final")
            .addMavenResourceURL("org.jboss.as:jboss-as-controller:7.1.2.Final")
            .addParentFirstClassPattern("org.jboss.as.controller.*")
            .addChildFirstClassPattern("org.jboss.as.controller.alias.*");

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertFalse(mainServices.getLegacyServices(modelVersion).isSuccessfulBoot());
    }

    @Test
    public void testTransformation_1_1_0() throws Exception {
        String subsystemXml = readResource("subsystem-1.1.0.xml");
        ModelVersion modelVersion = ModelVersion.create(1, 1, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(subsystemXml);

        //This legacy subsystem references classes in the removed org.jboss.as.controller.alias package,
        //which is why we need to include the jboss-as-controller artifact.
        builder.createLegacyKernelServicesBuilder(null, modelVersion)
            .addMavenResourceURL("org.jboss.as:jboss-as-web:7.1.2.Final")
            .addMavenResourceURL("org.jboss.as:jboss-as-controller:7.1.2.Final")
            .addParentFirstClassPattern("org.jboss.as.controller.*")
            .addChildFirstClassPattern("org.jboss.as.controller.alias.*");

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, modelVersion);

        ModelNode mainModel = mainServices.readWholeModel().get(SUBSYSTEM, SUBSYSTEM_NAME);
        ModelNode legacyModel = legacyServices.readWholeModel().get(SUBSYSTEM, SUBSYSTEM_NAME);

        //Now do some checks to make sure that the actual data is correct in the transformed model
        ModelNode sslConfig = mainModel.get(Constants.CONNECTOR, "https", Constants.CONFIGURATION, Constants.SSL);
        Assert.assertTrue(sslConfig.isDefined());
        Assert.assertFalse(legacyModel.get(Constants.CONNECTOR, "https", Constants.CONFIGURATION, Constants.SSL).isDefined());
        compare(sslConfig, legacyModel.get(Constants.CONNECTOR, "https", Constants.SSL, Constants.CONFIGURATION), true);

        ModelNode ssoConfig = mainModel.get(Constants.VIRTUAL_SERVER, "default-host", Constants.CONFIGURATION, Constants.SSO);
        Assert.assertTrue(ssoConfig.isDefined());
        Assert.assertFalse(legacyModel.get(Constants.VIRTUAL_SERVER, "default-host", Constants.CONFIGURATION, Constants.SSO).isDefined());
        compare(ssoConfig, legacyModel.get(Constants.VIRTUAL_SERVER, "default-host", Constants.SSO, Constants.CONFIGURATION), true);

        ModelNode mainAccessLog = mainModel.get(Constants.VIRTUAL_SERVER, "default-host", Constants.CONFIGURATION, Constants.ACCESS_LOG);
        Assert.assertTrue(mainAccessLog.isDefined());
        Assert.assertFalse(legacyModel.get(Constants.VIRTUAL_SERVER, "default-host", Constants.CONFIGURATION, Constants.ACCESS_LOG).isDefined());
        ModelNode legacyAccessLog = legacyModel.get(Constants.VIRTUAL_SERVER, "default-host", Constants.ACCESS_LOG, Constants.CONFIGURATION);
        Assert.assertTrue(legacyAccessLog.isDefined());
        ModelNode mainDir = mainAccessLog.remove(Constants.SETTING).get(Constants.DIRECTORY);
        Assert.assertTrue(mainDir.isDefined());
        Assert.assertFalse(legacyAccessLog.hasDefined(Constants.SETTING));
        ModelNode legacyDir = legacyAccessLog.remove(Constants.DIRECTORY).get(Constants.CONFIGURATION);
        Assert.assertTrue(legacyDir.isDefined());
        compare(mainDir, legacyDir);
        compare(mainAccessLog, legacyAccessLog, true);

        //Test that virtual server gets rejected in the legacy controller
        ModelNode connectorWriteVirtualServer = createOperation(WRITE_ATTRIBUTE_OPERATION, SUBSYSTEM, WebExtension.SUBSYSTEM_NAME, Constants.CONNECTOR, "http");
        connectorWriteVirtualServer.get(NAME).set(VIRTUAL_SERVER);
        connectorWriteVirtualServer.get(VALUE).add("vs1");
        mainServices.executeForResult(connectorWriteVirtualServer);
        ModelNode result = mainServices.executeOperation(modelVersion, mainServices.transformOperation(modelVersion, connectorWriteVirtualServer));
        Assert.assertEquals(WebMessages.MESSAGES.transformationVersion_1_1_0_JBPAPP_9314(), result.get(FAILURE_DESCRIPTION).asString());

        //Grab the current connector values and remove the connector
        ModelNode connectorValues = mainServices.readWholeModel().get(SUBSYSTEM, WebExtension.SUBSYSTEM_NAME, Constants.CONNECTOR, "http");
        Assert.assertTrue(connectorValues.hasDefined(VIRTUAL_SERVER));
        ModelNode connectorRemove = createOperation(REMOVE, SUBSYSTEM, WebExtension.SUBSYSTEM_NAME, Constants.CONNECTOR, "http");
        mainServices.executeForResult(connectorRemove);
        checkOutcome(mainServices.executeOperation(modelVersion, mainServices.transformOperation(modelVersion, connectorRemove)));

        //Now test that adding the connector with virtual server fails in the legacy controller
        ModelNode connectorAdd = createOperation(ADD, SUBSYSTEM, WebExtension.SUBSYSTEM_NAME, Constants.CONNECTOR, "test");
        for (String key: connectorValues.keys()) {
            connectorAdd.get(key).set(connectorValues.get(key));
        }
        checkOutcome(mainServices.executeOperation(connectorAdd));
        TransformedOperation transOp = mainServices.transformOperation(modelVersion, connectorAdd);
        result = mainServices.executeOperation(modelVersion, transOp);
        Assert.assertEquals(FAILED, result.get(OUTCOME).asString());
        Assert.assertEquals(WebMessages.MESSAGES.transformationVersion_1_1_0_JBPAPP_9314(), result.get(FAILURE_DESCRIPTION).asString());
    }


    private void testSSLAlias(KernelServices services, ModelNode noAliasModel, ModelNode aliasModel) throws Exception {
        //Check the aliased entry is not there
        String[] targetAddr = getAddress(SUBSYSTEM, SUBSYSTEM_NAME, CONNECTOR, "https", CONFIGURATION, SSL);
        String[] aliasAddr = getAddress(SUBSYSTEM, SUBSYSTEM_NAME, CONNECTOR, "https", SSL, CONFIGURATION);
        testAliases(services, noAliasModel, aliasModel, targetAddr, aliasAddr);

        testChangeAttribute(services, "ca-certificate-password", "pwd123", "123pwd", targetAddr, aliasAddr);
    }

    private void testSSOAlias(KernelServices services, ModelNode noAliasModel, ModelNode aliasModel) throws Exception {
        String[] targetAddr = getAddress(SUBSYSTEM, SUBSYSTEM_NAME, VIRTUAL_SERVER, "default-host", CONFIGURATION, SSO);
        String[] aliasAddr = getAddress(SUBSYSTEM, SUBSYSTEM_NAME, VIRTUAL_SERVER, "default-host", SSO, CONFIGURATION);
        testAliases(services, noAliasModel, aliasModel, targetAddr, aliasAddr);

        testChangeAttribute(services, "domain", "domain123", "123domain", targetAddr, aliasAddr);
    }

    private void testAccessLogAlias(KernelServices services, ModelNode noAliasModel, ModelNode aliasModel) throws Exception {
        String[] targetAddr = getAddress(SUBSYSTEM, SUBSYSTEM_NAME, VIRTUAL_SERVER, "default-host", CONFIGURATION, ACCESS_LOG);
        String[] aliasAddr = getAddress(SUBSYSTEM, SUBSYSTEM_NAME, VIRTUAL_SERVER, "default-host", ACCESS_LOG, CONFIGURATION);
        testAliases(services, noAliasModel, aliasModel, targetAddr, aliasAddr);


        //Check the aliased child entry is not there
        String[] targetChildMainAddr = getAddress(SUBSYSTEM, SUBSYSTEM_NAME, VIRTUAL_SERVER, "default-host", CONFIGURATION, ACCESS_LOG, SETTING, DIRECTORY);
        String[] targetChildAliasAddr = getAddress(SUBSYSTEM, SUBSYSTEM_NAME, VIRTUAL_SERVER, "default-host", CONFIGURATION, ACCESS_LOG, DIRECTORY, CONFIGURATION);
        Assert.assertTrue(noAliasModel.get(targetChildMainAddr).isDefined());
        Assert.assertFalse(noAliasModel.get(targetChildAliasAddr).isDefined());

        //Check the aliased child entry is there
        String[] aliasChildMainAddr = getAddress(SUBSYSTEM, SUBSYSTEM_NAME, VIRTUAL_SERVER, "default-host", ACCESS_LOG, CONFIGURATION, SETTING, DIRECTORY);
        String[] aliasChildAliasAddr = getAddress(SUBSYSTEM, SUBSYSTEM_NAME, VIRTUAL_SERVER, "default-host", ACCESS_LOG, CONFIGURATION, DIRECTORY, CONFIGURATION);
        Assert.assertEquals(aliasModel.get(targetChildMainAddr), aliasModel.get(targetChildAliasAddr));
        Assert.assertEquals(aliasModel.get(aliasChildMainAddr), aliasModel.get(targetChildAliasAddr));
        Assert.assertEquals(aliasModel.get(aliasChildMainAddr), aliasModel.get(aliasChildAliasAddr));

        testChangeAttribute(services, "pattern", "pattern123", "123pattern", targetAddr, aliasAddr);

        testChangeAttribute(services, "path", "path123", "123path", targetChildMainAddr, targetChildAliasAddr);
        testChangeAttribute(services, "path", "path345", "345path", targetChildMainAddr, aliasChildAliasAddr);
        testChangeAttribute(services, "path", "path678", "678path", targetChildMainAddr, aliasChildMainAddr);
    }

    private void testAliases(KernelServices services, ModelNode noAliasModel, ModelNode aliasModel, String[] targetAddr, String[] aliasAddr) throws Exception {
        //Check the aliased entry is not there
        Assert.assertTrue(noAliasModel.get(targetAddr).isDefined());
        Assert.assertFalse(noAliasModel.get(aliasAddr).isDefined());

        //Check the aliased version is there
        Assert.assertEquals(aliasModel.get(targetAddr), aliasModel.get(aliasAddr));
    }

    private void testChangeAttribute(KernelServices services, String attributeName, String value1, String value2, String[] targetAddr, String[] aliasAddr) throws Exception {
        writeAttribute(services, attributeName, value1, aliasAddr);
        Assert.assertEquals(value1, readAttribute(services, attributeName, aliasAddr));
        Assert.assertEquals(value1, readAttribute(services, attributeName, targetAddr));

        writeAttribute(services, attributeName, value2, targetAddr);
        Assert.assertEquals(value2, readAttribute(services, attributeName, aliasAddr));
        Assert.assertEquals(value2, readAttribute(services, attributeName, targetAddr));
    }

    private void writeAttribute(KernelServices services, String name, String value, String...address) throws Exception {
        ModelNode op = createOperation(WRITE_ATTRIBUTE_OPERATION, address);
        op.get(NAME).set(name);
        op.get(VALUE).set(value);
        services.executeForResult(op);
    }

    private String readAttribute(KernelServices services, String name, String...address) throws Exception {
        ModelNode op = createOperation(READ_ATTRIBUTE_OPERATION, address);
        op.get(NAME).set(name);
        ModelNode result =  services.executeForResult(op);
        if (result.isDefined()) {
            return result.asString();
        }
        return null;
    }

    private ModelNode createOperation(String operationName, String...address) {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(operationName);
        if (address.length > 0) {
            if (address.length % 2 != 0) {
                throw new IllegalArgumentException("Address must be in pairs");
            }
            for (int i = 0 ; i < address.length ; i+=2) {
                operation.get(OP_ADDR).add(address[i], address[i + 1]);
            }
        } else {
            operation.get(OP_ADDR).setEmptyList();
        }

        return operation;
    }


    private String[] getAddress(String...addr) {
        return addr;
    }

}

