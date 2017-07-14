/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.clustering.controller.Definable;
import org.jboss.as.clustering.controller.ResourceDefinitionProvider;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleAttribute;
import org.jboss.as.clustering.controller.SimpleResourceRegistration;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
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
import org.jboss.as.controller.transform.description.AttributeConverter.DefaultValueAttributeConverter;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Radoslav Husar
 * @author Paul Ferraro
 * @version Aug 2014
 */
public enum ThreadPoolResourceDefinition implements ResourceDefinitionProvider, ThreadPoolDefinition {

    DEFAULT("default", "Incoming", 2, 30, 0, 60000L),
    OOB("oob", "OOB", 20, 200, 0, 60000L),
    INTERNAL("internal", "INT", 5, 20, 500, 60000L),
    TIMER("timer", "Timer", 2, 4, 500, 5000L, (definition, address) -> new TimerFactoryBuilder(definition, address)),
    ;

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    private static PathElement pathElement(String name) {
        return PathElement.pathElement("thread-pool", name);
    }

    private final SimpleResourceDefinition definition;
    private final String prefix;
    private final Attribute minThreads;
    private final Attribute maxThreads;
    private final Attribute queueLength;
    private final Attribute keepAliveTime;
    private final BiFunction<ThreadPoolDefinition, PathAddress, ResourceServiceBuilder<? extends Object>> factory;

    ThreadPoolResourceDefinition(String name, String prefix, int defaultMinThreads, int defaultMaxThreads, int defaultQueueLength, long defaultKeepAliveTime) {
        this(name, prefix, defaultMinThreads, defaultMaxThreads, defaultQueueLength, defaultKeepAliveTime, (definition, address) -> new ThreadPoolFactoryBuilder(definition, address));
    }

    ThreadPoolResourceDefinition(String name, String prefix, int defaultMinThreads, int defaultMaxThreads, int defaultQueueLength, long defaultKeepAliveTime, BiFunction<ThreadPoolDefinition, PathAddress, ResourceServiceBuilder<? extends Object>> factory) {
        this.definition = new SimpleResourceDefinition(pathElement(name), JGroupsExtension.SUBSYSTEM_RESOLVER.createChildResolver(pathElement(PathElement.WILDCARD_VALUE)));
        this.prefix = prefix;
        this.minThreads = new SimpleAttribute(createBuilder("min-threads", ModelType.INT, new ModelNode(defaultMinThreads), new IntRangeValidatorBuilder().min(0)).build());
        this.maxThreads = new SimpleAttribute(createBuilder("max-threads", ModelType.INT, new ModelNode(defaultMaxThreads), new IntRangeValidatorBuilder().min(0)).build());
        this.queueLength = new SimpleAttribute(createBuilder("queue-length", ModelType.INT, new ModelNode(defaultQueueLength), new IntRangeValidatorBuilder().min(0)).build());
        this.keepAliveTime = new SimpleAttribute(createBuilder("keepalive-time", ModelType.LONG, new ModelNode(defaultKeepAliveTime), new LongRangeValidatorBuilder().min(0)).build());
        this.factory = factory;
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
    public void register(ManagementResourceRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubModel(this.definition);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.definition.getResourceDescriptionResolver()).addAttributes(this.getAttributes());
        ResourceServiceHandler handler = new SimpleResourceServiceHandler<>(address -> this.factory.apply(this, address));
        new SimpleResourceRegistration(descriptor, handler).register(registration);
    }

    Collection<Attribute> getAttributes() {
        return Arrays.asList(this.minThreads, this.maxThreads, this.queueLength, this.keepAliveTime);
    }

    @Override
    public String getThreadGroupPrefix() {
        return this.prefix;
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

    void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = parent.addChildResource(this.definition);

        if (JGroupsModel.VERSION_5_0_0.requiresTransformation(version)) {
            Stream.of(this.minThreads, this.maxThreads, this.queueLength)
                    .map(Definable::getDefinition)
                    .forEach(attribute -> builder.getAttributeBuilder().setValueConverter(new DefaultValueAttributeConverter(attribute), attribute));
        }
    }
}