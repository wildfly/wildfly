/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.clustering.controller.CapabilityProvider;
import org.jboss.as.clustering.controller.ResourceDefinitionProvider;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleAttribute;
import org.jboss.as.clustering.controller.SimpleResourceRegistration;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.clustering.controller.transform.UndefinedAttributesDiscardPolicy;
import org.jboss.as.clustering.controller.validation.IntRangeValidatorBuilder;
import org.jboss.as.clustering.controller.validation.LongRangeValidatorBuilder;
import org.jboss.as.clustering.controller.validation.ParameterValidatorBuilder;
import org.jboss.as.clustering.infinispan.subsystem.remote.ClientThreadPoolServiceConfigurator;
import org.jboss.as.clustering.infinispan.subsystem.remote.RemoteCacheContainerResourceDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceName;

/**
 * Thread pool resource definitions for Infinispan subsystem. See {@link org.infinispan.factories.KnownComponentNames}
 * and {@link org.infinispan.commons.executors.BlockingThreadPoolExecutorFactory#create} for the hardcoded
 * Infinispan default values.
 *
 * @author Radoslav Husar
 */
public enum ThreadPoolResourceDefinition implements ResourceDefinitionProvider, ThreadPoolDefinition, ResourceServiceConfiguratorFactory, UnaryOperator<SimpleResourceDefinition.Parameters>, BiConsumer<ResourceTransformationDescriptionBuilder, ModelVersion> {

    // cache-container
    @Deprecated ASYNC_OPERATIONS("async-operations", 25, 25, 1000, TimeUnit.MINUTES.toMillis(1), true, CacheContainerResourceDefinition.Capability.CONFIGURATION) {
        @Override
        public SimpleResourceDefinition.Parameters apply(SimpleResourceDefinition.Parameters parameters) {
            return parameters.setDeprecatedSince(InfinispanModel.VERSION_13_0_0.getVersion());
        }
    },
    BLOCKING("blocking", 1, 150, 5000, TimeUnit.MINUTES.toMillis(1), false, CacheContainerResourceDefinition.Capability.CONFIGURATION) {
        @Override
        public void buildTransformation(ResourceTransformationDescriptionBuilder parent, ModelVersion version) {
            if (InfinispanModel.VERSION_13_0_0.requiresTransformation(version)) {
                parent.discardChildResource(this.getPathElement());
            }
        }
    },
    LISTENER("listener", 1, 1, 1000, TimeUnit.MINUTES.toMillis(1), false, CacheContainerResourceDefinition.Capability.CONFIGURATION) {
        @Override
        public void accept(ResourceTransformationDescriptionBuilder builder, ModelVersion version) {
            if (InfinispanModel.VERSION_12_0_0.requiresTransformation(version)) {
                builder.getAttributeBuilder().setValueConverter(AttributeConverter.DEFAULT_VALUE, this.getQueueLength().getName());
            }
        }
    },
    NON_BLOCKING("non-blocking", 1, 2, 1000, TimeUnit.MINUTES.toMillis(1), true, CacheContainerResourceDefinition.Capability.CONFIGURATION) {
        @Override
        public void buildTransformation(ResourceTransformationDescriptionBuilder parent, ModelVersion version) {
            if (InfinispanModel.VERSION_13_0_0.requiresTransformation(version)) {
                parent.discardChildResource(this.getPathElement());
            }
        }
    },
    @Deprecated PERSISTENCE("persistence", 4, 4, 5000, TimeUnit.MINUTES.toMillis(1), false, CacheContainerResourceDefinition.Capability.CONFIGURATION) {
        @Override
        public SimpleResourceDefinition.Parameters apply(SimpleResourceDefinition.Parameters parameters) {
            return parameters.setDeprecatedSince(InfinispanModel.VERSION_13_0_0.getVersion());
        }

        @Override
        public void accept(ResourceTransformationDescriptionBuilder builder, ModelVersion version) {
            if (InfinispanModel.VERSION_12_0_0.requiresTransformation(version)) {
                builder.getAttributeBuilder()
                        .setValueConverter(AttributeConverter.DEFAULT_VALUE, this.getMinThreads().getName(), this.getQueueLength().getName())
                        ;
            }
        }
    },
    @Deprecated REMOTE_COMMAND("remote-command", 1, 200, 0, TimeUnit.MINUTES.toMillis(1), false, CacheContainerResourceDefinition.Capability.CONFIGURATION) {
        @Override
        public SimpleResourceDefinition.Parameters apply(SimpleResourceDefinition.Parameters parameters) {
            return parameters.setDeprecatedSince(InfinispanModel.VERSION_13_0_0.getVersion());
        }
    },
    @Deprecated STATE_TRANSFER("state-transfer", 1, 60, 0, TimeUnit.MINUTES.toMillis(1), true, CacheContainerResourceDefinition.Capability.CONFIGURATION) {
        @Override
        public SimpleResourceDefinition.Parameters apply(SimpleResourceDefinition.Parameters parameters) {
            return parameters.setDeprecatedSince(InfinispanModel.VERSION_12_0_0.getVersion());
        }
    },
    @Deprecated TRANSPORT("transport", 10, 10, 1000, TimeUnit.MINUTES.toMillis(1), true, CacheContainerResourceDefinition.Capability.CONFIGURATION) {
        @Override
        public SimpleResourceDefinition.Parameters apply(SimpleResourceDefinition.Parameters parameters) {
            return parameters.setDeprecatedSince(InfinispanModel.VERSION_13_0_0.getVersion());
        }

        @Override
        public void accept(ResourceTransformationDescriptionBuilder builder, ModelVersion version) {
            if (InfinispanModel.VERSION_12_0_0.requiresTransformation(version)) {
                builder.getAttributeBuilder()
                        .setValueConverter(AttributeConverter.DEFAULT_VALUE, this.getMinThreads().getName(), this.getMaxThreads().getName(), this.getQueueLength().getName())
                        ;
            }
        }
    },
    // remote-cache-container
    CLIENT("async", 99, 99, 0, 0L, true, RemoteCacheContainerResourceDefinition.Capability.CONFIGURATION) {
        @Override
        public ResourceServiceConfigurator createServiceConfigurator(PathAddress address) {
            return new ClientThreadPoolServiceConfigurator(this, address);
        }

        @Override
        public void buildTransformation(ResourceTransformationDescriptionBuilder parent, ModelVersion version) {
            ResourceTransformationDescriptionBuilder builder = parent.addChildResource(this.getPathElement());
            if (InfinispanModel.VERSION_12_0_0.requiresTransformation(version)) {
                builder.getAttributeBuilder().setValueConverter(AttributeConverter.DEFAULT_VALUE, this.getQueueLength().getName());
            }
        }
    },
    ;

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    private static PathElement pathElement(String name) {
        return PathElement.pathElement("thread-pool", name);
    }

