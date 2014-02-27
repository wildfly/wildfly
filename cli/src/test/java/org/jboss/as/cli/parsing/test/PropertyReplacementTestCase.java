/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * System property replacement tests.
 *
 * @author Alexey Loubyansky
 */
public class PropertyReplacementTestCase {

    private static final String NODE_TYPE_PROP_NAME = "test.node-type";
    private static final String NODE_TYPE_PROP_VALUE = "test-node-type";
    private static final String NODE_NAME_PROP_NAME = "test.node-name";
    private static final String NODE_NAME_PROP_VALUE = "test-node-name";
    private static final String OP_PROP_NAME = "test.op-name";
    private static final String OP_PROP_VALUE = "test-op";
    private static final String OP_PROP_PROP_NAME = "test.prop-name";
    private static final String OP_PROP_PROP_VALUE = "test-prop";


    @BeforeClass
    public static void setup() {
        WildFlySecurityManager.setPropertyPrivileged(NODE_TYPE_PROP_NAME, NODE_TYPE_PROP_VALUE);
        WildFlySecurityManager.setPropertyPrivileged(NODE_NAME_PROP_NAME, NODE_NAME_PROP_VALUE);
        WildFlySecurityManager.setPropertyPrivileged(OP_PROP_NAME, OP_PROP_VALUE);
        WildFlySecurityManager.setPropertyPrivileged(OP_PROP_PROP_NAME, OP_PROP_PROP_VALUE);
    }

    @AfterClass
    public static void cleanup() {
        WildFlySecurityManager.clearPropertyPrivileged(NODE_TYPE_PROP_NAME);
        WildFlySecurityManager.clearPropertyPrivileged(NODE_NAME_PROP_NAME);
        WildFlySecurityManager.clearPropertyPrivileged(OP_PROP_NAME);
        WildFlySecurityManager.clearPropertyPrivileged(OP_PROP_PROP_NAME);
    }

    @Test
    public void testUnresolvedOperationName() {
        assertFailedToParse(":${" + OP_PROP_NAME + "xxx}");
    }

    @Test
    public void testOperationName() throws Exception {
        final ParsedCommandLine parsed = parse(":${" + OP_PROP_NAME + "}");
        assertEquals(OP_PROP_VALUE, parsed.getOperationName());
    }

    @Test
    public void testUnresolvedNodeType() throws Exception {
        assertFailedToParse("/${" + NODE_TYPE_PROP_NAME + "xxx}=test:op");
    }

    @Test
    public void testNodeType() throws Exception {
        final ParsedCommandLine parsed = parse("/${" + NODE_TYPE_PROP_NAME + "}=test:op");
        final OperationRequestAddress address = parsed.getAddress();
        assertNotNull(address);
        assertEquals(NODE_TYPE_PROP_VALUE, address.getNodeType());
        assertEquals("test", address.getNodeName());
    }

    @Test
    public void testUnresolvedNodeName() throws Exception {
        assertFailedToParse("/test=${" + NODE_TYPE_PROP_NAME + "xxx}:op");
    }

    @Test
    public void testNodeName() throws Exception {
        final ParsedCommandLine parsed = parse("/test=${" + NODE_NAME_PROP_NAME + "}:op");
        final OperationRequestAddress address = parsed.getAddress();
        assertNotNull(address);
        assertEquals("test", address.getNodeType());
        assertEquals(NODE_NAME_PROP_VALUE, address.getNodeName());
    }

    @Test
    public void testUnresolvedOperationPropertyName() {
        assertFailedToParse(":write-attribute(${" + OP_PROP_PROP_NAME + "xxx}=value)");
    }

    @Test
    public void testOperationPropertyName() throws Exception {
        final ParsedCommandLine parsed = parse(":write-attribute(${" + OP_PROP_PROP_NAME + "}=test)");
        assertEquals("write-attribute", parsed.getOperationName());
        assertEquals(parsed.getPropertyValue(OP_PROP_PROP_VALUE), "test");
    }

    @Test
    public void testOperationNameAndValue() throws Exception {
        final ParsedCommandLine parsed = parse(":write-attribute(${" + OP_PROP_PROP_NAME + "}=${" + OP_PROP_PROP_NAME + "})");
        assertEquals("write-attribute", parsed.getOperationName());
        // variables unlike system properties are always resolved
        assertEquals("${" + OP_PROP_PROP_NAME + "}", parsed.getPropertyValue(OP_PROP_PROP_VALUE));
    }

