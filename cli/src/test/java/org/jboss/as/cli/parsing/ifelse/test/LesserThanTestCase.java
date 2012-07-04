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
public class LesserThanTestCase extends ComparisonTestBase {

    @Test
    public void testSimpleBoolean() throws Exception {
        ModelNode node = new ModelNode();
        node.get("result").set(true);
        assertFalse(node, "result < true");

        node.get("result").set(false);
        assertFalse(node, "result < false");

        node.get("result").set(false);
        assertTrue(node, "result < true");

        node.get("result").set(true);
        assertFalse(node, "result < false");
    }

    @Test
    public void testSimpleString() throws Exception {
        ModelNode node = new ModelNode();
        node.get("result").set("true");
        assertFalse(node, "result < \"true\"");

        node.get("result").set("false");
        assertTrue(node, "result < \"true\"");

        node.get("result").set("true");
        assertFalse(node, "result < \"false\"");

        node.get("result").set("2");
        assertFalse(node, "result < \"1\"");

        node.get("result").set("1");
        assertTrue(node, "result < \"2\"");
    }

    @Test
    public void testSimpleInt() throws Exception {
        ModelNode node = new ModelNode();
        node.get("result").set(11);
        assertFalse(node, "result < 11");

        node.get("result").set(111);
        assertFalse(node, "result < 11");

        node.get("result").set(11);
        assertTrue(node, "result < 111");

        node.get("result").clear().get("value").set(1);
        assertFalse(node, "result < 11");

        node.get("result").get("value").set(1);
        assertTrue(node, "result.value < 11");
        assertFalse(node, "result.value1 < 11");
        assertFalse(node, "result < 11");
    }
}
