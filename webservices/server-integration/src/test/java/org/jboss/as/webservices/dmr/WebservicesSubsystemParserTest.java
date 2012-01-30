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
 * @author <a href="mailto:ema@rehdat.com>Jim Ma</a>
 */
public class WebservicesSubsystemParserTest extends TestCase {

    private static final String namespace = "urn:jboss:domain:webservices:1.0";
    private static final WSSubsystemReader parser = WSSubsystemReader.getInstance();

    public void testParse() throws Exception {               
        final List<ModelNode> operations = new ArrayList<ModelNode>();
        XMLMapper mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(new QName(namespace, "subsystem"), parser);

        URL configURL = getClass().getResource("ws-subsystem.xml");
        Assert.assertNotNull("url is not null", configURL);

        BufferedReader reader = new BufferedReader(new InputStreamReader(configURL.openStream()));
        mapper.parseDocument(operations, XMLInputFactory.newInstance().createXMLStreamReader(reader));
        assertEquals(9090, operations.get(0).get(Constants.WSDL_PORT).asInt());
        assertEquals(9443, operations.get(0).get(Constants.WSDL_SECURE_PORT).asInt());
        assertEquals("localhost", operations.get(0).get(Constants.WSDL_HOST).asString());
        assertTrue(operations.get(0).get(Constants.MODIFY_WSDL_ADDRESS).asBoolean());
    }
}
