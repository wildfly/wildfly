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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.model.test.FailedOperationTransformationConfig.REJECTED_RESOURCE;
import static org.jboss.as.web.Constants.ACCESS_LOG;
import static org.jboss.as.web.Constants.CONFIGURATION;
import static org.jboss.as.web.Constants.CONNECTOR;
import static org.jboss.as.web.Constants.DIRECTORY;
import static org.jboss.as.web.Constants.PREFIX;
import static org.jboss.as.web.Constants.REDIRECT_PORT;
import static org.jboss.as.web.Constants.SETTING;
import static org.jboss.as.web.Constants.SSL;
import static org.jboss.as.web.Constants.SSO;
import static org.jboss.as.web.Constants.VIRTUAL_SERVER;
import static org.jboss.as.web.WebExtension.SUBSYSTEM_NAME;
import static org.jboss.as.web.WebExtension.VALVE_PATH;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.transform.OperationTransformer.TransformedOperation;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.FailedOperationTransformationConfig.AttributesPathAddressConfig;
import org.jboss.as.model.test.FailedOperationTransformationConfig.ChainedConfig;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.as.web.Constants;
import org.jboss.as.web.WebExtension;
import org.jboss.as.web.WebMessages;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Jean-Frederic Clere
 * @author Kabir Khan
 */
public class WebSubsystemTestCase extends AbstractSubsystemBaseTest {

    static {
        System.setProperty("jboss.server.config.dir", "target/jbossas");
    }

    public WebSubsystemTestCase() {
        super(WebExtension.SUBSYSTEM_NAME, new WebExtension());
    }

    private static FailedOperationTransformationConfig.ChainedConfig createChainedConfig(String[] rejectedExpression, String[] newAttributes) {
        String[] allAttributes = new String[rejectedExpression.length + newAttributes.length];
        System.arraycopy(rejectedExpression, 0, allAttributes, 0, rejectedExpression.length);
        System.arraycopy(newAttributes, 0, allAttributes, rejectedExpression.length, newAttributes.length);

        return FailedOperationTransformationConfig.ChainedConfig.createBuilder(allAttributes)
                .addConfig(new FailedOperationTransformationConfig.RejectExpressionsConfig(rejectedExpression))
                .addConfig(new FailedOperationTransformationConfig.NewAttributesConfig(newAttributes)).build();
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem-2.1.0.xml");

    }

    @Override
    protected String getSubsystemXml(String configId) throws IOException {
        return readResource(configId);
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization() {

            private static final long serialVersionUID = 1L;

            @Override
            protected ProcessType getProcessType() {
                return ProcessType.HOST_CONTROLLER;
            }

            @Override
            protected RunningMode getRunningMode() {
                return RunningMode.ADMIN_ONLY;
            }
        };

    }

    @Override
    protected void compareXml(String configId, String original, String marshalled) throws Exception {
        super.compareXml(configId, original, marshalled, true);
    }

    @Test
    public void testAliases() throws Exception {
        KernelServices services = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXmlResource("subsystem.xml")
                .build();
        ModelNode noAliasModel = services.readWholeModel();
        ModelNode aliasModel = services.readWholeModel(true);

        testSSLAlias(services, noAliasModel, aliasModel);
        testSSOAlias(services, noAliasModel, aliasModel);
        testAccessLogAlias(services, noAliasModel, aliasModel);
    }

