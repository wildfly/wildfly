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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
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
        KernelServices services = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXmlResource("ws-subsystem12.xml")
                .build();
        ModelNode model = services.readWholeModel().get("subsystem", getMainSubsystemName());
        standardSubsystemTest("ws-subsystem12.xml", false);
        checkSubsystemBasics(model);
        checkEndpointConfigs(model);
        checkClientConfigs(model);
    }

    @Test
    public void testParseV20() throws Exception {
        // no need to do extra standardSubsystemTest("ws-subsystem20.xml") as that is default!
        KernelServices services = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXmlResource("ws-subsystem20.xml")
                .build();
        ModelNode model = services.readWholeModel().get("subsystem", getMainSubsystemName());
        checkSubsystemBasics(model);
        checkEndpointConfigs(model);
        checkClientConfigs(model);
        checkExtraCongfigs(model);

    }

    private void checkSubsystemBasics(ModelNode model) throws Exception {
        assertEquals(9090, Attributes.WSDL_PORT.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, model).asInt());
        assertEquals(9443, Attributes.WSDL_SECURE_PORT.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, model).asInt());
        assertEquals("localhost", Attributes.WSDL_HOST.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, model).asString());
        assertTrue(Attributes.MODIFY_WSDL_ADDRESS.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, model).asBoolean());
        assertFalse(Attributes.STATISTICS_ENABLED.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, model).asBoolean());
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

    private void checkExtraCongfigs(ModelNode model) throws Exception {
        assertEquals("https", Attributes.WSDL_URI_SCHEME.resolveModelAttribute(ExpressionResolver.TEST_RESOLVER, model).asString());
    }
}
