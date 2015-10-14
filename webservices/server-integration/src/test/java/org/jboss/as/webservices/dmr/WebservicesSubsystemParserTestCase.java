/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.dmr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.junit.Assert;
import org.junit.Test;

/**
 * Checks the current parser can parse any webservices subsystem version of the model
 *
 * @author <a href="mailto:ema@rehdat.com>Jim Ma</a>
 * @author <a href="mailto:alessio.soldano@jboss.com>Alessio Soldano</a>
 */
public class WebservicesSubsystemParserTestCase extends AbstractSubsystemBaseTest {

    public WebservicesSubsystemParserTestCase() {
        super(WSExtension.SUBSYSTEM_NAME, new WSExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("ws-subsystem20.xml"); //for default test
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/jboss-as-webservices_2_0.xsd";
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return new String[] {
                "/subsystem-templates/webservices.xml"
        };
    }

    protected AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization() {
            @Override
            protected RunningMode getRunningMode() {
                return RunningMode.ADMIN_ONLY;
            }
        };
    }

    @Override
    protected String getSubsystemXml(String configId) throws IOException {
        return readResource(configId);
    }

    @Test
    public void testParseV10() throws Exception {
        KernelServices services = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXmlResource("ws-subsystem.xml")
                .build();
        ModelNode model = services.readWholeModel().get("subsystem", getMainSubsystemName());
        standardSubsystemTest("ws-subsystem.xml", false);
        checkSubsystemBasics(model);
    }

    @Test
    public void testParseV11() throws Exception {
        KernelServices services = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXmlResource("ws-subsystem11.xml")
                .build();
        ModelNode model = services.readWholeModel().get("subsystem", getMainSubsystemName());
        standardSubsystemTest("ws-subsystem11.xml", false);
        checkSubsystemBasics(model);
        checkEndpointConfigs(model);
    }

    @Test
    public void testParseV12() throws Exception {
        //no need to do extra standardSubsystemTest("ws-subsystem12.xml") as that is default!
        KernelServices services = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXmlResource("ws-subsystem12.xml")
                .build();
        ModelNode model = services.readWholeModel().get("subsystem", getMainSubsystemName());
        checkSubsystemBasics(model);
        checkEndpointConfigs(model);
        checkClientConfigs(model);
    }

