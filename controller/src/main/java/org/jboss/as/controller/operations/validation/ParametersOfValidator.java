/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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

import java.util.List;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class ParametersOfValidator implements ParameterValidator, MinMaxValidator, AllowedValuesValidator {
    private final ParametersValidator delegate;

    public ParametersOfValidator(final ParametersValidator delegate) {
        if (delegate == null)
            throw MESSAGES.nullVar("delegate");
        this.delegate = delegate;
    }

    @Override
    public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
        try {
            delegate.validate(value);
        } catch (OperationFailedException e) {
            final ModelNode failureDescription = new ModelNode().add(MESSAGES.validationFailed(parameterName));
            failureDescription.add(e.getFailureDescription());
            throw new OperationFailedException(e.getMessage(), e.getCause(), failureDescription);
        }
    }

    @Override
    public void validateResolvedParameter(String parameterName, ModelNode value) throws OperationFailedException {
        try {
            delegate.validateResolved(value);
        } catch (OperationFailedException e) {
            throw new OperationFailedException(e.getMessage(), e.getCause(), new ModelNode().set(parameterName + ": " + e.getFailureDescription().asString()));
        }
    }

    @Override
    public Long getMin() {
        return (delegate instanceof MinMaxValidator) ? ((MinMaxValidator) delegate).getMin() : null;
    }

    @Override
    public Long getMax() {
        return (delegate instanceof MinMaxValidator) ? ((MinMaxValidator) delegate).getMax() : null;
    }

    @Override
    public List<ModelNode> getAllowedValues() {
        return (delegate instanceof AllowedValuesValidator) ? ((AllowedValuesValidator) delegate).getAllowedValues() : null;
    }
}
