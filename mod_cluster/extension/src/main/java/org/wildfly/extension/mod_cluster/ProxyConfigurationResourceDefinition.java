/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.mod_cluster;

import static org.wildfly.extension.mod_cluster.ModClusterLogger.ROOT_LOGGER;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.AttributeTranslation;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.OperationHandler;
import org.jboss.as.clustering.controller.ReloadRequiredResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.UnaryCapabilityNameResolver;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ParameterCorrector;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AliasEntry;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.PathAddressTransformer;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.DiscardPolicy;
import org.jboss.as.controller.transform.description.DynamicDiscardPolicy;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modcluster.ModClusterServiceMBean;
import org.jboss.modcluster.config.impl.SessionDrainingStrategyEnum;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} implementation for the core mod_cluster configuration resource.
 *
 * @author Radoslav Husar
 */
public class ProxyConfigurationResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

    private static final String UNDERTOW_LISTENER_CAPABILITY_NAME = "org.wildfly.undertow.listener";

    static final PathElement LEGACY_PATH = PathElement.pathElement("mod-cluster-config", "configuration");
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
        ADVERTISE("advertise", ModelType.BOOLEAN, new ModelNode(true)),
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
        AUTO_ENABLE_CONTEXTS("auto-enable-contexts", ModelType.BOOLEAN, new ModelNode(true)),
        BALANCER("balancer", ModelType.STRING, null),
        EXCLUDED_CONTEXTS("excluded-contexts", ModelType.STRING, null),
        FLUSH_PACKETS("flush-packets", ModelType.BOOLEAN, new ModelNode(false)),
        FLUSH_WAIT("flush-wait", ModelType.INT, new ModelNode(-1)) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder
                        .setMeasurementUnit(MeasurementUnit.SECONDS)
                        .setValidator(new IntRangeValidator(-1, true, true))
                        .setCorrector(ZeroToNegativeOneParameterCorrector.INSTANCE)
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
        NODE_TIMEOUT("node-timeout", ModelType.INT, new ModelNode(-1)) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder
                        .setMeasurementUnit(MeasurementUnit.SECONDS)
                        .setValidator(new IntRangeValidator(-1, true, true))
                        .setCorrector(ZeroToNegativeOneParameterCorrector.INSTANCE)
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
        PROXY_LIST("proxy-list", ModelType.STRING, null) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder
                        .setValidator(new ParameterValidator() {
                            @Override
                            public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
                                if (value.isDefined()) {
                                    String str = value.asString();
                                    String[] results = str.split(",");
                                    for (String result : results) {
                                        int i = result.lastIndexOf(":");
                                        try {
                                            //validate that the port is >0 and that the host is not the empty string
                                            //this also validates that both a host and port have been supplied
                                            //<=1 as we want to make sure the host is not the empty string
                                            if (i <= 1 || Integer.parseInt(result.substring(i + 1)) <= 0) {
                                                throw new OperationFailedException(ModClusterLogger.ROOT_LOGGER.needHostAndPort(result));
                                            }
                                        } catch (NumberFormatException e) {
                                            throw new OperationFailedException(ModClusterLogger.ROOT_LOGGER.needHostAndPort(result));
                                        }
                                    }
                                }

                            }
                        })
                        .addAccessConstraint(ModClusterExtension.MOD_CLUSTER_PROXIES_DEF)
                        .setDeprecated(ModClusterModel.VERSION_2_0_0.getVersion())
                        .addAlternatives(PROXIES.getName())
                        ;
            }
        },
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
        SMAX("smax", ModelType.INT, new ModelNode(-1)) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder
                        .setValidator(new IntRangeValidator(-1, true, true))
                        .setCorrector(ZeroToNegativeOneParameterCorrector.INSTANCE)
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
                return builder.setMeasurementUnit(MeasurementUnit.SECONDS)
                        .setValidator(new IntRangeValidator(1, true, true));
            }
        },
        STICKY_SESSION("sticky-session", ModelType.BOOLEAN, new ModelNode(true)),
        STICKY_SESSION_REMOVE("sticky-session-remove", ModelType.BOOLEAN, new ModelNode(false)),
        STICKY_SESSION_FORCE("sticky-session-force", ModelType.BOOLEAN, new ModelNode(false)),
        STOP_CONTEXT_TIMEOUT("stop-context-timeout", ModelType.INT, new ModelNode(10)) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setMeasurementUnit(MeasurementUnit.SECONDS)
                        .setValidator(new IntRangeValidator(1, true, true));
            }
        },
        TTL("ttl", ModelType.INT, new ModelNode(-1)) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder
                        .setMeasurementUnit(MeasurementUnit.SECONDS)
                        .setValidator(new IntRangeValidator(-1, true, true))
                        .setCorrector(ZeroToNegativeOneParameterCorrector.INSTANCE)
                        ;
            }
        },
        WORKER_TIMEOUT("worker-timeout", ModelType.INT, new ModelNode(-1)) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder
                        .setMeasurementUnit(MeasurementUnit.SECONDS)
                        .setValidator(new IntRangeValidator(-1, true, true))
                        .setCorrector(ZeroToNegativeOneParameterCorrector.INSTANCE)
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
                    .addAlternatives("proxy-list")
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

    enum DeprecatedAttribute implements org.jboss.as.clustering.controller.Attribute {
        CONNECTOR("connector", ModelType.STRING, ModClusterModel.VERSION_6_0_0),
        SIMPLE_LOAD_PROVIDER("simple-load-provider", ModelType.INT, ModClusterModel.VERSION_6_0_0),
        ;

        private final AttributeDefinition definition;

        DeprecatedAttribute(String name, ModelType type, ModClusterModel deprecation) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type, true).setDeprecated(deprecation.getVersion()).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    public ProxyConfigurationResourceDefinition() {
        super(WILDCARD_PATH, ModClusterExtension.SUBSYSTEM_RESOLVER.createChildResolver(WILDCARD_PATH));
    }

    @SuppressWarnings("deprecation")
    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(EnumSet.complementOf(EnumSet.of(Attribute.SSL_CONTEXT)))
                .addExtraParameters(Attribute.SSL_CONTEXT)
                .addAttributeTranslation(DeprecatedAttribute.SIMPLE_LOAD_PROVIDER, SIMPLE_LOAD_PROVIDER_TRANSLATION)
                .addAlias(DeprecatedAttribute.CONNECTOR, Attribute.LISTENER)
                .addRequiredSingletonChildren(SimpleLoadProviderResourceDefinition.PATH)
                .addCapabilities(Capability.class)
                ;

        registration.registerReadWriteAttribute(Attribute.SSL_CONTEXT.getDefinition(), null, new ReloadRequiredWriteAttributeHandler() {
            @Override
            protected void validateUpdatedModel(OperationContext context, Resource model) {
                context.addStep(new OperationStepHandler() {
                    @Override
                    public void execute(OperationContext ctx, ModelNode op) throws OperationFailedException {
                        if (model.hasChild(SSLResourceDefinition.PATH)) {
                            throw new OperationFailedException(ROOT_LOGGER.bothElytronAndLegacySslContextDefined());
                        }
                    }
                }, OperationContext.Stage.MODEL);
            }
        });

        parent.registerAlias(LEGACY_PATH, new AliasEntry(registration) {
            @Override
            public PathAddress convertToTargetAddress(PathAddress aliasAddress, AliasContext aliasContext) {
                PathAddress rebuiltAddress = PathAddress.EMPTY_ADDRESS;
                for (PathElement pathElement : aliasAddress) {
                    if (pathElement.equals(LEGACY_PATH)) {
                        try {
                            if (aliasContext.readResourceFromRoot(rebuiltAddress, false).hasChildren(ProxyConfigurationResourceDefinition.WILDCARD_PATH.getKey())) {
                                Set<Resource.ResourceEntry> children = aliasContext.readResourceFromRoot(rebuiltAddress, false).getChildren(ProxyConfigurationResourceDefinition.WILDCARD_PATH.getKey());
                                if (children.size() > 1 && !Operations.getOperationName(aliasContext.getOperation()).equals(AliasContext.RECURSIVE_GLOBAL_OP)) {
                                    throw new IllegalStateException(ModClusterLogger.ROOT_LOGGER.legacyOperationsWithMultipleProxies());
                                }
                                PathAddress proxyPath = PathAddress.pathAddress(ProxyConfigurationResourceDefinition.pathElement(children.iterator().next().getName()));
                                rebuiltAddress = rebuiltAddress.append(proxyPath);
                            } else {
                                // handle :add
                                rebuiltAddress = rebuiltAddress.append(ProxyConfigurationResourceDefinition.pathElement("default"));
                            }
                        } catch (Resource.NoSuchResourceException ignore) {
                            // handle recursive-global-op
                            rebuiltAddress = rebuiltAddress.append(ProxyConfigurationResourceDefinition.WILDCARD_PATH);
                        }
                    } else {
                        rebuiltAddress = rebuiltAddress.append(pathElement);
                    }
                }
                return rebuiltAddress;
            }
        });

        if (registration.isRuntimeOnlyRegistrationValid()) {
            new OperationHandler<>(new ProxyOperationExecutor(), ProxyOperation.class).register(registration);
        }

        new ReloadRequiredResourceRegistration(descriptor).register(registration);
        new LegacyMetricOperationsRegistration().register(registration);

        new SimpleLoadProviderResourceDefinition().register(registration);
        new DynamicLoadProviderResourceDefinition().register(registration);

        new SSLResourceDefinition().register(registration);

        return registration;
    }

    private AttributeTranslation SIMPLE_LOAD_PROVIDER_TRANSLATION = new AttributeTranslation() {
        @Override
        public org.jboss.as.clustering.controller.Attribute getTargetAttribute() {
            return SimpleLoadProviderResourceDefinition.Attribute.FACTOR;
        }

        @Override
        public UnaryOperator<PathAddress> getPathAddressTransformation() {
            return new UnaryOperator<PathAddress>() {
                @Override
                public PathAddress apply(PathAddress pathAddress) {
                    return pathAddress.append(SimpleLoadProviderResourceDefinition.PATH);
                }
            };
        }

        @Override
        public UnaryOperator<ImmutableManagementResourceRegistration> getResourceRegistrationTransformation() {
            return new UnaryOperator<ImmutableManagementResourceRegistration>() {
                @Override
                public ImmutableManagementResourceRegistration apply(ImmutableManagementResourceRegistration registration) {
                    return registration.getSubModel(PathAddress.pathAddress(SimpleLoadProviderResourceDefinition.PATH));
                }
            };
        }
    };

    @SuppressWarnings("deprecation")
    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = ModClusterModel.VERSION_6_0_0.requiresTransformation(version) ? parent.addChildRedirection(WILDCARD_PATH, new PathAddressTransformer.BasicPathAddressTransformer(LEGACY_PATH), new ProxyConfigurationDynamicDiscardPolicy()) : parent.addChildResource(WILDCARD_PATH);

        if (ModClusterModel.VERSION_6_0_0.requiresTransformation(version)) {
            builder.discardChildResource(SimpleLoadProviderResourceDefinition.PATH);
            builder.setCustomResourceTransformer(new ResourceTransformer() {
                @Override
                public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException {
                    if (resource.hasChild(SimpleLoadProviderResourceDefinition.PATH)) {
                        ModelNode model = resource.getModel();

                        ModelNode simpleModel = Resource.Tools.readModel(resource.removeChild(SimpleLoadProviderResourceDefinition.PATH));
                        model.get(DeprecatedAttribute.SIMPLE_LOAD_PROVIDER.getName()).set(simpleModel.get(SimpleLoadProviderResourceDefinition.Attribute.FACTOR.getName()));
                    }
                    context.addTransformedResource(PathAddress.EMPTY_ADDRESS, resource).processChildren(resource);
                }
            });

            builder.getAttributeBuilder()
                    .addRename(Attribute.LISTENER.getDefinition(), DeprecatedAttribute.CONNECTOR.getName())
                    .end();
        }

        if (ModClusterModel.VERSION_5_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder()
                    .setDiscard(DiscardAttributeChecker.UNDEFINED, Attribute.SSL_CONTEXT.getDefinition())
                    .addRejectCheck(RejectAttributeChecker.DEFINED, Attribute.SSL_CONTEXT.getDefinition())
                    .end();
        }

        if (ModClusterModel.VERSION_4_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder()
                    .setValueConverter(new AttributeConverter.DefaultAttributeConverter() {
                        @Override
                        protected void convertAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
                            if (!attributeValue.isDefined()) {
                                // Workaround legacy slaves not accepting null/empty values
                                // JBAS014704: '' is an invalid value for parameter excluded-contexts. Values must have a minimum length of 1 characters
                                attributeValue.set(" ");
                            }
                        }
                    }, Attribute.EXCLUDED_CONTEXTS.getDefinition())
                    .end();
        }

        if (ModClusterModel.VERSION_3_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder()
                    // Discard if using default value, reject if set to other than previously hard-coded default of 10 seconds
                    .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(Attribute.STATUS_INTERVAL.getDefinition().getDefaultValue()), Attribute.STATUS_INTERVAL.getDefinition())
                    .addRejectCheck(new RejectAttributeChecker.SimpleAcceptAttributeChecker(Attribute.STATUS_INTERVAL.getDefinition().getDefaultValue()), Attribute.STATUS_INTERVAL.getDefinition())
                    // Reject if using proxies, discard if undefined
                    .setDiscard(DiscardAttributeChecker.UNDEFINED, Attribute.PROXIES.getDefinition())
                    .addRejectCheck(RejectAttributeChecker.DEFINED, Attribute.PROXIES.getDefinition())
                    .end();
        }

        SimpleLoadProviderResourceDefinition.buildTransformation(version, builder);
        DynamicLoadProviderResourceDefinition.buildTransformation(version, builder);

        SSLResourceDefinition.buildTransformation(version, builder);
    }

    private static class ProxyConfigurationDynamicDiscardPolicy implements DynamicDiscardPolicy {
        @Override
        public DiscardPolicy checkResource(TransformationContext context, PathAddress address) {
            Resource resource = context.readResourceFromRoot(address.getParent());
            if (resource.hasChildren(ProxyConfigurationResourceDefinition.WILDCARD_PATH.getKey()) && resource.getChildren(ProxyConfigurationResourceDefinition.WILDCARD_PATH.getKey()).size() > 1) {
                return DiscardPolicy.REJECT_AND_WARN;
            }
            return DiscardPolicy.NEVER;
        }
    }
}
