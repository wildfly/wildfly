/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.cli.parsing.test;

import java.util.Iterator;
import java.util.Set;

import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.operation.CommandLineParser;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.OperationRequestAddress.Node;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestParser;
import org.junit.Test;

import junit.framework.TestCase;

/**
 *
 * @author Alexey Loubyansky
 */
public class OperationParsingTestCase extends TestCase {

    private CommandLineParser parser = DefaultOperationRequestParser.INSTANCE;

    @Test
    public void testOperationNameEndsWithDash() throws Exception {
        DefaultCallbackHandler handler = new DefaultCallbackHandler(false);

        parse("/subsystem=threads/thread-factory=*:validate-", handler);

        assertTrue(handler.hasAddress());
        assertTrue(handler.hasOperationName());
        assertFalse(handler.hasProperties());
        assertFalse(handler.endsOnAddressOperationNameSeparator());
        assertFalse(handler.endsOnPropertyListStart());
        assertFalse(handler.endsOnPropertySeparator());
        assertFalse(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertFalse(handler.isRequestComplete());

        assertEquals("validate-", handler.getOperationName());

        OperationRequestAddress address = handler.getAddress();
        Iterator<Node> i = address.iterator();
        assertTrue(i.hasNext());
/*        Node node = i.next();
        assertEquals("subsystem", node.getType());
        assertEquals("logging", node.getName());
        assertFalse(i.hasNext());
*/    }

    @Test
    public void testOperationNameOnly() throws Exception {
        DefaultCallbackHandler handler = new DefaultCallbackHandler();

        parse("/subsystem=logging:read-resource", handler);

        assertTrue(handler.hasAddress());
        assertTrue(handler.hasOperationName());
        assertFalse(handler.hasProperties());
        assertFalse(handler.endsOnAddressOperationNameSeparator());
        assertFalse(handler.endsOnPropertyListStart());
        assertFalse(handler.endsOnPropertySeparator());
        assertFalse(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertFalse(handler.isRequestComplete());

        assertEquals("read-resource", handler.getOperationName());

        OperationRequestAddress address = handler.getAddress();
        Iterator<Node> i = address.iterator();
        assertTrue(i.hasNext());
        Node node = i.next();
        assertEquals("subsystem", node.getType());
        assertEquals("logging", node.getName());
        assertFalse(i.hasNext());
    }

    @Test
    public void testOperationNameWithPrefix() throws Exception {

        OperationRequestAddress prefix = new DefaultOperationRequestAddress();
        prefix.toNodeType("subsystem");
        DefaultCallbackHandler handler = new DefaultCallbackHandler(prefix);

        parse("./logging:read-resource", handler);

        assertTrue(handler.hasAddress());
        assertTrue(handler.hasOperationName());
        assertFalse(handler.hasProperties());
        assertFalse(handler.endsOnAddressOperationNameSeparator());
        assertFalse(handler.endsOnPropertyListStart());
        assertFalse(handler.endsOnPropertySeparator());
        assertFalse(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertFalse(handler.isRequestComplete());

        assertEquals("read-resource", handler.getOperationName());

        OperationRequestAddress address = handler.getAddress();
        Iterator<Node> i = address.iterator();
        assertTrue(i.hasNext());
        Node node = i.next();
        assertEquals("subsystem", node.getType());
        assertEquals("logging", node.getName());
        assertFalse(i.hasNext());
    }

    @Test
    public void testNoOperation() throws Exception {
        DefaultCallbackHandler handler = new DefaultCallbackHandler();

        try {
            parse("./subsystem=logging:", handler);
        } catch(OperationFormatException e) {
        }

        assertTrue(handler.hasAddress());
        assertFalse(handler.hasOperationName());
        assertFalse(handler.hasProperties());
        assertTrue(handler.endsOnAddressOperationNameSeparator());
        assertFalse(handler.endsOnPropertyListStart());
        assertFalse(handler.endsOnPropertySeparator());
        assertFalse(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertFalse(handler.isRequestComplete());

        OperationRequestAddress address = handler.getAddress();
        Iterator<Node> i = address.iterator();
        assertTrue(i.hasNext());
        Node node = i.next();
        assertEquals("subsystem", node.getType());
        assertEquals("logging", node.getName());
        assertFalse(i.hasNext());
    }

    @Test
    public void testOperationWithArguments() throws Exception {
        DefaultCallbackHandler handler = new DefaultCallbackHandler();

        parse("/subsystem=logging:read-resource(recursive=true)", handler);

        assertTrue(handler.hasAddress());
        assertTrue(handler.hasOperationName());
        assertTrue(handler.hasProperties());
        assertFalse(handler.endsOnAddressOperationNameSeparator());
        assertFalse(handler.endsOnPropertyListStart());
        assertFalse(handler.endsOnPropertySeparator());
        assertFalse(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertFalse(handler.isRequestComplete());

        assertEquals("read-resource", handler.getOperationName());

        OperationRequestAddress address = handler.getAddress();
        Iterator<Node> i = address.iterator();
        assertTrue(i.hasNext());
        Node node = i.next();
        assertEquals("subsystem", node.getType());
        assertEquals("logging", node.getName());
        assertFalse(i.hasNext());

        Set<String> args = handler.getPropertyNames();
        assertEquals(1, args.size());
        assertTrue(args.contains("recursive"));
        assertEquals("true", handler.getPropertyValue("recursive"));
    }

    @Test
    public void testComposite() throws Exception {

        final String op = "composite";
        final String propName = "steps";
        final String propValue = "[{\"operation\"=>\"add-system-property\",\"name\"=>\"test\",\"value\"=\"newValue\"},{\"operation\"=>\"add-system-property\",\"name\"=>\"test2\",\"value\"=>\"test2\"}]";

        DefaultCallbackHandler handler = new DefaultCallbackHandler();

        parse(':' + op + '(' + propName + '=' + propValue + ')', handler);

        assertFalse(handler.hasAddress());
        assertTrue(handler.hasOperationName());
        assertTrue(handler.hasProperties());
        assertFalse(handler.endsOnAddressOperationNameSeparator());
        assertFalse(handler.endsOnPropertyListStart());
        assertFalse(handler.endsOnPropertySeparator());
        assertFalse(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertFalse(handler.isRequestComplete());

        assertEquals("composite", handler.getOperationName());

        Set<String> args = handler.getPropertyNames();
        assertEquals(1, args.size());
        assertTrue(args.contains(propName));
        assertEquals(propValue, handler.getPropertyValue(propName));
    }

    @Test
    public void testOperationWithArgumentsAndWhitespaces() throws Exception {
        DefaultCallbackHandler handler = new DefaultCallbackHandler();

        parse("   / subsystem  =  logging  :  read-resource  ( recursive = true , another = \"   \" )   ", handler);

        assertTrue(handler.hasAddress());
        assertTrue(handler.hasOperationName());
        assertTrue(handler.hasProperties());
        assertFalse(handler.endsOnAddressOperationNameSeparator());
        assertFalse(handler.endsOnPropertyListStart());
        assertFalse(handler.endsOnPropertySeparator());
        assertFalse(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertFalse(handler.isRequestComplete());

        assertEquals("read-resource", handler.getOperationName());

        OperationRequestAddress address = handler.getAddress();
        Iterator<Node> i = address.iterator();
        assertTrue(i.hasNext());
        Node node = i.next();
        assertEquals("subsystem", node.getType());
        assertEquals("logging", node.getName());
        assertFalse(i.hasNext());

        Set<String> args = handler.getPropertyNames();
        assertEquals(2, args.size());
        assertTrue(args.contains("recursive"));
        assertEquals("true", handler.getPropertyValue("recursive"));
        assertTrue(args.contains("another"));
        assertEquals("\"   \"", handler.getPropertyValue("another"));
    }

    protected void parse(String opReq, DefaultCallbackHandler handler) throws CommandFormatException {
        parser.parse(opReq, handler);
    }
}
