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

import java.util.EnumSet;
import java.util.function.UnaryOperator;

import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.jboss.as.clustering.controller.AttributeTranslation;
import org.jboss.as.clustering.controller.BinaryCapabilityNameResolver;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.controller.PropertiesAttributeDefinition;
import org.jboss.as.clustering.controller.ReadAttributeTranslationHandler;
import org.jboss.as.clustering.controller.Registration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
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
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Base class for store resources which require common store attributes only.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Paul Ferraro
 */
public abstract class StoreResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> implements ResourceServiceConfiguratorFactory {

    protected static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    protected static PathElement pathElement(String value) {
        return PathElement.pathElement("store", value);
    }

    protected enum Capability implements org.jboss.as.clustering.controller.Capability {
        PERSISTENCE("org.wildfly.clustering.infinispan.cache.store", PersistenceConfiguration.class),
        ;
        private final RuntimeCapability<Void> definition;

        Capability(String name, Class<?> type) {
            this.definition = RuntimeCapability.Builder.of(name, true).setServiceType(type).setDynamicNameMapper(BinaryCapabilityNameResolver.GRANDPARENT_PARENT).build();
        }

        @Override
        public RuntimeCapability<Void> getDefinition() {
            return this.definition;
        }
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        FETCH_STATE("fetch-state", ModelType.BOOLEAN, ModelNode.TRUE),
        MAX_BATCH_SIZE("max-batch-size", ModelType.INT, new ModelNode(100)),
        PASSIVATION("passivation", ModelType.BOOLEAN, ModelNode.TRUE),
        PRELOAD("preload", ModelType.BOOLEAN, ModelNode.FALSE),
        PURGE("purge", ModelType.BOOLEAN, ModelNode.TRUE),
        SHARED("shared", ModelType.BOOLEAN, ModelNode.FALSE),
        PROPERTIES("properties"),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
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

    @Deprecated
    enum DeprecatedAttribute implements org.jboss.as.clustering.controller.Attribute {
        SINGLETON("singleton", ModelType.BOOLEAN, ModelNode.FALSE, InfinispanModel.VERSION_5_0_0),
        ;
        private final AttributeDefinition definition;

        DeprecatedAttribute(String name, ModelType type, ModelNode defaultValue, InfinispanModel deprecation) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .setDeprecated(deprecation.getVersion())
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    enum DeprecatedMetric implements AttributeTranslation, UnaryOperator<PathAddress>, Registration<ManagementResourceRegistration> {
        CACHE_LOADER_LOADS(StoreMetric.CACHE_LOADER_LOADS),
        CACHE_LOADER_MISSES(StoreMetric.CACHE_LOADER_MISSES),
        ;
        private final AttributeDefinition definition;
        private final org.jboss.as.clustering.controller.Attribute targetAttribute;

        DeprecatedMetric(StoreMetric metric) {
            this.targetAttribute = metric;
            this.definition = new SimpleAttributeDefinitionBuilder(metric.getName(), metric.getDefinition().getType())
                    .setDeprecated(InfinispanModel.VERSION_11_0_0.getVersion())
                    .setStorageRuntime()
                    .build();
        }

        @Override
        public void register(ManagementResourceRegistration registration) {
            registration.registerReadOnlyAttribute(this.definition, new ReadAttributeTranslationHandler(this));
        }

        @Override
        public org.jboss.as.clustering.controller.Attribute getTargetAttribute() {
            return this.targetAttribute;
        }

        @Override
        public UnaryOperator<PathAddress> getPathAddressTransformation() {
            return this;
        }

        @Override
        public PathAddress apply(PathAddress address) {
            PathAddress cacheAddress = address.getParent();
            return cacheAddress.getParent().append(CacheRuntimeResourceDefinition.pathElement(cacheAddress.getLastElement().getValue()), PersistenceRuntimeResourceDefinition.PATH);
        }
    }

    public static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder builder, PathElement path) {
        if (InfinispanModel.VERSION_6_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder().setDiscard(DiscardAttributeChecker.ALWAYS, Attribute.MAX_BATCH_SIZE.getDefinition());
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

        StoreWriteThroughResourceDefinition.buildTransformation(version, builder);
        StoreWriteBehindResourceDefinition.buildTransformation(version, builder);
    }

    private final PathElement legacyPath;
    private final UnaryOperator<ResourceDescriptor> configurator;

    protected StoreResourceDefinition(PathElement path, PathElement legacyPath, ResourceDescriptionResolver resolver, UnaryOperator<ResourceDescriptor> configurator) {
        super(path, resolver);
        this.legacyPath = legacyPath;
        this.configurator = configurator;
    }

    @SuppressWarnings("deprecation")
    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);
        if (this.legacyPath != null) {
            parent.registerAlias(this.legacyPath, new SimpleAliasEntry(registration));
        }

        ResourceDescriptor descriptor = this.configurator.apply(new ResourceDescriptor(this.getResourceDescriptionResolver()))
                .addAttributes(Attribute.class)
                .addIgnoredAttributes(DeprecatedAttribute.class)
                .addCapabilities(Capability.class)
                .addRequiredSingletonChildren(StoreWriteThroughResourceDefinition.PATH)
                ;
        ResourceServiceHandler handler = new SimpleResourceServiceHandler(this);
        new SimpleResourceRegistration(descriptor, handler).register(registration);

        if (registration.isRuntimeOnlyRegistrationValid()) {
            for (DeprecatedMetric metric : EnumSet.allOf(DeprecatedMetric.class)) {
                metric.register(registration);
            }
        }

        new StoreWriteBehindResourceDefinition().register(registration);
        new StoreWriteThroughResourceDefinition().register(registration);

        new StorePropertyResourceDefinition().register(registration);

        return registration;
    }
}
