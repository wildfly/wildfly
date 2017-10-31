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
import org.jboss.as.clustering.controller.ResourceDefinitionProvider;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.SimpleResourceRegistration;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleAttribute;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.clustering.controller.transform.UndefinedAttributesDiscardPolicy;
import org.jboss.as.clustering.controller.validation.IntRangeValidatorBuilder;
import org.jboss.as.clustering.controller.validation.LongRangeValidatorBuilder;
import org.jboss.as.clustering.controller.validation.ParameterValidatorBuilder;
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
 * and {@link org.infinispan.commons.executors.BlockingThreadPoolExecutorFactory#create(int, int)} for the hardcoded
 * Infinispan default values.
 *
 * @author Radoslav Husar
 * @version February 2015
 */
public enum ThreadPoolResourceDefinition implements ResourceDefinitionProvider, ThreadPoolDefinition {

    ASYNC_OPERATIONS("async-operations", 25, 25, 1000, 60000L),
    LISTENER("listener", 1, 1, 100000, 60000L),
    PERSISTENCE("persistence", 1, 4, 0, 60000L),
    REMOTE_COMMAND("remote-command", 1, 200, 0, 60000L),
    STATE_TRANSFER("state-transfer", 1, 60, 0, 60000L),
    TRANSPORT("transport", 25, 25, 100000, 60000L),
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

    ThreadPoolResourceDefinition(String name, int defaultMinThreads, int defaultMaxThreads, int defaultQueueLength, long defaultKeepaliveTime) {
        PathElement path = pathElement(name);
        this.definition = new SimpleResourceDefinition(path, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(path, pathElement(PathElement.WILDCARD_VALUE)));
        this.minThreads = new SimpleAttribute(createBuilder("min-threads", ModelType.INT, new ModelNode(defaultMinThreads), new IntRangeValidatorBuilder().min(0)).build());
        this.maxThreads = new SimpleAttribute(createBuilder("max-threads", ModelType.INT, new ModelNode(defaultMaxThreads), new IntRangeValidatorBuilder().min(0)).build());
        this.queueLength = new SimpleAttribute(createBuilder("queue-length", ModelType.INT, new ModelNode(defaultQueueLength), new IntRangeValidatorBuilder().min(0)).build());
        this.keepAliveTime = new SimpleAttribute(createBuilder("keepalive-time", ModelType.LONG, new ModelNode(defaultKeepaliveTime), new LongRangeValidatorBuilder().min(0)).build());
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
    public ResourceDefinition getDefinition() {
        return this.definition;
    }

    @Override
    public void register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);
        ResourceDescriptor descriptor = new ResourceDescriptor(this.definition.getResourceDescriptionResolver()).addAttributes(this.getAttributes());
        ResourceServiceHandler handler = new SimpleResourceServiceHandler<>(address -> new ThreadPoolBuilder(this, address.getParent()));
        new SimpleResourceRegistration(descriptor, handler).register(registration);
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

    void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        // Nothing to transform yet
    }

    DynamicDiscardPolicy getDiscardPolicy() {
        return new UndefinedAttributesDiscardPolicy(this.getAttributes());
    }
}