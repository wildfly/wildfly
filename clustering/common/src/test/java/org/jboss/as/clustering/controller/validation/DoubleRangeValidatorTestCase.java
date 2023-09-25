/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.validation;

import static org.junit.Assert.*;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public class DoubleRangeValidatorTestCase {

    @Test
    public void testFloat() {
        ParameterValidator validator = new DoubleRangeValidatorBuilder().lowerBound(Float.MIN_VALUE).upperBound(Float.MAX_VALUE).build();
        assertFalse(isValid(validator, new ModelNode(Double.MAX_VALUE)));
        assertFalse(isValid(validator, new ModelNode(Double.MIN_VALUE)));
        assertTrue(isValid(validator, new ModelNode(Float.MAX_VALUE)));
        assertTrue(isValid(validator, new ModelNode(Float.MIN_VALUE)));
    }

    @Test
    public void testExclusive() {
        int lower = 0;
        ParameterValidator validator = new DoubleRangeValidatorBuilder().lowerBoundExclusive(lower).build();
        assertFalse(isValid(validator, ModelNode.ZERO));
        assertTrue(isValid(validator, new ModelNode(0.1)));
        assertTrue(isValid(validator, new ModelNode(Double.MAX_VALUE)));

        int upper = 1;
        validator = new DoubleRangeValidatorBuilder().upperBoundExclusive(upper).build();
        assertTrue(isValid(validator, new ModelNode(Double.MIN_VALUE)));
        assertTrue(isValid(validator, new ModelNode(0.99)));
        assertFalse(isValid(validator, new ModelNode(1)));
    }

    static boolean isValid(ParameterValidator validator, ModelNode value) {
        try {
            validator.validateParameter("test", value);
            return true;
        } catch (OperationFailedException e) {
            return false;
        }
    }
}