    @Test
    public void testTransformation_1_1_0_JBPAPP_9314() throws Exception {
        String subsystemXml = readResource("subsystem-1.1.0-JBPAPP-9314.xml");
        ModelVersion modelVersion = ModelVersion.create(1, 1, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXml(subsystemXml);

        builder.createLegacyKernelServicesBuilder(null, ModelTestControllerVersion.V7_1_2_FINAL, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-web:7.1.2.Final")
                .skipReverseControllerCheck();

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        // The legacy slave would fail due to the presence of virtual servers, but the
        // test fixture checks that the op would be rejected and doesn't send it.
        // So the legacy slave will boot successfully
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());
        // But, the http-vs connector should not be there
        ModelNode legacyModel = legacyServices.readWholeModel();
        Assert.assertFalse(legacyModel.get("subsystem", "web", "connector").has("http-vs"));
        // Sanity check
        Assert.assertTrue(mainServices.readWholeModel().get("subsystem", "web", "connector").hasDefined("http-vs"));
    }

    @Test
    public void testRejectExpressionsAS712() throws Exception {
        testRejectExpressions_1_1_x(ModelTestControllerVersion.V7_1_2_FINAL, ModelVersion.create(1, 1, 0));
    }

    @Test
    public void testRejectExpressionsEAP600() throws Exception {
        testRejectExpressions_1_1_x(ModelTestControllerVersion.EAP_6_0_0, ModelVersion.create(1, 1, 0));
    }
    @Test
    public void testRejectExpressionsAS713() throws Exception {
        testRejectExpressions_1_1_x(ModelTestControllerVersion.V7_1_3_FINAL, ModelVersion.create(1, 1, 1));
    }

    @Test
    public void testRejectExpressionsEAP601() throws Exception {
        testRejectExpressions_1_1_x(ModelTestControllerVersion.EAP_6_0_1, ModelVersion.create(1, 1, 1));
    }

    private void testRejectExpressions_1_1_x(ModelTestControllerVersion controllerVersion, ModelVersion modelVersion) throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());

        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-web:" + controllerVersion.getMavenGavVersion())
                .setExtensionClassName("org.jboss.as.web.WebExtension")
                .configureReverseControllerCheck(createAdditionalInitialization(), null);

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        final PathAddress subsystem = PathAddress.EMPTY_ADDRESS.append("subsystem", "web");
        final PathAddress defaultHost = subsystem.append(PathElement.pathElement("virtual-server", "default-host"));

        List<ModelNode> xmlOps = builder.parseXmlResource("subsystem.xml");

        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig()
        // valve
        .addFailedAttribute(subsystem.append(VALVE_PATH), REJECTED_RESOURCE)
                // configuration=container
        .addFailedAttribute(subsystem.append(PathElement.pathElement("configuration", "container")),
                new FailedOperationTransformationConfig.RejectExpressionsConfig("welcome-file"))
                // configuration=static=resources
        .addFailedAttribute(subsystem.append(PathElement.pathElement("configuration", "static-resources")),
                new FailedOperationTransformationConfig.RejectExpressionsConfig("listings", "sendfile", "file-encoding",
                        "read-only", "webdav", "secret", "max-depth", "disabled"))
                // configuration=jsp-configuration
        .addFailedAttribute(subsystem.append(PathElement.pathElement("configuration", "jsp-configuration")),
                createChainedConfig(
                        new String[]{
                                "development", "disabled", "keep-generated",
                                "trim-spaces", "tag-pooling", "mapped-file", "check-interval", "modification-test-interval",
                                "recompile-on-fail", "smap", "dump-smap", "generate-strings-as-char-arrays",
                                "error-on-use-bean-invalid-class-attribute", "scratch-dir", "source-vm", "target-vm",
                                "java-encoding", "x-powered-by", "display-source-fragment"},
                        new String[0]))
                // connector=http
        .addFailedAttribute(subsystem.append(PathElement.pathElement("connector", "http")),
                new FailedOperationTransformationConfig.RejectExpressionsConfig("socket-binding", "enabled", "enable-lookups",
                        "proxy-name", "proxy-port", "max-post-size", "max-save-post-size", "redirect-port",
                        "max-connections", "executor"))
                // Connector https
        .addFailedAttribute(subsystem.append(PathElement.pathElement("connector", "https"), PathElement.pathElement("configuration", "ssl")),
                new ChainedConfig(Arrays.asList(new AttributesPathAddressConfig<?>[]{
                        new FailedOperationTransformationConfig.RejectExpressionsConfig("certificate-key-file", "ca-certificate-file", "key-alias",
                            "password", "cipher-suite", "protocol", "verify-client", "verify-depth", "certificate-file", "ca-revocation-url",
                            "ca-certificate-password", "keystore-type", "truststore-type", "session-cache-size", "session-timeout", "ssl-protocol"),
                            new FailedOperationTransformationConfig.NewAttributesConfig("ssl-protocol")
                    }),
                    "certificate-key-file", "ca-certificate-file", "key-alias",
                    "password", "cipher-suite", "protocol", "verify-client", "verify-depth", "certificate-file", "ca-revocation-url",
                    "ca-certificate-password", "keystore-type", "truststore-type", "session-cache-size", "session-timeout", "ssl-protocol"));

