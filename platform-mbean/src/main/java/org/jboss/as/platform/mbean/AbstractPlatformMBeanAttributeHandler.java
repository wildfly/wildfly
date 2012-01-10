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

package org.jboss.as.platform.mbean;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * Base class for handlers for reading and writing platform mbean attributes.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
abstract class AbstractPlatformMBeanAttributeHandler implements OperationStepHandler {

    protected final ParametersValidator readAttributeValidator = new ParametersValidator();
    protected final ParametersValidator writeAttributeValidator = new ParametersValidator();

    protected AbstractPlatformMBeanAttributeHandler() {
        readAttributeValidator.registerValidator(NAME, new StringLengthValidator(1));
        writeAttributeValidator.registerValidator(NAME, new StringLengthValidator(1));
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        String op = operation.require(OP).asString();
        if (READ_ATTRIBUTE_OPERATION.equals(op)) {
            readAttributeValidator.validate(operation);
            executeReadAttribute(context, operation);
        } else if (WRITE_ATTRIBUTE_OPERATION.equals(op)) {
            writeAttributeValidator.validate(operation);
            executeWriteAttribute(context, operation);
        }

        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    protected abstract void executeReadAttribute (OperationContext context, ModelNode operation) throws OperationFailedException;

    protected abstract void executeWriteAttribute (OperationContext context, ModelNode operation) throws OperationFailedException;

    protected abstract void register(ManagementResourceRegistration registration);

    protected static OperationFailedException unknownAttribute(final ModelNode operation) {
        return PlatformMBeanMessages.MESSAGES.unknownAttribute(operation.require(NAME).asString());
    }
}
