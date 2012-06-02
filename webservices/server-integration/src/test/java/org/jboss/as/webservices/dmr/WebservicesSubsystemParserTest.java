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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;

import junit.framework.TestCase;

import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLMapper;
import org.junit.Assert;
/**
 * Checks the current parser can parse any webservices subsystem version of the model
 * 
 * @author <a href="mailto:ema@rehdat.com>Jim Ma</a>
 * @author <a href="mailto:alessio.soldano@jboss.com>Alessio Soldano</a>
 */
public class WebservicesSubsystemParserTest extends TestCase {

    public void testParseV10() throws Exception {
        final List<ModelNode> operations = new ArrayList<ModelNode>();
        XMLMapper mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(new QName("urn:jboss:domain:webservices:1.0", "subsystem"), WSSubsystemLegacyReader.getInstance());

        URL configURL = getClass().getResource("ws-subsystem.xml");
        Assert.assertNotNull("url is null", configURL);
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(configURL.openStream()));
        mapper.parseDocument(operations, XMLInputFactory.newInstance().createXMLStreamReader(reader));
        
        checkSubsystemBasics(operations);
    }

    public void testParseV11() throws Exception {
        final List<ModelNode> operations = new ArrayList<ModelNode>();
        XMLMapper mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(new QName("urn:jboss:domain:webservices:1.1", "subsystem"), WSSubsystemReader.getInstance());

        URL configURL = getClass().getResource("ws-subsystem11.xml");
        Assert.assertNotNull("url is null", configURL);
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(configURL.openStream()));
        mapper.parseDocument(operations, XMLInputFactory.newInstance().createXMLStreamReader(reader));
        
        checkSubsystemBasics(operations);
        checkEndpointConfigs(operations);
        
    }

    public void testParseV12() throws Exception {
        final List<ModelNode> operations = new ArrayList<ModelNode>();
        XMLMapper mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(new QName("urn:jboss:domain:webservices:1.2", "subsystem"), WSSubsystemReader.getInstance());

        URL configURL = getClass().getResource("ws-subsystem12.xml");
        Assert.assertNotNull("url is null", configURL);

        BufferedReader reader = new BufferedReader(new InputStreamReader(configURL.openStream()));
        mapper.parseDocument(operations, XMLInputFactory.newInstance().createXMLStreamReader(reader));
        
        checkSubsystemBasics(operations);
        checkEndpointConfigs(operations);
        checkClientConfigs(operations);
    }

    private void checkSubsystemBasics(List<ModelNode> operations) throws Exception {
        assertEquals(9090, operations.get(0).get(Constants.WSDL_PORT).asInt());
        assertEquals(9443, operations.get(0).get(Constants.WSDL_SECURE_PORT).asInt());
        assertEquals("localhost", operations.get(0).get(Constants.WSDL_HOST).asString());
        assertTrue(operations.get(0).get(Constants.MODIFY_WSDL_ADDRESS).asBoolean());
    }

    private void checkEndpointConfigs(List<ModelNode> operations) throws Exception {
        assertEquals("Standard-Endpoint-Config", operations.get(1).get("address").get(1).get(Constants.ENDPOINT_CONFIG).asString());
        assertEquals("Recording-Endpoint-Config", operations.get(2).get("address").get(1).get(Constants.ENDPOINT_CONFIG).asString());
        assertEquals("Recording-Endpoint-Config", operations.get(3).get("address").get(1).get(Constants.ENDPOINT_CONFIG).asString());
        assertEquals("recording-handlers", operations.get(3).get("address").get(2).get(Constants.PRE_HANDLER_CHAIN).asString());
        assertEquals("recording-handlers", operations.get(4).get("address").get(2).get(Constants.PRE_HANDLER_CHAIN).asString());
        assertEquals("org.jboss.ws.common.invocation.RecordingServerHandler", operations.get(4).get(Constants.CLASS).asString());
    }

    private void checkClientConfigs(List<ModelNode> operations) throws Exception {
        assertEquals("My-Client-Config", operations.get(8).get("address").get(1).get(Constants.CLIENT_CONFIG).asString());
        assertEquals("My-Client-Config", operations.get(9).get("address").get(1).get(Constants.CLIENT_CONFIG).asString());
        assertEquals("My-Client-Config", operations.get(10).get("address").get(1).get(Constants.CLIENT_CONFIG).asString());
        assertEquals("my-handlers", operations.get(10).get("address").get(2).get(Constants.PRE_HANDLER_CHAIN).asString());
        assertEquals("org.jboss.ws.common.invocation.MyHandler", operations.get(10).get(Constants.CLASS).asString());
        assertEquals("my-handlers2", operations.get(11).get("address").get(2).get(Constants.POST_HANDLER_CHAIN).asString());
    }
}
