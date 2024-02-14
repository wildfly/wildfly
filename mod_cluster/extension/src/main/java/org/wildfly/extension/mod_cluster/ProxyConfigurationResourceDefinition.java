/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.mod_cluster;

import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.OperationHandler;
import org.jboss.as.clustering.controller.ReloadRequiredResourceRegistrar;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.ParameterCorrector;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modcluster.ModClusterServiceMBean;
import org.jboss.modcluster.config.impl.SessionDrainingStrategyEnum;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} implementation for the core mod_cluster configuration resource.
 *
 * @author Radoslav Husar
 */
public class ProxyConfigurationResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

    private static final String UNDERTOW_LISTENER_CAPABILITY_NAME = "org.wildfly.undertow.listener";

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static PathElement pathElement(String name) {
        return PathElement.pathElement("proxy", name);
    }

    public enum Capability implements org.jboss.as.clustering.controller.Capability {
        SERVICE("org.wildfly.mod_cluster.service", ModClusterServiceMBean.class),
        ;

        private final RuntimeCapability<Void> definition;

        Capability(String name, Class<?> type) {
            this.definition = RuntimeCapability.Builder.of(name, true, type).setDynamicNameMapper(UnaryCapabilityNameResolver.DEFAULT).build();
        }

        @Override
        public RuntimeCapability<Void> getDefinition() {
            return this.definition;
        }
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        ADVERTISE("advertise", ModelType.BOOLEAN, ModelNode.TRUE),
        ADVERTISE_SECURITY_KEY("advertise-security-key", ModelType.STRING, null) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder
                        .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
                        .addAccessConstraint(ModClusterExtension.MOD_CLUSTER_SECURITY_DEF)
                        ;
            }
        },
        ADVERTISE_SOCKET("advertise-socket", ModelType.STRING, null) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder
                        .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
                        .setCapabilityReference(CommonUnaryRequirement.SOCKET_BINDING.getName())
                        ;
            }
        },
        AUTO_ENABLE_CONTEXTS("auto-enable-contexts", ModelType.BOOLEAN, ModelNode.TRUE),
        BALANCER("balancer", ModelType.STRING, null),
        EXCLUDED_CONTEXTS("excluded-contexts", ModelType.STRING, null),
        FLUSH_PACKETS("flush-packets", ModelType.BOOLEAN, ModelNode.FALSE),
        FLUSH_WAIT("flush-wait", ModelType.INT, null) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder
                        .setMeasurementUnit(MeasurementUnit.SECONDS)
                        .setValidator(new IntRangeValidator(1, true, true))
                        ;
            }
        },
        LISTENER("listener", ModelType.STRING, null) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder
                        .setCapabilityReference(UNDERTOW_LISTENER_CAPABILITY_NAME)
                        .setRequired(true)
                        ;
            }
        },
        LOAD_BALANCING_GROUP("load-balancing-group", ModelType.STRING, null),
        MAX_ATTEMPTS("max-attempts", ModelType.INT, new ModelNode(1)) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder
                        .setValidator(new IntRangeValidator(0, true, true))
                        .setCorrector(new ParameterCorrector() {
                            @Override
                            public ModelNode correct(ModelNode newValue, ModelNode currentValue) {
                                return (newValue.getType().equals(ModelType.INT) && newValue.asInt() == -1) ? new ModelNode(1) : newValue;
                            }
                        })
                        ;
            }
        },
        NODE_TIMEOUT("node-timeout", ModelType.INT, null) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder
                        .setMeasurementUnit(MeasurementUnit.SECONDS)
                        .setValidator(new IntRangeValidator(1, true, true))
                        ;
            }
        },
        PING("ping", ModelType.INT, new ModelNode(10)) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setMeasurementUnit(MeasurementUnit.SECONDS);
            }
        },
        PROXIES("proxies"),
        PROXY_URL("proxy-url", ModelType.STRING, new ModelNode("/")) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder
                        .addAccessConstraint(ModClusterExtension.MOD_CLUSTER_PROXIES_DEF);

            }
        },
        SESSION_DRAINING_STRATEGY("session-draining-strategy", ModelType.STRING, new ModelNode(SessionDrainingStrategyEnum.DEFAULT.name())) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setValidator(new EnumValidator<>(SessionDrainingStrategyEnum.class, SessionDrainingStrategyEnum.values()));
            }
        },
        SMAX("smax", ModelType.INT, null) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder
                        .setValidator(new IntRangeValidator(1, true, true))
                        ;
            }
        },
        SOCKET_TIMEOUT("socket-timeout", ModelType.INT, new ModelNode(20)) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setMeasurementUnit(MeasurementUnit.SECONDS)
                        .setValidator(new IntRangeValidator(1, true, true));
            }
        },
        SSL_CONTEXT("ssl-context", ModelType.STRING, null) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder
                        .setCapabilityReference(CommonUnaryRequirement.SSL_CONTEXT.getName())
                        .setValidator(new StringLengthValidator(1))
                        .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SSL_REF)
                        ;
            }
        },
        STATUS_INTERVAL("status-interval", ModelType.INT, new ModelNode(10)) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder
                        .setMeasurementUnit(MeasurementUnit.SECONDS)
                        .setValidator(new IntRangeValidator(1, true, true));
            }
        },
        STICKY_SESSION("sticky-session", ModelType.BOOLEAN, ModelNode.TRUE),
        STICKY_SESSION_REMOVE("sticky-session-remove", ModelType.BOOLEAN, ModelNode.FALSE),
        STICKY_SESSION_FORCE("sticky-session-force", ModelType.BOOLEAN, ModelNode.FALSE),
        STOP_CONTEXT_TIMEOUT("stop-context-timeout", ModelType.INT, new ModelNode(10)) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setMeasurementUnit(MeasurementUnit.SECONDS)
                        .setValidator(new IntRangeValidator(1, true, true));
            }
        },
        TTL("ttl", ModelType.INT, null) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder
                        .setMeasurementUnit(MeasurementUnit.SECONDS)
                        .setValidator(new IntRangeValidator(1, true, true))
                        ;
            }
        },
        WORKER_TIMEOUT("worker-timeout", ModelType.INT, null) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder
                        .setMeasurementUnit(MeasurementUnit.SECONDS)
                        .setValidator(new IntRangeValidator(1, true, true))
                        ;
            }
        },
        ;

        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .setRestartAllServices()
            ).build();
        }

        // proxies
        Attribute(String name) {
            this.definition = new StringListAttributeDefinition.Builder(name)
                    .setRequired(false)
                    .setAllowExpression(false) // expressions are not allowed for model references
                    .setRestartAllServices()
                    .setCapabilityReference(CommonUnaryRequirement.OUTBOUND_SOCKET_BINDING.getName())
                    .setAttributeMarshaller(AttributeMarshaller.STRING_LIST)
                    .addAccessConstraint(ModClusterExtension.MOD_CLUSTER_PROXIES_DEF)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }

        @Override
        public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
            return builder;
        }
    }

    private final FunctionExecutorRegistry<ModClusterServiceMBean> executors;

    public ProxyConfigurationResourceDefinition(FunctionExecutorRegistry<ModClusterServiceMBean> executors) {
        super(WILDCARD_PATH, ModClusterExtension.SUBSYSTEM_RESOLVER.createChildResolver(WILDCARD_PATH));
        this.executors = executors;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addRequiredSingletonChildren(SimpleLoadProviderResourceDefinition.PATH)
                .addCapabilities(Capability.class)
                ;

        if (registration.isRuntimeOnlyRegistrationValid()) {
            new OperationHandler<>(new ProxyOperationExecutor(this.executors), ProxyOperation.class).register(registration);
        }

        new ReloadRequiredResourceRegistrar(descriptor).register(registration);

        new SimpleLoadProviderResourceDefinition().register(registration);
        new DynamicLoadProviderResourceDefinition().register(registration);

        return registration;
    }

}
