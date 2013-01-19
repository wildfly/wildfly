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
package org.jboss.as.controller;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ExpressionResolverUnitTestCase {

    @Test(expected = OperationFailedException.class)
    public void testDefaultExpressionResolverWithNoResolutions() throws OperationFailedException {
        ModelNode unresolved = createModelNode();
        ExpressionResolver.TEST_RESOLVER.resolveExpressions(unresolved);
        fail("Did not fail with ISE: " + unresolved);
    }

    @Test
    public void testDefaultExpressionResolverWithSystemPropertyResolutions() throws OperationFailedException {
        System.setProperty("test.prop.expr", "EXPR");
        System.setProperty("test.prop.b", "B");
        System.setProperty("test.prop.c", "C");
        System.setProperty("test.prop.two", "TWO");
        System.setProperty("test.prop.three", "THREE");
        System.setProperty("test.prop.prop", "PROP");
        try {
            ModelNode node = ExpressionResolver.TEST_RESOLVER.resolveExpressions(createModelNode());
            checkResolved(node);
        } finally {
            System.clearProperty("test.prop.expr");
            System.clearProperty("test.prop.b");
            System.clearProperty("test.prop.c");
            System.clearProperty("test.prop.two");
            System.clearProperty("test.prop.three");
            System.clearProperty("test.prop.prop");
        }
    }
    @Test
    public void testPluggableExpressionResolver() throws OperationFailedException {
        ModelNode node = new ExpressionResolverImpl() {
            @Override
            protected void resolvePluggableExpression(ModelNode node) {
                String s = node.asString();
                if (s.equals("${test.prop.expr}")) {
                    node.set("EXPR");
                } else if (s.equals("${test.prop.b}")) {
                    node.set("B");
                } else if (s.equals("${test.prop.c}")) {
                    node.set("C");
                } else if (s.equals("${test.prop.two}")) {
                    node.set("TWO");
                } else if (s.equals("${test.prop.three}")) {
                    node.set("THREE");
                } else if (s.equals("${test.prop.prop}")) {
                    node.set("PROP");
                }
            }

        }.resolveExpressions(createModelNode());

        checkResolved(node);
    }

    @Test(expected = OperationFailedException.class)
    public void testPluggableExpressionResolverNotResolved() throws OperationFailedException {
        ModelNode unresolved = createModelNode();
        new ExpressionResolverImpl() {
            @Override
            protected void resolvePluggableExpression(ModelNode node) {
            }

        }.resolveExpressions(unresolved);

        fail("Did not fail with ISE: " + unresolved);
    }

    @Test
    public void testPluggableExpressionResolverSomeResolvedAndSomeByDefault() throws OperationFailedException {
        System.setProperty("test.prop.c", "C");
        System.setProperty("test.prop.three", "THREE");
        System.setProperty("test.prop.prop", "PROP");
        try {
            ModelNode node = new ExpressionResolverImpl() {
                @Override
                protected void resolvePluggableExpression(ModelNode node) {
                    String s = node.asString();
                    if (s.equals("${test.prop.expr}")) {
                        node.set("EXPR");
                    } else if (s.equals("${test.prop.b}")) {
                        node.set("B");
                    } else if (s.equals("${test.prop.two}")) {
                        node.set("TWO");
                    }
                }

            }.resolveExpressions(createModelNode());

            checkResolved(node);
        } finally {
            System.clearProperty("test.prop.c");
            System.clearProperty("test.prop.three");
            System.clearProperty("test.prop.prop");
        }
    }

    private void checkResolved(ModelNode node) {
        assertEquals(6, node.keys().size());
        assertEquals(1, node.get("int").asInt());
        assertEquals("EXPR", node.get("expr").asString());
        assertEquals(3, node.get("map").keys().size());

        assertEquals("a", node.get("map", "plain").asString());
        assertEquals("B", node.get("map", "prop.b").asString());
        assertEquals("C", node.get("map", "prop.c").asString());

        assertEquals(3, node.get("list").asList().size());
        assertEquals("one", node.get("list").asList().get(0).asString());
        assertEquals("TWO", node.get("list").asList().get(1).asString());
        assertEquals("THREE", node.get("list").asList().get(2).asString());

        assertEquals("plain", node.get("plainprop").asProperty().getValue().asString());
        assertEquals("PROP", node.get("prop").asProperty().getValue().asString());
    }

    private ModelNode createModelNode() {
        ModelNode node = new ModelNode();
        node.get("int").set(1);
        node.get("expr").setExpression("${test.prop.expr}");
        node.get("map", "plain").set("a");
        node.get("map", "prop.b").setExpression("${test.prop.b}");
        node.get("map", "prop.c").setExpression("${test.prop.c}");
        node.get("list").add("one");
        node.get("list").addExpression("${test.prop.two}");
        node.get("list").addExpression("${test.prop.three}");
        node.get("plainprop").set("plain", "plain");
        node.get("prop").setExpression("test", "${test.prop.prop}");
        return node;
    }
}
