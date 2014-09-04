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
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.parsing.StateParser;
import org.jboss.as.cli.parsing.arguments.ArgumentValueCallbackHandler;
import org.jboss.as.cli.parsing.arguments.ArgumentValueInitialState;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class ArgumentValueParsingTestCase {

    @Test
    public void testExpressionOnly() throws Exception {
        final ModelNode value = parse("${test.expression}");
        assertNotNull(value);
        assertEquals(ModelType.STRING, value.getType());
        assertEquals("${test.expression}", value.asString());
    }

    @Test
    public void testExpressionInTheMiddleOnly() throws Exception {
        final ModelNode value = parse("test ${expression} in the middle");
        assertNotNull(value);
        assertEquals(ModelType.STRING, value.getType());
        assertEquals("test ${expression} in the middle", value.asString());
    }

    @Test
    public void testQuotedExpression() throws Exception {
        final ModelNode value = parse("\"${test.expression}\"");
        assertNotNull(value);
        assertEquals(ModelType.STRING, value.getType());
        assertEquals("${test.expression}", value.asString());
    }

    @Test
    public void testSimpleString() throws Exception {
        final ModelNode value = parse("text");
        assertNotNull(value);
        assertEquals(ModelType.STRING, value.getType());
        assertEquals("text", value.asString());
    }

    @Test
    public void testListInBrackets() throws Exception {
        final ModelNode value = parse("[a,b,c]");
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
    public void testNestedList() throws Exception {
        final ModelNode value = parse("[a,b,[c,d]]");
        assertNotNull(value);
        assertEquals(ModelType.LIST, value.getType());
        List<ModelNode> list = value.asList();
        assertEquals(3, list.size());
        assertNotNull(list.get(0));
        assertEquals("a", list.get(0).asString());
        assertNotNull(list.get(1));
        assertEquals("b", list.get(1).asString());
        final ModelNode c = list.get(2);
        assertNotNull(c);
        assertEquals(ModelType.LIST, c.getType());
        list = c.asList();
        assertEquals(2, list.size());
        assertEquals("c", c.get(0).asString());
        assertEquals("d", c.get(1).asString());
    }

    @Test
    public void testListNoBrackets() throws Exception {
        final ModelNode value = parse("a,b,c");
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
    public void testPropertyListInBrackets() throws Exception {
        final ModelNode value = parse("[a=b,c=d]");
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
    public void testObject() throws Exception {
        final ModelNode value = parse("a=b,c=d");
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
    public void testObjectInBraces() throws Exception {
        final ModelNode value = parse("{a=b,c=d}");
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
    public void testObjectWithList() throws Exception {
        final ModelNode value = parse("a=b,c=[d,e]");
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
        final ModelNode de = prop.getValue();
        assertEquals(ModelType.LIST, de.getType());
        final List<ModelNode> deList = de.asList();
        assertEquals(2, deList.size());
        assertEquals("d", deList.get(0).asString());
        assertEquals("e", deList.get(1).asString());
    }

    @Test
    public void testObjectWithChildObject() throws Exception {
        final ModelNode value = parse("a=b,c={d=e}");
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
        final ModelNode de = prop.getValue();
        assertEquals(ModelType.OBJECT, de.getType());
        assertEquals(1, de.keys().size());
        assertEquals("e", de.get("d").asString());
    }

    @Test
    public void testObjectWithPropertyList() throws Exception {
        final ModelNode value = parse("a=b,c=[d=e,f=g]");
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
        final ModelNode c = prop.getValue();
        assertEquals(ModelType.LIST, c.getType());
        final List<Property> propList = c.asPropertyList();
        assertEquals(2, propList.size());
        prop = propList.get(0);
        assertEquals("d", prop.getName());
        assertEquals("e", prop.getValue().asString());
        prop = propList.get(1);
        assertEquals("f", prop.getName());
        assertEquals("g", prop.getValue().asString());
    }

    @Test
    public void testListOfObjects() throws Exception {
        final ModelNode value = parse("[{a=b},{c=[d=e,f={g=h}]}]");
        assertNotNull(value);
        assertEquals(ModelType.LIST, value.getType());
        final List<ModelNode> list = value.asList();
        assertEquals(2, list.size());
        ModelNode item = list.get(0);
        assertNotNull(item);
        assertEquals(1, item.keys().size());
        assertEquals("b", item.get("a").asString());
        item = list.get(1);
        assertNotNull(item);
        assertEquals(1, item.keys().size());
        item = item.get("c");
        assertTrue(item.isDefined());
        assertEquals(ModelType.LIST, item.getType());
        final List<Property> propList = item.asPropertyList();
        assertEquals(2, propList.size());
        Property prop = propList.get(0);
        assertEquals("d", prop.getName());
        assertEquals("e", prop.getValue().asString());
        prop = propList.get(1);
        assertEquals("f", prop.getName());
        final ModelNode gh = prop.getValue();
        assertEquals(1, gh.keys().size());
        assertEquals("h", gh.get("g").asString());
    }

    @Test
    public void testMix() throws Exception {
        final ModelNode value = parse("a=b,c=[d=e,f={g=h}]");
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
        final ModelNode c = prop.getValue();
        assertEquals(ModelType.LIST, c.getType());
        final List<Property> propList = c.asPropertyList();
        assertEquals(2, propList.size());
        prop = propList.get(0);
        assertEquals("d", prop.getName());
        assertEquals("e", prop.getValue().asString());
        prop = propList.get(1);
        assertEquals("f", prop.getName());
        final ModelNode gh = prop.getValue();
        assertEquals(1, gh.keys().size());
        assertEquals("h", gh.get("g").asString());
    }

    @Test
    public void testOpeningCurlyBracesInValue() throws Exception {

        ModelNode value = parse(">{b\\=c}");
        assertNotNull(value);
        assertEquals(ModelType.STRING, value.getType());
        assertEquals(">{b=c}", value.asString());

        value = parse(">\\{b\\=c}");
        assertNotNull(value);
        assertEquals(ModelType.STRING, value.getType());
        assertEquals(">{b=c}", value.asString());
    }

    @Test
    public void testOpeningBracketInValue() throws Exception {
        ModelNode value = parse("a[bc");
        assertNotNull(value);
        assertEquals(ModelType.STRING, value.getType());
        assertEquals("a[bc", value.asString());

        value = parse("a\\[bc");
        assertNotNull(value);
        assertEquals(ModelType.STRING, value.getType());
        assertEquals("a[bc", value.asString());

        value = parse("a \\[ b c");
        assertNotNull(value);
        assertEquals(ModelType.STRING, value.getType());
        assertEquals("a [ b c", value.asString());
    }

    @Test
    public void testDeactivatedEqualsSign() throws Exception {
        final ModelNode value = parse("a=b{c[d=e]}]}");
        assertNotNull(value);
        assertEquals(ModelType.OBJECT, value.getType());
        assertEquals(1, value.keys().size());
        final ModelNode a = value.get("a");
        assertTrue(a.isDefined());
        assertEquals(ModelType.STRING, a.getType());
        assertEquals("b{c[d=e]}]}", a.asString());
    }

    @Test
    public void testAny() throws Exception {
        ModelNode value = parse("\"any(not(match(\\\"Prepared response is\\\")),match(\\\"Scanning\\\"))\"");
        assertNotNull(value);
        assertEquals(ModelType.STRING, value.getType());
        assertEquals("any(not(match(\"Prepared response is\")),match(\"Scanning\"))", value.asString());

        value = parse("any(not(match(\"Prepared response is\")),match(\"Scanning\"))");
        assertNotNull(value);
        assertEquals(ModelType.LIST, value.getType());
        final List<ModelNode> list = value.asList();
        assertEquals(2, list.size());

        ModelNode item = list.get(0);
        assertEquals(ModelType.STRING, item.getType());
        assertEquals("any(not(match(\"Prepared response is\"))", item.asString());

        item = list.get(1);
        assertEquals(ModelType.STRING, item.getType());
        assertEquals("match(\"Scanning\"))", item.asString());
    }

    @Test
    public void testQuotesAndSpaces() throws Exception {
        ModelNode value = parse("\" \"");
        assertNotNull(value);
        assertEquals(ModelType.STRING, value.getType());
        assertEquals(" ", value.asString());

        value = parse(" \" \"");
        assertNotNull(value);
        assertEquals(ModelType.STRING, value.getType());
        assertEquals(" ", value.asString());

        value = parse(" \" \\\"\"");
        assertNotNull(value);
        assertEquals(ModelType.STRING, value.getType());
        assertEquals(" \"", value.asString());
    }

    @Test
    public void testEscapedQuotes() throws Exception {
        ModelNode value = parse("\"substituteAll(\\\"JBAS\\\",\\\"DUMMY\\\")\"");
        assertNotNull(value);
        assertEquals(ModelType.STRING, value.getType());
        assertEquals("substituteAll(\"JBAS\",\"DUMMY\")", value.asString());
    }

    @Test
    public void testLoginModules() throws Exception {
        ModelNode value = parse("[{code=Database, flag=required, module-options=[unauthenticatedIdentity=guest," +
                "dsJndiName=java:jboss/jdbc/ApplicationDS," +
                "principalsQuery= select password from users where username=? ," +
                "rolesQuery = \"select name, 'Roles' FROM user_roless ur, roles r, user u WHERE u.username=? and u.id = ur.user_id and ur.role_id = r.id\" ," +
                "hashAlgorithm = MD5,hashEncoding = hex] }]");
        assertLoginModules(value);
    }

    @Test
    public void testLoginModulesWithLineBreaks() throws Exception {
        final StringBuilder buf = new StringBuilder();
        buf.append("[ \\").append(Util.LINE_SEPARATOR);
        buf.append("\t\t{ \\").append(Util.LINE_SEPARATOR);
        buf.append("\t\t\tcode=Database, \\").append(Util.LINE_SEPARATOR);
        buf.append("\t\t\tflag= \\").append(Util.LINE_SEPARATOR).append("required, \\").append(Util.LINE_SEPARATOR);
        buf.append("\t\t\t").append("module-options=[ \\").append(Util.LINE_SEPARATOR);
        buf.append("\t\t\t\t\t").append("unauthenticatedIdentity=guest, \\").append(Util.LINE_SEPARATOR);
        buf.append("\t\t\t\t\t").append("dsJndiName=java:jboss/jdbc/ApplicationDS, \\").append(Util.LINE_SEPARATOR);
        buf.append("\t\t\t\t\t").append("principalsQuery= select password from users where username=? , \\").append(Util.LINE_SEPARATOR);
        buf.append("\t\t\t\t\t").append("rolesQuery = \"select name, 'Roles' FROM user_roless ur, roles r, user u WHERE u.username=? and u.id = ur.user_id and ur.role_id = r.id\" , \\").append(Util.LINE_SEPARATOR);
        buf.append("\t\t\t\t\t").append("hashAlgorithm = MD5, \\").append(Util.LINE_SEPARATOR);
        buf.append("\t\t\t\t\t").append("hashEncoding = hex \\").append(Util.LINE_SEPARATOR);
        buf.append("\t\t\t\t").append("] \\").append(Util.LINE_SEPARATOR);
        buf.append("\t\t} \\").append(Util.LINE_SEPARATOR);
        buf.append(']');
        ModelNode value = parse(buf.toString());
        assertLoginModules(value);
    }

    protected void assertLoginModules(ModelNode value) {
        assertNotNull(value);
        assertEquals(ModelType.LIST, value.getType());
        List<ModelNode> list = value.asList();
        assertEquals(1, list.size());
        value = list.get(0);
        assertEquals("Database", value.get("code").asString());
        assertEquals("required", value.get("flag").asString());
        value = value.get("module-options");
        assertEquals(ModelType.LIST, value.getType());
        list = value.asList();
        assertEquals(6, list.size());
        assertEquals("guest", list.get(0).get("unauthenticatedIdentity").asString());
        assertEquals("java:jboss/jdbc/ApplicationDS", list.get(1).get("dsJndiName").asString());
        assertEquals("select password from users where username=?", list.get(2).get("principalsQuery").asString());
        assertEquals("select name, 'Roles' FROM user_roless ur, roles r, user u WHERE u.username=? and u.id = ur.user_id and ur.role_id = r.id", list.get(3).get("rolesQuery").asString());
        assertEquals("MD5", list.get(4).get("hashAlgorithm").asString());
        assertEquals("hex", list.get(5).get("hashEncoding").asString());
    }

    protected ModelNode parse(String str) throws CommandFormatException {
        final ArgumentValueCallbackHandler handler = new ArgumentValueCallbackHandler();
        StateParser.parse(str, handler, ArgumentValueInitialState.INSTANCE);
        return handler.getResult();
    }
}
