/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.management.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACTIVE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.core.security.AccessMechanism;
import org.jboss.as.domain.management._private.DomainManagementResolver;
import org.jboss.dmr.ModelType;

/**
 * {@code ResourceDefinition} for a currently executing operation.
 *
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
public class ActiveOperationResourceDefinition extends SimpleResourceDefinition {

    public static final PathElement PATH_ELEMENT = PathElement.pathElement(ACTIVE_OPERATION);

    static final ResourceDefinition INSTANCE = new ActiveOperationResourceDefinition();

    static final AttributeDefinition OPERATION_NAME =
            SimpleAttributeDefinitionBuilder.create(OP, ModelType.STRING).build();
    static final AttributeDefinition ADDRESS =
            PrimitiveListAttributeDefinition.Builder.of(OP_ADDR, ModelType.PROPERTY)
                    .build();
    private static final AttributeDefinition CALLER_THREAD =
            SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.CALLER_THREAD, ModelType.STRING).build();
    private static final AttributeDefinition ACCESS_MECHANISM =
            SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.ACCESS_MECHANISM, ModelType.STRING)
                    .setAllowNull(true)
                    .setValidator(EnumValidator.create(AccessMechanism.class, true, false))
                    .build();
    private static final AttributeDefinition EXECUTION_STATUS =
            SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.EXECUTION_STATUS, ModelType.STRING)
                    .setValidator(EnumValidator.create(OperationContext.ExecutionStatus.class, false, false))
                    .build();
    private static final AttributeDefinition CANCELLED =
            SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.CANCELLED, ModelType.BOOLEAN).build();

    private ActiveOperationResourceDefinition() {
        super(PATH_ELEMENT, DomainManagementResolver.getResolver(CORE, MANAGEMENT_OPERATIONS, ACTIVE_OPERATION));
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);

        resourceRegistration.registerOperationHandler(CancelActiveOperationHandler.DEFINITION, CancelActiveOperationHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        resourceRegistration.registerReadOnlyAttribute(OPERATION_NAME, SecureOperationReadHandler.INSTANCE);
        resourceRegistration.registerReadOnlyAttribute(ADDRESS, SecureOperationReadHandler.INSTANCE);
        resourceRegistration.registerReadOnlyAttribute(CALLER_THREAD, null);
        resourceRegistration.registerReadOnlyAttribute(ACCESS_MECHANISM, null);
        resourceRegistration.registerReadOnlyAttribute(EXECUTION_STATUS, null);
        resourceRegistration.registerReadOnlyAttribute(CANCELLED, null);

        // HACK -- workaround WFLY-3057
        resourceRegistration.setRuntimeOnly(true);
    }
}
