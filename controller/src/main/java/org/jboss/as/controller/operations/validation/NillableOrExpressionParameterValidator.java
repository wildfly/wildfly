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

package org.jboss.as.controller.operations.validation;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * {@link ParameterValidator} that validates undefined values and expression types, delegating to a provided
 * validator for everything else.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class NillableOrExpressionParameterValidator implements ParameterValidator {

    private final ParameterValidator delegate;
    private final Boolean allowNull;
    private final boolean allowExpression;

    /**
     * Creates a new {@code NillableOrExpressionParameterValidator}.
     *
     * @param delegate validator to delegate to once null and expression validation is done
     * @param allowNull whether undefined values are allowed. If this param is {@code null}, checking for undefined
     *                  is delegated to the provided {@code delegate}
     * @param allowExpression  whether expressions are allowed
     */
    public NillableOrExpressionParameterValidator(ParameterValidator delegate, Boolean allowNull, boolean allowExpression) {
        this.delegate = delegate;
        this.allowNull = allowNull;
        this.allowExpression = allowExpression;
    }


    @Override
    public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
        switch (value.getType()) {
            case EXPRESSION:
                if (!allowExpression) {
                    throw MESSAGES.expressionNotAllowed(parameterName);
                }
                break;
            case UNDEFINED:
                if (allowNull != null) {
                    if (!allowNull) {
                        throw MESSAGES.nullNotAllowed(parameterName);
                    }
                    break;
                } // else fall through and let the delegate validate
            default:
                delegate.validateParameter(parameterName, value);
        }
    }

    @Override
    public void validateResolvedParameter(String parameterName, ModelNode value) throws OperationFailedException {
        if (!value.isDefined()) {
            if (!allowExpression) {
                throw MESSAGES.nullNotAllowed(parameterName);
            }
        } else {
            delegate.validateResolvedParameter(parameterName, value);
        }
    }
}
