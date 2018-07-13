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
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.CapabilityProvider;
import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.MetricHandler;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.SimpleResourceRegistration;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.UnaryRequirementCapability;
import org.jboss.as.clustering.controller.transform.OperationTransformer;
import org.jboss.as.clustering.controller.transform.SimpleOperationTransformer;
import org.jboss.as.clustering.controller.validation.EnumValidator;
import org.jboss.as.clustering.controller.validation.ModuleIdentifierValidatorBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.ListOperations;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.spi.InfinispanRequirement;
import org.wildfly.clustering.service.UnaryRequirement;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Paul Ferraro
 */
public class CacheContainerResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static PathElement pathElement(String containerName) {
        return PathElement.pathElement("cache-container", containerName);
    }

    enum Capability implements CapabilityProvider {
        CONTAINER(InfinispanRequirement.CONTAINER),
        CONFIGURATION(InfinispanRequirement.CONFIGURATION),
        KEY_AFFINITY_FACTORY(InfinispanRequirement.KEY_AFFINITY_FACTORY),
        ;
        private final org.jboss.as.clustering.controller.Capability capability;

        Capability(UnaryRequirement requirement) {
            this.capability = new UnaryRequirementCapability(requirement);
        }

        @Override
        public org.jboss.as.clustering.controller.Capability getCapability() {
            return this.capability;
        }
    }

    static final Map<InfinispanCacheRequirement, org.jboss.as.clustering.controller.Capability> DEFAULT_CAPABILITIES = new EnumMap<>(InfinispanCacheRequirement.class);
    static {
        for (InfinispanCacheRequirement requirement : EnumSet.allOf(InfinispanCacheRequirement.class)) {
            DEFAULT_CAPABILITIES.put(requirement, new UnaryRequirementCapability(requirement.getDefaultRequirement()));
        }
    }

    static final Map<ClusteringCacheRequirement, org.jboss.as.clustering.controller.Capability> DEFAULT_CLUSTERING_CAPABILITIES = new EnumMap<>(ClusteringCacheRequirement.class);
    static {
        for (ClusteringCacheRequirement requirement : EnumSet.allOf(ClusteringCacheRequirement.class)) {
            DEFAULT_CLUSTERING_CAPABILITIES.put(requirement, new UnaryRequirementCapability(requirement.getDefaultRequirement()));
        }
    }

    @Deprecated
    static final AttributeDefinition ALIAS = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.NAME, ModelType.STRING)
            .setAllowExpression(false)
            .build();

    @Deprecated
    static final OperationDefinition ALIAS_ADD = new SimpleOperationDefinitionBuilder("add-alias", InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(WILDCARD_PATH))
            .setParameters(ALIAS)
            .setDeprecated(InfinispanModel.VERSION_3_0_0.getVersion())
            .build();

    @Deprecated
    static final OperationDefinition ALIAS_REMOVE = new SimpleOperationDefinitionBuilder("remove-alias", InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(WILDCARD_PATH))
            .setParameters(ALIAS)
            .setDeprecated(InfinispanModel.VERSION_3_0_0.getVersion())
            .build();

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        ALIASES("aliases"),
        DEFAULT_CACHE("default-cache", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(false).setCapabilityReference(new CapabilityReference(DEFAULT_CAPABILITIES.get(InfinispanCacheRequirement.CONFIGURATION), InfinispanCacheRequirement.CONFIGURATION, WILDCARD_PATH));
            }
        },
        MODULE("module", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setDefaultValue(new ModelNode("org.jboss.as.clustering.infinispan"))
                        .setValidator(new ModuleIdentifierValidatorBuilder().configure(builder).build())
                        ;
            }
        },
        STATISTICS_ENABLED("statistics-enabled", ModelType.BOOLEAN) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setDefaultValue(new ModelNode(false));
            }
        },
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type) {
            this.definition = this.apply(createBuilder(name, type)).build();
        }

        Attribute(String name) {
            this.definition = new StringListAttributeDefinition.Builder(name)
                    .setRequired(false)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
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

    @Deprecated
    enum ExecutorAttribute implements org.jboss.as.clustering.controller.Attribute {
        EVICTION("eviction-executor"),
        LISTENER("listener-executor"),
        REPLICATION_QUEUE("replication-queue-executor"),
        ;
        private final AttributeDefinition definition;

        ExecutorAttribute(String name) {
            this.definition = createBuilder(name, ModelType.STRING).setAllowExpression(false).setDeprecated(InfinispanModel.VERSION_3_0_0.getVersion()).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    @Deprecated
    enum DeprecatedAttribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        JNDI_NAME("jndi-name", ModelType.STRING, InfinispanModel.VERSION_6_0_0),
        START("start", ModelType.STRING, InfinispanModel.VERSION_3_0_0) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setDefaultValue(new ModelNode(StartMode.LAZY.name()))
                        .setValidator(new EnumValidator<>(StartMode.class))
                        ;
            }
        },
        ;
        private final AttributeDefinition definition;

        DeprecatedAttribute(String name, ModelType type, InfinispanModel deprecation) {
            this.definition = this.apply(createBuilder(name, type)).setDeprecated(deprecation.getVersion()).build();
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

    static SimpleAttributeDefinitionBuilder createBuilder(String name, ModelType type) {
        return new SimpleAttributeDefinitionBuilder(name, type)
                .setAllowExpression(true)
                .setRequired(false)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                ;
    }

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = parent.addChildResource(WILDCARD_PATH);

        if (InfinispanModel.VERSION_4_0_0.requiresTransformation(version)) {
            for (ThreadPoolResourceDefinition pool : EnumSet.complementOf(EnumSet.of(ThreadPoolResourceDefinition.CLIENT))) {
                builder.addChildResource(pool.getPathElement(), pool.getDiscardPolicy());
            }
            for (ScheduledThreadPoolResourceDefinition pool : EnumSet.allOf(ScheduledThreadPoolResourceDefinition.class)) {
                builder.addChildResource(pool.getPathElement(), pool.getDiscardPolicy());
            }
        } else {
            for (ThreadPoolResourceDefinition pool : EnumSet.complementOf(EnumSet.of(ThreadPoolResourceDefinition.CLIENT))) {
                pool.buildTransformation(version, parent);
            }
            for (ScheduledThreadPoolResourceDefinition pool : EnumSet.allOf(ScheduledThreadPoolResourceDefinition.class)) {
                pool.buildTransformation(version, parent);
            }
        }

        if (InfinispanModel.VERSION_3_0_0.requiresTransformation(version)) {
            OperationTransformer addAliasTransformer = new OperationTransformer() {
                @Override
                public ModelNode transformOperation(ModelNode operation) {
                    String attributeName = Operations.getAttributeName(operation);
                    if (Attribute.ALIASES.getName().equals(attributeName)) {
                        ModelNode value = Operations.getAttributeValue(operation);
                        PathAddress address = Operations.getPathAddress(operation);
                        ModelNode transformedOperation = Util.createOperation(ALIAS_ADD, address);
                        transformedOperation.get(ALIAS.getName()).set(value);
                        return transformedOperation;
                    }
                    return operation;
                }
            };
            builder.addRawOperationTransformationOverride(ListOperations.LIST_ADD_DEFINITION.getName(), new SimpleOperationTransformer(addAliasTransformer));

            OperationTransformer removeAliasTransformer = new OperationTransformer() {
                @Override
                public ModelNode transformOperation(ModelNode operation) {
                    String attributeName = Operations.getAttributeName(operation);
                    if (Attribute.ALIASES.getName().equals(attributeName)) {
                        ModelNode value = Operations.getAttributeValue(operation);
                        PathAddress address = Operations.getPathAddress(operation);
                        ModelNode transformedOperation = Util.createOperation(ALIAS_REMOVE, address);
                        transformedOperation.get(ALIAS.getName()).set(value);
                        return transformedOperation;
                    }
                    return operation;
                }
            };
            builder.addRawOperationTransformationOverride(ListOperations.LIST_REMOVE_DEFINITION.getName(), new SimpleOperationTransformer(removeAliasTransformer));
        }

        NoTransportResourceDefinition.buildTransformation(version, builder);
        JGroupsTransportResourceDefinition.buildTransformation(version, builder);

        ScatteredCacheResourceDefinition.buildTransformation(version, builder);
        DistributedCacheResourceDefinition.buildTransformation(version, builder);
        ReplicatedCacheResourceDefinition.buildTransformation(version, builder);
        InvalidationCacheResourceDefinition.buildTransformation(version, builder);
        LocalCacheResourceDefinition.buildTransformation(version, builder);
    }

    CacheContainerResourceDefinition() {
        super(WILDCARD_PATH, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(WILDCARD_PATH));
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addAttributes(ExecutorAttribute.class)
                .addAttributes(DeprecatedAttribute.class)
                .addCapabilities(Capability.class)
                .addCapabilities(model -> model.hasDefined(Attribute.DEFAULT_CACHE.getName()), DEFAULT_CAPABILITIES.values())
                .addCapabilities(model -> model.hasDefined(Attribute.DEFAULT_CACHE.getName()), DEFAULT_CLUSTERING_CAPABILITIES.values())
                .addRequiredChildren(EnumSet.complementOf(EnumSet.of(ThreadPoolResourceDefinition.CLIENT)))
                .addRequiredChildren(ScheduledThreadPoolResourceDefinition.class)
                .addRequiredSingletonChildren(NoTransportResourceDefinition.PATH)
                ;
        ResourceServiceHandler handler = new CacheContainerServiceHandler();
        new SimpleResourceRegistration(descriptor, handler).register(registration);

        // Translate legacy add-alias operation to list-add operation
        OperationStepHandler addAliasHandler = new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode legacyOperation) {
                String value = legacyOperation.get(ALIAS.getName()).asString();
                ModelNode operation = Operations.createListAddOperation(context.getCurrentAddress(), Attribute.ALIASES, value);
                context.addStep(operation, ListOperations.LIST_ADD_HANDLER, context.getCurrentStage());
            }
        };
        registration.registerOperationHandler(ALIAS_ADD, addAliasHandler);

        // Translate legacy remove-alias operation to list-remove operation
        OperationStepHandler removeAliasHandler = new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode legacyOperation) throws OperationFailedException {
                String value = legacyOperation.get(ALIAS.getName()).asString();
                ModelNode operation = Operations.createListRemoveOperation(context.getCurrentAddress(), Attribute.ALIASES, value);
                context.addStep(operation, ListOperations.LIST_REMOVE_HANDLER, context.getCurrentStage());
            }
        };
        registration.registerOperationHandler(ALIAS_REMOVE, removeAliasHandler);

        if (registration.isRuntimeOnlyRegistrationValid()) {
            new MetricHandler<>(new CacheContainerMetricExecutor(), CacheContainerMetric.class).register(registration);
        }

        new JGroupsTransportResourceDefinition().register(registration);
        new NoTransportResourceDefinition().register(registration);

        for (ThreadPoolResourceDefinition pool : EnumSet.complementOf(EnumSet.of(ThreadPoolResourceDefinition.CLIENT))) {
            pool.register(registration);
        }
        for (ScheduledThreadPoolResourceDefinition pool : EnumSet.allOf(ScheduledThreadPoolResourceDefinition.class)) {
            pool.register(registration);
        }

        new LocalCacheResourceDefinition().register(registration);
        new InvalidationCacheResourceDefinition().register(registration);
        new ReplicatedCacheResourceDefinition().register(registration);
        new DistributedCacheResourceDefinition().register(registration);
        new ScatteredCacheResourceDefinition().register(registration);

        return registration;
    }
}
