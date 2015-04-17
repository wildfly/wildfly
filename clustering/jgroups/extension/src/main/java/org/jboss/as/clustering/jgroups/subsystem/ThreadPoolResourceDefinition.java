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

import java.util.Collections;
import java.util.List;

import org.jboss.as.clustering.controller.ReloadRequiredAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.DefaultResourceDescriptionProvider;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Radoslav Husar
 * @author Paul Ferraro
 * @version Aug 2014
 */
public enum ThreadPoolResourceDefinition implements ResourceDefinition {

    DEFAULT(ModelKeys.DEFAULT, 20, 300, 100, 60L),
    OOB(ModelKeys.OOB, 20, 300, 0, 60L),
    INTERNAL(ModelKeys.INTERNAL, 2, 4, 100, 60L),
    TIMER(ModelKeys.TIMER, 2, 4, 500, 5L),
    ;

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    private static PathElement pathElement(String name) {
        return PathElement.pathElement(ModelKeys.THREAD_POOL, name);
    }

    private final String name;
    private final ResourceDescriptionResolver descriptionResolver;
    private final SimpleAttributeDefinition minThreads;
    private final SimpleAttributeDefinition maxThreads;
    private final SimpleAttributeDefinition queueLength;
    private final SimpleAttributeDefinition keepaliveTime;

    private ThreadPoolResourceDefinition(String name, int defaultMinThreads, int defaultMaxThreads, int defaultQueueLength, long defaultKeepaliveTime) {
        this.name = name;
        this.descriptionResolver = new JGroupsResourceDescriptionResolver(ModelKeys.TRANSPORT, ModelKeys.THREAD_POOL);
        this.minThreads = new SimpleAttributeDefinitionBuilder(Attribute.MIN_THREADS.getLocalName(), ModelType.INT, true)
                .setDefaultValue(new ModelNode(defaultMinThreads))
                .setValidator(new IntRangeValidator(0, Integer.MAX_VALUE, false, true))
                .setAllowExpression(true)
                .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                .build();
        this.maxThreads = new SimpleAttributeDefinitionBuilder(Attribute.MAX_THREADS.getLocalName(), ModelType.INT, true)
                .setDefaultValue(new ModelNode(defaultMaxThreads))
                .setValidator(new IntRangeValidator(0, Integer.MAX_VALUE, false, true))
                .setAllowExpression(true)
                .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                .build();
        this.queueLength = new SimpleAttributeDefinitionBuilder(Attribute.QUEUE_LENGTH.getLocalName(), ModelType.INT, true)
                .setDefaultValue(new ModelNode(defaultQueueLength))
                .setValidator(new IntRangeValidator(0, Integer.MAX_VALUE, false, true))
                .setAllowExpression(true)
                .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                .build();
        this.keepaliveTime = new SimpleAttributeDefinitionBuilder(Attribute.KEEPALIVE_TIME.getLocalName(), ModelType.LONG, true)
                .setDefaultValue(new ModelNode(defaultKeepaliveTime))
                .setValidator(new LongRangeValidator(0, Long.MAX_VALUE, false, true))
                .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
                .setAllowExpression(true)
                .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                .build();
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
    public void registerOperations(ManagementResourceRegistration registration) {
        OperationDefinition addOperation = new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.ADD, this.descriptionResolver)
                .setParameters(this.getAttributes())
                .build();
        registration.registerOperationHandler(addOperation, new ReloadRequiredAddStepHandler(this.getAttributes()));
        registration.registerOperationHandler(new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.REMOVE, this.descriptionResolver).build(), ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(this.getAttributes());
        for (AttributeDefinition attribute : this.getAttributes()) {
            registration.registerReadWriteAttribute(attribute, null, writeHandler);
        }
    }

    @Override
    public void registerNotifications(ManagementResourceRegistration registration) {
    }

    @Override
    public void registerChildren(ManagementResourceRegistration registration) {
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return Collections.emptyList();
    }

    @Override
    public boolean isRuntime() {
        return false;
    }

    AttributeDefinition[] getAttributes() {
        return new AttributeDefinition[] { this.minThreads, this.maxThreads, this.queueLength, this.keepaliveTime };
    }

    SimpleAttributeDefinition getMinThreads() {
        return this.minThreads;
    }

    SimpleAttributeDefinition getMaxThreads() {
        return this.maxThreads;
    }

    SimpleAttributeDefinition getQueueLength() {
        return this.queueLength;
    }

    SimpleAttributeDefinition getKeepaliveTime() {
        return this.keepaliveTime;
    }

    void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        // Nothing to transform yet
    }
}