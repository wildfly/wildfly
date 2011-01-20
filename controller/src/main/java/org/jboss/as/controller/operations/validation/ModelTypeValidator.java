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

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Validates that the given parameter is of the correct type.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ModelTypeValidator implements ParameterValidator {
    protected final ModelType type;
    protected final boolean nullable;
    protected final boolean allowExpressions;

    public ModelTypeValidator(final ModelType type) {
        this(type, false, false);
    }

    public ModelTypeValidator(final ModelType type, final boolean nullable) {
        this(type, nullable, false);
    }

    public ModelTypeValidator(final ModelType type, final boolean nullable, final boolean allowExpressions) {
        this.type = type;
        this.nullable = nullable;
        this.allowExpressions = allowExpressions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String validateParameter(String parameterName, ModelNode value) {
        if (!value.isDefined()) {
            if (!nullable) {
                return "Parameter " + parameterName + " may not be null "; //TODO i18n
            }
        } else {
            if (value.getType() != type && (!allowExpressions || value.getType() != ModelType.EXPRESSION)) {
                return "Wrong type for " + parameterName + ". Expected " + type + " but was " + value.getType(); //TODO i18n
            }
        }
        return null;
    }

}
