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

import java.util.Arrays;
import java.util.List;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.audit.AuditLogger;
import org.jboss.as.controller.audit.ManagedAuditLogger;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.management._private.DomainManagementResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for the management audit logging resource.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class AuditLogLoggerResourceDefinition extends SimpleResourceDefinition {

    public static final PathElement PATH_ELEMENT = PathElement.pathElement(ModelDescriptionConstants.LOGGER, ModelDescriptionConstants.AUDIT_LOG);

    public static final PathElement HOST_SERVER_PATH_ELEMENT = PathElement.pathElement(ModelDescriptionConstants.SERVER_LOGGER, ModelDescriptionConstants.AUDIT_LOG);

    public static final SimpleAttributeDefinition LOG_BOOT = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.LOG_BOOT, ModelType.BOOLEAN, true)
        .setAllowExpression(true)
        .setDefaultValue(new ModelNode(true)).build();


    public static final SimpleAttributeDefinition LOG_READ_ONLY = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.LOG_READ_ONLY, ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(false)).build();

    public static final SimpleAttributeDefinition ENABLED = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.ENABLED, ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(true)).build();

    static final List<SimpleAttributeDefinition> ATTRIBUTE_DEFINITIONS = Arrays.asList(LOG_BOOT, LOG_READ_ONLY, ENABLED);

    private final boolean executeRuntime;
    private final ManagedAuditLogger auditLogger;

    private AuditLogLoggerResourceDefinition(final PathElement pathElement, final ManagedAuditLogger auditLogger, boolean executeRuntime) {
        super(pathElement, DomainManagementResolver.getResolver("core.management.audit-log"),
                new AuditLogLoggerAddHandler(auditLogger, executeRuntime), new AuditLogLoggerRemoveHandler(auditLogger));
        this.auditLogger = auditLogger;
        this.executeRuntime = executeRuntime;
    }

    static AuditLogLoggerResourceDefinition createDefinition(ManagedAuditLogger auditLogger){
        return new AuditLogLoggerResourceDefinition(PATH_ELEMENT, auditLogger, true);
    }

    static AuditLogLoggerResourceDefinition createHostServerDefinition(ManagedAuditLogger auditLogger){
        return new AuditLogLoggerResourceDefinition(HOST_SERVER_PATH_ELEMENT, auditLogger, false);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        //This one only takes effect at boot
        resourceRegistration.registerReadWriteAttribute(LOG_BOOT, null, new ModelOnlyWriteAttributeHandler(LOG_BOOT));

        resourceRegistration.registerReadWriteAttribute(LOG_READ_ONLY, null, new AuditLogReadOnlyWriteAttributeHandler(auditLogger));
        resourceRegistration.registerReadWriteAttribute(ENABLED, null, new AuditLogEnabledWriteAttributeHandler(auditLogger, executeRuntime));
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new AuditLogHandlerReferenceResourceDefinition(auditLogger, executeRuntime));
    }


    public static void createServerAddOperations(List<ModelNode> addOps, PathAddress loggerAddress, ModelNode logger) {
        addOps.add(createLoggerAddOperation(loggerAddress, logger));

        final String handler = AuditLogHandlerReferenceResourceDefinition.PATH_ELEMENT.getKey();
        if (logger.hasDefined(handler)){
            for (Property prop : logger.get(handler).asPropertyList()) {
                addOps.add(Util.createAddOperation(loggerAddress.append(PathElement.pathElement(handler, prop.getName()))));
            }
        }
    }

    public static ModelNode createLoggerAddOperation(PathAddress loggerAddress, ModelNode logger){
        ModelNode addOp = Util.createAddOperation(loggerAddress);
        for (AttributeDefinition def : ATTRIBUTE_DEFINITIONS){
            addOp.get(def.getName()).set(logger.get(def.getName()));
        }
        return addOp;
    }

    private static class AuditLogLoggerAddHandler implements OperationStepHandler {

        private final ManagedAuditLogger auditLoggerProvider;
        private final boolean executeRuntime;

        AuditLogLoggerAddHandler(ManagedAuditLogger auditLoggerProvider, boolean executeRuntime) {
            this.auditLoggerProvider = auditLoggerProvider;
            this.executeRuntime = executeRuntime;
        }

        /** {@inheritDoc */
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            final Resource resource = context.createResource(PathAddress.EMPTY_ADDRESS);
            final ModelNode model = resource.getModel();
            for (AttributeDefinition attr : AuditLogLoggerResourceDefinition.ATTRIBUTE_DEFINITIONS) {
                attr.validateAndSet(operation, model);
            }

            if (executeRuntime) {
                context.addStep(new OperationStepHandler() {
                    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                        final boolean wasReadOnly = auditLoggerProvider.isLogReadOnly();
                        final AuditLogger.Status oldStatus = auditLoggerProvider.getLoggerStatus();

                        auditLoggerProvider.setLogBoot(AuditLogLoggerResourceDefinition.LOG_BOOT.resolveModelAttribute(context, model).asBoolean());
                        auditLoggerProvider.setLogReadOnly(AuditLogLoggerResourceDefinition.LOG_READ_ONLY.resolveModelAttribute(context, model).asBoolean());
                        boolean enabled = AuditLogLoggerResourceDefinition.ENABLED.resolveModelAttribute(context, model).asBoolean();
                        final AuditLogger.Status status = enabled ? AuditLogger.Status.LOGGING : AuditLogger.Status.DISABLED;
                        // Change the logger status in a new step to give any subsequent handler adds a chance
                        // to run before we flush any queued up log records
                        context.addStep(new OperationStepHandler() {
                            @Override
                            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                                auditLoggerProvider.setLoggerStatus(status);
                                context.completeStep(new OperationContext.RollbackHandler() {
                                    @Override
                                    public void handleRollback(OperationContext context, ModelNode operation) {
                                        auditLoggerProvider.setLoggerStatus(oldStatus);
                                    }
                                });
                            }
                        }, OperationContext.Stage.RUNTIME);


                        context.completeStep(new OperationContext.RollbackHandler() {
                            @Override
                            public void handleRollback(OperationContext context, ModelNode operation) {
                                auditLoggerProvider.setLogReadOnly(wasReadOnly);
                            }
                        });
                    }
                }, OperationContext.Stage.RUNTIME);
            } else {
                auditLoggerProvider.setLoggerStatus(AuditLogger.Status.DISABLED);
            }
            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
        }
    }

    private static class AuditLogLoggerRemoveHandler implements OperationStepHandler {

        private final ManagedAuditLogger auditLogger;

        AuditLogLoggerRemoveHandler(ManagedAuditLogger auditLogger) {
            this.auditLogger = auditLogger;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

            context.removeResource(PathAddress.EMPTY_ADDRESS);

            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

                    final boolean wasReadOnly = auditLogger.isLogReadOnly();
                    final AuditLogger.Status oldStatus = auditLogger.getLoggerStatus();

                    auditLogger.setLoggerStatus(AuditLogger.Status.DISABLE_NEXT);

                    context.completeStep(new OperationContext.RollbackHandler() {
                        @Override
                        public void handleRollback(OperationContext context, ModelNode operation) {
                            auditLogger.setLogReadOnly(wasReadOnly);
                            auditLogger.setLoggerStatus(oldStatus);
                        }
                    });
                }
            }, OperationContext.Stage.RUNTIME);

            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
        }
    }

    private static class AuditLogEnabledWriteAttributeHandler extends AbstractWriteAttributeHandler<ManagedAuditLogger.Status> {

        private final ManagedAuditLogger auditLogger;
        private final boolean executeRuntime;

        AuditLogEnabledWriteAttributeHandler(ManagedAuditLogger auditLogger, boolean executeRuntime) {
            super(AuditLogLoggerResourceDefinition.ENABLED);
            this.auditLogger = auditLogger;
            this.executeRuntime = executeRuntime;
        }

        @Override
        protected boolean requiresRuntime(OperationContext context) {
            return executeRuntime;
        }

        @Override
        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                               ModelNode resolvedValue, ModelNode currentValue,
                                               HandbackHolder<ManagedAuditLogger.Status> handbackHolder) throws OperationFailedException {
            handbackHolder.setHandback(auditLogger.getLoggerStatus());
            boolean enabled = resolvedValue.asBoolean();
            ManagedAuditLogger.Status status = enabled ? AuditLogger.Status.LOGGING : AuditLogger.Status.DISABLE_NEXT;
            auditLogger.setLoggerStatus(status);
            return false;
        }

        @Override
        protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                             ModelNode valueToRestore, ModelNode valueToRevert, ManagedAuditLogger.Status handback) throws OperationFailedException {
            auditLogger.setLoggerStatus(handback);
        }
    }

    class AuditLogReadOnlyWriteAttributeHandler extends AbstractWriteAttributeHandler<Boolean> {

        private final ManagedAuditLogger auditLogger;

        public AuditLogReadOnlyWriteAttributeHandler(ManagedAuditLogger auditLogger) {
            super(AuditLogLoggerResourceDefinition.LOG_READ_ONLY);
            this.auditLogger = auditLogger;
        }

        @Override
        protected boolean requiresRuntime(OperationContext context) {
            return executeRuntime;
        }

        @Override
        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                               ModelNode resolvedValue, ModelNode currentValue,
                                               HandbackHolder<Boolean> handbackHolder) throws OperationFailedException {
            handbackHolder.setHandback(auditLogger.isLogReadOnly());
            auditLogger.setLogReadOnly(resolvedValue.asBoolean());
            return false;
        }

        @Override
        protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Boolean handback) throws OperationFailedException {
            auditLogger.setLogReadOnly(handback);
        }
    }

}
