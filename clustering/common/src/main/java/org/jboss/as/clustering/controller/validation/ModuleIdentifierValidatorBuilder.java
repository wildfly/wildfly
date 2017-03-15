/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link ParameterValidatorBuilder} that builds a validator that validates that a given value is a valid {@link ModuleIdentifier}.
 * @author Paul Ferraro
 */
public class ModuleIdentifierValidatorBuilder extends AbstractParameterValidatorBuilder {

    @Override
    public ParameterValidator build() {
        return new ModuleIdentifierValidator(this.allowsUndefined, this.allowsExpressions);
    }

    private static class ModuleIdentifierValidator extends ModelTypeValidator {

        ModuleIdentifierValidator(boolean allowsUndefined, boolean allowsExpression) {
            super(ModelType.STRING, allowsUndefined, allowsExpression);
        }

        @SuppressWarnings("deprecation")
        @Override
        public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
            super.validateParameter(parameterName, value);
            if (value.isDefined()) {
                String module = value.asString();
                try {
                    org.jboss.modules.ModuleIdentifier.fromString(module);
                } catch (IllegalArgumentException e) {
                    throw new OperationFailedException(e.getMessage() + ": " + module, e);
                }
            }
        }
    }
}
