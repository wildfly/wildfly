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

import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class MiscStringContentTestCase extends BaseStateParserTest {

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
}
