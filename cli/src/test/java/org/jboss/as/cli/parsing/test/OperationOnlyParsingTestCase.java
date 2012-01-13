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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.jboss.as.cli.operation.CommandLineParser;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestParser;
import org.junit.Test;


/**
 *
 * @author Alexey Loubyansky
 */
public class OperationOnlyParsingTestCase extends BaseStateParserTest {

    private CommandLineParser parser = DefaultOperationRequestParser.INSTANCE;

    @Test
    public void testOperationNameOnly() throws Exception {
        DefaultCallbackHandler handler = new DefaultCallbackHandler();

        parseOperation(":read-resource", handler);

        assertFalse(handler.hasAddress());
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
    }

    @Test
    public void testArgListStart() throws Exception {
        DefaultCallbackHandler handler = new DefaultCallbackHandler();

        parseOperation(":read-resource(", handler);

        assertFalse(handler.hasAddress());
        assertTrue(handler.hasOperationName());
        assertFalse(handler.hasProperties());
        assertFalse(handler.endsOnAddressOperationNameSeparator());
        assertTrue(handler.endsOnPropertyListStart());
        assertFalse(handler.endsOnPropertySeparator());
        assertFalse(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertFalse(handler.isRequestComplete());

        assertEquals("read-resource", handler.getOperationName());
        assertTrue(handler.getPropertyNames().isEmpty());
    }

    @Test
    public void testEmptyArgList() throws Exception {
        DefaultCallbackHandler handler = new DefaultCallbackHandler();

        parseOperation(":read-resource()", handler);

        assertFalse(handler.hasAddress());
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
    }

    @Test
    public void testArgNameOnly() throws Exception {
        DefaultCallbackHandler handler = new DefaultCallbackHandler();

        parseOperation(":read-resource(recursive", handler);

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

        assertEquals("read-resource", handler.getOperationName());

        Set<String> argNames = handler.getPropertyNames();
        assertEquals(1, argNames.size());
        assertTrue(argNames.contains("recursive"));
    }

    @Test
    public void testNameAndValueSeparator() throws Exception {
        DefaultCallbackHandler handler = new DefaultCallbackHandler();

        parseOperation(":read-resource(recursive=", handler);

        assertFalse(handler.hasAddress());
        assertTrue(handler.hasOperationName());
        assertTrue(handler.hasProperties());
        assertFalse(handler.endsOnAddressOperationNameSeparator());
        assertFalse(handler.endsOnPropertyListStart());
        assertFalse(handler.endsOnPropertySeparator());
        assertTrue(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertFalse(handler.isRequestComplete());

        assertEquals("read-resource", handler.getOperationName());

        Set<String> argNames = handler.getPropertyNames();
        assertEquals(1, argNames.size());
        assertTrue(argNames.contains("recursive"));
    }

    @Test
    public void testNameValue() throws Exception {
        DefaultCallbackHandler handler = new DefaultCallbackHandler();

        parseOperation(":read-resource(recursive=true", handler);

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

        assertEquals("read-resource", handler.getOperationName());

        Set<String> argNames = handler.getPropertyNames();
        assertEquals(1, argNames.size());
        assertTrue(argNames.contains("recursive"));
        assertEquals("true", handler.getPropertyValue("recursive"));
    }

    @Test
    public void testNameValueAndSeparator() throws Exception {
        DefaultCallbackHandler handler = new DefaultCallbackHandler();

        parseOperation(":read-resource(recursive=true,", handler);

        assertFalse(handler.hasAddress());
        assertTrue(handler.hasOperationName());
        assertTrue(handler.hasProperties());
        assertFalse(handler.endsOnAddressOperationNameSeparator());
        assertFalse(handler.endsOnPropertyListStart());
        assertTrue(handler.endsOnPropertySeparator());
        assertFalse(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertFalse(handler.isRequestComplete());

        assertEquals("read-resource", handler.getOperationName());

        Set<String> argNames = handler.getPropertyNames();
        assertEquals(1, argNames.size());
        assertTrue(argNames.contains("recursive"));
        assertEquals("true", handler.getPropertyValue("recursive"));
    }

    @Test
    public void testNameValueSeparatorAndName() throws Exception {
        DefaultCallbackHandler handler = new DefaultCallbackHandler();

        parseOperation(":read-resource(recursive=true,other", handler);

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

        assertEquals("read-resource", handler.getOperationName());

        Set<String> argNames = handler.getPropertyNames();
        assertEquals(2, argNames.size());
        assertTrue(argNames.contains("recursive"));
        assertEquals("true", handler.getPropertyValue("recursive"));
        assertTrue(argNames.contains("other"));
    }

    @Test
    public void testNameValueSeparatorNameAndValueSeparator() throws Exception {
        DefaultCallbackHandler handler = new DefaultCallbackHandler();

        parseOperation(":read-resource(recursive=true,other=", handler);

        assertFalse(handler.hasAddress());
        assertTrue(handler.hasOperationName());
        assertTrue(handler.hasProperties());
        assertFalse(handler.endsOnAddressOperationNameSeparator());
        assertFalse(handler.endsOnPropertyListStart());
        assertFalse(handler.endsOnPropertySeparator());
        assertTrue(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertFalse(handler.isRequestComplete());

        assertEquals("read-resource", handler.getOperationName());

        Set<String> argNames = handler.getPropertyNames();
        assertEquals(2, argNames.size());
        assertTrue(argNames.contains("recursive"));
        assertEquals("true", handler.getPropertyValue("recursive"));
        assertTrue(argNames.contains("other"));
    }

    @Test
    public void testComplete() throws Exception {
        DefaultCallbackHandler handler = new DefaultCallbackHandler();

        parseOperation(":read-resource(recursive=true,other=done)", handler);

        assertFalse(handler.hasAddress());
        assertTrue(handler.hasOperationName());
        assertTrue(handler.hasProperties());
        assertFalse(handler.endsOnAddressOperationNameSeparator());
        assertFalse(handler.endsOnPropertyListStart());
        assertFalse(handler.endsOnPropertySeparator());
        assertFalse(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertFalse(handler.isRequestComplete()); // there are headers

        assertEquals("read-resource", handler.getOperationName());

        Set<String> argNames = handler.getPropertyNames();
        assertEquals(2, argNames.size());
        assertTrue(argNames.contains("recursive"));
        assertEquals("true", handler.getPropertyValue("recursive"));
        assertTrue(argNames.contains("other"));
        assertEquals("done", handler.getPropertyValue("other"));
    }

    protected void parseOperation(String operation, DefaultCallbackHandler handler)
            throws OperationFormatException {
        parser.parse(operation, handler);
        //ParsingUtil.parseOperation(operation, 0, handler);
    }
}
