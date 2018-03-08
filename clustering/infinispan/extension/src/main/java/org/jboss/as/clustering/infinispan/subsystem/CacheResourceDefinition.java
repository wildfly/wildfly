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
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import org.infinispan.configuration.cache.Index;
import org.jboss.as.clustering.controller.BinaryRequirementCapability;
import org.jboss.as.clustering.controller.CapabilityProvider;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.MetricHandler;
import org.jboss.as.clustering.controller.PropertiesAttributeDefinition;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistration;
import org.jboss.as.clustering.controller.validation.EnumValidator;
import org.jboss.as.clustering.controller.validation.ModuleIdentifierValidatorBuilder;
import org.jboss.as.controller.AbstractAttributeDefinitionBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.service.BinaryRequirement;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;

/**
 * Base class for cache resources which require common cache attributes only.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class CacheResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> implements Consumer<ManagementResourceRegistration> {

    enum Capability implements CapabilityProvider {
        CACHE(InfinispanCacheRequirement.CACHE),
        CONFIGURATION(InfinispanCacheRequirement.CONFIGURATION),
        ;
        private final org.jboss.as.clustering.controller.Capability capability;

        Capability(BinaryRequirement requirement) {
            this.capability = new BinaryRequirementCapability(requirement);
        }

        @Override
        public org.jboss.as.clustering.controller.Capability getCapability() {
            return this.capability;
        }
    }

    static final Map<ClusteringCacheRequirement, org.jboss.as.clustering.controller.Capability> CLUSTERING_CAPABILITIES = new EnumMap<>(ClusteringCacheRequirement.class);
    static {
        for (ClusteringCacheRequirement requirement : EnumSet.allOf(ClusteringCacheRequirement.class)) {
            CLUSTERING_CAPABILITIES.put(requirement, new BinaryRequirementCapability(requirement));
        }
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        MODULE("module", ModelType.STRING, builder -> builder.setValidator(new ModuleIdentifierValidatorBuilder().configure(builder).build())),
        STATISTICS_ENABLED("statistics-enabled", ModelType.BOOLEAN, builder -> builder.setDefaultValue(new ModelNode(false))),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, UnaryOperator<SimpleAttributeDefinitionBuilder> configurator) {
            this.definition = configurator.apply(createBuilder(name, type)).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    @Deprecated
    enum DeprecatedAttribute implements org.jboss.as.clustering.controller.Attribute {
        @Deprecated BATCHING("batching", ModelType.BOOLEAN, builder -> builder.setDefaultValue(new ModelNode(false)), InfinispanModel.VERSION_3_0_0),
        @Deprecated INDEXING("indexing", ModelType.STRING, builder -> builder.setDefaultValue(new ModelNode(Index.NONE.name())).setValidator(new EnumValidator<>(Index.class)), InfinispanModel.VERSION_4_0_0),
        @Deprecated INDEXING_PROPERTIES("indexing-properties", InfinispanModel.VERSION_4_0_0),
        @Deprecated JNDI_NAME("jndi-name", ModelType.STRING, UnaryOperator.identity(), InfinispanModel.VERSION_6_0_0),
        @Deprecated START("start", ModelType.STRING, builder -> builder.setDefaultValue(new ModelNode(StartMode.LAZY.name())).setValidator(new EnumValidator<>(StartMode.class)), InfinispanModel.VERSION_3_0_0),
        ;
        private final AttributeDefinition definition;

        DeprecatedAttribute(String name, ModelType type, UnaryOperator<SimpleAttributeDefinitionBuilder> configurator, InfinispanModel deprecation) {
            this(configurator.apply(createBuilder(name, type)), deprecation);
        }

        DeprecatedAttribute(String name, InfinispanModel deprecation) {
            this(new PropertiesAttributeDefinition.Builder(name).setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES), deprecation);
        }

        <A extends AttributeDefinition, B extends AbstractAttributeDefinitionBuilder<B, A>> DeprecatedAttribute(B builder, InfinispanModel deprecation) {
            this.definition = builder.setDeprecated(deprecation.getVersion()).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static SimpleAttributeDefinitionBuilder createBuilder(String name, ModelType type) {
        return new SimpleAttributeDefinitionBuilder(name, type)
                .setAllowExpression(true)
                .setRequired(false)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                ;
    }

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder builder) {

        if (InfinispanModel.VERSION_4_0_0.requiresTransformation(version)) {
            builder.discardChildResource(NoStoreResourceDefinition.PATH);
        } else {
            NoStoreResourceDefinition.buildTransformation(version, builder);
        }

        if (InfinispanModel.VERSION_3_0_0.requiresTransformation(version)) {
            // Set batching=true if transaction mode=BATCH
            ResourceTransformer batchingTransformer = new ResourceTransformer() {
                @Override
                public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException {
                    PathAddress transactionAddress = address.append(TransactionResourceDefinition.PATH);
                    try {
                        ModelNode transaction = context.readResourceFromRoot(transactionAddress).getModel();
                        if (transaction.hasDefined(TransactionResourceDefinition.Attribute.MODE.getName())) {
                            ModelNode mode = transaction.get(TransactionResourceDefinition.Attribute.MODE.getName());
                            if ((mode.getType() == ModelType.STRING) && (TransactionMode.valueOf(mode.asString()) == TransactionMode.BATCH)) {
                                resource.getModel().get(DeprecatedAttribute.BATCHING.getName()).set(true);
                            }
                        }
                    } catch (NoSuchElementException e) {
                        // Ignore, nothing to convert
                    }
                    context.addTransformedResource(PathAddress.EMPTY_ADDRESS, resource).processChildren(resource);
                }
            };
            builder.setCustomResourceTransformer(batchingTransformer);
        }


        ObjectMemoryResourceDefinition.buildTransformation(version, builder);

        if (InfinispanModel.VERSION_6_0_0.requiresTransformation(version)) {
            builder.rejectChildResource(BinaryMemoryResourceDefinition.PATH);
            builder.rejectChildResource(OffHeapMemoryResourceDefinition.PATH);
        }

        LockingResourceDefinition.buildTransformation(version, builder);
        ExpirationResourceDefinition.buildTransformation(version, builder);
        TransactionResourceDefinition.buildTransformation(version, builder);

        FileStoreResourceDefinition.buildTransformation(version, builder);
        BinaryKeyedJDBCStoreResourceDefinition.buildTransformation(version, builder);
        MixedKeyedJDBCStoreResourceDefinition.buildTransformation(version, builder);
        StringKeyedJDBCStoreResourceDefinition.buildTransformation(version, builder);
        RemoteStoreResourceDefinition.buildTransformation(version, builder);
        CustomStoreResourceDefinition.buildTransformation(version, builder);
    }

    private final Consumer<ResourceDescriptor> descriptorConfigurator;
    private final ResourceServiceHandler handler;
    private final Consumer<ManagementResourceRegistration> registrationConfigurator;

    public CacheResourceDefinition(PathElement path, Consumer<ResourceDescriptor> descriptorConfigurator, CacheServiceHandler handler, Consumer<ManagementResourceRegistration> registrationConfigurator) {
        super(path, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(path, PathElement.pathElement("cache")));
        this.descriptorConfigurator = descriptorConfigurator.andThen(descriptor -> descriptor
            .addAttributes(Attribute.class)
            .addAttributes(DeprecatedAttribute.class)
            .addCapabilities(Capability.class)
            .addCapabilities(CLUSTERING_CAPABILITIES.values())
            .addRequiredChildren(ExpirationResourceDefinition.PATH, LockingResourceDefinition.PATH, TransactionResourceDefinition.PATH)
            .addRequiredSingletonChildren(ObjectMemoryResourceDefinition.PATH, NoStoreResourceDefinition.PATH)
        );
        this.handler = handler;
        this.registrationConfigurator = registrationConfigurator.andThen(this);
    }

    @Override
    public void accept(ManagementResourceRegistration registration) {
        if (registration.isRuntimeOnlyRegistrationValid()) {
            new MetricHandler<>(new CacheMetricExecutor(), CacheMetric.class).register(registration);
        }

        new ObjectMemoryResourceDefinition().register(registration);
        new BinaryMemoryResourceDefinition().register(registration);
        new OffHeapMemoryResourceDefinition().register(registration);

        new ExpirationResourceDefinition().register(registration);
        new LockingResourceDefinition().register(registration);
        new TransactionResourceDefinition().register(registration);

        new NoStoreResourceDefinition().register(registration);
        new CustomStoreResourceDefinition().register(registration);
        new FileStoreResourceDefinition().register(registration);
        new BinaryKeyedJDBCStoreResourceDefinition().register(registration);
        new MixedKeyedJDBCStoreResourceDefinition().register(registration);
        new StringKeyedJDBCStoreResourceDefinition().register(registration);
        new RemoteStoreResourceDefinition().register(registration);
    }

    @Override
    public void register(ManagementResourceRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver());
        this.descriptorConfigurator.accept(descriptor);
        new SimpleResourceRegistration(descriptor, this.handler).register(registration);

        this.registrationConfigurator.accept(registration);
    }
}