        if (modelVersion.getMicro() == 0) {
                // Connector http-vs
            config.addFailedAttribute(subsystem.append(PathElement.pathElement("connector", "http-vs")),
                new FailedOperationTransformationConfig.NewAttributesConfig("virtual-server"));
        }
                // virtual-server=default-host
        config.addFailedAttribute(defaultHost.append(PathElement.pathElement("rewrite", "myrewrite")),
                new FailedOperationTransformationConfig.RejectExpressionsConfig("flags", "pattern", "substitution"))
        .addFailedAttribute(defaultHost.append(PathElement.pathElement("rewrite", "with-conditions")),
                new FailedOperationTransformationConfig.RejectExpressionsConfig("flags", "pattern", "substitution"))
        .addFailedAttribute(defaultHost.append(PathElement.pathElement("rewrite", "with-conditions"), PathElement.pathElement("condition", "https")),
                new FailedOperationTransformationConfig.RejectExpressionsConfig("flags", "pattern", "test"))
        .addFailedAttribute(defaultHost.append(PathElement.pathElement("rewrite", "with-conditions"), PathElement.pathElement("condition", "no-flags")),
                new SetMissingRewriteConditionFlagsConfig("flags"))
        .addFailedAttribute(defaultHost.append(PathElement.pathElement("configuration", "sso")),
                new FailedOperationTransformationConfig.RejectExpressionsConfig("reauthenticate", "domain"));

        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, xmlOps, config);

        checkUndefinedCipherSuite(mainServices, modelVersion);
    }

    @Test
    public void testTransformationAS712() throws Exception {
        testTransformation_1_1_0(ModelTestControllerVersion.V7_1_2_FINAL, ModelVersion.create(1, 1, 0));
    }

    @Test
    public void testTransformationEAP600() throws Exception {
        testTransformation_1_1_0(ModelTestControllerVersion.EAP_6_0_0, ModelVersion.create(1, 1, 0));
    }

    @Test
    public void testTransformationAS713() throws Exception {
        testTransformation_1_1_0(ModelTestControllerVersion.V7_1_3_FINAL, ModelVersion.create(1, 1, 1));
    }

    @Test
    public void testTransformationEAP601() throws Exception {
        testTransformation_1_1_0(ModelTestControllerVersion.EAP_6_0_1, ModelVersion.create(1, 1, 1));
    }

    private void testTransformation_1_1_0(ModelTestControllerVersion controllerVersion, ModelVersion modelVersion) throws Exception {
        String subsystemXml = readResource("subsystem-1.1.0.xml");
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXml(subsystemXml);

        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-web:" + controllerVersion.getMavenGavVersion())
                .setExtensionClassName("org.jboss.as.web.WebExtension")
                .configureReverseControllerCheck(createAdditionalInitialization(), null);

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, modelVersion, AccessLogPrefixFixer_1_1_x.INSTANCE);

        ModelNode mainModel = mainServices.readWholeModel().get(SUBSYSTEM, SUBSYSTEM_NAME);
        ModelNode legacyModel = AccessLogPrefixFixer_1_1_x.INSTANCE.fixModel(legacyServices.readWholeModel().get(SUBSYSTEM, SUBSYSTEM_NAME));

        //Now do some checks to make sure that the actual data is correct in the transformed model
        ModelNode sslConfig = mainModel.get(Constants.CONNECTOR, "https", Constants.CONFIGURATION, Constants.SSL);
        Assert.assertTrue(sslConfig.isDefined());
        Assert.assertFalse(legacyModel.get(Constants.CONNECTOR, "https", Constants.CONFIGURATION, Constants.SSL).isDefined());
        sslConfig.remove(Constants.NAME);
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

        //This section tests stuff that was fixed in version 1.1.1, so it should fail for 1.1.0 and pass in 1.1.1

        //Test that virtual server gets rejected in the legacy controller
        ModelNode connectorWriteVirtualServer = createOperation(WRITE_ATTRIBUTE_OPERATION, SUBSYSTEM, WebExtension.SUBSYSTEM_NAME, Constants.CONNECTOR, "http");
        connectorWriteVirtualServer.get(NAME).set(VIRTUAL_SERVER);
        connectorWriteVirtualServer.get(VALUE).add("vs1");
        mainServices.executeForResult(connectorWriteVirtualServer);
        ModelNode result = mainServices.executeOperation(modelVersion, mainServices.transformOperation(modelVersion, connectorWriteVirtualServer));
        if (modelVersion.getMicro() == 0) {
            Assert.assertTrue(result.get(FAILURE_DESCRIPTION).asString().endsWith(WebMessages.MESSAGES.transformationVersion_1_1_0_JBPAPP_9314()));
        } else {
            checkOutcome(result);
        }

        //Grab the current connector values and remove the connector
        ModelNode connectorValues = mainServices.readWholeModel().get(SUBSYSTEM, WebExtension.SUBSYSTEM_NAME, Constants.CONNECTOR, "http");
        Assert.assertTrue(connectorValues.hasDefined(VIRTUAL_SERVER));
        ModelNode connectorRemove = createOperation(REMOVE, SUBSYSTEM, WebExtension.SUBSYSTEM_NAME, Constants.CONNECTOR, "http");
        mainServices.executeForResult(connectorRemove);
        checkOutcome(mainServices.executeOperation(modelVersion, mainServices.transformOperation(modelVersion, connectorRemove)));

        //Now test that adding the connector with virtual server fails in the legacy controller
        ModelNode connectorAdd = createOperation(ADD, SUBSYSTEM, WebExtension.SUBSYSTEM_NAME, Constants.CONNECTOR, "test");
        for (String key : connectorValues.keys()) {
            connectorAdd.get(key).set(connectorValues.get(key));
        }
        checkOutcome(mainServices.executeOperation(connectorAdd));
        TransformedOperation transOp = mainServices.transformOperation(modelVersion, connectorAdd);
        result = mainServices.executeOperation(modelVersion, transOp);
        if (modelVersion.getMicro() == 0) {
            Assert.assertEquals(FAILED, result.get(OUTCOME).asString());
            Assert.assertTrue(result.get(FAILURE_DESCRIPTION).asString().endsWith(WebMessages.MESSAGES.transformationVersion_1_1_0_JBPAPP_9314()));
        } else {
            checkOutcome(result);
        }

        //End - This section tests stuff that was fixed in version 1.1.1

        //Now test the correction of the default redirect-port

        // First, in add
        connectorAdd = createOperation(ADD, SUBSYSTEM, WebExtension.SUBSYSTEM_NAME, Constants.CONNECTOR, "as75871");
        for (String key : connectorValues.keys()) {
            if (!key.equals(REDIRECT_PORT) && !key.equals(VIRTUAL_SERVER)) {
                connectorAdd.get(key).set(connectorValues.get(key));
            }
        }
        Assert.assertFalse(connectorAdd.hasDefined(REDIRECT_PORT));
        checkOutcome(mainServices.executeOperation(connectorAdd));
        transOp = mainServices.transformOperation(modelVersion, connectorAdd);
        Assert.assertTrue(transOp.getTransformedOperation().hasDefined(REDIRECT_PORT));
        Assert.assertEquals(443, transOp.getTransformedOperation().get(REDIRECT_PORT).asInt());
        checkOutcome(mainServices.executeOperation(modelVersion, transOp));
        ModelNode transformed = mainServices.readTransformedModel(modelVersion).get(SUBSYSTEM, WebExtension.SUBSYSTEM_NAME, CONNECTOR, "as75871");
        Assert.assertTrue(transformed.hasDefined(REDIRECT_PORT));
        Assert.assertEquals(443, transformed.get(REDIRECT_PORT).asInt());

        // Next, in a write-attribute setting to undefined
        ModelNode write = createOperation(WRITE_ATTRIBUTE_OPERATION, SUBSYSTEM, WebExtension.SUBSYSTEM_NAME, Constants.CONNECTOR, "as75871");
        write.get(NAME).set(REDIRECT_PORT);
        write.get(VALUE);
        transOp = mainServices.transformOperation(modelVersion, write);
        ModelNode translatedWrite = transOp.getTransformedOperation();
        Assert.assertTrue(translatedWrite.hasDefined(VALUE));
        Assert.assertEquals(443, translatedWrite.get(VALUE).asInt());
        checkOutcome(mainServices.executeOperation(modelVersion, transOp));
        transformed = mainServices.readTransformedModel(modelVersion).get(SUBSYSTEM, WebExtension.SUBSYSTEM_NAME, CONNECTOR, "as75871");
        Assert.assertTrue(transformed.hasDefined(REDIRECT_PORT));
        Assert.assertEquals(443, transformed.get(REDIRECT_PORT).asInt());

        // Finally, test undefine-attribute translating to write-attribute
        ModelNode undefine = createOperation(UNDEFINE_ATTRIBUTE_OPERATION, SUBSYSTEM, WebExtension.SUBSYSTEM_NAME, Constants.CONNECTOR, "as75871");
        undefine.get(NAME).set(REDIRECT_PORT);
        transOp = mainServices.transformOperation(modelVersion, undefine);
        Assert.assertEquals(translatedWrite, transOp.getTransformedOperation());
        checkOutcome(mainServices.executeOperation(modelVersion, transOp));
        transformed = mainServices.readTransformedModel(modelVersion).get(SUBSYSTEM, WebExtension.SUBSYSTEM_NAME, CONNECTOR, "as75871");
        Assert.assertTrue(transformed.hasDefined(REDIRECT_PORT));
        Assert.assertEquals(443, transformed.get(REDIRECT_PORT).asInt());
    }

    @Test
    public void testTransformationAS720() throws Exception {
        testTransformation_1_2_0(ModelTestControllerVersion.V7_2_0_FINAL);
    }

    @Test
    public void testTransformationEAP610() throws Exception {
        testTransformation_1_2_0(ModelTestControllerVersion.EAP_6_1_0);
    }

    @Test
    public void testTransformationEAP611() throws Exception {
        testTransformation_1_2_0(ModelTestControllerVersion.EAP_6_1_1);
    }

    @Test
    public void testTransformationEAP620() throws Exception {
        testTransformation_1_3_0(ModelTestControllerVersion.EAP_6_2_0);
    }

    @Test
    @Ignore("WFLY-3153")
    public void testTransformationWildFly8() throws Exception {
        testTransformation_2_0(ModelTestControllerVersion.WILDFLY_8_0_0_FINAL);
    }


    private void testTransformation_2_0(ModelTestControllerVersion controllerVersion) throws Exception {
        ModelVersion modelVersion = ModelVersion.create(2, 0, 0);
        String subsystemXml = readResource("subsystem-2.0.0.xml");
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXml(subsystemXml);

        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.wildfly:wildfly-web:" + controllerVersion.getMavenGavVersion())
                .setExtensionClassName("org.jboss.as.web.WebExtension")
                .configureReverseControllerCheck(createAdditionalInitialization(), null);

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, modelVersion, new ModelFixer.CumulativeModelFixer(SSLConfigurationNameFixer.INSTANCE, AccessLogPrefixFixer_1_2_0.INSTANCE));
    }

    private void testTransformation_1_3_0(ModelTestControllerVersion controllerVersion) throws Exception {
        ModelVersion modelVersion = ModelVersion.create(1, 3, 0);
        String subsystemXml = readResource("subsystem-1.3.0.xml");
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXml(subsystemXml);

        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-web:" + controllerVersion.getMavenGavVersion())
                .setExtensionClassName("org.jboss.as.web.WebExtension")
                .configureReverseControllerCheck(createAdditionalInitialization(), null);

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, modelVersion, new ModelFixer.CumulativeModelFixer(SSLConfigurationNameFixer.INSTANCE, AccessLogPrefixFixer_1_2_0.INSTANCE));
    }

    private void testTransformation_1_2_0(ModelTestControllerVersion controllerVersion) throws Exception {
        ModelVersion modelVersion = ModelVersion.create(1, 2, 0);
        String subsystemXml = readResource("subsystem-1.2.0.xml");
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXml(subsystemXml);

        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-web:" + controllerVersion.getMavenGavVersion())
                .setExtensionClassName("org.jboss.as.web.WebExtension")
                .configureReverseControllerCheck(createAdditionalInitialization(), null);

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, modelVersion, new ModelFixer.CumulativeModelFixer(SSLConfigurationNameFixer.INSTANCE, AccessLogPrefixFixer_1_2_0.INSTANCE));
    }

    @Test
    public void testRejectingTransformersAS720() throws Exception {
        testRejectingTransformers_1_2_0(ModelTestControllerVersion.V7_2_0_FINAL);
    }

    @Test
    public void testRejectingTransformersEAP610() throws Exception {
        testRejectingTransformers_1_2_0(ModelTestControllerVersion.EAP_6_1_0);
    }


    @Test
    public void testRejectingTransformersAS611() throws Exception {
        testRejectingTransformers_1_2_0(ModelTestControllerVersion.EAP_6_1_1);
    }

    @Test
    public void testRejectingTransformersAS620() throws Exception {
        testRejectingTransformers_1_3_0(ModelTestControllerVersion.EAP_6_2_0);
    }

    @Test
    @Ignore("WFLY-3153")
    public void testRejectingTransformersWildFly8() throws Exception {
        testRejectingTransformers_2_0(ModelTestControllerVersion.WILDFLY_8_0_0_FINAL);
    }

    private void testRejectingTransformers_1_2_0(ModelTestControllerVersion controllerVersion) throws Exception {
        ModelVersion modelVersion = ModelVersion.create(1, 2, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());

        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-web:" + controllerVersion.getMavenGavVersion())
                .setExtensionClassName("org.jboss.as.web.WebExtension")
                .configureReverseControllerCheck(createAdditionalInitialization(), null);

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        final PathAddress subsystem = PathAddress.EMPTY_ADDRESS.append("subsystem", "web");
        final PathAddress defaultHost = subsystem.append(PathElement.pathElement("virtual-server", "default-host"));

        List<ModelNode> xmlOps = builder.parseXmlResource("subsystem.xml");

        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig()

        .addFailedAttribute(subsystem.append(PathElement.pathElement("connector", "https"), PathElement.pathElement("configuration", "ssl")),
                            new FailedOperationTransformationConfig.NewAttributesConfig("ssl-protocol"))
        .addFailedAttribute(defaultHost.append(PathElement.pathElement("rewrite", "with-conditions"), PathElement.pathElement("condition", "no-flags")),
                new SetMissingRewriteConditionFlagsConfig("flags"));

        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, xmlOps, config);

        checkUndefinedCipherSuite(mainServices, modelVersion);
    }


    private void testRejectingTransformers_1_3_0(ModelTestControllerVersion controllerVersion) throws Exception {

        ModelVersion modelVersion = ModelVersion.create(1, 3, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());

        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-web:" + controllerVersion.getMavenGavVersion())
                .setExtensionClassName("org.jboss.as.web.WebExtension")
                .configureReverseControllerCheck(createAdditionalInitialization(), null);

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        final PathAddress subsystem = PathAddress.EMPTY_ADDRESS.append("subsystem", "web");

        List<ModelNode> xmlOps = builder.parseXmlResource("subsystem-2.1.0.xml");

        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig()
        .addFailedAttribute(subsystem.append(PathElement.pathElement("connector", "http")),
                            new FailedOperationTransformationConfig.NewAttributesConfig("redirect-binding", "proxy-binding"));

        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, xmlOps, config);

        checkUndefinedCipherSuite(mainServices, modelVersion);
    }

    private void testRejectingTransformers_2_0(ModelTestControllerVersion controllerVersion) throws Exception {

        ModelVersion modelVersion = ModelVersion.create(2, 0, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());

        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.wildfly:wildfly-web:" + controllerVersion.getMavenGavVersion())
                .setExtensionClassName("org.jboss.as.web.WebExtension")
                .configureReverseControllerCheck(createAdditionalInitialization(), null);

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        final PathAddress subsystem = PathAddress.EMPTY_ADDRESS.append("subsystem", "web");

        List<ModelNode> xmlOps = builder.parseXmlResource("subsystem-2.1.0.xml");

        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig()
        .addFailedAttribute(subsystem.append(PathElement.pathElement("connector", "http")),
                            new FailedOperationTransformationConfig.NewAttributesConfig("redirect-binding", "proxy-binding"));

        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, xmlOps, config);

        checkUndefinedCipherSuite(mainServices, modelVersion);
    }

    private void checkUndefinedCipherSuite(KernelServices services, ModelVersion version) throws Exception  {
        final ModelNode success = new ModelNode();
        success.get(ModelDescriptionConstants.OUTCOME).set(ModelDescriptionConstants.SUCCESS);
        success.get(ModelDescriptionConstants.RESULT);
        success.protect();

        PathAddress addr = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, getMainSubsystemName()),
                PathElement.pathElement("connector", "https"), PathElement.pathElement("configuration", "ssl"));

        ModelNode op = Util.createOperation(WRITE_ATTRIBUTE_OPERATION, addr);
        op.get(NAME).set("cipher-suite");
        op.get(VALUE).set(new ModelNode());
        TransformedOperation transOp = services.transformOperation(version, op);
        Assert.assertTrue(transOp.rejectOperation(success));

        op.get(VALUE).set("SSL_RSA_WITH_3DES_EDE_CBC_SHA");
        transOp = services.transformOperation(version, op);
        Assert.assertFalse(transOp.rejectOperation(success));
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

    private void writeAttribute(KernelServices services, String name, String value, String... address) throws Exception {
        ModelNode op = createOperation(WRITE_ATTRIBUTE_OPERATION, address);
        op.get(NAME).set(name);
        op.get(VALUE).set(value);
        services.executeForResult(op);
    }

    private String readAttribute(KernelServices services, String name, String... address) throws Exception {
        ModelNode op = createOperation(READ_ATTRIBUTE_OPERATION, address);
        op.get(NAME).set(name);
        ModelNode result = services.executeForResult(op);
        if (result.isDefined()) {
            return result.asString();
        }
        return null;
    }

    private ModelNode createOperation(String operationName, String... address) {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(operationName);
        if (address.length > 0) {
            if (address.length % 2 != 0) {
                throw new IllegalArgumentException("Address must be in pairs");
            }
            for (int i = 0; i < address.length; i += 2) {
                operation.get(OP_ADDR).add(address[i], address[i + 1]);
            }
        } else {
            operation.get(OP_ADDR).setEmptyList();
        }

        return operation;
    }

    private String[] getAddress(String... addr) {
        return addr;
    }

    @Override
    protected void validateDescribeOperation(KernelServices hc, AdditionalInitialization serverInit, ModelNode expectedModel) throws Exception {
        final ModelNode operation = createDescribeOperation();
        final ModelNode result = hc.executeOperation(operation);
        Assert.assertTrue("The subsystem describe operation must fail",
                result.hasDefined(ModelDescriptionConstants.FAILURE_DESCRIPTION));
    }

    private static class SSLConfigurationNameFixer implements ModelFixer {
        private static final ModelFixer INSTANCE = new SSLConfigurationNameFixer();

        @Override
        public ModelNode fixModel(ModelNode modelNode) {
            //In the current and legacy models this is handled by a read attribute handler rather than existing in the model
            modelNode.get("connector","https", "configuration", "ssl", "name").set("ssl");
            return modelNode;
        }

    }

    private static class AccessLogPrefixFixer_1_1_x implements ModelFixer {

        private static final ModelFixer INSTANCE = new AccessLogPrefixFixer_1_1_x();

        @Override
        public ModelNode fixModel(ModelNode modelNode) {
            if (modelNode.hasDefined(VIRTUAL_SERVER)) {
                for (Property property : modelNode.get(VIRTUAL_SERVER).asPropertyList()) {
                    ModelNode virtualServer = property.getValue();
                    if (virtualServer.hasDefined(ACCESS_LOG)) {
                        ModelNode prefix = virtualServer.get(ACCESS_LOG, CONFIGURATION, PREFIX);
                        if (prefix.getType() == ModelType.BOOLEAN) {
                            modelNode.get(VIRTUAL_SERVER, property.getName(), ACCESS_LOG, CONFIGURATION, PREFIX).set("access_log.");
                        }
                    }
                }
            }
            return modelNode;
        }
    }


    private static class AccessLogPrefixFixer_1_2_0 implements ModelFixer {

        private static final ModelFixer INSTANCE = new AccessLogPrefixFixer_1_2_0();

        @Override
        public ModelNode fixModel(ModelNode modelNode) {
            if (modelNode.hasDefined(VIRTUAL_SERVER)) {
                for (Property property : modelNode.get(VIRTUAL_SERVER).asPropertyList()) {
                    ModelNode virtualServer = property.getValue();
                    if (virtualServer.hasDefined(CONFIGURATION)) {
                        if (virtualServer.get(CONFIGURATION).hasDefined(ACCESS_LOG)) {
                            ModelNode prefix = virtualServer.get(CONFIGURATION, ACCESS_LOG, PREFIX);
                            if (prefix.getType() == ModelType.BOOLEAN) {
                                modelNode.get(VIRTUAL_SERVER, property.getName(), CONFIGURATION, ACCESS_LOG, PREFIX).set("access_log.");
                            }
                        }
                    }
                }
            }
            return modelNode;
        }
    }

    private static class SetMissingRewriteConditionFlagsConfig extends AttributesPathAddressConfig<SetMissingRewriteConditionFlagsConfig>{
        public SetMissingRewriteConditionFlagsConfig(String... attributes) {
            // FIXME SetMissingRewriteConditionFlagsConfig constructor
            super(attributes);
        }

        @Override
        protected boolean isAttributeWritable(String attributeName) {
            return true;
        }

        @Override
        protected boolean checkValue(String attrName, ModelNode attribute, boolean isWriteAttribute) {
            return !attribute.isDefined();
        }

        @Override
        protected ModelNode correctValue(ModelNode toResolve, boolean isWriteAttribute) {
            return new ModelNode("NC");
        }
	}
}
