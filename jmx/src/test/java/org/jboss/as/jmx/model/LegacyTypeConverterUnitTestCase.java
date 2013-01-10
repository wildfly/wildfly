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
package org.jboss.as.jmx.model;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXPRESSIONS_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularType;

import junit.framework.Assert;

import org.jboss.as.jmx.model.TypeConverters.TypeConverter;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class LegacyTypeConverterUnitTestCase {


    @Test
    public void testBigIntegerConverter() {
        ModelNode description = createDescription(ModelType.BIG_INTEGER);
        TypeConverter converter = getConverter(description);
        Assert.assertEquals(SimpleType.BIGINTEGER, converter.getOpenType());
        Assert.assertEquals(BigInteger.valueOf(1), assertCast(BigInteger.class, converter.fromModelNode(new ModelNode().set(BigInteger.valueOf(1)))));
        Assert.assertEquals(2, converter.toModelNode(BigInteger.valueOf(2)).asInt());
        assertToArray(converter, BigInteger.valueOf(1), BigInteger.valueOf(2));
    }

    @Test
    public void testBigDecimalConverter() {
        ModelNode description = createDescription(ModelType.BIG_DECIMAL);
        TypeConverter converter = getConverter(description);
        Assert.assertEquals(SimpleType.BIGDECIMAL, converter.getOpenType());
        Assert.assertEquals(BigDecimal.valueOf(1), assertCast(BigDecimal.class, converter.fromModelNode(new ModelNode().set(BigDecimal.valueOf(1)))));
        Assert.assertEquals(2, converter.toModelNode(BigDecimal.valueOf(2)).asInt());
        assertToArray(converter, BigDecimal.valueOf(1), BigDecimal.valueOf(2));
    }

    @Test
    public void testIntConverter() {
        ModelNode description = createDescription(ModelType.INT);
        TypeConverter converter = getConverter(description);
        Assert.assertEquals(SimpleType.INTEGER, converter.getOpenType());
        Assert.assertEquals(Integer.valueOf(1), assertCast(Integer.class, converter.fromModelNode(new ModelNode().set(1))));
        Assert.assertEquals(2, converter.toModelNode(Integer.valueOf(2)).asInt());
        assertToArray(converter, Integer.valueOf(1), Integer.valueOf(2));
    }

    @Test
    public void testBooleanConverter() {
        ModelNode description = createDescription(ModelType.BOOLEAN);
        TypeConverter converter = getConverter(description);
        Assert.assertEquals(SimpleType.BOOLEAN, converter.getOpenType());
        Assert.assertEquals(Boolean.FALSE, assertCast(Boolean.class, converter.fromModelNode(new ModelNode().set(false))));
        Assert.assertEquals(true, converter.toModelNode(Boolean.TRUE).asBoolean());
        assertToArray(converter, Boolean.TRUE, Boolean.FALSE);
    }

    @Test
    public void testBytesConverter() {
        ModelNode description = createDescription(ModelType.BYTES);
        TypeConverter converter = getConverter(description);
        Assert.assertEquals(ArrayType.getPrimitiveArrayType(byte[].class), converter.getOpenType());
        Assert.assertTrue(Arrays.equals(new byte[] {1,2,3}, assertCast(byte[].class, converter.fromModelNode(new ModelNode().set(new byte[] {1,2,3})))));
        Assert.assertTrue(Arrays.equals(new byte[] {1,2,3}, converter.toModelNode(new byte[] {1,2,3}).asBytes()));
        byte[][] bytes = assertCast(byte[][].class, converter.toArray(createList(new byte[] {1,2,3}, new byte[] {4,5,6})));
        Assert.assertEquals(2, bytes.length);
        Assert.assertTrue(Arrays.equals(new byte[] {1,2,3}, bytes[0]));
        Assert.assertTrue(Arrays.equals(new byte[] {4,5,6}, bytes[1]));
    }

    @Test
    public void testDoubleConverter() {
        ModelNode description = createDescription(ModelType.DOUBLE);
        TypeConverter converter = getConverter(description);
        Assert.assertEquals(SimpleType.DOUBLE, converter.getOpenType());
        Assert.assertEquals(Double.valueOf(1), assertCast(Double.class, converter.fromModelNode(new ModelNode().set(1))));
        Assert.assertEquals(Double.valueOf(2), converter.toModelNode(Double.valueOf(2)).asDouble());
        assertToArray(converter, Double.valueOf(1), Double.valueOf(2));
    }

    public void testStringConverter() {
        ModelNode description = createDescription(ModelType.STRING);
        TypeConverter converter = getConverter(description);
        Assert.assertEquals(SimpleType.STRING, converter.getOpenType());
        Assert.assertEquals("A", assertCast(String.class, converter.fromModelNode(new ModelNode().set("A"))));
        Assert.assertEquals("B", converter.toModelNode("B").asString());
        assertToArray(converter, "A", "B");
    }

    @Test
    public void testLongConverter() {
        ModelNode description = createDescription(ModelType.LONG);
        TypeConverter converter = getConverter(description);
        Assert.assertEquals(SimpleType.LONG, converter.getOpenType());
        Assert.assertEquals(Long.valueOf(1), assertCast(Long.class, converter.fromModelNode(new ModelNode().set(1L))));
        Assert.assertEquals(2L, converter.toModelNode(Long.valueOf(2)).asLong());
        assertToArray(converter, Long.valueOf(1), Long.valueOf(2));
    }

    @Test
    public void testTypeConverter() {
        ModelNode description = createDescription(ModelType.TYPE);
        TypeConverter converter = getConverter(description);
        Assert.assertEquals(SimpleType.STRING, converter.getOpenType());
        Assert.assertEquals("OBJECT", assertCast(String.class, converter.fromModelNode(new ModelNode().set(ModelType.OBJECT))));
        Assert.assertEquals(ModelType.LONG, converter.toModelNode("LONG").asType());
        assertToArray(converter, "LONG", "INT");
    }

    @Test
    public void testUndefinedTypeConverter() {
        TypeConverter converter = getConverter(new ModelNode());
        Assert.assertEquals(SimpleType.STRING, converter.getOpenType());

        ModelNode node = new ModelNode();
        // BES 2013/01/10 This uses BigInteger; I'm not sure why. But use a value > Long.MAX_VALUE
        // so the json parser won't convert it down to a long or int resulting in a different value
        // See AS7-4913
        // Likely BigInteger was used *because of* the problem discussed in AS7-4913
        node.get("abc").set(new BigInteger(String.valueOf(Long.MAX_VALUE) + "0"));
        node.get("def").set(false);
        node.protect();

        String json = assertCast(String.class, converter.fromModelNode(node));
        Assert.assertEquals(node, ModelNode.fromJSONString(json));
        Assert.assertEquals(json, assertCast(String.class, converter.fromModelNode(node)));
        assertToArray(converter, json);
    }

    @Test
    public void testPropertyTypeConverter() {
        ModelNode description = createDescription(ModelType.PROPERTY);
        TypeConverter converter = getConverter(description);

        ModelNode node = new ModelNode();
        node.set("name", "value");
        node.protect();

        Assert.assertEquals(SimpleType.STRING, converter.getOpenType());
        String dmr = assertCast(String.class, converter.fromModelNode(node));
        Assert.assertEquals(node, ModelNode.fromString(dmr));
        Assert.assertEquals(dmr, assertCast(String.class, converter.fromModelNode(node)));
        assertToArray(converter, dmr);
    }

    @Test
    public void testPropertyValueTypeConverter() {
        ModelNode description = createDescription(ModelType.PROPERTY, ModelType.INT);
        TypeConverter converter = getConverter(description);

        ModelNode node = new ModelNode();
        node.set("name", 1);
        node.protect();

        Assert.assertEquals(SimpleType.STRING, converter.getOpenType());
        String dmr = assertCast(String.class, converter.fromModelNode(node));
        Assert.assertEquals(node, ModelNode.fromString(dmr));
        Assert.assertEquals(dmr, assertCast(String.class, converter.fromModelNode(node)));
        assertToArray(converter, dmr);
    }

    @Test
    public void testProperPropertyTypeConverter() throws Exception {
        ModelNode description = createDescription(ModelType.PROPERTY);
        TypeConverter converter = TypeConverters.createLegacyTypeConverters(true).getConverter(description);

        CompositeType type = assertCast(CompositeType.class, converter.getOpenType());
        Set<String> keys = type.keySet();
        Assert.assertEquals(2, keys.size());
        assertCompositeType(type, "name", String.class.getName(), "The property name");
        assertCompositeType(type, "value", String.class.getName(), "The property value");

        CompositeData data = assertCast(CompositeData.class, converter.fromModelNode(new ModelNode().set("one", "uno")));
        Assert.assertEquals(type, data.getCompositeType());
        Assert.assertEquals("one", data.get("name"));
        Assert.assertEquals("uno", data.get("value"));

        data = new CompositeDataSupport(type, new String[] {"name", "value"}, new String[] {"two", "dos"});
        ModelNode newNode = converter.toModelNode(data);
        Assert.assertEquals(ModelType.PROPERTY, newNode.getType());
        Assert.assertEquals(new ModelNode().set("two", "dos"), newNode);

        assertToArray(converter, data);
    }

    @Test
    public void testProperPropertyValueTypeConverter() throws Exception {
        ModelNode description = createDescription(ModelType.PROPERTY, ModelType.INT);
        TypeConverter converter = TypeConverters.createLegacyTypeConverters(true).getConverter(description);

        CompositeType type = assertCast(CompositeType.class, converter.getOpenType());
        Set<String> keys = type.keySet();
        Assert.assertEquals(2, keys.size());
        assertCompositeType(type, "name", String.class.getName(), "The property name");
        assertCompositeType(type, "value", Integer.class.getName(), "The property value");

        CompositeData data = assertCast(CompositeData.class, converter.fromModelNode(new ModelNode().set("one", 1)));
        Assert.assertEquals(type, data.getCompositeType());
        Assert.assertEquals("one", data.get("name"));
        Assert.assertEquals(1, data.get("value"));

        data = new CompositeDataSupport(type, new String[] {"name", "value"}, new Object[] {"two", Integer.valueOf(2)});
        ModelNode newNode = converter.toModelNode(data);
        Assert.assertEquals(ModelType.PROPERTY, newNode.getType());
        Assert.assertEquals(new ModelNode().set("two", 2), newNode);

        assertToArray(converter, data);
    }

    @Test
    public void testSimpleTypeList() throws Exception {
        ModelNode description = createDescription(ModelType.LIST, ModelType.INT);
        TypeConverter converter = getConverter(description);
        Assert.assertEquals(ArrayType.getArrayType(SimpleType.INTEGER), converter.getOpenType());
        ModelNode node = new ModelNode();
        node.add(1);
        node.add(2);
        Assert.assertTrue(Arrays.equals(new Integer[] {1,2} ,assertCast(Integer[].class, converter.fromModelNode(node))));
        Assert.assertEquals(node, converter.toModelNode(new Integer[] {1,2}));
    }

    @Test
    public void testByteArrayList() throws Exception {
        ModelNode description = createDescription(ModelType.LIST, ModelType.BYTES);
        TypeConverter converter = getConverter(description);
        Assert.assertEquals(ArrayType.getArrayType(ArrayType.getPrimitiveArrayType(byte[].class)), converter.getOpenType());
        ModelNode node = new ModelNode();
        node.add(new byte[] {1,2});
        node.add(new byte[] {3,4});
        byte[][] bytes = assertCast(byte[][].class, converter.fromModelNode(node));
        Assert.assertEquals(2, bytes.length);
        Assert.assertTrue(Arrays.equals(new byte[] {1,2}, bytes[0]));
        Assert.assertTrue(Arrays.equals(new byte[] {3,4}, bytes[1]));
        Assert.assertEquals(node, converter.toModelNode(new byte[][]{{1,2},{3,4}}));
    }

    @Test
    public void testSimpleTypeObject() throws Exception {
        ModelNode description = createDescription(ModelType.OBJECT, ModelType.LONG);
        TypeConverter converter = getConverter(description);

        assertMapType(assertCast(TabularType.class, converter.getOpenType()), SimpleType.STRING, SimpleType.LONG);

        ModelNode node = new ModelNode();
        node.get("one").set(1L);
        node.get("two").set(2L);

        TabularData tabularData = assertCast(TabularData.class, converter.fromModelNode(node));
        Assert.assertEquals(2, tabularData.size());
        Assert.assertEquals(Long.valueOf(1), tabularData.get(new Object[] {"one"}).get("value"));
        Assert.assertEquals(Long.valueOf(2), tabularData.get(new Object[] {"two"}).get("value"));

        Assert.assertEquals(node, converter.toModelNode(tabularData));

        //Allow plain map as well? Yeah why not!
        Map<String, Long> map = new HashMap<String, Long>();
        map.put("one", 1L);
        map.put("two", 2L);
        Assert.assertEquals(node, converter.toModelNode(map));
    }

    @Test
    public void testByteArrayObject() throws Exception {
        ModelNode description = createDescription(ModelType.OBJECT, ModelType.BYTES);
        TypeConverter converter = getConverter(description);

        assertMapType(assertCast(TabularType.class, converter.getOpenType()), SimpleType.STRING, ArrayType.getPrimitiveArrayType(byte[].class));

        ModelNode node = new ModelNode();
        node.get("one").set(new byte[] {1,2});
        node.get("two").set(new byte[] {3,4});

        TabularData tabularData = assertCast(TabularData.class, converter.fromModelNode(node));
        Assert.assertEquals(2, tabularData.size());
        Assert.assertTrue(Arrays.equals(new byte[] {1,2}, (byte[])tabularData.get(new Object[] {"one"}).get("value")));
        Assert.assertTrue(Arrays.equals(new byte[] {3,4}, (byte[])tabularData.get(new Object[] {"two"}).get("value")));

        //Allow plain map as well? Yeah why not!
        Map<String, byte[]> map = new HashMap<String, byte[]>();
        map.put("one", new byte[] {1,2});
        map.put("two", new byte[] {3,4});
        Assert.assertEquals(node, converter.toModelNode(map));
    }

    @Test
    public void testComplexValue() throws Exception {
        ModelNode description = createDescription(ModelType.OBJECT);
        ModelNode complexValueType = new ModelNode();
        complexValueType.get("int-value", DESCRIPTION).set("An int value");
        complexValueType.get("int-value", TYPE).set(ModelType.INT);
        complexValueType.get("bigint-value", DESCRIPTION).set("A biginteger value");
        complexValueType.get("bigint-value", TYPE).set(ModelType.BIG_INTEGER);
        complexValueType.get("bigdecimal-value", DESCRIPTION).set("A bigdecimal value");
        complexValueType.get("bigdecimal-value", TYPE).set(ModelType.BIG_DECIMAL);
        complexValueType.get("boolean-value", DESCRIPTION).set("A boolean value");
        complexValueType.get("boolean-value", TYPE).set(ModelType.BOOLEAN);
        complexValueType.get("bytes-value", DESCRIPTION).set("A bytes value");
        complexValueType.get("bytes-value", TYPE).set(ModelType.BYTES);
        complexValueType.get("double-value", DESCRIPTION).set("A double value");
        complexValueType.get("double-value", TYPE).set(ModelType.DOUBLE);
        complexValueType.get("string-value", DESCRIPTION).set("A string value");
        complexValueType.get("string-value", TYPE).set(ModelType.STRING);
        complexValueType.get("long-value", DESCRIPTION).set("A long value");
        complexValueType.get("long-value", TYPE).set(ModelType.LONG);
        complexValueType.get("type-value", DESCRIPTION).set("A type value");
        complexValueType.get("type-value", TYPE).set(ModelType.TYPE);
        complexValueType.get("list-int-value", DESCRIPTION).set("An int list value");
        complexValueType.get("list-int-value", TYPE).set(ModelType.LIST);
        complexValueType.get("list-int-value", VALUE_TYPE).set(ModelType.INT);
        complexValueType.get("map-int-value", DESCRIPTION).set("An int map value");
        complexValueType.get("map-int-value", TYPE).set(ModelType.OBJECT);
        complexValueType.get("map-int-value", VALUE_TYPE).set(ModelType.INT);
        description.get(VALUE_TYPE).set(complexValueType);

        TypeConverter converter = getConverter(description);

        CompositeType type = assertCast(CompositeType.class, converter.getOpenType());
        Set<String> keys = type.keySet();
        Assert.assertEquals(11, keys.size());
        assertCompositeType(type, "int-value", Integer.class.getName(), "An int value");
        assertCompositeType(type, "bigint-value", BigInteger.class.getName(), "A biginteger value");
        assertCompositeType(type, "bigdecimal-value", BigDecimal.class.getName(), "A bigdecimal value");
        assertCompositeType(type, "boolean-value", Boolean.class.getName(), "A boolean value");
        assertCompositeType(type, "bytes-value", byte[].class.getName(), "A bytes value");
        assertCompositeType(type, "double-value", Double.class.getName(), "A double value");
        assertCompositeType(type, "string-value", String.class.getName(), "A string value");
        assertCompositeType(type, "long-value", Long.class.getName(), "A long value");
        assertCompositeType(type, "type-value", String.class.getName(), "A type value");
        assertCompositeType(type, "list-int-value", Integer[].class.getName(), "An int list value");
        assertMapType(assertCast(TabularType.class, assertCompositeType(type, "map-int-value", TabularType.class.getName(), "An int map value", false)), SimpleType.STRING, SimpleType.INTEGER);

        ModelNode node = new ModelNode();
        node.get("int-value").set(1);
        node.get("bigint-value").set(BigInteger.valueOf(2));
        node.get("bigdecimal-value").set(BigDecimal.valueOf(3));
        node.get("boolean-value").set(Boolean.TRUE);
        node.get("bytes-value").set(new byte[] {4,5});
        node.get("double-value").set(Double.valueOf(6));
        node.get("string-value").set("Seven");
        node.get("long-value").set(Long.valueOf(8));
        node.get("type-value").set(ModelType.INT);
        node.get("list-int-value").add(9);
        node.get("list-int-value").add(10);
        node.get("map-int-value", "one").set(11);
        node.get("map-int-value", "two").set(12);

        CompositeData data = assertCast(CompositeData.class, converter.fromModelNode(node));
        Assert.assertEquals(type, data.getCompositeType());
        Assert.assertEquals(node, converter.toModelNode(data));

        //Another test testing missing data in fromModelNode();
        node = new ModelNode();
        node.get("int-value").set(1);
        data = assertCast(CompositeData.class, converter.fromModelNode(node));
        Assert.assertEquals(node, converter.toModelNode(data));

        //And another test testing missing data in fromModelNode();
        node = new ModelNode();
        node.get("boolean-value").set(true);
        data = assertCast(CompositeData.class, converter.fromModelNode(node));
        Assert.assertEquals(node, converter.toModelNode(data));
    }

    @Test
    public void testComplexList() throws Exception {
        ModelNode description = createDescription(ModelType.LIST);
        ModelNode complexValueType = new ModelNode();
        complexValueType.get("int-value", DESCRIPTION).set("An int value");
        complexValueType.get("int-value", TYPE).set(ModelType.INT);
        complexValueType.get("list-int-value", DESCRIPTION).set("An int list value");
        complexValueType.get("list-int-value", TYPE).set(ModelType.LIST);
        complexValueType.get("list-int-value", VALUE_TYPE).set(ModelType.INT);
        description.get(VALUE_TYPE).set(complexValueType);

        TypeConverter converter = getConverter(description);

        ArrayType<CompositeType> arrayType = assertCast(ArrayType.class, converter.getOpenType());
        CompositeType type = assertCast(CompositeType.class, arrayType.getElementOpenType());
        Set<String> keys = type.keySet();
        Assert.assertEquals(2, keys.size());
        assertCompositeType(type, "int-value", Integer.class.getName(), "An int value");
        assertCompositeType(type, "list-int-value", Integer[].class.getName(), "An int list value");

        ModelNode node = new ModelNode();
        ModelNode entry = new ModelNode();
        entry.get("int-value").set(1);
        entry.get("list-int-value").add(2);
        entry.get("list-int-value").add(3);
        node.add(entry);
        entry = new ModelNode();
        entry.get("int-value").set(4);
        entry.get("list-int-value").add(5);
        entry.get("list-int-value").add(6);
        node.add(entry);

        CompositeData[] datas = assertCast(CompositeData[].class, converter.fromModelNode(node));
        Assert.assertEquals(datas[0].getCompositeType(), datas[1].getCompositeType());
        Assert.assertEquals(type, datas[0].getCompositeType());

        Assert.assertEquals(node, converter.toModelNode(datas));
    }

    @Test
    public void testJsonObject() throws Exception {
        ModelNode description = createDescription(ModelType.OBJECT);

        TypeConverter converter = getConverter(description);

        Assert.assertEquals(SimpleType.STRING, converter.getOpenType());

        ModelNode node = new ModelNode();
        node.get("long").set(5L);
        node.get("string").set("Value");
        node.get("a", "b").set(true);
        node.get("c", "d").set(40);

        String json = node.toJSONString(false);
        String data = assertCast(String.class, converter.fromModelNode(node));
        Assert.assertEquals(json, data);

        Assert.assertEquals(ModelNode.fromJSONString(json), converter.toModelNode(data));
    }

    @Test
    public void testJsonObjectInList() throws Exception {
        ModelNode description = createDescription(ModelType.LIST, ModelType.OBJECT);

        TypeConverter converter = getConverter(description);

        ArrayType<String> arrayType = assertCast(ArrayType.class, converter.getOpenType());
        Assert.assertEquals(SimpleType.STRING, assertCast(SimpleType.class, arrayType.getElementOpenType()));

        ModelNode list = new ModelNode();
        ModelNode value1 = new ModelNode();
        value1.get("long").set(5L);
        value1.get("string").set("Value");
        value1.get("a", "b").set(true);
        value1.get("c", "d").set(40);
        list.add(value1);
        ModelNode value2 = new ModelNode();
        value2.get("long").set(10L);
        list.add(value2);

        String json1 = value1.toJSONString(false);
        String json2 = value2.toJSONString(false);
        String[] data = assertCast(String[].class, converter.fromModelNode(list));
        Assert.assertEquals(2, data.length);
        Assert.assertEquals(json1, data[0]);
        Assert.assertEquals(json2, data[1]);

        Assert.assertEquals(ModelNode.fromJSONString(list.toJSONString(false)), converter.toModelNode(data));
    }

    @Test
    public void testJsonObjectInComplexValue() throws Exception {
        ModelNode description = createDescription(ModelType.OBJECT);
        ModelNode complexValueType = new ModelNode();
        complexValueType.get("value", DESCRIPTION).set("A  value");
        complexValueType.get("value", TYPE).set(ModelType.OBJECT);
        description.get(VALUE_TYPE).set(complexValueType);

        TypeConverter converter = getConverter(description);

        CompositeType type = assertCast(CompositeType.class, converter.getOpenType());
        Set<String> keys = type.keySet();
        Assert.assertEquals(1, keys.size());

        Assert.assertEquals(SimpleType.STRING, type.getType("value"));

        ModelNode node = new ModelNode();
        node.get("value", "long").set(1L);
        node.get("value", "string").set("test");

        CompositeData data = assertCast(CompositeData.class, converter.fromModelNode(node));
        Assert.assertEquals(type, data.getCompositeType());
        Assert.assertEquals(ModelNode.fromJSONString(node.toJSONString(false)), converter.toModelNode(data));

    }

    @Test
    public void testBigIntegerExpressionConverter() throws Exception {
        ModelNode description = createDescription(ModelType.BIG_INTEGER);
        TypeConverter converter = getConverter(description);
        BigInteger data = assertCast(BigInteger.class, converter.fromModelNode(new ModelNode().setExpression("${this.should.not.exist.!!!!!:1}")));
        Assert.assertEquals(BigInteger.valueOf(1), data);
        try {
            converter.toModelNode("${this.should.not.exist.!!!!!:2}");
            Assert.fail("Should not have been able to convert to a model node");
        } catch (IllegalArgumentException expexted) {
        }

    }

    @Test
    public void testBigDecimalExpressionConverter() throws Exception {
        ModelNode description = createDescription(ModelType.BIG_DECIMAL);
        TypeConverter converter = getConverter(description);
        BigDecimal data = assertCast(BigDecimal.class, converter.fromModelNode(new ModelNode().setExpression("${this.should.not.exist.!!!!!:1}")));
        Assert.assertEquals(BigDecimal.valueOf(1), data);
        try {
            converter.toModelNode("${this.should.not.exist.!!!!!:2}");
            Assert.fail("Should not have been able to convert to a model node");
        } catch (IllegalArgumentException expexted) {
        }
    }

    @Test
    public void testIntExpressionConverter() throws Exception {
        ModelNode description = createDescription(ModelType.INT);
        TypeConverter converter = getConverter(description);
        Integer data = assertCast(Integer.class, converter.fromModelNode(new ModelNode().setExpression("${this.should.not.exist.!!!!!:1}")));
        Assert.assertEquals(Integer.valueOf(1), data);
        try {
            converter.toModelNode("${this.should.not.exist.!!!!!:2}");
            Assert.fail("Should not have been able to convert to a model node");
        } catch (IllegalArgumentException expexted) {
        }
    }

    @Test
    public void testBooleanExpressionConverter() throws Exception {
        ModelNode description = createDescription(ModelType.BOOLEAN);
        TypeConverter converter = getConverter(description);
        Boolean data = assertCast(Boolean.class, converter.fromModelNode(new ModelNode().setExpression("${this.should.not.exist.!!!!!:false}")));
        Assert.assertEquals(Boolean.FALSE, data);
        try {
            converter.toModelNode("${this.should.not.exist.!!!!!:2}");
            Assert.fail("Should not have been able to convert to a model node");
        } catch (IllegalArgumentException expexted) {
        }
    }

    @Test
    public void testDoubleExpressionConverter() throws Exception {
        ModelNode description = createDescription(ModelType.DOUBLE);
        TypeConverter converter = getConverter(description);
        Double data = assertCast(Double.class, converter.fromModelNode(new ModelNode().setExpression("${this.should.not.exist.!!!!!:1}")));
        Assert.assertEquals(Double.valueOf(1), data);
        try {
            converter.toModelNode("${this.should.not.exist.!!!!!:2}");
            Assert.fail("Should not have been able to convert to a model node");
        } catch (IllegalArgumentException expexted) {
        }
    }

    @Test
    public void testStringExpressionConverter() throws Exception {
        ModelNode description = createDescription(ModelType.STRING);
        TypeConverter converter = getConverter(description);
        String data = assertCast(String.class, converter.fromModelNode(new ModelNode().setExpression("${this.should.not.exist.!!!!!:A}")));
        Assert.assertEquals("A", data);
        ModelNode newNode = converter.toModelNode("${this.should.not.exist.!!!!!:B}");
        Assert.assertEquals(ModelType.EXPRESSION, newNode.getType());
        Assert.assertEquals("${this.should.not.exist.!!!!!:B}", newNode.asString());
    }

    @Test
    public void testStringVaultExpressionConverter() throws Exception {
        ModelNode description = createDescription(ModelType.STRING);
        TypeConverter converter = getConverter(description);
        String data = assertCast(String.class, converter.fromModelNode(new ModelNode().setExpression("${VAULT::keystore_pass::password::xxx}")));
        Assert.assertEquals("${VAULT::keystore_pass::password::xxx}", data);
        ModelNode newNode = converter.toModelNode("${VAULT::keystore_pass::password::yyyy}");
        Assert.assertEquals(ModelType.EXPRESSION, newNode.getType());
        Assert.assertEquals("${VAULT::keystore_pass::password::yyyy}", newNode.asString());

    }

    @Test
    public void testLongExpressionConverter() throws Exception {
        ModelNode description = createDescription(ModelType.LONG);
        TypeConverter converter = getConverter(description);
        Long data = assertCast(Long.class, converter.fromModelNode(new ModelNode().setExpression("${this.should.not.exist.!!!!!:1}")));
        Assert.assertEquals(Long.valueOf(1), data);
        try {
            converter.toModelNode("${this.should.not.exist.!!!!!:2}");
            Assert.fail("Should not have been able to convert to a model node");
        } catch (IllegalArgumentException expexted) {
        }
    }

    @Test
    public void testTypeExpressionConverter() throws Exception {
        ModelNode description = createDescription(ModelType.TYPE);
        TypeConverter converter = getConverter(description);
        String data = assertCast(String.class, converter.fromModelNode(new ModelNode().setExpression("${this.should.not.exist.!!!!!:OBJECT}")));
        Assert.assertEquals(ModelType.OBJECT, ModelType.valueOf(data));
        try {
            converter.toModelNode("${this.should.not.exist.!!!!!:LONG}");
            Assert.fail("Should not have been able to convert to a model node");
        } catch (IllegalArgumentException expexted) {
        }
    }

    @Test
    public void testUndefinedTypeExpressionConverter() throws Exception {
        ModelNode description = new ModelNode();
        description.get(EXPRESSIONS_ALLOWED).set(true);
        TypeConverter converter = getConverter(description);

        ModelNode node = new ModelNode();
        node.get("abc").setExpression("${this.should.not.exist.!!!!!:10}");
        node.get("def").setExpression("${this.should.not.exist.!!!!!:false}");
        node.protect();

        String json = assertCast(String.class, converter.fromModelNode(node));
        Assert.assertEquals(node, ModelNode.fromJSONString(json));
        assertToArray(converter, json, null);
    }

    @Test
    public void testPropertyTypeExpressionConverter() {
        ModelNode description = createDescription(ModelType.PROPERTY);
        TypeConverter converter = getConverter(description);

        ModelNode node = new ModelNode();
        node.set("name", "${this.should.not.exist.!!!!!:value}");
        node.protect();

        ModelNode expected = node.clone().resolve();

        Assert.assertEquals(SimpleType.STRING, converter.getOpenType());
        String dmr = assertCast(String.class, converter.fromModelNode(node));
        Assert.assertEquals(expected, ModelNode.fromString(dmr));
        Assert.assertEquals(dmr, assertCast(String.class, converter.fromModelNode(expected)));
        assertToArray(converter, dmr);
    }


    @Test
    public void testPropertyValueTypeExpressionConverter() throws Exception {
        ModelNode description = createDescription(ModelType.PROPERTY, ModelType.INT);
        TypeConverter converter = getConverter(description);

        ModelNode node = new ModelNode();
        node.set("name", "${this.should.not.exist.!!!!!:1}");
        node.protect();

        ModelNode expected = node.clone().resolve();

        Assert.assertEquals(SimpleType.STRING, converter.getOpenType());
        String dmr = assertCast(String.class, converter.fromModelNode(node));
        Assert.assertEquals(expected, ModelNode.fromString(dmr));
        Assert.assertEquals(dmr, assertCast(String.class, converter.fromModelNode(expected)));
        assertToArray(converter, dmr);
    }

    @Test
    public void testProperPropertyTypeExpressionConverter() throws Exception {
        ModelNode description = createDescription(ModelType.PROPERTY);
        TypeConverter converter = TypeConverters.createLegacyTypeConverters(true).getConverter(description);

        CompositeType type = assertCast(CompositeType.class, converter.getOpenType());
        Set<String> keys = type.keySet();
        Assert.assertEquals(2, keys.size());
        assertCompositeType(type, "name", String.class.getName(), "The property name");
        assertCompositeType(type, "value", String.class.getName(), "The property value");

        CompositeData data = assertCast(CompositeData.class, converter.fromModelNode(new ModelNode().setExpression("one", "${this.should.not.exist.!!!!!:uno}")));
        Assert.assertEquals(type, data.getCompositeType());
        Assert.assertEquals("one", data.get("name"));
        Assert.assertEquals("uno", data.get("value"));

        data = new CompositeDataSupport(type, new String[] {"name", "value"}, new String[] {"two", "dos"});
        ModelNode newNode = converter.toModelNode(data);
        Assert.assertEquals(ModelType.PROPERTY, newNode.getType());
        Assert.assertEquals(new ModelNode().set("two", "dos"), newNode);

        assertToArray(converter, data);
    }

    @Test
    public void testProperPropertyValueTypeExpressionConverter() throws Exception {
        ModelNode description = createDescription(ModelType.PROPERTY, ModelType.INT);
        TypeConverter converter = TypeConverters.createLegacyTypeConverters(true).getConverter(description);

        CompositeType type = assertCast(CompositeType.class, converter.getOpenType());
        Set<String> keys = type.keySet();
        Assert.assertEquals(2, keys.size());
        assertCompositeType(type, "name", String.class.getName(), "The property name");
        assertCompositeType(type, "value", Integer.class.getName(), "The property value");

        CompositeData data = assertCast(CompositeData.class, converter.fromModelNode(new ModelNode().setExpression("one", "${this.should.not.exist.!!!!!:1}")));
        Assert.assertEquals(type, data.getCompositeType());
        Assert.assertEquals("one", data.get("name"));
        Assert.assertEquals(1, data.get("value"));

        data = new CompositeDataSupport(type, new String[] {"name", "value"}, new Object[] {"two", Integer.valueOf(2)});
        ModelNode newNode = converter.toModelNode(data);
        Assert.assertEquals(ModelType.PROPERTY, newNode.getType());
        Assert.assertEquals(new ModelNode().set("two", 2), newNode);

        assertToArray(converter, data);
    }
    @Test
    public void testSimpleTypeExpressionList() throws Exception {
        ModelNode description = createDescription(ModelType.LIST, ModelType.INT);
        description.get(EXPRESSIONS_ALLOWED).set(true);
        TypeConverter converter = getConverter(description);
        ArrayType<?> arrayType = assertCast(ArrayType.class, converter.getOpenType());
        Assert.assertEquals(SimpleType.INTEGER, arrayType.getElementOpenType());

        ModelNode node = new ModelNode();
        node.addExpression("${this.should.not.exist.!!!!!:1}");
        node.addExpression("${this.should.not.exist.!!!!!:2}");

        Integer[] data = assertCast(Integer[].class, converter.fromModelNode(node));
        Assert.assertEquals(Integer.valueOf(1), data[0]);
        Assert.assertEquals(Integer.valueOf(2), data[1]);
    }

    @Test
    public void testSimpleTypeObjectExpressions() throws Exception {
        ModelNode description = createDescription(ModelType.OBJECT, ModelType.LONG);
        description.get(EXPRESSIONS_ALLOWED).set(true);
        TypeConverter converter = getConverter(description);

        assertMapType(assertCast(TabularType.class, converter.getOpenType()), SimpleType.STRING, SimpleType.LONG);

        ModelNode node = new ModelNode();
        node.get("one").setExpression("${this.should.not.exist.!!!!!:1}");
        node.get("two").setExpression("${this.should.not.exist.!!!!!:2}");

        TabularData tabularData = assertCast(TabularData.class, converter.fromModelNode(node));
        Assert.assertEquals(2, tabularData.size());
        Assert.assertEquals(Long.valueOf(1), tabularData.get(new Object[] {"one"}).get("value"));
        Assert.assertEquals(Long.valueOf(2), tabularData.get(new Object[] {"two"}).get("value"));
    }

    @Test
    public void testBigIntegerEmptyConverter() {
        ModelNode description = createDescription(ModelType.BIG_INTEGER);
        TypeConverter converter = getConverter(description);
        Assert.assertEquals(SimpleType.BIGINTEGER, converter.getOpenType());
        Assert.assertNull(converter.fromModelNode(new ModelNode().set("")));
    }

    @Test
    public void testBigDecimalEmptyConverter() {
        ModelNode description = createDescription(ModelType.BIG_DECIMAL);
        TypeConverter converter = getConverter(description);
        Assert.assertEquals(SimpleType.BIGDECIMAL, converter.getOpenType());
        Assert.assertNull(converter.fromModelNode(new ModelNode().set("")));
    }

    @Test
    public void testIntEmptyConverter() {
        ModelNode description = createDescription(ModelType.INT);
        TypeConverter converter = getConverter(description);
        Assert.assertEquals(SimpleType.INTEGER, converter.getOpenType());
        Assert.assertNull(converter.fromModelNode(new ModelNode().set("")));
    }

    @Test
    public void testBooleanEmptyConverter() {
        ModelNode description = createDescription(ModelType.BOOLEAN);
        TypeConverter converter = getConverter(description);
        Assert.assertEquals(SimpleType.BOOLEAN, converter.getOpenType());
        Assert.assertNull(converter.fromModelNode(new ModelNode().set("")));
    }

    @Test
    public void testDoubleEmptyConverter() {
        ModelNode description = createDescription(ModelType.DOUBLE);
        TypeConverter converter = getConverter(description);
        Assert.assertEquals(SimpleType.DOUBLE, converter.getOpenType());
        Assert.assertNull(converter.fromModelNode(new ModelNode().set("")));
    }

    @Test
    public void testStringEmptyConverter() {
        ModelNode description = createDescription(ModelType.STRING);
        TypeConverter converter = getConverter(description);
        Assert.assertEquals(SimpleType.STRING, converter.getOpenType());
        Assert.assertNull(converter.fromModelNode(new ModelNode().set("")));
    }

    @Test
    public void testLongEmptyConverter() {
        ModelNode description = createDescription(ModelType.LONG);
        TypeConverter converter = getConverter(description);
        Assert.assertEquals(SimpleType.LONG, converter.getOpenType());
        Assert.assertNull(converter.fromModelNode(new ModelNode().set("")));
    }

    @Test
    public void testTypeEmptyConverter() {
        ModelNode description = createDescription(ModelType.TYPE);
        TypeConverter converter = getConverter(description);
        Assert.assertEquals(SimpleType.STRING, converter.getOpenType());
        Assert.assertNull(converter.fromModelNode(new ModelNode().set("")));
    }

    @Test
    public void testUndefinedTypeEmptyConverter() {
        TypeConverter converter = getConverter(new ModelNode());
        Assert.assertEquals(SimpleType.STRING, converter.getOpenType());

        ModelNode node = new ModelNode();
        node.get("abc");
        node.get("def");
        node.protect();

        String json = assertCast(String.class, converter.fromModelNode(node));
        Assert.assertEquals(node.resolve(), ModelNode.fromJSONString(json));
        Assert.assertEquals(json, assertCast(String.class, converter.fromModelNode(node)));
        assertToArray(converter, json);
    }

    @Test
    public void testSimpleTypeEmptyList() throws Exception {
        ModelNode description = createDescription(ModelType.LIST, ModelType.INT);
        TypeConverter converter = getConverter(description);
        Assert.assertEquals(ArrayType.getArrayType(SimpleType.INTEGER), converter.getOpenType());
        ModelNode node = new ModelNode();
        node.add("");
        node.add("");
        Assert.assertTrue(Arrays.equals(new Integer[] {null, null} , assertCast(Integer[].class, converter.fromModelNode(node))));
    }

    @Test
    public void testSimpleTypeObjectEmpty() throws Exception {
        ModelNode description = createDescription(ModelType.OBJECT, ModelType.LONG);
        TypeConverter converter = getConverter(description);

        assertMapType(assertCast(TabularType.class, converter.getOpenType()), SimpleType.STRING, SimpleType.LONG);

        ModelNode node = new ModelNode();
        node.get("one").set("");
        node.get("two").set("");

        TabularData tabularData = assertCast(TabularData.class, converter.fromModelNode(node));
        Assert.assertEquals(2, tabularData.size());
        Assert.assertNull(tabularData.get(new Object[] {"one"}).get("value"));
        Assert.assertNull(tabularData.get(new Object[] {"two"}).get("value"));

        ModelNode expected = new ModelNode();
        expected.get("one");
        expected.get("two");

        Assert.assertEquals(expected, converter.toModelNode(tabularData));
    }

    private OpenType<?> assertCompositeType(CompositeType composite, String name, String type, String description){
        return assertCompositeType(composite, name, type, description, true);
    }

    private OpenType<?> assertCompositeType(CompositeType composite, String name, String type, String description, boolean validateType){
        Assert.assertTrue(composite.keySet().contains(name));
        if (validateType) {
            Assert.assertEquals(type, composite.getType(name).getTypeName());
        }
        Assert.assertEquals(description, composite.getDescription(name));
        return composite.getType(name);
    }

    private void assertMapType(OpenType<?> mapType, OpenType<?> keyType, OpenType<?> valueType) {
        TabularType type = assertCast(TabularType.class, mapType);
        Assert.assertEquals(1, type.getIndexNames().size());
        Assert.assertEquals("key", type.getIndexNames().get(0));
        Assert.assertEquals(2, type.getRowType().keySet().size());
        Assert.assertTrue(type.getRowType().keySet().contains("key"));
        Assert.assertTrue(type.getRowType().keySet().contains("value"));
        Assert.assertEquals(keyType, type.getRowType().getType("key"));
        Assert.assertEquals(valueType, type.getRowType().getType("value"));

    }

    private <T> T assertCast(Class<T> clazz, Object value) {
        Assert.assertNotNull(value);
        Assert.assertTrue("value " + value.getClass().getName() + " can not be changed to a " + clazz.getName(), clazz.isAssignableFrom(value.getClass()));
        return clazz.cast(value);
    }

    private void assertToArray(TypeConverter converter, Object...values) {
        Object[] array = converter.toArray(createList(values));
        Assert.assertEquals(array.length, values.length);
        for (int i = 0 ; i < values.length ; i++) {
            Assert.assertEquals(array[i], values[i]);
        }
    }

    private List<Object> createList(Object...values){
        List<Object> list = new ArrayList<Object>();
        for (Object value : values) {
            list.add(value);
        }
        return list;
    }

    private TypeConverter getConverter(ModelNode description) {
        return TypeConverters.createLegacyTypeConverters(false).getConverter(description);
    }

    private ModelNode createDescription(ModelType type) {
        return createDescription(type, null);
    }

    private ModelNode createDescription(ModelType type, ModelType valueType) {
        ModelNode node = new ModelNode();
        node.get(TYPE).set(type);
        if (valueType != null) {
            node.get(VALUE_TYPE).set(valueType);
        }
        return node;
    }
}
