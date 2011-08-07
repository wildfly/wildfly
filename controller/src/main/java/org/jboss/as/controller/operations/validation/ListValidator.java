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

import java.util.List;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Validates parameters of type {@link ModelType#LIST}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 *
 */
public class ListValidator extends ModelTypeValidator implements ParameterValidator {

    private final int min;
    private final int max;
    private final ParameterValidator elementValidator;

    /**
     * Constructs a new {@code ListValidator}
     *
     * @param elementValidator validator for list elements
     */
    public ListValidator(ParameterValidator elementValidator) {
        this(elementValidator, false, 1, Integer.MAX_VALUE);
    }

    /**
     * @param elementValidator validator for list elements
     * @param nullable {@code true} if the model node for the list can be {@code null} or {@link ModelType#UNDEFINED}
     */
    public ListValidator(ParameterValidator elementValidator, boolean nullable) {
        this(elementValidator, nullable, 1, Integer.MAX_VALUE);
    }

    /**
     * @param elementValidator validator for list elements
     * @param nullable {@code true} if the model node for the list can be {@code null} or {@link ModelType#UNDEFINED}
     * @param minSize minimum number of elements in the list
     * @param maxSize maximum number of elements in the list
     */
    public ListValidator(ParameterValidator elementValidator, boolean nullable, int minSize, int maxSize) {
        super(ModelType.LIST, nullable, false, true);
        assert elementValidator != null : "elementValidator is null";
        this.min = minSize;
        this.max = maxSize;
        this.elementValidator = elementValidator;
    }

    @Override
    public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
        super.validateParameter(parameterName, value);
        if (value.isDefined()) {
            List<ModelNode> list = value.asList();
            int size = list.size();
            if (size < min) {
                throw new OperationFailedException(new ModelNode().set(String.format("[%d] is an invalid size for parameter %s. A minimum length of [%d] is required", size, parameterName, min)));
            }
            else if (size > max) {
                throw new OperationFailedException(new ModelNode().set(String.format("[%d] is an invalid size for parameter %s. A maximum length of [%d] is required", size, parameterName, max)));
            }
            else {
                for (ModelNode element : list) {
                    elementValidator.validateParameter(parameterName, element);
                }
            }
        }
    }

}
