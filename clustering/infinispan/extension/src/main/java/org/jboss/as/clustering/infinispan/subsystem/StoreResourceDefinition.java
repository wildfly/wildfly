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

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.MetricHandler;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.controller.PropertiesAttributeDefinition;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceBuilderFactory;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleAliasEntry;
import org.jboss.as.clustering.controller.SimpleResourceRegistration;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.clustering.controller.transform.LegacyPropertyAddOperationTransformer;
import org.jboss.as.clustering.controller.transform.LegacyPropertyMapGetOperationTransformer;
import org.jboss.as.clustering.controller.transform.LegacyPropertyResourceTransformer;
import org.jboss.as.clustering.controller.transform.LegacyPropertyWriteOperationTransformer;
import org.jboss.as.clustering.controller.transform.SimpleOperationTransformer;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.global.MapOperations;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Base class for store resources which require common store attributes only.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Paul Ferraro
 */
public abstract class StoreResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static PathElement pathElement(String value) {
        return PathElement.pathElement("store", value);
    }

    enum Capability implements org.jboss.as.clustering.controller.Capability {
        PERSISTENCE("org.wildfly.clustering.infinispan.cache.store", PersistenceConfiguration.class),
        ;
        private final RuntimeCapability<Void> definition;

        Capability(String name, Class<?> type) {
            this.definition = RuntimeCapability.Builder.of(name, true, type).build();
        }

        @Override
        public RuntimeCapability<Void> getDefinition() {
            return this.definition;
        }

        @Override
        public RuntimeCapability<Void> resolve(PathAddress address) {
            PathAddress cacheAddress = address.getParent();
            PathAddress containerAddress = cacheAddress.getParent();
            return this.definition.fromBaseCapability(containerAddress.getLastElement().getValue(), cacheAddress.getLastElement().getValue());
        }
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        FETCH_STATE("fetch-state", true, UnaryOperator.identity()),
        PASSIVATION("passivation", true, UnaryOperator.identity()),
        PRELOAD("preload", false, UnaryOperator.identity()),
        PURGE("purge", true, UnaryOperator.identity()),
        SHARED("shared", false, UnaryOperator.identity()),
        SINGLETON("singleton", false, builder -> builder.setDeprecated(InfinispanModel.VERSION_4_2_0.getVersion())),
        PROPERTIES("properties"),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, boolean defaultValue, UnaryOperator<SimpleAttributeDefinitionBuilder> configurator) {
            this.definition = configurator.apply(new SimpleAttributeDefinitionBuilder(name, ModelType.BOOLEAN)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(new ModelNode(defaultValue))
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    ).build();
        }

        Attribute(String name) {
            this.definition = new PropertiesAttributeDefinition.Builder(name)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder builder, PathElement path) {
        if (InfinispanModel.VERSION_4_0_0.requiresTransformation(version)) {
            builder.discardChildResource(StoreWriteThroughResourceDefinition.PATH);
        } else {
            StoreWriteThroughResourceDefinition.buildTransformation(version, builder);
        }

        if (InfinispanModel.VERSION_3_0_0.requiresTransformation(version)) {
            builder.addOperationTransformationOverride(ModelDescriptionConstants.ADD)
                    .setCustomOperationTransformer(new SimpleOperationTransformer(new LegacyPropertyAddOperationTransformer()))
                    .inheritResourceAttributeDefinitions()
                    .end();

            builder.setCustomResourceTransformer(new LegacyPropertyResourceTransformer());

            builder.addRawOperationTransformationOverride(MapOperations.MAP_GET_DEFINITION.getName(), new SimpleOperationTransformer(new LegacyPropertyMapGetOperationTransformer()));
            for (String name : Operations.getAllWriteAttributeOperationNames()) {
                builder.addOperationTransformationOverride(name)
                        .inheritResourceAttributeDefinitions()
                        .setCustomOperationTransformer(new LegacyPropertyWriteOperationTransformer(address -> address.getParent().append(path)))
                        .end();
            }
        }

        StoreWriteBehindResourceDefinition.buildTransformation(version, builder);
    }

    private final PathElement legacyPath;
    private final Consumer<ResourceDescriptor> descriptorConfigurator;
    private final ResourceServiceHandler handler;
    private final Consumer<ManagementResourceRegistration> registrationConfigurator;

    @SuppressWarnings("deprecation")
    StoreResourceDefinition(PathElement path, PathElement legacyPath, ResourceDescriptionResolver resolver, Consumer<ResourceDescriptor> descriptorConfigurator, ResourceServiceBuilderFactory<PersistenceConfiguration> builderFactory, Consumer<ManagementResourceRegistration> registrationConfigurator) {
        super(path, resolver);
        this.legacyPath = legacyPath;
        this.descriptorConfigurator = descriptorConfigurator.andThen(descriptor -> descriptor
                .addAttributes(StoreResourceDefinition.Attribute.class)
                .addCapabilities(Capability.class)
                .addRequiredSingletonChildren(StoreWriteThroughResourceDefinition.PATH)
        );
        this.handler = new SimpleResourceServiceHandler<>(builderFactory);
        this.registrationConfigurator = registrationConfigurator.andThen(registration -> {
            if (registration.isRuntimeOnlyRegistrationValid()) {
                new MetricHandler<>(new StoreMetricExecutor(), StoreMetric.class).register(registration);
            }

            new StoreWriteBehindResourceDefinition().register(registration);
            new StoreWriteThroughResourceDefinition().register(registration);

            new StorePropertyResourceDefinition().register(registration);
        });
    }

    @Override
    public void register(ManagementResourceRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubModel(this);
        if (this.legacyPath != null) {
            parentRegistration.registerAlias(this.legacyPath, new SimpleAliasEntry(registration));
        }

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver());
        this.descriptorConfigurator.accept(descriptor);
        new SimpleResourceRegistration(descriptor, this.handler).register(registration);

        this.registrationConfigurator.accept(registration);
    }
}