    private void checkSubsystemBasics(ModelNode model) throws Exception {
        assertEquals(9090, Attributes.WSDL_PORT.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, model).asInt());
        assertEquals(9443, Attributes.WSDL_SECURE_PORT.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, model).asInt());
        assertEquals("localhost", Attributes.WSDL_HOST.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, model).asString());
        assertTrue(Attributes.MODIFY_WSDL_ADDRESS.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, model).asBoolean());
    }


    private void checkEndpointConfigs(ModelNode model) throws Exception {
        List<Property> endpoints = model.get(Constants.ENDPOINT_CONFIG).asPropertyList();
        assertEquals("Standard-Endpoint-Config", endpoints.get(0).getName());
        assertEquals("Recording-Endpoint-Config", endpoints.get(1).getName());
        ModelNode recordingEndpoint = endpoints.get(1).getValue();
        assertEquals("bar", Attributes.VALUE.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, recordingEndpoint.get(Constants.PROPERTY).get("foo")).asString());
        List<Property> chain = recordingEndpoint.get(Constants.PRE_HANDLER_CHAIN).asPropertyList();
        assertEquals("recording-handlers", chain.get(0).getName());
        ModelNode recordingHandler = chain.get(0).getValue();
        assertEquals("##SOAP11_HTTP ##SOAP11_HTTP_MTOM ##SOAP12_HTTP ##SOAP12_HTTP_MTOM", Attributes.PROTOCOL_BINDINGS.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, recordingHandler).asString());
        assertEquals("org.jboss.ws.common.invocation.RecordingServerHandler", recordingHandler.get(Constants.HANDLER, "RecordingHandler", Constants.CLASS).asString());
    }

    private void checkClientConfigs(ModelNode model) throws Exception {
        List<Property> clientConfigs = model.get(Constants.CLIENT_CONFIG).asPropertyList();
        assertEquals("My-Client-Config", clientConfigs.get(0).getName());
        List<Property> preHandlers = clientConfigs.get(0).getValue().get(Constants.PRE_HANDLER_CHAIN).asPropertyList();
        List<Property> postHandlers = clientConfigs.get(0).getValue().get(Constants.POST_HANDLER_CHAIN).asPropertyList();
        assertEquals("my-handlers", preHandlers.get(0).getName());
        assertEquals("org.jboss.ws.common.invocation.MyHandler", preHandlers.get(1).getValue().get(Constants.HANDLER).asPropertyList().get(0).getValue().get(Constants.CLASS).asString());
        assertEquals("my-handlers2", postHandlers.get(0).getName());
    }

    private FailedOperationTransformationConfig getConfig() {
        PathAddress subsystemAddress = PathAddress.pathAddress(WSExtension.SUBSYSTEM_PATH);
        PathAddress endpoint = subsystemAddress.append(WSExtension.ENDPOINT_CONFIG_PATH);
        return new FailedOperationTransformationConfig()
                .addFailedAttribute(subsystemAddress, new FailedOperationTransformationConfig.RejectExpressionsConfig(Attributes.SUBSYSTEM_ATTRIBUTES))
                .addFailedAttribute(endpoint.append(WSExtension.PRE_HANDLER_CHAIN_PATH),new FailedOperationTransformationConfig.RejectExpressionsConfig(Attributes.PROTOCOL_BINDINGS))
                .addFailedAttribute(endpoint.append(WSExtension.POST_HANDLER_CHAIN_PATH),new FailedOperationTransformationConfig.RejectExpressionsConfig(Attributes.PROTOCOL_BINDINGS))
                .addFailedAttribute(subsystemAddress.append(WSExtension.CLIENT_CONFIG_PATH),FailedOperationTransformationConfig.REJECTED_RESOURCE)
                .addFailedAttribute(subsystemAddress.append(WSExtension.CLIENT_CONFIG_PATH).append(WSExtension.PRE_HANDLER_CHAIN_PATH),FailedOperationTransformationConfig.REJECTED_RESOURCE)
                .addFailedAttribute(subsystemAddress.append(WSExtension.CLIENT_CONFIG_PATH).append(WSExtension.PRE_HANDLER_CHAIN_PATH).append(WSExtension.HANDLER_PATH),FailedOperationTransformationConfig.REJECTED_RESOURCE)
                .addFailedAttribute(subsystemAddress.append(WSExtension.CLIENT_CONFIG_PATH).append(WSExtension.POST_HANDLER_CHAIN_PATH),FailedOperationTransformationConfig.REJECTED_RESOURCE)
                .addFailedAttribute(subsystemAddress.append(WSExtension.CLIENT_CONFIG_PATH).append(WSExtension.POST_HANDLER_CHAIN_PATH).append(WSExtension.HANDLER_PATH),FailedOperationTransformationConfig.REJECTED_RESOURCE);
    }


   private void testRejectExpressions_1_1_0(ModelTestControllerVersion controllerVersion) throws Exception {
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());

        // create builder for legacy subsystem version
        ModelVersion version_1_1_0 = ModelVersion.create(1, 1, 0);
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, version_1_1_0)
                .addMavenResourceURL("org.jboss.as:jboss-as-webservices-server-integration:" + controllerVersion.getMavenGavVersion());

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(version_1_1_0);

        Assert.assertNotNull(legacyServices);
        Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        List<ModelNode> xmlOps = builder.parseXmlResource("ws-subsystem12.xml");

        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, version_1_1_0, xmlOps, getConfig());
    }

    @Test
    public void testTransformersAS712() throws Exception {
        testRejectExpressions_1_1_0(ModelTestControllerVersion.V7_1_2_FINAL);
    }

    @Test
    public void testTransformersAS713() throws Exception {
        testRejectExpressions_1_1_0(ModelTestControllerVersion.V7_1_3_FINAL);
    }

    @Test
    public void testTransformersEAP600() throws Exception {
        testRejectExpressions_1_1_0(ModelTestControllerVersion.EAP_6_0_0);
    }

    @Test
    public void testTransformersEAP601() throws Exception {
        testRejectExpressions_1_1_0(ModelTestControllerVersion.EAP_6_0_1);
    }

    @Test
    public void testTransformersAS720() throws Exception {
        testTransformers_1_2_0(ModelTestControllerVersion.V7_2_0_FINAL);
    }

    @Test
    public void testTransformersEAP610() throws Exception {
        testTransformers_1_2_0(ModelTestControllerVersion.EAP_6_1_0);
    }
    
    @Test
    public void testTransformersEAP611() throws Exception {
        testTransformers_1_2_0(ModelTestControllerVersion.EAP_6_1_1);
    }

    private void testTransformers_1_2_0(ModelTestControllerVersion controllerVersion) throws Exception {
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXmlResource("ws-subsystem12.xml");

        // create builder for legacy subsystem version
        ModelVersion version_1_2_0 = ModelVersion.create(1, 2, 0);
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, version_1_2_0)
                .addMavenResourceURL("org.jboss.as:jboss-as-webservices-server-integration:" + controllerVersion.getMavenGavVersion())
                .configureReverseControllerCheck(AdditionalInitialization.MANAGEMENT, null);

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(version_1_2_0);

        Assert.assertNotNull(legacyServices);
        Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, version_1_2_0);
    }

}
