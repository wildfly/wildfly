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
package org.jboss.as.controller.operations.validation;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Validates that the given parameter is a int in a given range.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class IntRangeValidator extends ModelTypeValidator {
    protected final int min;
    protected final int max;

    public IntRangeValidator(final int min) {
        this(min, Integer.MAX_VALUE, false, false);
    }

    public IntRangeValidator(final int min, final boolean nullable) {
        this(min, Integer.MAX_VALUE, nullable, false);
    }

    public IntRangeValidator(final int min, final int max, final boolean nullable, final boolean allowExpressions) {
        super(ModelType.INT, nullable, allowExpressions, false);
        this.min = min;
        this.max = max;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
        super.validateParameter(parameterName, value);
        if (value.isDefined() && value.getType() != ModelType.EXPRESSION) {
            int val = value.asInt();
            if (val < min) {
                throw new OperationFailedException(new ModelNode().set(val + " is an invalid value for parameter " + parameterName + ". A minimum value of " + min + " is required"));
            }
            else if (val > max) {
                throw new OperationFailedException(new ModelNode().set(val + " is an invalid value for parameter " + parameterName + ". A maximum value of " + max + " is required"));
            }
        }
    }

}
