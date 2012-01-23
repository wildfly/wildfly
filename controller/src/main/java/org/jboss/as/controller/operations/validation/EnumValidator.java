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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.operations.validation.ParameterValidator} that validates the value is a string matching one of the {@link java.util.concurrent.TimeUnit} names.
 *
 * @author Jason T. Greene
 * @author Brian Stansberry
 */
public class EnumValidator<E extends Enum<E>> extends ModelTypeValidator implements AllowedValuesValidator {

    private final EnumSet<E> allowedValues;
    private final Class<E> enumType;

    public EnumValidator(final Class<E> enumType, final boolean nullable, final E... allowed) {
        this(enumType, nullable, false, allowed);
    }

    public EnumValidator(final Class<E> enumType,  final boolean nullable, final boolean allowExpressions) {
        super(ModelType.STRING, nullable, allowExpressions);
        this.enumType = enumType;
        allowedValues = EnumSet.allOf(enumType);
    }

    public EnumValidator(final Class<E> enumType,  final boolean nullable, final boolean allowExpressions, final E... allowed) {
        super(ModelType.STRING, nullable, allowExpressions);
        this.enumType = enumType;
        allowedValues = EnumSet.noneOf(enumType);
        for (E value : allowed) {
            allowedValues.add(value);
        }
    }

    @Override
    public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
        super.validateParameter(parameterName, value);
        if (value.isDefined()) {
            String tuString = value.asString();
            E enumValue = Enum.valueOf(enumType, tuString.toUpperCase(Locale.ENGLISH));
            if (enumValue == null || !allowedValues.contains(enumValue)) {
                throw new OperationFailedException(new ModelNode().set(String.format("Invalid value %s for %s; legal values are %s",
                        tuString, parameterName, allowedValues)));
            }
        }
    }

    @Override
    public List<ModelNode> getAllowedValues() {
        List<ModelNode> result = new ArrayList<ModelNode>();
        for (E value : allowedValues) {
            result.add(new ModelNode().set(value.name()));
        }
        return result;
    }
}
