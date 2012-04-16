/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
import org.jboss.dmr.ModelType;


/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class PropertyValidator extends ModelTypeValidator {

    private final ParameterValidator valueValidator;

    public PropertyValidator(boolean nullable, ParameterValidator valueValidator) {
        super(ModelType.PROPERTY, nullable);
        this.valueValidator = valueValidator;
    }

    public void validateParameter(final String parameterName, final ModelNode value) throws OperationFailedException {
        super.validateParameter(parameterName, value);
        if (value.isDefined()) {
            if (value.asProperty().getName().length() < 1) {
                throw new OperationFailedException(new ModelNode().set(MESSAGES.invalidMinLength(value.asProperty().getName(), parameterName, 1)));
            }
            if (valueValidator != null) {
                valueValidator.validateParameter(parameterName, value.asProperty().getValue());
            }
        }
    }
}
