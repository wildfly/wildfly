/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ee.subsystem;

import org.glassfish.enterprise.concurrent.AbstractManagedExecutorService;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.ee.logging.EeLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Eduardo Martins
 */
public class ManagedExecutorServiceResourceDefinition extends SimpleResourceDefinition {

    public static final String JNDI_NAME = "jndi-name";
    public static final String CONTEXT_SERVICE = "context-service";
    public static final String THREAD_FACTORY = "thread-factory";
    public static final String HUNG_TASK_THRESHOLD = "hung-task-threshold";
    public static final String LONG_RUNNING_TASKS = "long-running-tasks";
    public static final String CORE_THREADS = "core-threads";
    public static final String MAX_THREADS = "max-threads";
    public static final String KEEPALIVE_TIME = "keepalive-time";
    public static final String QUEUE_LENGTH = "queue-length";
    public static final String REJECT_POLICY = "reject-policy";

    public static final SimpleAttributeDefinition JNDI_NAME_AD =
            new SimpleAttributeDefinitionBuilder(JNDI_NAME, ModelType.STRING, false)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition CONTEXT_SERVICE_AD =
            new SimpleAttributeDefinitionBuilder(CONTEXT_SERVICE, ModelType.STRING, true)
                    .setAllowExpression(false)
                    .setValidator(new StringLengthValidator(0, true))
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition THREAD_FACTORY_AD =
            new SimpleAttributeDefinitionBuilder(THREAD_FACTORY, ModelType.STRING, true)
                    .setAllowExpression(false)
                    .setValidator(new StringLengthValidator(0, true))
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition HUNG_TASK_THRESHOLD_AD =
            new SimpleAttributeDefinitionBuilder(HUNG_TASK_THRESHOLD, ModelType.LONG, true)
                    .setAllowExpression(true)
                    .setValidator(new LongRangeValidator(0, Long.MAX_VALUE, true, true))
                    .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
                    .setDefaultValue(new ModelNode(0))
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition LONG_RUNNING_TASKS_AD =
            new SimpleAttributeDefinitionBuilder(LONG_RUNNING_TASKS, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition CORE_THREADS_AD =
            new SimpleAttributeDefinitionBuilder(CORE_THREADS, ModelType.INT, true)
                    .setAllowExpression(true)
                    .setValidator(new IntRangeValidator(0, Integer.MAX_VALUE, true, true))
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition MAX_THREADS_AD =
            new SimpleAttributeDefinitionBuilder(MAX_THREADS, ModelType.INT, true)
                    .setAllowExpression(true)
                    .setValidator(new IntRangeValidator(0, Integer.MAX_VALUE, true, true))
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition KEEPALIVE_TIME_AD =
            new SimpleAttributeDefinitionBuilder(KEEPALIVE_TIME, ModelType.LONG, true)
                    .setAllowExpression(true)
                    .setValidator(new LongRangeValidator(0, Long.MAX_VALUE, true, true))
                    .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setDefaultValue(new ModelNode(60000))
                    .build();

    public static final SimpleAttributeDefinition QUEUE_LENGTH_AD =
            new SimpleAttributeDefinitionBuilder(QUEUE_LENGTH, ModelType.INT, true)
                    .setAllowExpression(true)
                    .setValidator(new IntRangeValidator(0, Integer.MAX_VALUE, true, true))
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition REJECT_POLICY_AD =
            new SimpleAttributeDefinitionBuilder(REJECT_POLICY, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setDefaultValue(new ModelNode(AbstractManagedExecutorService.RejectPolicy.ABORT.toString()))
                    .setValidator(EnumValidator.create(AbstractManagedExecutorService.RejectPolicy.class, true, true))
                    .build();


    static final SimpleAttributeDefinition[] ATTRIBUTES = {JNDI_NAME_AD, CONTEXT_SERVICE_AD, THREAD_FACTORY_AD, HUNG_TASK_THRESHOLD_AD, LONG_RUNNING_TASKS_AD, CORE_THREADS_AD, MAX_THREADS_AD, KEEPALIVE_TIME_AD, QUEUE_LENGTH_AD, REJECT_POLICY_AD};

    public static final ManagedExecutorServiceResourceDefinition INSTANCE = new ManagedExecutorServiceResourceDefinition();

    private ManagedExecutorServiceResourceDefinition() {
        super(PathElement.pathElement(EESubsystemModel.MANAGED_EXECUTOR_SERVICE), EeExtension.getResourceDescriptionResolver(EESubsystemModel.MANAGED_EXECUTOR_SERVICE), ManagedExecutorServiceAdd.INSTANCE, ManagedExecutorServiceRemove.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        OperationStepHandler writeHandler = new ValidatingWriteHandler(ATTRIBUTES);
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, writeHandler);
        }
    }

    void registerTransformers_4_0(final ResourceTransformationDescriptionBuilder builder) {
        final PathElement pathElement = getPathElement();
        final ResourceTransformationDescriptionBuilder resourceBuilder = builder.addChildResource(pathElement);
        resourceBuilder.getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.UNDEFINED, CORE_THREADS_AD)
                .end();
    }

