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
public class ArgumentValueConverterTestCase {

    private final CommandContext ctx = new MockCommandContext();

    @Test
    public void testDefault_String() throws Exception {
        final ModelNode value = parseObject("text");
        assertNotNull(value);
        assertEquals(ModelType.STRING, value.getType());
        assertEquals("text", value.asString());
    }

    @Test
    public void testDefault_List() throws Exception {
        final ModelNode value = parseObject("[\"item1\",\"item2\"]");
        assertNotNull(value);
        assertEquals(ModelType.LIST, value.getType());
        final List<ModelNode> list = value.asList();
        assertEquals(2, list.size());
        assertEquals("item1", list.get(0).asString());
        assertEquals("item2", list.get(1).asString());
    }

    @Test
    public void testDefault_Object() throws Exception {
        final ModelNode value = parseObject("{\"item1\"=>\"value1\",\"item2\"=>\"value2\"}");
        assertNotNull(value);
        assertEquals(ModelType.OBJECT, value.getType());
        final List<Property> list = value.asPropertyList();
        assertEquals(2, list.size());
        assertEquals("item1", list.get(0).getName());
        assertEquals("value1", list.get(0).getValue().asString());
        assertEquals("item2", list.get(1).getName());
        assertEquals("value2", list.get(1).getValue().asString());
    }

    @Test
    public void testList_NoBrackets() throws Exception {
        final ModelNode value = parseList("a,b,c");
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
    public void testList_NoBracketsOneItem() throws Exception {
        final ModelNode value = parseList("a");
        assertNotNull(value);
        assertEquals(ModelType.LIST, value.getType());
        final List<ModelNode> list = value.asList();
        assertEquals(1, list.size());
        assertNotNull(list.get(0));
        assertEquals("a", list.get(0).asString());
    }

    @Test
    public void testList_WithBrackets() throws Exception {
        final ModelNode value = parseList("[a,b,c]");
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
    public void testList_DMR() throws Exception {
        final ModelNode value = parseList("[\"a\",\"b\",\"c\"]");
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
    public void testPropertyList_DMR() throws Exception {
        final ModelNode value = parseProperties("[(\"a\"=>\"b\"),(\"c\"=>\"d\")]");
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

    @Test
    public void testPropertyList_SimpleCommaSeparated() throws Exception {
        final ModelNode value = parseProperties("a=b,c=d");
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

    @Test
    public void testPropertyList_SimpleCommaSeparatedInBrackets() throws Exception {
        final ModelNode value = parseProperties("[a=b,c=d]");
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

    @Test
    public void testObject_DMR() throws Exception {
        final ModelNode value = parseObject("{\"a\"=>\"b\",\"c\"=>\"d\"}");
        assertNotNull(value);
        assertEquals(ModelType.OBJECT, value.getType());
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

    @Test
    public void testObject_SimpleCommaSeparated() throws Exception {
        final ModelNode value = parseObject("a=b,c=d");
        assertNotNull(value);
        assertEquals(ModelType.OBJECT, value.getType());
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

    @Test
    public void testObject_SimpleCommaSeparatedInCurlyBraces() throws Exception {
        final ModelNode value = parseObject("{a=b,c=d}");
        assertNotNull(value);
        assertEquals(ModelType.OBJECT, value.getType());
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

    @Test
    public void testObject_TextValue() throws Exception {
        final ModelNode value = parseObject("\"text\"");
        assertNotNull(value);
        assertEquals(ModelType.STRING, value.getType());
        assertEquals("text", value.asString());
    }

    protected ModelNode parseObject(String value) throws CommandFormatException {
        return ArgumentValueConverter.DEFAULT.fromString(ctx, value);
    }

    protected ModelNode parseList(String value) throws CommandFormatException {
        return ArgumentValueConverter.LIST.fromString(ctx, value);
    }

    protected ModelNode parseProperties(String value) throws CommandFormatException {
        return ArgumentValueConverter.PROPERTIES.fromString(ctx, value);
    }
}
