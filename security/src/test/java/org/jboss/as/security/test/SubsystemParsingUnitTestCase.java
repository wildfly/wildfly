/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.security.test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.security.SecuritySubsystemParser;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLMapper;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author anil saldhana
 */
public class SubsystemParsingUnitTestCase {
    private static final String namespace = "urn:jboss:domain:security:1.0";
    private static final SecuritySubsystemParser parser = SecuritySubsystemParser.getInstance();

    @Test
    public void test() throws Exception {

        List<ModelNode> operations = parse("subsystem.xml");
        assertNotNull(operations);
        ModelNode props = operations.get(0);
        assertNotNull(props);


        ModelNode node = operations.get(1);
        assertNotNull(node);
        ModelNode address = node.get(OP_ADDR);
        List<Property> properties = address.asPropertyList();
        assertEquals(2, properties.size());
        for (Property prop : properties) {
            String propName = prop.getName();
            if (!(propName.equals(SUBSYSTEM) || propName.equals(SECURITY_DOMAIN)))
                fail("either subsystem or security-domain expected");
            if (propName.equals(SECURITY_DOMAIN)) {
                ModelNode securityDomainValue = prop.getValue();
                String value = securityDomainValue.asString();
                assertEquals("other", value);
            }
        }

        node = operations.get(2);
        assertNotNull(node);
        address = node.get(OP_ADDR);
        properties = address.asPropertyList();
        assertEquals(3, properties.size());
        assertEquals("authentication", properties.get(2).getName());
        assertEquals("classic", properties.get(2).getValue().asString());

        List<ModelNode> loginNodes = node.get("login-modules").asList();
        assertEquals(1, loginNodes.size());

        ModelNode modelNode = loginNodes.get(0);
        ModelNode code = modelNode.get("code");
        assertEquals("UsersRoles", code.asString());
        ModelNode flag = modelNode.get("flag");
        assertEquals("required", flag.asString());
    }

    List<ModelNode> parse(final String name) throws XMLStreamException, IOException {
        final List<ModelNode> operations = new ArrayList<ModelNode>();

        XMLMapper mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(new QName(namespace, "subsystem"), parser);

        URL configURL = getClass().getResource(name);
        Assert.assertNotNull(name + " url is not null", configURL);
        System.out.println("configURL = " + configURL);

        BufferedReader reader = new BufferedReader(new InputStreamReader(configURL.openStream()));
        mapper.parseDocument(operations, XMLInputFactory.newInstance().createXMLStreamReader(reader));

        return operations;
    }

}