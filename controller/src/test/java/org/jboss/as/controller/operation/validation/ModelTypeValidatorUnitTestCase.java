/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.controller.operation.validation;

import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Test;

/**
 * Unit tests of {@link ModelTypeValidator}
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ModelTypeValidatorUnitTestCase {

    @Test
    public void testAllowNull() {
        ModelTypeValidator testee = new ModelTypeValidator(ModelType.BOOLEAN, true);
        assertOk(testee, new ModelNode());

        testee = new ModelTypeValidator(ModelType.BOOLEAN, false);
        assertInvalid(testee, new ModelNode());
    }

    @Test
    public void testAllowExpressions() {
        ModelTypeValidator testee = new ModelTypeValidator(ModelType.BOOLEAN, false, true);
        assertOk(testee, new ModelNode().setExpression("{test}"));

        testee = new ModelTypeValidator(ModelType.BOOLEAN, false, false);
        assertInvalid(testee, new ModelNode().setExpression("{test}"));
    }

    @Test
    public void testBigDecimal() {
        ModelTypeValidator testee = new ModelTypeValidator(ModelType.BIG_DECIMAL, false, false, false);
        validateNumbers(testee);

        testee = new ModelTypeValidator(ModelType.BIG_DECIMAL, false, false, true);
        assertOk(testee, new ModelNode().set(new BigDecimal(1)));
        assertInvalid(testee, new ModelNode().set(1));
    }

    @Test
    public void testBigInteger() {
        ModelTypeValidator testee = new ModelTypeValidator(ModelType.BIG_INTEGER, false, false, false);
        validateNumbers(testee);

        testee = new ModelTypeValidator(ModelType.BIG_INTEGER, false, false, true);
        assertOk(testee, new ModelNode().set(new BigInteger("1")));
        assertInvalid(testee, new ModelNode().set(1));
    }

    @Test
    public void testDouble() {
        ModelTypeValidator testee = new ModelTypeValidator(ModelType.DOUBLE, false, false, false);
        validateNumbers(testee);

        testee = new ModelTypeValidator(ModelType.DOUBLE, false, false, true);
        assertOk(testee, new ModelNode().set((double) 1));
        assertInvalid(testee, new ModelNode().set(1));
    }

    @Test
    public void testInt() {
        ModelTypeValidator testee = new ModelTypeValidator(ModelType.INT, false, false, false);
        validateNumbers(testee);
        invalidateIntRange(testee);

        testee = new ModelTypeValidator(ModelType.INT, false, false, true);
        assertOk(testee, new ModelNode().set(1));
        assertInvalid(testee, new ModelNode().set((double) 1));
    }

    @Test
    public void testLong() {
        ModelTypeValidator testee = new ModelTypeValidator(ModelType.LONG, false, false, false);
        validateNumbers(testee);
        invalidateLongRange(testee);

        testee = new ModelTypeValidator(ModelType.LONG, false, false, true);
        assertOk(testee, new ModelNode().set(1L));
        assertInvalid(testee, new ModelNode().set(1));
    }

    @Test
    public void testString() {
        ModelTypeValidator testee = new ModelTypeValidator(ModelType.STRING, false, false, false);
        validateNumbers(testee);

        testee = new ModelTypeValidator(ModelType.STRING, false, false, true);
        assertOk(testee, new ModelNode().set("1"));
        assertInvalid(testee, new ModelNode().set(1));
    }

    @Test
    public void testBoolean() {
        ModelTypeValidator testee = new ModelTypeValidator(ModelType.BOOLEAN, false, false, false);
        assertOk(testee, new ModelNode().set(true));
        assertOk(testee, new ModelNode().set("true"));
        assertOk(testee, new ModelNode().set("TruE"));
        assertOk(testee, new ModelNode().set("false"));
        assertOk(testee, new ModelNode().set("fAlsE"));
        assertInvalid(testee, new ModelNode().set("fals"));
        assertInvalid(testee, new ModelNode().set(0));

        testee = new ModelTypeValidator(ModelType.BOOLEAN, false, false, true);
        assertOk(testee, new ModelNode().set(true));
        assertInvalid(testee, new ModelNode().set("false"));
        assertInvalid(testee, new ModelNode().set(0));
    }

    @Test
    public void testProperty() {
        ModelTypeValidator testee = new ModelTypeValidator(ModelType.PROPERTY, false, false, false);
        assertOk(testee, new ModelNode().set("a", "b"));
        ModelNode node = new ModelNode();
        node.get("a").set("b");
        assertOk(testee, node);
        // We may decide not to support this
        node = new ModelNode();
        node.add("a");
        node.add("b");
        assertOk(testee, node);

        testee = new ModelTypeValidator(ModelType.PROPERTY, false, false, true);
        assertOk(testee, new ModelNode().set("a", "b"));
        node = new ModelNode();
        node.get("a").set("b");
        assertInvalid(testee, node);
    }

    @Test
    public void testObject() {
        ModelTypeValidator testee = new ModelTypeValidator(ModelType.OBJECT, false, false, false);
        assertOk(testee, new ModelNode().set("a", "b"));
        ModelNode node = new ModelNode();
        node.get("a").set("b");
        assertOk(testee, node);
        node = new ModelNode();
        node.add(new ModelNode().set("a", 1));
        node.add(new ModelNode().set("b", 2));
        assertOk(testee, node);
        node = new ModelNode();
        node.add("a");
        node.add("b");
        assertInvalid(testee, node);

        testee = new ModelTypeValidator(ModelType.OBJECT, false, false, true);
        node = new ModelNode();
        node.get("a").set("b");
        assertOk(testee, node);
        assertInvalid(testee, new ModelNode().set("a", "b"));
    }

    @Test
    public void testList() {
        ModelTypeValidator testee = new ModelTypeValidator(ModelType.LIST, false, false, false);
        ModelNode node = new ModelNode();
        node.add(new ModelNode().set("a", 1));
        node.add(new ModelNode().set("b", 2));
        assertOk(testee, node);
        node = new ModelNode();
        node.get("a").set(1);
        node.get("b").set(2);
        assertInvalid(testee, node);
    }

    @Test
    public void testBytes() {
        ModelTypeValidator testee = new ModelTypeValidator(ModelType.BYTES, false, false, false);
        assertOk(testee, new ModelNode().set(new byte[0]));
        assertInvalid(testee, new ModelNode().set("bytes"));
    }

    @Test
    public void testExpression() {
        ModelTypeValidator testee = new ModelTypeValidator(ModelType.EXPRESSION, false, false, false);
        assertOk(testee, new ModelNode().setExpression("${test}"));
        assertInvalid(testee, new ModelNode().set("${test}"));
    }

    private static void validateNumbers(ModelTypeValidator testee) {
        assertOk(testee, new ModelNode().set(new BigDecimal(1)));
        assertOk(testee, new ModelNode().set(new BigInteger("1")));
        assertOk(testee, new ModelNode().set(1));
        assertOk(testee, new ModelNode().set(1L));
        assertOk(testee, new ModelNode().set((double) 1));
        assertOk(testee, new ModelNode().set("1"));
    }

    private static void invalidateIntRange(ModelTypeValidator testee) {
        assertInvalid(testee, new ModelNode().set(BigDecimal.valueOf(Integer.MAX_VALUE).add(BigDecimal.ONE)));
        assertInvalid(testee, new ModelNode().set(BigDecimal.valueOf(Integer.MIN_VALUE).subtract(BigDecimal.ONE)));
        assertInvalid(testee, new ModelNode().set(BigInteger.valueOf(Integer.MAX_VALUE).add(BigInteger.ONE)));
        assertInvalid(testee, new ModelNode().set(BigInteger.valueOf(Integer.MIN_VALUE).subtract(BigInteger.ONE)));
        assertInvalid(testee, new ModelNode().set(Integer.MAX_VALUE + 1L));
        assertInvalid(testee, new ModelNode().set(Integer.MIN_VALUE - 1L));
        assertInvalid(testee, new ModelNode().set(Integer.MAX_VALUE + 1d));
        assertInvalid(testee, new ModelNode().set(Integer.MIN_VALUE - 1d));
    }

    private static void invalidateLongRange(ModelTypeValidator testee) {
        assertInvalid(testee, new ModelNode().set(BigDecimal.valueOf(Long.MAX_VALUE).add(BigDecimal.ONE)));
        assertInvalid(testee, new ModelNode().set(BigDecimal.valueOf(Long.MIN_VALUE).subtract(BigDecimal.ONE)));
        assertInvalid(testee, new ModelNode().set(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE)));
        assertInvalid(testee, new ModelNode().set(BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE)));
        assertInvalid(testee, new ModelNode().set(Long.MAX_VALUE * 10d));
        assertInvalid(testee, new ModelNode().set(Long.MIN_VALUE * 10d));
    }

    private static void assertOk(ModelTypeValidator validator, ModelNode toTest) {
        try {
            validator.validateParameter("test", toTest);
        }
        catch (OperationFailedException e) {
            fail("Validation should have passed but received " + e.getFailureDescription().toString());
        }
    }

    private static void assertInvalid(ModelTypeValidator validator, ModelNode toTest) {
        try {
            validator.validateParameter("test", toTest);
            fail("Validation should have failed ");
        }
        catch (OperationFailedException e) {
            // good
        }
    }
}