    @Test
    public void testUnresolvedCommandName() {
        assertFailedToParse("${" + OP_PROP_NAME + "xxx}");
    }

    @Test
    public void testCommandName() throws Exception {
        final ParsedCommandLine parsed = parse("${" + OP_PROP_NAME + "}");
        assertEquals(OP_PROP_VALUE, parsed.getOperationName());
    }

    @Test
    public void testUnresolvedCommandArgumentName() {
        assertFailedToParse("command-name --${" + OP_PROP_PROP_NAME + "xxx}=value");
    }

    @Test
    public void testCommandArgumentName() throws Exception {
        final ParsedCommandLine parsed = parse("command-name --${" + OP_PROP_PROP_NAME + "}=test");
        assertEquals("command-name", parsed.getOperationName());
        assertEquals(parsed.getPropertyValue("--" + OP_PROP_PROP_VALUE), "test");
    }

    @Test
    public void testCommandArgumentNameAndValue() throws Exception {
        final ParsedCommandLine parsed = parse("command-name --${" + OP_PROP_PROP_NAME + "}=${" + OP_PROP_PROP_NAME + "}");
        assertEquals("command-name", parsed.getOperationName());
        // there is a different config option whether to resolve argument values
        assertEquals(parsed.getPropertyValue("--" + OP_PROP_PROP_VALUE), "${" + OP_PROP_PROP_NAME + "}");
    }

    @Test
    // there is a different config option whether to resolve argument values
    public void testUnresolvedCommandArgumentValue() throws Exception {
        final ParsedCommandLine parsed = parse("command-name ${" + OP_PROP_PROP_NAME + "}=test");
        assertEquals("command-name", parsed.getOperationName());
        assertFalse(parsed.hasProperty(OP_PROP_PROP_VALUE));
        assertFalse(parsed.hasProperty("--" + OP_PROP_PROP_VALUE));
        assertEquals(1, parsed.getOtherProperties().size());
        assertEquals("${" + OP_PROP_PROP_NAME + "}=test", parsed.getOtherProperties().get(0));
    }

    @Test
    public void testUnresolvedHeaderName() {
        assertFailedToParse(":write-attribute{${" + OP_PROP_PROP_NAME + "xxx}=value}");
    }

    @Test
    public void testHeaderNameAndValue() throws Exception {
        ParsedCommandLine parsed = parse(":write-attribute{${" + OP_PROP_PROP_NAME + "}=${" + OP_PROP_NAME + "}}");
        assertEquals("write-attribute", parsed.getOperationName());
        assertTrue(parsed.hasHeaders());
        assertTrue(parsed.hasHeader(OP_PROP_PROP_VALUE));
        ModelNode headers = new ModelNode();
        parsed.getLastHeader().addTo(null, headers);
        assertEquals(OP_PROP_VALUE, headers.get(OP_PROP_PROP_VALUE).asString());

        parsed = parse(":write-attribute{${" + OP_PROP_PROP_NAME + "} ${" + OP_PROP_NAME + "}}");
        assertEquals("write-attribute", parsed.getOperationName());
        assertTrue(parsed.hasHeaders());
        assertTrue(parsed.hasHeader(OP_PROP_PROP_VALUE));
        headers = new ModelNode();
        parsed.getLastHeader().addTo(null, headers);
        assertEquals(OP_PROP_VALUE, headers.get(OP_PROP_PROP_VALUE).asString());
    }

    @Test
    public void testSystemPropertiesInTheMix() throws Exception {
        final ParsedCommandLine parsed = parse("co${" + OP_PROP_NAME + "}${" + OP_PROP_NAME + "} "
                + "--${" + OP_PROP_PROP_NAME + "}_${" + OP_PROP_PROP_NAME + "}=${" + OP_PROP_PROP_NAME + "}");
        assertEquals("co" + OP_PROP_VALUE + OP_PROP_VALUE, parsed.getOperationName());
        assertTrue(parsed.getOtherProperties().isEmpty());
        assertEquals("${" + OP_PROP_PROP_NAME + "}", parsed.getPropertyValue("--" + OP_PROP_PROP_VALUE + "_" + OP_PROP_PROP_VALUE));
    }

    private void assertFailedToParse(String line) {
        try {
            parse(line);
            fail("should fail to resolve the property");
        } catch(CommandFormatException e) {
            // expected
        }
    }

    protected ParsedCommandLine parse(String line) throws CommandFormatException {
        DefaultCallbackHandler args = new DefaultCallbackHandler();
        args.parse(null, line, null);
        return args;
    }
}