    private final PathElement path;
    private final Attribute minThreads;
    private final Attribute maxThreads;
    private final Attribute queueLength;
    private final Attribute keepAliveTime;
    private final boolean nonBlocking;
    private final CapabilityProvider baseCapability;

    ThreadPoolResourceDefinition(String name, int defaultMinThreads, int defaultMaxThreads, int defaultQueueLength, long defaultKeepaliveTime, boolean nonBlocking, CapabilityProvider baseCapability) {
        this.path = pathElement(name);
        this.minThreads = new SimpleAttribute(createBuilder("min-threads", ModelType.INT, new ModelNode(defaultMinThreads), new IntRangeValidatorBuilder().min(0)).build());
        this.maxThreads = new SimpleAttribute(createBuilder("max-threads", ModelType.INT, new ModelNode(defaultMaxThreads), new IntRangeValidatorBuilder().min(0)).build());
        this.queueLength = new SimpleAttribute(createBuilder("queue-length", ModelType.INT, new ModelNode(defaultQueueLength), new IntRangeValidatorBuilder().min(0)).build());
        this.keepAliveTime = new SimpleAttribute(createBuilder("keepalive-time", ModelType.LONG, new ModelNode(defaultKeepaliveTime), new LongRangeValidatorBuilder().min(0)).build());
        this.nonBlocking = nonBlocking;
        this.baseCapability = baseCapability;
    }

    private static SimpleAttributeDefinitionBuilder createBuilder(String name, ModelType type, ModelNode defaultValue, ParameterValidatorBuilder validatorBuilder) {
        SimpleAttributeDefinitionBuilder builder = new SimpleAttributeDefinitionBuilder(name, type)
                .setAllowExpression(true)
                .setRequired(false)
                .setDefaultValue(defaultValue)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .setMeasurementUnit((type == ModelType.LONG) ? MeasurementUnit.MILLISECONDS : null)
                ;
        return builder.setValidator(validatorBuilder.configure(builder).build());
    }

    @Override
    public SimpleResourceDefinition.Parameters apply(SimpleResourceDefinition.Parameters parameters) {
        return parameters;
    }

    @Override
    public void register(ManagementResourceRegistration parent) {
        ResourceDescriptionResolver resolver = InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(this.path, pathElement(PathElement.WILDCARD_VALUE));
        ResourceDefinition definition = new SimpleResourceDefinition(this.apply(new SimpleResourceDefinition.Parameters(this.path, resolver)));
        ManagementResourceRegistration registration = parent.registerSubModel(definition);
        ResourceDescriptor descriptor = new ResourceDescriptor(resolver)
                .addAttributes(this.minThreads, this.maxThreads, this.queueLength, this.keepAliveTime)
                ;
        ResourceServiceHandler handler = new SimpleResourceServiceHandler(this);
        new SimpleResourceRegistration(descriptor, handler).register(registration);
    }

    @Override
    public ResourceServiceConfigurator createServiceConfigurator(PathAddress address) {
        return new ThreadPoolServiceConfigurator(this, address);
    }

    @Override
    public ServiceName getServiceName(PathAddress containerAddress) {
        return this.baseCapability.getServiceName(containerAddress).append(this.path.getKeyValuePair());
    }

    @Override
    public Attribute getMinThreads() {
        return this.minThreads;
    }

    @Override
    public Attribute getMaxThreads() {
        return this.maxThreads;
    }

    @Override
    public Attribute getQueueLength() {
        return this.queueLength;
    }

    @Override
    public Attribute getKeepAliveTime() {
        return this.keepAliveTime;
    }

    @Override
    public boolean isNonBlocking() {
        return this.nonBlocking;
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public void buildTransformation(ResourceTransformationDescriptionBuilder parent, ModelVersion version) {
        if (InfinispanModel.VERSION_4_0_0.requiresTransformation(version)) {
            parent.addChildResource(this.path, new UndefinedAttributesDiscardPolicy(this.minThreads, this.maxThreads, this.queueLength, this.keepAliveTime));
        } else {
            ResourceTransformationDescriptionBuilder builder = parent.addChildResource(this.path);
            this.accept(builder, version);
        }
    }

    @Override
    public void accept(ResourceTransformationDescriptionBuilder builder, ModelVersion version) {
        // Do nothing
    }
}