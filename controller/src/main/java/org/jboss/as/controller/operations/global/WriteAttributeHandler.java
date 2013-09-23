/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.operations.global;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.operations.global.GlobalOperationAttributes.NAME;
import static org.jboss.as.controller.operations.global.GlobalOperationAttributes.VALUE;

import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.access.AuthorizationResult;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * {@link org.jboss.as.controller.OperationStepHandler} writing a single attribute. The required request parameter "name" represents the attribute name.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class WriteAttributeHandler implements OperationStepHandler {

    public static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION, ControllerResolver.getResolver("global"))
            .setParameters(NAME, VALUE)
            .setRuntimeOnly()
            .build();

    public static final OperationStepHandler INSTANCE = new WriteAttributeHandler();

    private ParametersValidator nameValidator = new ParametersValidator();

    WriteAttributeHandler() {
        nameValidator.registerValidator(NAME.getName(), new StringLengthValidator(1));
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        nameValidator.validate(operation);
        final String attributeName = operation.require(NAME.getName()).asString();
        final AttributeAccess attributeAccess = context.getResourceRegistration().getAttributeAccess(PathAddress.EMPTY_ADDRESS, attributeName);
        if (attributeAccess == null) {
            throw new OperationFailedException(new ModelNode().set(MESSAGES.unknownAttribute(attributeName)));
        } else if (attributeAccess.getAccessType() != AttributeAccess.AccessType.READ_WRITE) {
            throw new OperationFailedException(new ModelNode().set(MESSAGES.attributeNotWritable(attributeName)));
        } else {

            // Authorize
            ModelNode currentValue;
            if (attributeAccess.getStorageType() == AttributeAccess.Storage.CONFIGURATION) {
                ModelNode model = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();
                currentValue = model.has(attributeName) ? model.get(attributeName) : new ModelNode();
            } else {
                currentValue = new ModelNode();
            }
            AuthorizationResult authorizationResult = context.authorize(operation, attributeName, currentValue);
            if (authorizationResult.getDecision() == AuthorizationResult.Decision.DENY) {
                throw ControllerMessages.MESSAGES.unauthorized(operation.require(OP).asString(),
                        PathAddress.pathAddress(operation.get(OP_ADDR)),
                        authorizationResult.getExplanation());
            }

            OperationStepHandler handler = attributeAccess.getWriteHandler();
            ClassLoader oldTccl = WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(handler.getClass());
            try {
                handler.execute(context, operation);
            } finally {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(oldTccl);
            }
        }
    }
}
