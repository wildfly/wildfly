/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.ejb3.component.AccessLogHolder;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.wildfly.extension.io.IOServices;

public class AccessLogResourceDefinition extends SimpleResourceDefinition {

    static final RuntimeCapability<Void> ACCESS_LOG_CAPABILITY =
            RuntimeCapability.Builder.of("org.wildfly.ejb3.access-log", AccessLogService.class).build();

    /** Wire-format tokens for the {@code destination} attribute; matches the switch in {@link AccessLogService}. */
    public enum Destination {
        CONSOLE("console"),
        LOGGING("logging"),
        FILE("file");

        private final String token;

        Destination(String token) {
            this.token = token;
        }

        @Override
        public String toString() {
            return token;
        }
    }

    static final SimpleAttributeDefinition DESTINATION =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DESTINATION, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(EnumValidator.create(Destination.class))
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setDefaultValue(new ModelNode(Destination.FILE.toString()))
                    .build();

    static final SimpleAttributeDefinition PATH =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.PATH, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setDefaultValue(new ModelNode("ejb-access.log"))
                    .build();

    static final SimpleAttributeDefinition RELATIVE_TO =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.RELATIVE_TO, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setDefaultValue(new ModelNode(ServerEnvironment.SERVER_LOG_DIR))
                    .build();

    static final SimpleAttributeDefinition ROTATE_SUFFIX =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.ROTATE_SUFFIX, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setDefaultValue(new ModelNode(".yyyy-MM-dd"))
                    // min=0: empty string is valid and means "no rotation" (D23).
                    // The default STRING validator enforces min=1; override it.
                    .setValidator(new StringLengthValidator(0, true, true))
                    .build();

