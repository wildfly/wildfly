/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.clustering.controller.ResourceDefinitionProvider;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleAttribute;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.clustering.controller.validation.IntRangeValidatorBuilder;
import org.jboss.as.clustering.controller.validation.LongRangeValidatorBuilder;
import org.jboss.as.clustering.controller.validation.ParameterValidatorBuilder;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceName;

/**
 * Scheduled thread pool resource definitions for Infinispan subsystem.
 *
 * See {@link org.infinispan.factories.KnownComponentNames} and {@link org.infinispan.commons.executors.BlockingThreadPoolExecutorFactory#create(int, int)}
 * for the hardcoded Infinispan default values.
 *
 * @author Radoslav Husar
 */
public enum ScheduledThreadPoolResourceDefinition implements ResourceDefinitionProvider, ScheduledThreadPoolDefinition, ResourceServiceConfiguratorFactory {

    EXPIRATION("expiration", 1, 60000), // called eviction prior to Infinispan 8
    ;

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    private static PathElement pathElement(String name) {
        return PathElement.pathElement("thread-pool", name);
    }

    private final PathElement path;
    private final Attribute minThreads;
    private final Attribute keepAliveTime;

    ScheduledThreadPoolResourceDefinition(String name, int defaultMinThreads, long defaultKeepaliveTime) {
        this.path = pathElement(name);
        this.minThreads = new SimpleAttribute(createBuilder("min-threads", ModelType.INT, new ModelNode(defaultMinThreads), new IntRangeValidatorBuilder().min(0), null).build());
        this.keepAliveTime = new SimpleAttribute(createBuilder("keepalive-time", ModelType.LONG, new ModelNode(defaultKeepaliveTime), new LongRangeValidatorBuilder().min(0), null).build());
    }

    private static SimpleAttributeDefinitionBuilder createBuilder(String name, ModelType type, ModelNode defaultValue, ParameterValidatorBuilder validatorBuilder, InfinispanSubsystemModel deprecation) {
        SimpleAttributeDefinitionBuilder builder = new SimpleAttributeDefinitionBuilder(name, type)
                .setAllowExpression(true)
                .setRequired(false)
                .setDefaultValue(defaultValue)
                .setFlags((deprecation != null) ? AttributeAccess.Flag.RESTART_RESOURCE_SERVICES : AttributeAccess.Flag.RESTART_NONE)
                .setMeasurementUnit((type == ModelType.LONG) ? MeasurementUnit.MILLISECONDS : null)
                ;
        return builder.setValidator(validatorBuilder.configure(builder).build());
    }

    @Override
    public void register(ManagementResourceRegistration parent) {
        ResourceDescriptionResolver resolver = InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(this.path, pathElement(PathElement.WILDCARD_VALUE));
        ResourceDefinition definition = new SimpleResourceDefinition(this.path, resolver);
        ManagementResourceRegistration registration = parent.registerSubModel(definition);
        ResourceDescriptor descriptor = new ResourceDescriptor(resolver)
                .addAttributes(this.minThreads, this.keepAliveTime)
                ;
        ResourceServiceHandler handler = new SimpleResourceServiceHandler(this);
        new SimpleResourceRegistrar(descriptor, handler).register(registration);
    }

    @Override
    public ResourceServiceConfigurator createServiceConfigurator(PathAddress address) {
        return new ScheduledThreadPoolServiceConfigurator(this, address);
    }

    @Override
    public ServiceName getServiceName(PathAddress containerAddress) {
        return CacheContainerResourceDefinition.Capability.CONFIGURATION.getServiceName(containerAddress).append(this.getPathElement().getKeyValuePair());
    }

    @Override
    public Attribute getMinThreads() {
        return this.minThreads;
    }

    @Override
    public Attribute getKeepAliveTime() {
        return this.keepAliveTime;
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }
}