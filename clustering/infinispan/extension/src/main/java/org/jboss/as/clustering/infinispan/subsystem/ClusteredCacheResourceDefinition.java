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

import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.UnaryCapabilityNameResolver;
import org.jboss.as.clustering.controller.BinaryCapabilityNameResolver;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.MetricHandler;
import org.jboss.as.clustering.controller.ResourceCapabilityReference;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.validation.EnumValidator;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.transform.description.AttributeConverter.DefaultValueAttributeConverter;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Base class for cache resources which require common cache attributes and clustered cache attributes.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class ClusteredCacheResourceDefinition extends CacheResourceDefinition {

    enum Capability implements org.jboss.as.clustering.controller.Capability {
        TRANSPORT("org.wildfly.clustering.infinispan.cache-container.cache.transport"),
        ;
        private final RuntimeCapability<Void> definition;

        Capability(String name) {
            this.definition = RuntimeCapability.Builder.of(name, true).setDynamicNameMapper(BinaryCapabilityNameResolver.PARENT_CHILD).build();
        }

        @Override
        public RuntimeCapability<?> getDefinition() {
            return this.definition;
        }
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        REMOTE_TIMEOUT("remote-timeout", ModelType.LONG, new ModelNode(10000L)),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = createBuilder(name, type, defaultValue).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    @Deprecated
    enum DeprecatedAttribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        ASYNC_MARSHALLING("async-marshalling", ModelType.BOOLEAN, new ModelNode(false), InfinispanModel.VERSION_4_0_0),
        MODE("mode", ModelType.STRING, new ModelNode(Mode.SYNC.name()), InfinispanModel.VERSION_6_0_0) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setValidator(new EnumValidator<>(Mode.class));
            }
        },
        QUEUE_FLUSH_INTERVAL("queue-flush-interval", ModelType.LONG, new ModelNode(10L), InfinispanModel.VERSION_4_1_0),
        QUEUE_SIZE("queue-size", ModelType.INT, new ModelNode(0), InfinispanModel.VERSION_4_1_0),
        ;
        private final AttributeDefinition definition;

        DeprecatedAttribute(String name, ModelType type, ModelNode defaultValue, InfinispanModel deprecation) {
            this.definition = this.apply(createBuilder(name, type, defaultValue)).setDeprecated(deprecation.getVersion()).build();
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

    static SimpleAttributeDefinitionBuilder createBuilder(String name, ModelType type, ModelNode defaultValue) {
        return new SimpleAttributeDefinitionBuilder(name, type)
                .setAllowExpression(true)
                .setRequired(defaultValue == null)
                .setDefaultValue(defaultValue)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .setMeasurementUnit((type == ModelType.LONG) ? MeasurementUnit.MILLISECONDS : null)
                ;
    }

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder builder) {

        if (InfinispanModel.VERSION_6_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder()
                    .setValueConverter(AttributeConverter.Factory.createHardCoded(new ModelNode(Mode.SYNC.name())), DeprecatedAttribute.MODE.getDefinition())
                    .end();
        }

        if (InfinispanModel.VERSION_5_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder()
                    .setValueConverter(new DefaultValueAttributeConverter(Attribute.REMOTE_TIMEOUT.getDefinition()), Attribute.REMOTE_TIMEOUT.getDefinition())
                    .end();
        }

        CacheResourceDefinition.buildTransformation(version, builder);
    }

    private static class ResourceDescriptorConfigurator implements UnaryOperator<ResourceDescriptor> {
        private final UnaryOperator<ResourceDescriptor> configurator;

        ResourceDescriptorConfigurator(UnaryOperator<ResourceDescriptor> configurator) {
            this.configurator = configurator;
        }

        @Override
        public ResourceDescriptor apply(ResourceDescriptor descriptor) {
            return this.configurator.apply(descriptor)
                    .addAttributes(Attribute.class)
                    .addAttributes(DeprecatedAttribute.class)
                    .addCapabilities(Capability.class)
                    .addResourceCapabilityReference(new ResourceCapabilityReference(Capability.TRANSPORT, JGroupsTransportResourceDefinition.Requirement.CHANNEL, UnaryCapabilityNameResolver.PARENT))
                    ;
        }
    }

    ClusteredCacheResourceDefinition(PathElement path, UnaryOperator<ResourceDescriptor> configurator, ClusteredCacheServiceHandler handler) {
        super(path, new ResourceDescriptorConfigurator(configurator), handler);
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = super.register(parent);

        if (registration.isRuntimeOnlyRegistrationValid()) {
            new MetricHandler<>(new ClusteredCacheMetricExecutor(), ClusteredCacheMetric.class).register(registration);
        }

        return registration;
    }
}
