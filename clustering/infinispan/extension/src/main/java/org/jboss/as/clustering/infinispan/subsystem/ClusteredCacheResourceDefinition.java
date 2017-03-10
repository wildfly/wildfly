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

import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.MetricHandler;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.validation.EnumValidatorBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.transform.description.AttributeConverter.DefaultValueAttributeConverter;
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
            this.definition = RuntimeCapability.Builder.of(name, true).build();
        }

        @Override
        public RuntimeCapability<?> getDefinition() {
            return this.definition;
        }

        @Override
        public RuntimeCapability<?> resolve(PathAddress address) {
            return this.definition.fromBaseCapability(address.getParent().getLastElement().getValue() + "." + address.getLastElement().getValue());
        }
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        MODE("mode", ModelType.STRING, new ModelNode(Mode.SYNC.name()), builder -> builder.setValidator(new EnumValidatorBuilder<>(Mode.class).configure(builder).build())),
        REMOTE_TIMEOUT("remote-timeout", ModelType.LONG, new ModelNode(10000L), UnaryOperator.identity()),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue, UnaryOperator<SimpleAttributeDefinitionBuilder> configurator) {
            this.definition = configurator.apply(createBuilder(name, type, defaultValue)).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    @Deprecated
    enum DeprecatedAttribute implements org.jboss.as.clustering.controller.Attribute {
        ASYNC_MARSHALLING("async-marshalling", ModelType.BOOLEAN, new ModelNode(false), InfinispanModel.VERSION_4_0_0),
        QUEUE_FLUSH_INTERVAL("queue-flush-interval", ModelType.LONG, new ModelNode(10L), InfinispanModel.VERSION_4_1_0),
        QUEUE_SIZE("queue-size", ModelType.INT, new ModelNode(0), InfinispanModel.VERSION_4_1_0),
        ;
        private final AttributeDefinition definition;

        DeprecatedAttribute(String name, ModelType type, ModelNode defaultValue, InfinispanModel deprecation) {
            this.definition = createBuilder(name, type, defaultValue).setDeprecated(deprecation.getVersion()).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
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

        if (InfinispanModel.VERSION_4_2_0.requiresTransformation(version)) {
            builder.getAttributeBuilder()
                    .setValueConverter(new DefaultValueAttributeConverter(Attribute.REMOTE_TIMEOUT.getDefinition()), Attribute.REMOTE_TIMEOUT.getDefinition())
                    .setValueConverter(new DefaultValueAttributeConverter(Attribute.MODE.getDefinition()), Attribute.MODE.getDefinition())
                    .end();
        }

        CacheResourceDefinition.buildTransformation(version, builder);
    }

    ClusteredCacheResourceDefinition(PathElement path, PathManager pathManager, boolean allowRuntimeOnlyRegistration, Consumer<ResourceDescriptor> descriptorConfigurator, ClusteredCacheServiceHandler handler, Consumer<ManagementResourceRegistration> registrationConfigurator) {
        super(path, pathManager, allowRuntimeOnlyRegistration, descriptorConfigurator.andThen(descriptor -> descriptor
                .addAttributes(Attribute.class)
                .addAttributes(DeprecatedAttribute.class)
                .addCapabilities(Capability.class)
                .addResourceCapabilityReference(new CapabilityReference(Capability.TRANSPORT, JGroupsTransportResourceDefinition.Requirement.CHANNEL_FACTORY), address -> address.getParent().getLastElement().getValue())
            ), handler, registrationConfigurator.andThen(registration -> {
                if (allowRuntimeOnlyRegistration) {
                    new MetricHandler<>(new ClusteredCacheMetricExecutor(), ClusteredCacheMetric.class).register(registration);
                }
            }));
    }
}
