/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.infinispan.configuration.global.ThreadPoolConfiguration;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.resource.DurationAttributeDefinition;

/**
 * Enumerates resource registrations for thread pools of a cache container.
 * @author Radoslav Husar
 */
public enum ThreadPool implements ThreadPoolResourceRegistration<ThreadPoolConfiguration> {

    BLOCKING("blocking", 1, 150, 5000, Duration.ofMinutes(1)),
    LISTENER("listener", 1, 1, 1000, Duration.ofMinutes(1)),
    NON_BLOCKING("non-blocking", 2, 2, 1000, Duration.ofMinutes(1)),
    ;

    private final PathElement path;
    private final UnaryServiceDescriptor<ThreadPoolConfiguration> descriptor;
    private final RuntimeCapability<Void> capability;
    private final AttributeDefinition minThreads;
    private final AttributeDefinition maxThreads;
    private final AttributeDefinition queueLength;
    private final DurationAttributeDefinition keepAlive;

    ThreadPool(String name, int defaultMinThreads, int defaultMaxThreads, int defaultQueueLength, Duration defaultKeepAlive) {
        this.path = PathElement.pathElement("thread-pool", name);
        this.descriptor = UnaryServiceDescriptorFactory.createServiceDescriptor(this, ThreadPoolConfiguration.class);
        this.capability = RuntimeCapability.Builder.of(this.descriptor).setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT).build();
        this.minThreads = createBuilder("min-threads", defaultMinThreads);
        this.maxThreads = createBuilder("max-threads", defaultMaxThreads);
        this.queueLength = createBuilder("queue-length", defaultQueueLength);
        this.keepAlive = DurationAttributeDefinition.builder("keepalive-time", ChronoUnit.MILLIS).setDefaultValue(defaultKeepAlive).build();
    }

    private static AttributeDefinition createBuilder(String name, int defaultValue) {
        return new SimpleAttributeDefinitionBuilder(name, ModelType.INT)
                .setAllowExpression(true)
                .setRequired(false)
                .setDefaultValue(new ModelNode(defaultValue))
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .setValidator(new IntRangeValidator(0))
                .build();
    }

    @Override
    public AttributeDefinition getMinThreads() {
        return this.minThreads;
    }

    @Override
    public AttributeDefinition getMaxThreads() {
        return this.maxThreads;
    }

    @Override
    public AttributeDefinition getQueueLength() {
        return this.queueLength;
    }

    @Override
    public DurationAttributeDefinition getKeepAlive() {
        return this.keepAlive;
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public UnaryServiceDescriptor<ThreadPoolConfiguration> getServiceDescriptor() {
        return this.descriptor;
    }

    @Override
    public RuntimeCapability<Void> getCapability() {
        return this.capability;
    }
}