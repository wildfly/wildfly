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

import java.util.Arrays;
import java.util.Collection;

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
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.DynamicDiscardPolicy;
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
public enum ThreadPoolResourceDefinition implements ResourceDefinitionProvider, ThreadPoolDefinition {

    // cache-container
    ASYNC_OPERATIONS("async-operations", 25, 25, 1000, 60000L, CacheContainerResourceDefinition.Capability.CONFIGURATION),
    LISTENER("listener", 1, 1, 100000, 60000L, CacheContainerResourceDefinition.Capability.CONFIGURATION),
    PERSISTENCE("persistence", 1, 4, 0, 60000L, CacheContainerResourceDefinition.Capability.CONFIGURATION, InfinispanModel.VERSION_8_0_0),
    REMOTE_COMMAND("remote-command", 1, 200, 0, 60000L, CacheContainerResourceDefinition.Capability.CONFIGURATION),
    STATE_TRANSFER("state-transfer", 1, 60, 0, 60000L, CacheContainerResourceDefinition.Capability.CONFIGURATION),
    TRANSPORT("transport", 25, 25, 100000, 60000L, CacheContainerResourceDefinition.Capability.CONFIGURATION),
    // remote-cache-container
    CLIENT("async", 99, 99, 10000, 0L, RemoteCacheContainerResourceDefinition.Capability.CONFIGURATION),
    ;

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    private static PathElement pathElement(String name) {
        return PathElement.pathElement("thread-pool", name);
    }

    private final SimpleResourceDefinition definition;
    private final Attribute minThreads;
    private final Attribute maxThreads;
    private final Attribute queueLength;
    private final Attribute keepAliveTime;
    private final CapabilityProvider baseCapability;
    private final ResourceServiceConfiguratorFactory serviceConfiguratorFactory;

    ThreadPoolResourceDefinition(String name, int defaultMinThreads, int defaultMaxThreads, int defaultQueueLength, long defaultKeepaliveTime, CapabilityProvider baseCapability) {
        this(name, defaultMinThreads, defaultMaxThreads, defaultQueueLength, defaultKeepaliveTime, baseCapability, null);
    }

    ThreadPoolResourceDefinition(String name, int defaultMinThreads, int defaultMaxThreads, int defaultQueueLength, long defaultKeepaliveTime, CapabilityProvider baseCapability, InfinispanModel scheduledThreadPool) {
        PathElement path = pathElement(name);
        this.definition = new SimpleResourceDefinition(path, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(path, pathElement(PathElement.WILDCARD_VALUE)));
        this.minThreads = new SimpleAttribute(createBuilder("min-threads", ModelType.INT, new ModelNode(defaultMinThreads), new IntRangeValidatorBuilder().min(0), scheduledThreadPool).build());
        this.maxThreads = new SimpleAttribute(createBuilder("max-threads", ModelType.INT, new ModelNode(defaultMaxThreads), new IntRangeValidatorBuilder().min(0), null).build());
        this.queueLength = new SimpleAttribute(createBuilder("queue-length", ModelType.INT, new ModelNode(defaultQueueLength), new IntRangeValidatorBuilder().min(0), scheduledThreadPool).build());
        this.keepAliveTime = new SimpleAttribute(createBuilder("keepalive-time", ModelType.LONG, new ModelNode(defaultKeepaliveTime), new LongRangeValidatorBuilder().min(0), null).build());
        this.baseCapability = baseCapability;
        this.serviceConfiguratorFactory = new ResourceServiceConfiguratorFactory() {
            @Override
            public ResourceServiceConfigurator createServiceConfigurator(PathAddress address) {
                if (baseCapability == RemoteCacheContainerResourceDefinition.Capability.CONFIGURATION) {
                    return new ClientThreadPoolServiceConfigurator(ThreadPoolResourceDefinition.this, address);
                }
                return (scheduledThreadPool != null) ? new ScheduledThreadPoolServiceConfigurator(ThreadPoolResourceDefinition.this, address) : new ThreadPoolServiceConfigurator(ThreadPoolResourceDefinition.this, address);
            }
        };
    }

    private static SimpleAttributeDefinitionBuilder createBuilder(String name, ModelType type, ModelNode defaultValue, ParameterValidatorBuilder validatorBuilder, InfinispanModel deprecation) {
        SimpleAttributeDefinitionBuilder builder = new SimpleAttributeDefinitionBuilder(name, type)
                .setAllowExpression(true)
                .setRequired(false)
                .setDefaultValue(defaultValue)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .setMeasurementUnit((type == ModelType.LONG) ? MeasurementUnit.MILLISECONDS : null)
                ;
        if (deprecation != null) {
            builder.setDeprecated(deprecation.getVersion());
        }
        return builder.setValidator(validatorBuilder.configure(builder).build());
    }

    @Override
    public ResourceDefinition getDefinition() {
        return this.definition;
    }

    @Override
    public void register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);
        ResourceDescriptor descriptor = new ResourceDescriptor(this.definition.getResourceDescriptionResolver()).addAttributes(this.getAttributes());
        ResourceServiceHandler handler = new SimpleResourceServiceHandler(this.serviceConfiguratorFactory);
        new SimpleResourceRegistration(descriptor, handler).register(registration);
    }

    @Override
    public ServiceName getServiceName(PathAddress containerAddress) {
        return this.baseCapability.getServiceName(containerAddress).append(this.getPathElement().getKeyValuePair());
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

    Collection<Attribute> getAttributes() {
        return Arrays.asList(this.minThreads, this.maxThreads, this.queueLength, this.keepAliveTime);
    }

    public void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        // Nothing to transform yet
    }

    DynamicDiscardPolicy getDiscardPolicy() {
        return new UndefinedAttributesDiscardPolicy(this.getAttributes());
    }
}