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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class CommandTestCase {

    @Test
    public void testCommandOnly() throws Exception {

        DefaultCallbackHandler cmd = parse("some-command");
        assertEquals("some-command", cmd.getOperationName());
        assertFalse(cmd.hasProperties());
        assertNull(cmd.getOutputTarget());
        assertFalse(cmd.endsOnSeparator());
        assertFalse(cmd.endsOnPropertyListStart());
        assertFalse(cmd.endsOnPropertySeparator());
        assertEquals(0, cmd.getLastChunkIndex());
    }

    @Test
    public void testPropertyListStart() throws Exception {

        DefaultCallbackHandler cmd = parse("some-command ");
        assertEquals("some-command", cmd.getOperationName());
        assertFalse(cmd.hasProperties());
        assertNull(cmd.getOutputTarget());
        assertTrue(cmd.endsOnSeparator());
        assertTrue(cmd.endsOnPropertyListStart());
        assertFalse(cmd.endsOnPropertySeparator());
        assertFalse(cmd.endsOnPropertyValueSeparator());
        assertEquals(0, cmd.getLastChunkIndex());
    }

    @Test
    public void testSingleArgValue() throws Exception {

        DefaultCallbackHandler cmd = parse("some-command arg-value");
        assertEquals("some-command", cmd.getOperationName());
        assertTrue(cmd.hasProperties());
        assertNull(cmd.getOutputTarget());
        assertFalse(cmd.endsOnSeparator());
        assertFalse(cmd.endsOnPropertyListStart());
        assertFalse(cmd.endsOnPropertySeparator());
        assertFalse(cmd.endsOnPropertyValueSeparator());
        assertTrue(cmd.getPropertyNames().isEmpty());
        assertEquals(1, cmd.getOtherProperties().size());
        assertEquals("arg-value", cmd.getOtherProperties().get(0));
        assertEquals(13, cmd.getLastChunkIndex());
    }

    @Test
    public void testSingleArgValueAndSeparator() throws Exception {

        DefaultCallbackHandler cmd = parse("some-command arg-value ");
        assertEquals("some-command", cmd.getOperationName());
        assertTrue(cmd.hasProperties());
        assertNull(cmd.getOutputTarget());
        assertTrue(cmd.endsOnSeparator());
        assertFalse(cmd.endsOnPropertyListStart());
        assertTrue(cmd.endsOnPropertySeparator());
        assertFalse(cmd.endsOnPropertyValueSeparator());
        assertTrue(cmd.getPropertyNames().isEmpty());
        assertEquals(1, cmd.getOtherProperties().size());
        assertEquals("arg-value", cmd.getOtherProperties().get(0));
        assertEquals(13, cmd.getLastChunkIndex());
    }

    @Test
    public void testSingleArgName() throws Exception {

        DefaultCallbackHandler cmd = parse("some-command --arg-name");
        assertEquals("some-command", cmd.getOperationName());
        assertTrue(cmd.hasProperties());
        assertNull(cmd.getOutputTarget());
        assertFalse(cmd.endsOnSeparator());
        assertFalse(cmd.endsOnPropertyListStart());
        assertFalse(cmd.endsOnPropertySeparator());
        assertFalse(cmd.endsOnPropertyValueSeparator());
        assertEquals(1, cmd.getPropertyNames().size());
        assertTrue(cmd.getOtherProperties().isEmpty());
        assertTrue(cmd.hasProperty("--arg-name"));
        assertNull(cmd.getPropertyValue("--arg-name"));
        assertEquals(13, cmd.getLastChunkIndex());
    }

    @Test
    public void testSingleArgNameAndSeparator() throws Exception {

        DefaultCallbackHandler cmd = parse("some-command --arg-name ");
        assertEquals("some-command", cmd.getOperationName());
        assertTrue(cmd.hasProperties());
        assertNull(cmd.getOutputTarget());
        assertTrue(cmd.endsOnSeparator());
        assertFalse(cmd.endsOnPropertyListStart());
        assertTrue(cmd.endsOnPropertySeparator());
        assertFalse(cmd.endsOnPropertyValueSeparator());
        assertEquals(1, cmd.getPropertyNames().size());
        assertTrue(cmd.getOtherProperties().isEmpty());
        assertTrue(cmd.hasProperty("--arg-name"));
        assertNull(cmd.getPropertyValue("--arg-name"));
        assertEquals(13, cmd.getLastChunkIndex());
    }

    @Test
    public void testSingleArgNameAndValueSeparator() throws Exception {

        DefaultCallbackHandler cmd = parse("some-command --arg-name=");
        assertEquals("some-command", cmd.getOperationName());
        assertTrue(cmd.hasProperties());
        assertNull(cmd.getOutputTarget());
        assertTrue(cmd.endsOnSeparator());
        assertFalse(cmd.endsOnPropertyListStart());
        assertFalse(cmd.endsOnPropertySeparator());
        assertTrue(cmd.endsOnPropertyValueSeparator());
        assertEquals(1, cmd.getPropertyNames().size());
        assertTrue(cmd.getOtherProperties().isEmpty());
        assertTrue(cmd.hasProperty("--arg-name"));
        assertNull(cmd.getPropertyValue("--arg-name"));
        assertEquals(23, cmd.getLastChunkIndex());
    }

    @Test
    public void testSingleArgNameWithValue() throws Exception {

        DefaultCallbackHandler cmd = parse("some-command --arg-name=value");
        assertEquals("some-command", cmd.getOperationName());
        assertTrue(cmd.hasProperties());
        assertNull(cmd.getOutputTarget());
        assertFalse(cmd.endsOnSeparator());
        assertFalse(cmd.endsOnPropertyListStart());
        assertFalse(cmd.endsOnPropertySeparator());
        assertFalse(cmd.endsOnPropertyValueSeparator());
        assertEquals(1, cmd.getPropertyNames().size());
        assertTrue(cmd.getOtherProperties().isEmpty());
        assertTrue(cmd.hasProperty("--arg-name"));
        assertEquals("value", cmd.getPropertyValue("--arg-name"));
        assertEquals(24, cmd.getLastChunkIndex());
    }

    @Test
    public void testSingleArgNameWithValueAndSeparator() throws Exception {

        DefaultCallbackHandler cmd = parse("some-command --arg-name=value ");
        assertEquals("some-command", cmd.getOperationName());
        assertTrue(cmd.hasProperties());
        assertNull(cmd.getOutputTarget());
        assertTrue(cmd.endsOnSeparator());
        assertFalse(cmd.endsOnPropertyListStart());
        assertTrue(cmd.endsOnPropertySeparator());
        assertFalse(cmd.endsOnPropertyValueSeparator());
        assertEquals(1, cmd.getPropertyNames().size());
        assertTrue(cmd.getOtherProperties().isEmpty());
        assertTrue(cmd.hasProperty("--arg-name"));
        assertEquals("value", cmd.getPropertyValue("--arg-name"));
        assertEquals(24, cmd.getLastChunkIndex());
    }

    @Test
    public void testEmptyString() throws Exception {

        DefaultCallbackHandler cmd = parse("");
        assertFalse(cmd.hasOperationName());
        assertFalse(cmd.hasProperties());
        assertNull(cmd.getOutputTarget());
        assertFalse(cmd.endsOnSeparator());
        assertFalse(cmd.endsOnAddressOperationNameSeparator());
        assertFalse(cmd.endsOnPropertyListStart());
        assertFalse(cmd.endsOnPropertySeparator());
        assertFalse(cmd.endsOnPropertyValueSeparator());
        assertTrue(cmd.getPropertyNames().isEmpty());
        assertTrue(cmd.getOtherProperties().isEmpty());
        assertEquals(0, cmd.getLastChunkIndex());
    }

    @Test
    public void testWhitespaces() throws Exception {

        DefaultCallbackHandler cmd = parse("   ");
        assertFalse(cmd.hasOperationName());
        assertFalse(cmd.hasProperties());
        assertNull(cmd.getOutputTarget());
        assertTrue(cmd.endsOnSeparator());
        assertTrue(cmd.endsOnAddressOperationNameSeparator());
        assertFalse(cmd.endsOnPropertyListStart());
        assertFalse(cmd.endsOnPropertySeparator());
        assertFalse(cmd.endsOnPropertyValueSeparator());
        assertTrue(cmd.getPropertyNames().isEmpty());
        assertTrue(cmd.getOtherProperties().isEmpty());
        assertEquals(0, cmd.getLastChunkIndex());
    }

    @Test
    public void testCommandWithArgsAndOutputTarget() throws Exception {

        DefaultCallbackHandler cmd = parse(" some-command --name=value --name1 value1 > command.log");
        assertEquals("some-command", cmd.getOperationName());
        assertTrue(cmd.hasProperties());
        assertTrue(cmd.hasProperty("--name"));
        assertEquals("value", cmd.getPropertyValue("--name"));
        assertTrue(cmd.hasProperty("--name1"));
        assertNull(cmd.getPropertyValue("--name1"));

        List<String> otherArgs = cmd.getOtherProperties();
        assertEquals(1, otherArgs.size());
        assertEquals("value1", otherArgs.get(0));

        assertEquals("command.log", cmd.getOutputTarget());
    }

    @Test
    public void testQuotedArgument() throws Exception {

        DefaultCallbackHandler cmd = parse(" some-command \"a b\"");
        assertEquals("some-command", cmd.getOperationName());
        assertTrue(cmd.hasProperties());
        assertTrue(cmd.getPropertyNames().isEmpty());
        final List<String> props = cmd.getOtherProperties();
        assertEquals(1, props.size());
        assertEquals("\"a b\"", props.get(0));
    }

    protected DefaultCallbackHandler parse(String line) {
        DefaultCallbackHandler args = new DefaultCallbackHandler();
        try {
            args.parse(null, line, null);
        } catch (CommandFormatException e) {
            e.printStackTrace();
            org.junit.Assert.fail(e.getLocalizedMessage());
        }
        return args;
    }
}
