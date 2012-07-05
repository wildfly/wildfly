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
import static org.junit.Assert.assertNotNull;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

import org.jboss.as.cli.ArgumentValueConverter;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.completion.mock.MockCommandContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class ParameterValueResolutionTestCase {

    @Test
    public void testSimpleText() throws Exception {
        final CommandContext ctx = new MockCommandContext();
        ModelNode value = parseObject(ctx, "the value is $\\{cli.test.prop\\}");
        assertNotNull(value);
        assertEquals(ModelType.STRING, value.getType());
        assertEquals("the value is ${cli.test.prop}", value.asString());

        ctx.setResolveParameterValues(true);
        setProperty("cli.test.prop", "valuable");
        value = parseObject(ctx, "the value is ${cli.test.prop}");
        assertNotNull(value);
        assertEquals(ModelType.STRING, value.getType());
        assertEquals("the value is valuable", value.asString());
    }

    @Test
    public void testDefault_List() throws Exception {
        final CommandContext ctx = new MockCommandContext();
        ctx.setResolveParameterValues(true);
        setProperty("cli.test.prop1", "one");
        setProperty("cli.test.prop2", "two");
        final ModelNode value = parseObject(ctx, "[\"${cli.test.prop1}\",\"${cli.test.prop2}\"]");
        assertNotNull(value);
        assertEquals(ModelType.LIST, value.getType());
        final List<ModelNode> list = value.asList();
        assertEquals(2, list.size());
        assertEquals("one", list.get(0).asString());
        assertEquals("two", list.get(1).asString());
    }

    @Test
    public void testDefault_Object() throws Exception {
        final CommandContext ctx = new MockCommandContext();
        ctx.setResolveParameterValues(true);
        setProperty("cli.test.prop1", "one");
        setProperty("cli.test.prop2", "two");
        setProperty("cli.test.prop3", "three");
        setProperty("cli.test.prop4", "four");
        final ModelNode value = parseObject(ctx, "{\"${cli.test.prop1}\"=>\"${cli.test.prop2}\",\"${cli.test.prop3}\"=>\"${cli.test.prop4}\"}");
        assertNotNull(value);
        assertEquals(ModelType.OBJECT, value.getType());
        final List<Property> list = value.asPropertyList();
        assertEquals(2, list.size());
        assertEquals("one", list.get(0).getName());
        assertEquals("two", list.get(0).getValue().asString());
        assertEquals("three", list.get(1).getName());
        assertEquals("four", list.get(1).getValue().asString());
    }

    @Test
    public void testList_NoBrackets() throws Exception {
        final CommandContext ctx = new MockCommandContext();
        ctx.setResolveParameterValues(true);
        setProperty("cli.test.prop1", "a");
        setProperty("cli.test.prop2", "b");
        setProperty("cli.test.prop3", "c");
        final ModelNode value = parseList(ctx, "${cli.test.prop1},${cli.test.prop2},${cli.test.prop3}");
        assertNotNull(value);
        assertEquals(ModelType.LIST, value.getType());
        final List<ModelNode> list = value.asList();
        assertEquals(3, list.size());
        assertNotNull(list.get(0));
        assertEquals("a", list.get(0).asString());
        assertNotNull(list.get(1));
        assertEquals("b", list.get(1).asString());
        assertNotNull(list.get(2));
        assertEquals("c", list.get(2).asString());
    }

    @Test
    public void testPropertyList_SimpleCommaSeparated() throws Exception {
        final CommandContext ctx = new MockCommandContext();
        ctx.setResolveParameterValues(true);
        setProperty("cli.test.prop1", "a");
        setProperty("cli.test.prop2", "b");
        setProperty("cli.test.prop3", "c");
        setProperty("cli.test.prop4", "d");
        final ModelNode value = parseProperties(ctx, "${cli.test.prop1}=${cli.test.prop2},${cli.test.prop3}=${cli.test.prop4}");
        assertNotNull(value);
        assertEquals(ModelType.LIST, value.getType());
        final List<Property> list = value.asPropertyList();
        assertEquals(2, list.size());
        Property prop = list.get(0);
        assertNotNull(prop);
        assertEquals("a", prop.getName());
        assertEquals("b", prop.getValue().asString());
        prop = list.get(1);
        assertNotNull(prop);
        assertEquals("c", prop.getName());
        assertEquals("d", prop.getValue().asString());
    }

    protected void setProperty(final String name, final String value) {
        if(System.getSecurityManager() == null) {
            System.setProperty(name, value);
        } else {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {
                    System.setProperty(name, value);
                    return null;
                }});
        }
    }

    protected ModelNode parseObject(CommandContext ctx, String value) throws CommandFormatException {
        return ArgumentValueConverter.DEFAULT.fromString(ctx, value);
    }

    protected ModelNode parseList(CommandContext ctx, String value) throws CommandFormatException {
        return ArgumentValueConverter.LIST.fromString(ctx, value);
    }

    protected ModelNode parseProperties(CommandContext ctx, String value) throws CommandFormatException {
        return ArgumentValueConverter.PROPERTIES.fromString(ctx, value);
    }
}
