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


import org.jboss.as.cli.operation.CommandLineParser;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestParser;
import org.junit.Test;


/**
 *
 * @author Alexey Loubyansky
 */
public class OperationOutputRedirectionParsingTestCase extends BaseStateParserTest {

    private CommandLineParser parser = DefaultOperationRequestParser.INSTANCE;

    @Test
    public void testOperationNameOnly() throws Exception {
        DefaultCallbackHandler handler = new DefaultCallbackHandler();

        parseOperation(":  read-resource > cli.log", handler);

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
        assertEquals("cli.log", handler.getOutputTarget());
    }

    @Test
    public void testOperationWithProps() throws Exception {
        DefaultCallbackHandler handler = new DefaultCallbackHandler();

        parseOperation(":  read-resource (recursive=true) > cli.log", handler);

        assertFalse(handler.hasAddress());
        assertTrue(handler.hasOperationName());
        assertTrue(handler.hasProperties());
        assertFalse(handler.endsOnAddressOperationNameSeparator());
        assertFalse(handler.endsOnPropertyListStart());
        assertFalse(handler.endsOnPropertySeparator());
        assertFalse(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertFalse(handler.hasHeaders());
        assertFalse(handler.isRequestComplete());

        assertEquals("read-resource", handler.getOperationName());
        assertEquals("cli.log", handler.getOutputTarget());
    }

    @Test
    public void testOperationWithHeaders() throws Exception {
        DefaultCallbackHandler handler = new DefaultCallbackHandler();

        parseOperation(":  read-resource (recursive=true) {header_name=header_value} > cli.log", handler);

        assertFalse(handler.hasAddress());
        assertTrue(handler.hasOperationName());
        assertTrue(handler.hasProperties());
        assertFalse(handler.endsOnAddressOperationNameSeparator());
        assertFalse(handler.endsOnPropertyListStart());
        assertFalse(handler.endsOnPropertySeparator());
        assertFalse(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertTrue(handler.hasHeaders());
        assertTrue(handler.isRequestComplete());

        assertEquals("read-resource", handler.getOperationName());
        assertEquals("cli.log", handler.getOutputTarget());
    }

    protected void parseOperation(String operation, DefaultCallbackHandler handler)
            throws OperationFormatException {
        parser.parse(operation, handler);
        //ParsingUtil.parseOperation(operation, 0, handler);
    }
}
