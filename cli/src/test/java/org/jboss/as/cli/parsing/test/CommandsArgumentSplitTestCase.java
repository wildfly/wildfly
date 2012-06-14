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

import java.util.List;

import org.jboss.as.cli.Util;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class CommandsArgumentSplitTestCase {

    @Test
    public void testSingleOperationWithParameters() throws Exception {
        final String op = ":read-resource(recursive=false,include-defaults=true)";
        final List<String> split = split(op);
        assertEquals(1, split.size());
        assertEquals(op, split.get(0));
    }

    @Test
    public void testCommaSeparatedWords() throws Exception {
        final List<String> words = split("one,two,three");
        assertEquals(3, words.size());
        assertEquals("one", words.get(0));
        assertEquals("two", words.get(1));
        assertEquals("three", words.get(2));
    }

    @Test
    public void testParenthesesCommaSeparatedWords() throws Exception {
        final List<String> words = split("(one,two),three");
        assertEquals(2, words.size());
        assertEquals("(one,two)", words.get(0));
        assertEquals("three", words.get(1));
    }

    @Test
    public void testCurliesCommaSeparatedWords() throws Exception {
        final List<String> words = split("{one,two},three");
        assertEquals(2, words.size());
        assertEquals("{one,two}", words.get(0));
        assertEquals("three", words.get(1));
    }

    @Test
    public void testBracketsCommaSeparatedWords() throws Exception {
        final List<String> words = split("[one,two],three");
        assertEquals(2, words.size());
        assertEquals("[one,two]", words.get(0));
        assertEquals("three", words.get(1));
    }

    @Test
    public void testQuotesCommaSeparatedWords() throws Exception {
        final List<String> words = split("\"one,two\",three");
        assertEquals(2, words.size());
        assertEquals("\"one,two\"", words.get(0));
        assertEquals("three", words.get(1));
    }

    @Test
    public void testEscapeCommaSeparatedWords() throws Exception {
        final List<String> words = split("one\\,two,three");
        assertEquals(2, words.size());
        assertEquals("one\\,two", words.get(0));
        assertEquals("three", words.get(1));
    }

    protected List<String> split(String line) {
        return Util.splitCommands(line);
    }

}
