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
package org.jboss.as.domain.management.audit;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FILE_HANDLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.audit.FileAuditLogHandler;
import org.jboss.as.controller.audit.ManagedAuditLogger;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.domain.management.DomainManagementMessages;
import org.jboss.as.domain.management._private.DomainManagementResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class FileAuditLogHandlerResourceDefinition extends AuditLogHandlerResourceDefinition {

    public static final SimpleAttributeDefinition FORMATTER = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.FORMATTER, ModelType.STRING)
        .setAllowNull(false)
        .setMinSize(1)
        .build();

    public static final SimpleAttributeDefinition PATH = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.PATH, ModelType.STRING)
        .setAllowNull(false)
        .setAllowExpression(true)
        .setMinSize(1)
        .build();

    public static final SimpleAttributeDefinition RELATIVE_TO = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.RELATIVE_TO, ModelType.STRING)
        .setAllowNull(true)
        .setMinSize(1)
        .build();

    private static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[]{FORMATTER, PATH, RELATIVE_TO, MAX_FAILURE_COUNT};


    public FileAuditLogHandlerResourceDefinition(ManagedAuditLogger auditLogger, PathManagerService pathManager) {
        super(auditLogger, pathManager, PathElement.pathElement(FILE_HANDLER), DomainManagementResolver.getResolver("core.management.file-handler"),
                new FileAuditLogHandlerAddHandler(auditLogger, pathManager, ATTRIBUTES), new HandlerRemoveHandler(auditLogger));
    }

    public static ModelNode createServerAddOperation(final PathAddress address, final ModelNode fileHandler){
        ModelNode add = Util.createAddOperation(address);
        for (AttributeDefinition def : ATTRIBUTES) {
            if (fileHandler.get(def.getName()).isDefined()) {
                add.get(def.getName()).set(fileHandler.get(def.getName()));
            }
        }
        return add;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        HandlerWriteAttributeHandler write = new HandlerWriteAttributeHandler(auditLogger, pathManager, ATTRIBUTES);
        for (AttributeDefinition def : ATTRIBUTES){
            resourceRegistration.registerReadWriteAttribute(def, null, write);
        }
    }

    private static FileAuditLogHandler createHandler(final PathManagerService pathManager,
                                               final OperationContext context, final ModelNode operation) throws OperationFailedException {
        final String name = Util.getNameFromAddress(operation.require(OP_ADDR));
        final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        final String relativeTo = model.hasDefined(RELATIVE_TO.getName()) ? RELATIVE_TO.resolveModelAttribute(context, model).asString() : null;
        final String path = PATH.resolveModelAttribute(context, model).asString();
        final String formatterName = FORMATTER.resolveModelAttribute(context, model).asString();
        final int maxFailureCount = MAX_FAILURE_COUNT.resolveModelAttribute(context, model).asInt();
        return new FileAuditLogHandler(name, formatterName, maxFailureCount, pathManager, path, relativeTo);
    }

    private static class FileAuditLogHandlerAddHandler extends AbstractAddStepHandler {

        private final ManagedAuditLogger auditLogger;
        private final PathManagerService pathManager;

        private FileAuditLogHandlerAddHandler(ManagedAuditLogger auditLogger, PathManagerService pathManager, AttributeDefinition[] attributes) {
            super(attributes);
            this.auditLogger = auditLogger;
            this.pathManager = pathManager;
        }

        @Override
        protected void populateModel(OperationContext context, ModelNode operation, Resource resource)
                throws OperationFailedException {
            HandlerUtil.checkNoOtherHandlerWithTheSameName(context, operation);
            String formatterName = operation.get(FORMATTER.getName()).asString();
            if (!HandlerUtil.lookForFormatter(context, PathAddress.pathAddress(operation.require(OP_ADDR)), formatterName)) {
                throw DomainManagementMessages.MESSAGES.noFormatterCalled(formatterName);
            }
            super.populateModel(context, operation, resource);
        }

        protected boolean requiresRuntime(OperationContext context){
            return true;
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model,
                ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
                throws OperationFailedException {
            auditLogger.getUpdater().addHandler(createHandler(pathManager, context, operation));
        }

        @Override
        protected void rollbackRuntime(OperationContext context, ModelNode operation, ModelNode model,
                List<ServiceController<?>> controllers)  {
            auditLogger.getUpdater().rollbackChanges();
        }
    }

    private static class HandlerWriteAttributeHandler extends AuditLogHandlerResourceDefinition.HandlerWriteAttributeHandler {

        public HandlerWriteAttributeHandler(ManagedAuditLogger auditLogger, PathManagerService pathManager, AttributeDefinition... attributeDefinitions) {
            super(auditLogger, pathManager, attributeDefinitions);
        }

        protected boolean requiresRuntime(OperationContext context){
            return true;
        }

        @Override
        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {
            if (!super.handleApplyAttributeRuntime(context, operation, attributeName, resolvedValue, currentValue, handbackHolder)) {
                auditLogger.getUpdater().updateHandler(createHandler(pathManager, context, operation));
            }
            return false;
        }

        @Override
        protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
            if (!super.handlerRevertUpdateToRuntime(context, operation, attributeName, valueToRestore, valueToRevert, handback)) {
                auditLogger.getUpdater().rollbackChanges();
            }
        }
    }
}
