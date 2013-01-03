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
package org.jboss.as.cli.parsing.ifelse.test;


import org.jboss.dmr.ModelNode;
import org.junit.Test;


/**
 *
 * @author Alexey Loubyansky
 */
public class AndOrTestCase extends ComparisonTestBase {

    @Test
    public void testSimpleAnd() throws Exception {
        ModelNode node = new ModelNode();
        node.get("result").get("i").set(10);
        node.get("result").get("b").set(true);

        assertTrue(node, "result.i < 11 && result.b == true");
        assertFalse(node, "result.i < 11 && result.b != true");
        assertFalse(node, "result.i < 1 && result.b == true");
        assertFalse(node, "result.i < 1 && result.b != true");
        assertTrue(node, "result.i > 1 && result.b != false");
    }

    @Test
    public void testSimpleOr() throws Exception {
        ModelNode node = new ModelNode();
        node.get("result").get("i").set(10);
        node.get("result").get("b").set(true);

        assertTrue(node, "result.i < 11 || result.b == true");
        assertTrue(node, "result.i < 11 || result.b != true");
        assertTrue(node, "result.i < 1 || result.b == true");
        assertFalse(node, "result.i < 1 || result.b != true");
    }

    @Test
    public void testAndOr() throws Exception {
        ModelNode node = new ModelNode();
        node.get("result").get("a").set("a");
        node.get("result").get("b").set("b");
        node.get("result").get("c").set("c");

        assertTrue(node, "result.a == a || result.b == b && result.c == c");
        assertTrue(node, "result.a == x || result.b == b && result.c == c");
        assertFalse(node, "result.a == x || result.b == x && result.c == c");
        assertFalse(node, "result.a == x || result.b == b && result.c == x");
        assertTrue(node, "result.a == a || result.b == b && result.c == x");
    }

    @Test
    public void testParentheses() throws Exception {
        ModelNode node = new ModelNode();
        node.get("result").get("a").set("a");
        node.get("result").get("b").set("b");
        node.get("result").get("c").set("c");

        assertTrue(node, "(result.a == a || result.b == b) && result.c == c");
        assertFalse(node, "(result.a == a || result.b == b) && result.c == x");
        assertTrue(node, "(result.a == a || result.b == x) && result.c == c");
        assertTrue(node, "(result.a == x || result.b == b) && result.c == c");
        assertFalse(node, "(result.a == x || result.b == x) && result.c == c");
    }

    @Test
    public void testNestedParentheses() throws Exception {
        ModelNode node = new ModelNode();
        node.get("result").get("a").set("a");
        node.get("result").get("b").set("b");
        node.get("result").get("c").set("c");
        node.get("result").get("d").set("d");
        node.get("result").get("e").set("e");

        assertTrue(node, "(result.a == a || result.e == e && (result.b == b || result.d == d)) && result.c == c");
        assertFalse(node, "(result.a == a || result.e == e && (result.b == b || result.d == d)) && result.c == x");
        assertTrue(node, "(result.a == a || result.e == e && (result.b == x || result.d == d)) && result.c == c");
        assertTrue(node, "(result.a == a || result.e == x && (result.b == x || result.d == d)) && result.c == c");
        assertFalse(node, "(result.a == x || result.e == x && (result.b == x || result.d == d)) && result.c == c");
    }
}
