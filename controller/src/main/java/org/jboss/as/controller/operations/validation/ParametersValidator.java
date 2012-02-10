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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * Performs multiple {@link ParameterValidator parameter validations} against
 * a detyped operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ParametersValidator implements ParameterValidator {

    final Map<String, ParameterValidator> validators = Collections.synchronizedMap(new HashMap<String, ParameterValidator>());

    public ParametersValidator() {
    }

    public ParametersValidator(ParametersValidator toCopy) {
        validators.putAll(toCopy.validators);
    }

    public void registerValidator(String parameterName, ParameterValidator validator) {
        validators.put(parameterName, validator);
    }

    public void validate(ModelNode operation) throws OperationFailedException {
        for (Map.Entry<String, ParameterValidator> entry : validators.entrySet()) {
            String paramName = entry.getKey();
            ModelNode paramVal = operation.has(paramName) ? operation.get(paramName) : new ModelNode();
            entry.getValue().validateParameter(paramName, paramVal);
        }
    }

    public void validateResolved(ModelNode operation) throws OperationFailedException {
        for (Map.Entry<String, ParameterValidator> entry : validators.entrySet()) {
            String paramName = entry.getKey();
            ModelNode paramVal = operation.has(paramName) ? operation.get(paramName) : new ModelNode();
            entry.getValue().validateResolvedParameter(paramName, paramVal);
        }
    }

    public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
        ParameterValidator parameterValidator = validators.get(parameterName);
        if (parameterValidator != null) parameterValidator.validateParameter(parameterName, value);
    }

    public void validateResolvedParameter(String parameterName, ModelNode value) throws OperationFailedException {
        ParameterValidator parameterValidator = validators.get(parameterName);
        if (parameterValidator != null) parameterValidator.validateResolvedParameter(parameterName, value);
    }
}
