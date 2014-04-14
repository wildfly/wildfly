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

package org.jboss.as.domain.management.audit;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.audit.ManagedAuditLogger;
import org.jboss.as.controller.audit.spi.CustomAuditLogEventFormatter;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.domain.management._private.DomainManagementResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;

public class CustomAuditLogFormatterPropertyResourceDefinition extends SimpleResourceDefinition {
    public static final SimpleAttributeDefinition VALUE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.VALUE, ModelType.STRING, true)
        .setAllowNull(false)
        .setAllowExpression(true)
        .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();

    private static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[]{VALUE};

    private final ManagedAuditLogger auditLogger;

    public CustomAuditLogFormatterPropertyResourceDefinition(ManagedAuditLogger auditLogger) {
        super(PathElement.pathElement(ModelDescriptionConstants.PROPERTY),
                DomainManagementResolver.getResolver("core.management.custom-formatter.property"),
                new CustomAuditLogFormatterPropertyAddHandler(auditLogger, ATTRIBUTES),
                new CustomAuditLogFormatterPropertyRemoveHandler(auditLogger),
                OperationEntry.Flag.RESTART_RESOURCE_SERVICES,
                OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
        this.auditLogger = auditLogger;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        CustomAuditLogFormatterPropertyWriteAttributeHandler handler = new CustomAuditLogFormatterPropertyWriteAttributeHandler(auditLogger, ATTRIBUTES);
        for (AttributeDefinition def : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(def, null, handler);
        }
    }

    private static CustomAuditLogEventFormatter getFormatter(ManagedAuditLogger auditLogger, ModelNode operation){
        PathAddress address = CustomAuditLogFormatterResourceDefinition.getFormatterAddress(operation);
        return auditLogger.getFormatter(CustomAuditLogEventFormatter.class, address.getLastElement().getValue());
    }

    private static class CustomAuditLogFormatterPropertyAddHandler extends AbstractAddStepHandler {
        private final ManagedAuditLogger auditLogger;

        private CustomAuditLogFormatterPropertyAddHandler(ManagedAuditLogger auditLogger, AttributeDefinition... attributes) {
            super(attributes);
            this.auditLogger = auditLogger;
        }

        @Override
        protected boolean requiresRuntime(OperationContext context) {
            return true;
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model,
                ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
                throws OperationFailedException {
            CustomAuditLogEventFormatter formatter = getFormatter(auditLogger, operation);
            String name = Util.getNameFromAddress(operation.require(OP_ADDR));
            String value = VALUE.resolveModelAttribute(context, model).asString();
            formatter.addProperty(name, value);
        }

        @Override
        protected void rollbackRuntime(OperationContext context, ModelNode operation, ModelNode model, List<ServiceController<?>> controllers) {
            CustomAuditLogEventFormatter formatter = getFormatter(auditLogger, operation);
            String name = Util.getNameFromAddress(operation.require(OP_ADDR));
            formatter.deleteProperty(name);
        }
    }

    private static class CustomAuditLogFormatterPropertyRemoveHandler extends AbstractRemoveStepHandler {
        private final ManagedAuditLogger auditLogger;

        private CustomAuditLogFormatterPropertyRemoveHandler(ManagedAuditLogger auditLogger) {
            this.auditLogger = auditLogger;
        }

        @Override
        protected boolean requiresRuntime(OperationContext context){
            return true;
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model)
                throws OperationFailedException {
            CustomAuditLogEventFormatter formatter = getFormatter(auditLogger, operation);
            String name = Util.getNameFromAddress(operation.require(OP_ADDR));
            formatter.deleteProperty(name);
        }

        @Override
        protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model)
                throws OperationFailedException {
            CustomAuditLogEventFormatter formatter = getFormatter(auditLogger, operation);
            String name = Util.getNameFromAddress(operation.require(OP_ADDR));
            String value = VALUE.resolveModelAttribute(context, model).asString();
            formatter.updateProperty(name, value);
        }
    }

    private static class CustomAuditLogFormatterPropertyWriteAttributeHandler extends AbstractWriteAttributeHandler<String> {
        private final ManagedAuditLogger auditLogger;

        public CustomAuditLogFormatterPropertyWriteAttributeHandler(ManagedAuditLogger auditLogger, AttributeDefinition... definitions) {
            super(definitions);
            this.auditLogger = auditLogger;
        }

        @Override
        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<String> handbackHolder) throws OperationFailedException {
            CustomAuditLogEventFormatter formatter = getFormatter(auditLogger, operation);
            String name = Util.getNameFromAddress(operation.require(OP_ADDR));
            String value = resolvedValue.asString();
            String old = formatter.updateProperty(name, value);
            handbackHolder.setHandback(value);
            return false;
        }

        @Override
        protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                ModelNode valueToRestore, ModelNode valueToRevert, String handback) throws OperationFailedException {
            CustomAuditLogEventFormatter formatter = getFormatter(auditLogger, operation);
            String name = Util.getNameFromAddress(operation.require(OP_ADDR));
            String old = formatter.updateProperty(name, handback);
        }


    }
}