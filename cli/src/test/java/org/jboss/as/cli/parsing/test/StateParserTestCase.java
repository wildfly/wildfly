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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.parsing.BasicInitialParsingState;
import org.jboss.as.cli.operation.parsing.StateParser;
import org.jboss.as.cli.operation.parsing.PropertyListState;
import org.jboss.as.cli.operation.parsing.ParsingStateCallbackHandler;
import org.jboss.as.cli.operation.parsing.ParsingContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class StateParserTestCase {

    private ParsingStateCallbackHandler callbackHandler = getCallbackHandler();
    private ParsedTerm result;
    private ParsedTerm temp;

    @Before
    public void setup() {
        result = new ParsedTerm(null);
    }

    @Test
    public void testDefault() throws Exception {

        parse("name");

        assertNotNull(result);
        assertEquals(1, result.children.size());
        assertNull(result.buffer);

        ParsedTerm child = result.children.get(0);
        assertNotNull(child);
        assertEquals(0, child.children.size());
        assertEquals("name", child.buffer.toString());
    }

    @Test
    public void testQuotes() throws Exception {
        parse("\"name\"");

        assertNotNull(result);
        assertEquals(1, result.children.size());
        assertNull(result.buffer);

        ParsedTerm child = result.children.get(0);
        assertNotNull(child);
        assertTrue(child.children.isEmpty());
        assertEquals("name", child.buffer.toString());
    }

    @Test
    public void testParathesis() throws Exception {
        parse("(name)");
        assertNotNull(result);
        assertEquals(1, result.children.size());
        assertNull(result.buffer);

        ParsedTerm child = result.children.get(0);
        assertNotNull(child);
        assertTrue(child.children.isEmpty());
        assertEquals("name", child.buffer.toString());
    }

    @Test
    public void testMix() throws Exception {
        parse("a (b) c [d[e(f{g}h)i]k]l ({[]})");

        assertNotNull(result);
        assertEquals(1, result.children.size());
        assertNull(result.buffer);

        ParsedTerm firstChild = result.children.get(0);
        assertNotNull(firstChild);
        assertEquals(3, firstChild.children.size());
        assertEquals("a  c l ", firstChild.buffer.toString());

        ParsedTerm child = firstChild.children.get(0);
        assertNotNull(child);
        assertTrue(child.children.isEmpty());
        assertEquals("b", child.buffer.toString());

        child = firstChild.children.get(1);
        assertNotNull(child);
        assertEquals(1, child.children.size());
        assertEquals("dk", child.buffer.toString());

        child = child.children.get(0);
        assertNotNull(child);
        assertEquals(1, child.children.size());
        assertEquals("ei", child.buffer.toString());

        child = child.children.get(0);
        assertNotNull(child);
        assertEquals(1, child.children.size());
        assertEquals("fh", child.buffer.toString());

        child = child.children.get(0);
        assertNotNull(child);
        assertEquals(0, child.children.size());
        assertEquals("g", child.buffer.toString());

        child = firstChild.children.get(2);
        assertNotNull(child);
        assertEquals(1, child.children.size());
        assertNull(child.buffer);

        child = child.children.get(0);
        assertNotNull(child);
        assertEquals(1, child.children.size());
        assertNull(child.buffer);

        child = child.children.get(0);
        assertNotNull(child);
        assertEquals(0, child.children.size());
        assertNull(child.buffer);
    }

    @Test
    public void testEscapingQuotesInQuotes() throws Exception {
        parse("\"a\\\"b\"");

        assertNotNull(result);
        assertEquals(1, result.children.size());
        assertNull(result.buffer);

        ParsedTerm child = result.children.get(0);
        assertNotNull(child);;
        assertEquals(1, child.children.size());
        assertEquals("ab", child.buffer.toString());

        child = child.children.get(0);
        assertNotNull(child);;
        assertEquals(0, child.children.size());
        assertEquals("\"", child.buffer.toString());
    }

    @Test
    public void testEscapingQuotesInUnquotedContent() throws Exception {
        parse("a\\\"b");

        assertNotNull(result);
        assertEquals(1, result.children.size());
        assertNull(result.buffer);

        ParsedTerm child = result.children.get(0);
        assertNotNull(child);
        assertEquals(1, child.children.size());
        assertEquals("ab", child.buffer.toString());

        child = child.children.get(0);
        assertNotNull(child);
        assertEquals(0, child.children.size());
        assertEquals("\"", child.buffer.toString());
    }

    @Test
    public void testBracketsInQuotes() throws Exception {
        parse("\"({[]})\"");
        assertNotNull(result);
        assertEquals(1, result.children.size());
        assertNull(result.buffer);

        ParsedTerm child = result.children.get(0);
        assertNotNull(child);
        assertEquals(0, child.children.size());
        assertEquals("({[]})", child.buffer.toString());
    }

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
        } catch (OperationFormatException e) {
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

    protected void parse(String str) throws OperationFormatException {
        StateParser.parse(str, callbackHandler, BasicInitialParsingState.INSTANCE);
    }

    private ParsingStateCallbackHandler getCallbackHandler() {
        return new ParsingStateCallbackHandler() {
            @Override
            public void enteredState(ParsingContext ctx) {
                //System.out.println("[" + ctx.getLocation() + "] '" + ctx.getCharacter() + "' entered " + ctx.getState().getId());

                if (temp == null) {
                    temp = result;
                }

                ParsedTerm newTerm = new ParsedTerm(temp);
                temp = newTerm;
                //temp.append(ctx.getCharacter());
            }

            @Override
            public void leavingState(ParsingContext ctx) {
                //System.out.println("[" + ctx.getLocation() + "] '" + ctx.getCharacter() + "' left " + ctx.getState().getId());

                //temp.append(ctx.getCharacter());
                temp.parent.addChild(temp);
                temp = temp.parent;
            }

            @Override
            public void character(ParsingContext ctx)
                    throws OperationFormatException {
                //System.out.println("[" + ctx.getLocation() + "] '" + ctx.getCharacter() + "' content of " + ctx.getState().getId());

                temp.append(ctx.getCharacter());
            }};
    }

    private class ParsedTerm {
        final ParsedTerm parent;
        StringBuilder buffer;
        List<ParsedTerm> children = Collections.emptyList();
        StringBuilder valueAsString = new StringBuilder();

        ParsedTerm(ParsedTerm parent) {
            this.parent = parent;
        }

        void append(char ch) {
            if(buffer == null) {
                buffer = new StringBuilder();
            }
            buffer.append(ch);
            valueAsString.append(ch);
        }

        void addChild(ParsedTerm child) {
            if (children.isEmpty()) {
                children = new ArrayList<ParsedTerm>();
            }
            children.add(child);
            valueAsString.append(child.valueAsString());
        }

        String valueAsString() {
            return valueAsString.toString();
        }
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
