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
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.parsing.StateParser;
import org.jboss.as.cli.parsing.operation.PropertyListState;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class OperationParamListStateTestCase extends BaseStateParserTest {

    @Test
    public void testParamSimple() throws Exception {
        parseAsParamList("a", "b");
    }

    @Test
    public void testParamSimpleQuotes() throws Exception {
        parseAsParamList("simple-quotes", "\"simple quotes\"");
    }

    @Test
    public void testParamSimpleBrackets() {
        parseAsParamList("simple-brackets", "[simple brackets]");
    }

    @Test
    public void testParamSimpleParenthesis() {
        parseAsParamList("simple-parenthesis", "(simple parenthesis)");
    }

    @Test
    public void testParamSimpleBraces() {
        parseAsParamList("simple-braces", "{simple braces}");
    }

    @Test
    public void testParamSteps() {
        parseAsParamList("steps", "[{\"operation\"=>\"add-system-property\",\"name\"=>\"test\",\"value\"=\"newValue\"},{\"operation\"=>\"add-system-property\",\"name\"=>\"test2\",\"value\"=>\"test2\"}]");
    }

    @Test
    public void testAllParams() {
        parseAsParamList(Param.allParams());
        assertNotNull(result);
        assertNull(result.buffer);
        assertEquals(1, result.children.size());

        ParsedTerm params = result.children.get(0);
        assertNotNull(params);
        assertNull(params.buffer);
        assertEquals(Param.all.size(), params.children.size());

        for(int i = 0; i < Param.all.size(); ++i) {
            Param param = Param.all.get(i);
            assertParam(param.name, param.value, params.children.get(i));
        }
    }

    protected void parseAsParamList(String name, String value) {

        Param param = new Param(name, value);

        parseAsParamList('(' + param.name + '=' + param.value + ')');

        assertNotNull(result);
        assertNull(result.buffer);
        assertEquals(1, result.children.size());

        ParsedTerm params = result.children.get(0);
        assertNotNull(params);
        assertNull(params.buffer);
        assertEquals(1, params.children.size());

        assertParam(param.name, param.value, params.children.get(0));
    }

    protected void parseAsParamList(String str) {

        StateParser parser = new StateParser();
        parser.addState('(', PropertyListState.INSTANCE);
        try {
            parser.parse(str, callbackHandler);
        } catch (CommandFormatException e) {
            Assert.fail(e.getLocalizedMessage());
        }
    }

    protected void assertParam(String name, String value, ParsedTerm param) {
        assertNotNull(param);
        assertNotNull(param.buffer);
        assertEquals(name, param.buffer.toString().trim());
        assertEquals(1, param.children.size());
        ParsedTerm paramValue = param.children.get(0);
        assertNotNull(paramValue);
        assertEquals(value, paramValue.valueAsString());
        //assertEquals(0, paramValue.children.size());
    }

    static class Param {
        static final List<Param> all = new ArrayList<Param>();

        static String allParams() {
            StringBuilder builder = new StringBuilder();
            builder.append('(');
            for(int i = 0; i < all.size(); ++i) {
                Param p = all.get(i);
                if(i > 0) {
                    builder.append(", ");
                }
                builder.append(p.name).append('=').append(p.value);
            }
            builder.append(')');
            return builder.toString();
        }

        final String name;
        final String value;

        Param(String name, String value) {
            this.name = name;
            this.value = value;
            all.add(this);
        }
    }
}
