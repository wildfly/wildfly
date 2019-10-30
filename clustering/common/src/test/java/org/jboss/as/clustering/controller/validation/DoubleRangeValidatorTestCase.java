/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
        assertFalse(isValid(validator, new ModelNode(0)));
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
