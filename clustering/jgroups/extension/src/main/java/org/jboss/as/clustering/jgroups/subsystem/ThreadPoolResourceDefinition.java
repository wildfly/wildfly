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
import java.util.function.BiConsumer;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.clustering.controller.ResourceDefinitionProvider;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
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
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Radoslav Husar
 * @author Paul Ferraro
 * @version Aug 2014
 */
public enum ThreadPoolResourceDefinition implements ResourceDefinitionProvider, ThreadPoolDefinition, ResourceServiceConfiguratorFactory, BiConsumer<ResourceTransformationDescriptionBuilder, ModelVersion> {

    DEFAULT("default", 0, 200, 0, 60000L, null) {
        @Override
        public void accept(ResourceTransformationDescriptionBuilder builder, ModelVersion version) {
            if (JGroupsModel.VERSION_6_0_0.requiresTransformation(version)) {
                builder.getAttributeBuilder().setValueConverter(AttributeConverter.DEFAULT_VALUE, this.getMinThreads().getName(), this.getMaxThreads().getName(), this.getQueueLength().getName());
            }
        }
    },
    @Deprecated OOB("oob", 20, 200, 0, 60000L, JGroupsModel.VERSION_6_0_0),
    @Deprecated INTERNAL("internal", 5, 20, 0, 60000L, JGroupsModel.VERSION_6_0_0),
    @Deprecated TIMER("timer", 2, 4, 0, 5000L, JGroupsModel.VERSION_6_0_0),
    ;

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    private static PathElement pathElement(String name) {
        return PathElement.pathElement("thread-pool", name);
    }

    private final PathElement path;
    private final JGroupsModel deprecation;
    private final Attribute minThreads;
    private final Attribute maxThreads;
    private final Attribute queueLength;
    private final Attribute keepAliveTime;

    ThreadPoolResourceDefinition(String name, int defaultMinThreads, int defaultMaxThreads, int defaultQueueLength, long defaultKeepAliveTime, JGroupsModel deprecation) {
        this.path = pathElement(name);
        this.deprecation = deprecation;
        this.minThreads = new SimpleAttribute(createBuilder("min-threads", ModelType.INT, new ModelNode(defaultMinThreads), new IntRangeValidatorBuilder().min(0), deprecation).build());
        this.maxThreads = new SimpleAttribute(createBuilder("max-threads", ModelType.INT, new ModelNode(defaultMaxThreads), new IntRangeValidatorBuilder().min(0), deprecation).build());
        this.queueLength = new SimpleAttribute(createBuilder("queue-length", ModelType.INT, new ModelNode(defaultQueueLength), new IntRangeValidatorBuilder().min(0), deprecation).setDeprecated(JGroupsModel.VERSION_6_0_0.getVersion()).build());
        this.keepAliveTime = new SimpleAttribute(createBuilder("keepalive-time", ModelType.LONG, new ModelNode(defaultKeepAliveTime), new LongRangeValidatorBuilder().min(0), deprecation).build());
    }

    private static SimpleAttributeDefinitionBuilder createBuilder(String name, ModelType type, ModelNode defaultValue, ParameterValidatorBuilder validatorBuilder, JGroupsModel deprecation) {
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
    public void register(ManagementResourceRegistration parentRegistration) {
        ResourceDescriptionResolver resolver = JGroupsExtension.SUBSYSTEM_RESOLVER.createChildResolver(this.path, pathElement(PathElement.WILDCARD_VALUE));
        SimpleResourceDefinition.Parameters parameters = new SimpleResourceDefinition.Parameters(this.path, resolver);
        if (this.deprecation != null) {
            parameters.setDeprecatedSince(this.deprecation.getVersion());
        }
        ResourceDefinition definition = new SimpleResourceDefinition(parameters);
        ManagementResourceRegistration registration = parentRegistration.registerSubModel(definition);

        ResourceDescriptor descriptor = new ResourceDescriptor(resolver)
                .addAttributes(this.minThreads, this.maxThreads, this.keepAliveTime)
                .addIgnoredAttributes(this.queueLength)
                ;
        ResourceServiceHandler handler = (this.deprecation == null) ? new SimpleResourceServiceHandler(this) : null;
        new SimpleResourceRegistration(descriptor, handler).register(registration);
    }

    @Override
    public ResourceServiceConfigurator createServiceConfigurator(PathAddress address) {
        return new ThreadPoolFactoryServiceConfigurator(this, address);
    }

    Collection<Attribute> getAttributes() {
        return Arrays.asList(this.minThreads, this.maxThreads, this.keepAliveTime);
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
    public Attribute getKeepAliveTime() {
        return this.keepAliveTime;
    }

    Attribute getQueueLength() {
        return this.queueLength;
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public void buildTransformation(ResourceTransformationDescriptionBuilder parent, ModelVersion version) {
        ResourceTransformationDescriptionBuilder builder = parent.addChildResource(this.path);
        this.accept(builder, version);
    }

    @Override
    public void accept(ResourceTransformationDescriptionBuilder builder, ModelVersion version) {
        if (JGroupsModel.VERSION_6_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder().setValueConverter(AttributeConverter.DEFAULT_VALUE, this.queueLength.getName());
        }

        if (JGroupsModel.VERSION_5_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder().setValueConverter(AttributeConverter.DEFAULT_VALUE, this.minThreads.getName(), this.maxThreads.getName());
        }
    }
}