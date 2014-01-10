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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.audit.ManagedAuditLogger;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.domain.management.DomainManagementMessages;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class AuditLogHandlerResourceDefinition extends SimpleResourceDefinition {

    public static final SimpleAttributeDefinition FORMATTER = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.FORMATTER, ModelType.STRING)
        .setAllowNull(false)
        .setMinSize(1)
        .build();

    public static final SimpleAttributeDefinition MAX_FAILURE_COUNT = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.MAX_FAILURE_COUNT, ModelType.INT)
        .setAllowNull(true)
        .setAllowExpression(true)
        .setDefaultValue(new ModelNode(10))
        .setValidator(new IntRangeValidator(0, true, true))
        .build();

    public static final SimpleAttributeDefinition FAILURE_COUNT = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.FAILURE_COUNT, ModelType.INT)
        .setAllowNull(false)
        .setStorageRuntime()
        .build();

    public static final SimpleAttributeDefinition DISABLED_DUE_TO_FAILURE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.DISABLED_DUE_TO_FAILURE, ModelType.BOOLEAN)
        .setAllowNull(false)
        .setStorageRuntime()
        .build();


    private static final AttributeDefinition[] RUNTIME_ATTRIBUTES = new AttributeDefinition[] {FAILURE_COUNT, DISABLED_DUE_TO_FAILURE};

    protected final ManagedAuditLogger auditLogger;
    protected final PathManagerService pathManager;


    AuditLogHandlerResourceDefinition(ManagedAuditLogger auditLogger, PathManagerService pathManager, PathElement pathElement, ResourceDescriptionResolver descriptionResolver,
            OperationStepHandler addHandler, OperationStepHandler removeHandler) {
        super(pathElement, descriptionResolver, addHandler, removeHandler);
        this.auditLogger = auditLogger;
        this.pathManager = pathManager;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition def : RUNTIME_ATTRIBUTES) {
            resourceRegistration.registerReadOnlyAttribute(def, new HandlerRuntimeAttributeHandler(auditLogger));
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(
                new SimpleOperationDefinitionBuilder(
                        ModelDescriptionConstants.RECYCLE, getResourceDescriptionResolver())
                            .setRuntimeOnly().
                            build(),
                new HandlerRecycleHandler(auditLogger));
    }

    private static class HandlerRuntimeAttributeHandler extends AbstractRuntimeOnlyHandler {
        private final ManagedAuditLogger auditLogger;

        public HandlerRuntimeAttributeHandler(ManagedAuditLogger auditLogger) {
            this.auditLogger = auditLogger;
        }

        @Override
        protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
            String attr = operation.require(ModelDescriptionConstants.NAME).asString();
            String handlerName = Util.getNameFromAddress(operation.require(OP_ADDR));
            if (attr.equals(FAILURE_COUNT.getName())) {
                context.getResult().set(auditLogger.getHandlerFailureCount(handlerName));
            } else if (attr.equals(DISABLED_DUE_TO_FAILURE.getName())) {
                context.getResult().set(auditLogger.getHandlerDisabledDueToFailure(handlerName));
            }
            context.stepCompleted();
        }
    }

    private static class HandlerRecycleHandler extends AbstractRuntimeOnlyHandler {
        private final ManagedAuditLogger auditLogger;

        public HandlerRecycleHandler(ManagedAuditLogger auditLogger) {
            this.auditLogger = auditLogger;
        }

        @Override
        protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
            auditLogger.recycleHandler(Util.getNameFromAddress(operation.require(OP_ADDR)));
            context.stepCompleted();
        }
    }

    static class HandlerRemoveHandler extends AbstractRemoveStepHandler {
        private final ManagedAuditLogger auditLogger;

        public HandlerRemoveHandler(ManagedAuditLogger auditLogger) {
            this.auditLogger = auditLogger;
        }

        @Override
        protected boolean requiresRuntime(OperationContext context){
            return true;
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            auditLogger.getUpdater().removeHandler(Util.getNameFromAddress(operation.require(OP_ADDR)));
        }

        @Override
        protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            auditLogger.getUpdater().rollbackChanges();
        }
    }

    abstract static class HandlerWriteAttributeHandler extends AbstractWriteAttributeHandler<Void> {
        final ManagedAuditLogger auditLogger;
        final PathManagerService pathManager;

        public HandlerWriteAttributeHandler(ManagedAuditLogger auditLogger, PathManagerService pathManager, AttributeDefinition... attributeDefinitions) {
            super(attributeDefinitions);
            this.auditLogger = auditLogger;
            this.pathManager = pathManager;
        }

        @Override
        protected void finishModelStage(OperationContext context, ModelNode operation, String attributeName, ModelNode newValue,
                ModelNode oldValue, Resource model) throws OperationFailedException {
            if (attributeName.equals(FORMATTER.getName())) {
                String formatterName = newValue.asString();
                if (!HandlerUtil.lookForFormatter(context, PathAddress.pathAddress(operation.require(OP_ADDR)), formatterName)) {
                    throw DomainManagementMessages.MESSAGES.noFormatterCalled(formatterName);
                }
            }
        }

        boolean handleApplyAttributeRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {
            if (attributeName.equals(FORMATTER.getName())) {
                auditLogger.updateHandlerFormatter(Util.getNameFromAddress(operation.require(OP_ADDR)), resolvedValue.asString());
                return true;
            } else if (attributeName.equals(MAX_FAILURE_COUNT.getName())) {
                auditLogger.updateHandlerMaxFailureCount(Util.getNameFromAddress(operation.require(OP_ADDR)), resolvedValue.asInt());
                return true;
            }
            return false;
        }


        boolean handlerRevertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
            if (attributeName.equals(FORMATTER.getName())) {
                auditLogger.updateHandlerFormatter(Util.getNameFromAddress(operation.require(OP_ADDR)), valueToRestore.asString());
                return true;
            } else if (attributeName.equals(MAX_FAILURE_COUNT.getName())) {
                auditLogger.updateHandlerMaxFailureCount(Util.getNameFromAddress(operation.require(OP_ADDR)), valueToRestore.asInt());
                return true;
            }
            return false;
        }

    }

}