    static final SimpleAttributeDefinition WORKER =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.WORKER, ModelType.STRING, true)
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setDefaultValue(new ModelNode("default"))
                    .setCapabilityReference(IOServices.IO_WORKER_CAPABILITY_NAME)
                    .build();

    static final SimpleAttributeDefinition INCLUDE_LOCAL =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.INCLUDE_LOCAL, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.FALSE)
                    .build();

    static final SimpleAttributeDefinition INCLUDE_NODE_NAME =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.INCLUDE_NODE_NAME, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.TRUE)
                    .build();

    public enum AttributeVocabulary {
        TIMESTAMP("timestamp"),
        APP("app"),
        MODULE("module"),
        BEAN("bean"),
        BEAN_CLASS("bean-class"),
        VIEW("view"),
        METHOD("method"),
        USER("user"),
        REMOTE_ADDRESS("remote-address"),
        REMOTE_PORT("remote-port"),
        LOCAL_ADDRESS("local-address"),
        LOCAL_PORT("local-port"),
        PROTOCOL("protocol"),
        TRANSPORT("transport"),
        INVOCATION_TYPE("invocation-type"),
        SESSION_ID("session-id"),
        OUTCOME("outcome"),
        EXCEPTION("exception"),
        DURATION("duration"),
        THREAD_NAME("thread-name"),
        NODE_NAME("node-name");

        private final String token;

        AttributeVocabulary(String token) {
            this.token = token;
        }

        public String getToken() {
            return token;
        }

        @Override
        public String toString() {
            return token;
        }
    }

    public static final StringListAttributeDefinition ATTRIBUTES = new StringListAttributeDefinition.Builder(EJB3SubsystemModel.ATTRIBUTES)
            .setAllowExpression(false)
            .setRequired(false)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setElementValidator(new EnumValidator<>(AttributeVocabulary.class, EnumSet.allOf(AttributeVocabulary.class)))
            .build();

    public static final PropertiesAttributeDefinition METADATA = new PropertiesAttributeDefinition.Builder(EJB3SubsystemModel.METADATA, true)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition EVENTS_LOGGED = new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.EVENTS_LOGGED, ModelType.LONG)
            .setStorageRuntime()
            .build();

    public static final SimpleAttributeDefinition EVENTS_DROPPED = new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.EVENTS_DROPPED, ModelType.LONG)
            .setStorageRuntime()
            .build();

    /**
     * Maximum number of events the async queue may hold before additional events are dropped.
     * Matches the {@code queue-length} attribute of the logging subsystem's {@code async-handler},
     * which carries the same concept. The bound is fixed when the queue is constructed;
     * changes require {@code RESTART_RESOURCE_SERVICES}.
     */
    static final SimpleAttributeDefinition QUEUE_LENGTH =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.QUEUE_LENGTH, ModelType.INT, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(1024))
                    .setValidator(new IntRangeValidator(1, true, true))
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    private static final AttributeDefinition[] RESTART_RESOURCE_SERVICES_ATTRIBUTES = new AttributeDefinition[]{
            DESTINATION, PATH, RELATIVE_TO, ROTATE_SUFFIX, WORKER, ATTRIBUTES, QUEUE_LENGTH
    };

    private static final AttributeDefinition[] RESTART_NONE_ATTRIBUTES = new AttributeDefinition[]{
            INCLUDE_LOCAL, INCLUDE_NODE_NAME, METADATA
    };

    private static final AttributeDefinition[] ALL_CONFIG_ATTRIBUTES = new AttributeDefinition[]{
            DESTINATION, PATH, RELATIVE_TO, ROTATE_SUFFIX, WORKER, INCLUDE_LOCAL, INCLUDE_NODE_NAME, ATTRIBUTES, METADATA, QUEUE_LENGTH
    };

    private static final AccessLogAdd ADD_HANDLER = new AccessLogAdd(ALL_CONFIG_ATTRIBUTES);

    /**
     * Remove handler for the access-log resource.
     *
     * <p>The access-log service is a <em>leaf</em>: nothing in the server depends on it at
     * runtime.  Deployments depend on {@link org.jboss.as.ejb3.component.AccessLogHolder}'s
     * service (installed unconditionally at subsystem boot, never removed), not on this service.
     * {@link org.jboss.as.controller.ServiceRemoveStepHandler}'s default behaviour gates removal
     * on {@link OperationContext#isResourceServiceRestartAllowed()}, reporting
     * {@code reload-required} when the header is absent — which is overly conservative for a
     * leaf.  This handler removes unconditionally, matching the approach used by
     * {@code ConsoleAccessLogDefinition.RemoveHandler} in Undertow.
     *
     * <p>{@link #recoverServices} restores the service through
     * {@code ADD_HANDLER.installService}, not {@code performRuntime}, so a rolled-back removal
     * leaves the counters as they were. Only the management {@code :add} operation zeroes them.
     */
    private static final class RemoveHandler extends AbstractRemoveStepHandler {

        static final RemoveHandler INSTANCE = new RemoveHandler();

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model)
                throws OperationFailedException {
            context.removeService(ACCESS_LOG_CAPABILITY.getCapabilityServiceName());
        }

        @Override
        protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model)
                throws OperationFailedException {
            ADD_HANDLER.installService(context, operation, model);
        }
    }

    AccessLogResourceDefinition() {
        super(new Parameters(EJB3SubsystemModel.ACCESS_LOG_PATH, EJB3Extension.getResourceDescriptionResolver(EJB3SubsystemModel.SERVICE + "." + EJB3SubsystemModel.ACCESS_LOG))
                .setAddHandler(ADD_HANDLER)
                .setRemoveHandler(RemoveHandler.INSTANCE)
                .addCapabilities(ACCESS_LOG_CAPABILITY));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        // RESTART_RESOURCE_SERVICES: remove and re-install the AccessLogService.
        // This closes the old writer and opens a new one — no server reload required.
        // The residual window where an in-flight invocation reaches AsyncEventLogger after closed is an accepted limitation of the lock-free design.
        AbstractWriteAttributeHandler<Void> restartServiceHandler = new AbstractWriteAttributeHandler<>(RESTART_RESOURCE_SERVICES_ATTRIBUTES) {
            @Override
            protected void validateUpdatedModel(OperationContext context, Resource resource) throws OperationFailedException {
                super.validateUpdatedModel(context, resource);
                AccessLogAdd.validateDestinationAttributes(resource.getModel());
            }

            @Override
            protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation,
                    String attributeName, ModelNode resolvedValue, ModelNode currentValue,
                    HandbackHolder<Void> handbackHolder) throws OperationFailedException {
                // Remove the current service, then re-install with the updated model.
                context.removeService(ACCESS_LOG_CAPABILITY.getCapabilityServiceName());
                ADD_HANDLER.installService(context, operation, context.readResource(org.jboss.as.controller.PathAddress.EMPTY_ADDRESS).getModel());
                return false; // false = no reload required
            }

            @Override
            protected void revertUpdateToRuntime(OperationContext context, ModelNode operation,
                    String attributeName, ModelNode valueToRestore, ModelNode resolvedValue,
                    Void handback) throws OperationFailedException {
                // Revert by re-installing with the original model (already restored by the framework).
                context.removeService(ACCESS_LOG_CAPABILITY.getCapabilityServiceName());
                ADD_HANDLER.installService(context, operation, context.readResource(org.jboss.as.controller.PathAddress.EMPTY_ADDRESS).getModel());
            }
        };

        for (AttributeDefinition attr : RESTART_RESOURCE_SERVICES_ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, restartServiceHandler);
        }

        // RESTART_NONE: mutate live state directly — no service restart, no reload.
        AbstractWriteAttributeHandler<Void> liveStateHandler = new AbstractWriteAttributeHandler<>(RESTART_NONE_ATTRIBUTES) {
            @Override
            protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation,
                    String attributeName, ModelNode resolvedValue, ModelNode currentValue,
                    HandbackHolder<Void> handbackHolder) throws OperationFailedException {
                final AccessLogService service = getLiveService(context.getServiceRegistry(false));
                if (service == null) {
                    return false;
                }
                switch (attributeName) {
                    case EJB3SubsystemModel.INCLUDE_LOCAL:
                        service.setIncludeLocal(resolvedValue.asBoolean());
                        break;
                    case EJB3SubsystemModel.INCLUDE_NODE_NAME:
                        service.setIncludeNodeName(resolvedValue.asBoolean());
                        break;
                    case EJB3SubsystemModel.METADATA:
                        final Map<String, Object> metadata = new LinkedHashMap<>();
                        if (resolvedValue.isDefined()) {
                            for (Property p : resolvedValue.asPropertyList()) {
                                metadata.put(p.getName(), p.getValue().asString());
                            }
                        }
                        service.setMetadata(metadata);
                        break;
                    default:
                        break;
                }
                return false; // false = no reload required
            }

            @Override
            protected void revertUpdateToRuntime(OperationContext context, ModelNode operation,
                    String attributeName, ModelNode valueToRestore, ModelNode resolvedValue,
                    Void handback) throws OperationFailedException {
                // Re-apply the reverted value using the same switch logic.
                applyUpdateToRuntime(context, operation, attributeName, valueToRestore, resolvedValue, new HandbackHolder<>());
            }
        };

        for (AttributeDefinition attr : RESTART_NONE_ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, liveStateHandler);
        }

        resourceRegistration.registerMetric(EVENTS_LOGGED, METRIC_HANDLER);
        resourceRegistration.registerMetric(EVENTS_DROPPED, METRIC_HANDLER);
    }

    private static AccessLogHolder getHolder(final ServiceRegistry registry) {
        @SuppressWarnings("unchecked")
        final ServiceController<AccessLogHolder> sc =
                (ServiceController<AccessLogHolder>) registry.getService(AccessLogHolder.ACCESS_LOG_HOLDER_SERVICE_NAME);
        return sc == null ? null : sc.getValue();
    }

    private static AccessLogService getLiveService(final ServiceRegistry registry) {
        final AccessLogHolder holder = getHolder(registry);
        return holder == null ? null : holder.get();
    }

    private static final AbstractRuntimeOnlyHandler METRIC_HANDLER = new AbstractRuntimeOnlyHandler() {
        @Override
        protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
            final AccessLogService service = getLiveService(context.getServiceRegistry(false));
            if (service == null) {
                return;
            }
            final String name = operation.get(
                    org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME).asString();
            final long value = EJB3SubsystemModel.EVENTS_LOGGED.equals(name)
                    ? service.getEventsLogged()
                    : service.getEventsDropped();
            context.getResult().set(value);
        }
    };
}
