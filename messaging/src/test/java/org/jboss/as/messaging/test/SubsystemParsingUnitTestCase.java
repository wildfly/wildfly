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

package org.jboss.as.messaging.test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import junit.framework.TestCase;

import org.jboss.as.messaging.MessagingSubsystemParser;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLMapper;
import org.junit.Assert;

/**
 * @author Emanuel Muckenhuber
 */
public class SubsystemParsingUnitTestCase extends TestCase {

    private static final String namespace = "urn:jboss:domain:messaging:1.0";
    private static final MessagingSubsystemParser parser = MessagingSubsystemParser.getInstance();

    public void test() throws Exception {

        List<ModelNode> operations = parse("subsystem.xml");
        Assert.assertEquals(6, operations.size());
        final ModelNode operation = operations.get(0);
        System.err.println(operation);

        operation.get(OP_ADDR).asPropertyList();

        final ModelNode node = new ModelNode();
        node.add("test", "test");

        node.asPropertyList();

        final ModelNode op = new ModelNode();
        op.get("address").set(node);
        op.get("address").asPropertyList();
        final ModelNode op2 = new ModelNode();
        op2.get("address").set(node).add("test", "test");
        op2.get("address").asPropertyList();
    }


    List<ModelNode> parse(final String name) throws XMLStreamException, IOException {
        final List<ModelNode> operations = new ArrayList<ModelNode>();

        XMLMapper mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(new QName(namespace, "subsystem"), parser);

        URL configURL = getClass().getResource(name);
        Assert.assertNotNull(name  + " url is not null", configURL);
        System.out.println("configURL = " + configURL);

        BufferedReader reader = new BufferedReader(new InputStreamReader(configURL.openStream()));
        mapper.parseDocument(operations, XMLInputFactory.newInstance().createXMLStreamReader(reader));

        return operations;
    }

}
