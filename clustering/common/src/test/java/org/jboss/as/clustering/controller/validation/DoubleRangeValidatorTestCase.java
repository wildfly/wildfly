/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.validation;

import static org.junit.Assert.*;

import org.jboss.as.clustering.controller.validation.DoubleRangeValidator.Bound;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public class DoubleRangeValidatorTestCase {

    @Test
    public void testDouble() {
        ParameterValidator validator = DoubleRangeValidator.NON_NEGATIVE;
        assertValid(validator, new ModelNode(Double.MAX_VALUE));
        assertValid(validator, new ModelNode(Double.MIN_VALUE));
        assertValid(validator, new ModelNode(0d));
        assertInvalid(validator, new ModelNode(0d - Double.MIN_VALUE));
        assertInvalid(validator, new ModelNode(0d - Double.MAX_VALUE));

        validator = DoubleRangeValidator.POSITIVE;
        assertValid(validator, new ModelNode(Double.MAX_VALUE));
        assertValid(validator, new ModelNode(Double.MIN_VALUE));
        assertInvalid(validator, new ModelNode(0d));
        assertInvalid(validator, new ModelNode(0d - Double.MIN_VALUE));
        assertInvalid(validator, new ModelNode(0d - Double.MAX_VALUE));

        validator = new DoubleRangeValidator(Bound.exclusive(0d), Bound.exclusive(1d));
        assertInvalid(validator, new ModelNode(Double.MAX_VALUE));
        assertInvalid(validator, new ModelNode(1d));
        assertValid(validator, new ModelNode(0.9999999999999999));
        assertValid(validator, new ModelNode(Double.MIN_VALUE));
        assertInvalid(validator, new ModelNode(0d));
        assertInvalid(validator, new ModelNode(0d - Double.MIN_VALUE));
        assertInvalid(validator, new ModelNode(0d - Double.MAX_VALUE));
    }

    @Test
    public void testFloat() {
        ParameterValidator validator = DoubleRangeValidator.FLOAT;
        assertInvalid(validator, new ModelNode(Double.MAX_VALUE));
        assertValid(validator, new ModelNode(Float.MAX_VALUE));
        assertValid(validator, new ModelNode(Float.MIN_VALUE));
        assertValid(validator, new ModelNode(Double.MIN_VALUE));
        assertValid(validator, new ModelNode(0f));
        assertValid(validator, new ModelNode(0d - Double.MIN_VALUE));
        assertValid(validator, new ModelNode(0f - Float.MIN_VALUE));
        assertValid(validator, new ModelNode(0f - Float.MAX_VALUE));
        assertInvalid(validator, new ModelNode(0d - Double.MAX_VALUE));

        validator = DoubleRangeValidator.NON_NEGATIVE_FLOAT;
        assertInvalid(validator, new ModelNode(Double.MAX_VALUE));
        assertValid(validator, new ModelNode(Float.MAX_VALUE));
        assertValid(validator, new ModelNode(Float.MIN_VALUE));
        assertValid(validator, new ModelNode(Double.MIN_VALUE));
        assertValid(validator, new ModelNode(0f));
        assertInvalid(validator, new ModelNode(0d - Double.MIN_VALUE));
        assertInvalid(validator, new ModelNode(0f - Float.MIN_VALUE));
        assertInvalid(validator, new ModelNode(0f - Float.MAX_VALUE));
        assertInvalid(validator, new ModelNode(0d - Double.MAX_VALUE));

        validator = DoubleRangeValidator.POSITIVE_FLOAT;
        assertInvalid(validator, new ModelNode(Double.MAX_VALUE));
        assertValid(validator, new ModelNode(Float.MAX_VALUE));
        assertValid(validator, new ModelNode(Float.MIN_VALUE));
        assertInvalid(validator, new ModelNode(Double.MIN_VALUE));
        assertInvalid(validator, new ModelNode(0f));
        assertInvalid(validator, new ModelNode(0d - Double.MIN_VALUE));
        assertInvalid(validator, new ModelNode(0f - Float.MIN_VALUE));
        assertInvalid(validator, new ModelNode(0f - Float.MAX_VALUE));
        assertInvalid(validator, new ModelNode(0d - Double.MAX_VALUE));
    }

    static void assertValid(ParameterValidator validator, ModelNode value) {
        try {
            validator.validateParameter("test", value);
        } catch (OperationFailedException e) {
            fail(e.getMessage());
        }
    }

    static void assertInvalid(ParameterValidator validator, ModelNode value) {
        assertThrows(OperationFailedException.class, () -> validator.validateParameter("test", value));
    }
}
