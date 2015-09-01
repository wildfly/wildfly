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
import java.util.Collections;
import java.util.List;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.clustering.controller.Registration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceBuilderFactory;
import org.jboss.as.clustering.controller.RestartParentResourceAddStepHandler;
import org.jboss.as.clustering.controller.RestartParentResourceRemoveStepHandler;
import org.jboss.as.clustering.controller.SimpleAttribute;
import org.jboss.as.clustering.controller.validation.IntRangeValidatorBuilder;
import org.jboss.as.clustering.controller.validation.LongRangeValidatorBuilder;
import org.jboss.as.clustering.controller.validation.ParameterValidatorBuilder;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.DefaultResourceDescriptionProvider;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.jgroups.spi.TransportConfiguration;

/**
 * @author Radoslav Husar
 * @author Paul Ferraro
 * @version Aug 2014
 */
public enum ThreadPoolResourceDefinition implements ResourceDefinition, Registration<ManagementResourceRegistration> {

    DEFAULT("default", "thread_pool", 20, 300, 100, 60L),
    OOB("oob", "oob_thread_pool", 20, 300, 0, 60L),
    INTERNAL("internal", "internal_thread_pool", 2, 4, 100, 60L),
    TIMER("timer", "timer", 2, 4, 500, 5L),
    ;

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    private static PathElement pathElement(String name) {
        return PathElement.pathElement("thread-pool", name);
    }

    private final String name;
    private final String prefix;
    private final ResourceDescriptionResolver descriptionResolver;
    private final Attribute minThreads;
    private final Attribute maxThreads;
    private final Attribute queueLength;
    private final Attribute keepAliveTime;

    private ThreadPoolResourceDefinition(String name, String prefix, int defaultMinThreads, int defaultMaxThreads, int defaultQueueLength, long defaultKeepaliveTime) {
        this.name = name;
        this.prefix = prefix;
        this.descriptionResolver = new JGroupsResourceDescriptionResolver(pathElement(PathElement.WILDCARD_VALUE));
        this.minThreads = new SimpleAttribute(createBuilder("min-threads", ModelType.INT, new ModelNode(defaultMinThreads), new IntRangeValidatorBuilder().min(0)).build());
        this.maxThreads = new SimpleAttribute(createBuilder("max-threads", ModelType.INT, new ModelNode(defaultMaxThreads), new IntRangeValidatorBuilder().min(0)).build());
        this.queueLength = new SimpleAttribute(createBuilder("queue-length", ModelType.INT, new ModelNode(defaultQueueLength), new IntRangeValidatorBuilder().min(0)).build());
        this.keepAliveTime = new SimpleAttribute(createBuilder("keepalive-time", ModelType.LONG, new ModelNode(defaultKeepaliveTime), new LongRangeValidatorBuilder().min(0)).build());
    }

    private static SimpleAttributeDefinitionBuilder createBuilder(String name, ModelType type, ModelNode defaultValue, ParameterValidatorBuilder validatorBuilder) {
        return new SimpleAttributeDefinitionBuilder(name, type)
                .setAllowExpression(true)
                .setAllowNull(true)
                .setDefaultValue(defaultValue)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .setMeasurementUnit((type == ModelType.LONG) ? MeasurementUnit.MILLISECONDS : null)
                .setValidator(validatorBuilder.allowExpression(true).allowUndefined(true).build())
                ;
    }

    @Override
    public PathElement getPathElement() {
        return pathElement(this.name);
    }

    @Override
    public DescriptionProvider getDescriptionProvider(ImmutableManagementResourceRegistration registration) {
        return new DefaultResourceDescriptionProvider(registration, this.descriptionResolver);
    }

    @Override
    public void register(ManagementResourceRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.descriptionResolver).addAttributes(this.getAttributes());
        ResourceServiceBuilderFactory<TransportConfiguration> transportBuilderFactory = new TransportConfigurationBuilderFactory();
        new RestartParentResourceAddStepHandler<>(transportBuilderFactory, descriptor).register(registration);
        new RestartParentResourceRemoveStepHandler<>(transportBuilderFactory, descriptor).register(registration);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
    }

    @Override
    public void registerNotifications(ManagementResourceRegistration registration) {
    }

    @Override
    public void registerChildren(ManagementResourceRegistration registration) {
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return Collections.emptyList();
    }

    @Override
    public boolean isRuntime() {
        return false;
    }

    @Override
    public boolean isOrderedChild() {
        return false;
    }

    Collection<Attribute> getAttributes() {
        return Arrays.asList(this.minThreads, this.maxThreads, this.queueLength, this.keepAliveTime);
    }

    String getPrefix() {
        return this.prefix;
    }

    Attribute getMinThreads() {
        return this.minThreads;
    }

    Attribute getMaxThreads() {
        return this.maxThreads;
    }

    Attribute getQueueLength() {
        return this.queueLength;
    }

    Attribute getKeepAliveTime() {
        return this.keepAliveTime;
    }

    void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        // Nothing to transform yet
    }
}