    static class ValidatingWriteHandler extends ReloadRequiredWriteAttributeHandler {
        public ValidatingWriteHandler(final AttributeDefinition... definitions) {
            super(definitions);
        }

        @Override
        protected void validateUpdatedModel(final OperationContext context, final Resource model) throws OperationFailedException {
            context.addStep(ExecutorQueueValidationStepHandler.MODEL_VALIDATION_INSTANCE, OperationContext.Stage.MODEL);
            super.validateUpdatedModel(context, model);
        }
    }

    static class ExecutorQueueValidationStepHandler implements OperationStepHandler {
        static final ExecutorQueueValidationStepHandler MODEL_VALIDATION_INSTANCE = new ExecutorQueueValidationStepHandler(false);
        private final boolean isRuntimeStage;

        private ExecutorQueueValidationStepHandler(final boolean isRuntimeStage) {
            this.isRuntimeStage = isRuntimeStage;
        }

        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();

            final ModelNode coreThreads;
            final ModelNode maxThreads;
            final ModelNode queueLength;

            if (isRuntimeStage) {
                coreThreads = CORE_THREADS_AD.resolveModelAttribute(context, model);
                maxThreads = MAX_THREADS_AD.resolveModelAttribute(context, model);
                queueLength = QUEUE_LENGTH_AD.resolveModelAttribute(context, model);
            } else {
                coreThreads = model.get(CORE_THREADS);
                maxThreads = model.get(MAX_THREADS);
                queueLength = model.get(QUEUE_LENGTH);
            }

            if (coreThreads.getType() == ModelType.EXPRESSION || maxThreads.getType() == ModelType.EXPRESSION ||
                    queueLength.getType() == ModelType.EXPRESSION) {
                context.addStep(new ExecutorQueueValidationStepHandler(true), OperationContext.Stage.RUNTIME, true);
                return;
            }

            // Validate an unbounded queue
            if (!queueLength.isDefined() || queueLength.asInt() == Integer.MAX_VALUE) {
                if (coreThreads.isDefined() && coreThreads.asInt() <= 0) {
                    throw EeLogger.ROOT_LOGGER.invalidCoreThreadsSize(queueLength.asString());
                }

            }

            // Validate a hand-off queue
            if (queueLength.isDefined() && queueLength.asInt() == 0) {
                if (coreThreads.isDefined() && coreThreads.asInt() <= 0) {
                    throw EeLogger.ROOT_LOGGER.invalidCoreThreadsSize(queueLength.asString());
                }
            }

            // max-threads must be defined and greater than 0 if core-threads is 0
            if (coreThreads.isDefined() && coreThreads.asInt() == 0) {
                if (!maxThreads.isDefined() || maxThreads.asInt() <= 0) {
                    throw EeLogger.ROOT_LOGGER.invalidMaxThreads(maxThreads.isDefined() ? maxThreads.asInt() : 0, coreThreads.asInt());
                }
            }

            // max-threads must be greater than or equal to core-threads
            if (coreThreads.isDefined() && maxThreads.isDefined()) {
                if (maxThreads.asInt() < coreThreads.asInt()) {
                    throw EeLogger.ROOT_LOGGER.invalidMaxThreads(maxThreads.asInt(), coreThreads.asInt());
                }
            }
        }
    }
}
