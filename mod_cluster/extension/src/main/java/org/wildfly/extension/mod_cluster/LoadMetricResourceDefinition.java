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

package org.wildfly.extension.mod_cluster;

import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.PropertiesAttributeDefinition;
import org.jboss.as.clustering.controller.ReloadRequiredResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.validation.EnumValidator;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modcluster.load.metric.LoadMetric;

/**
 * Definition for resource at address /subsystem=modcluster/proxy=X/dynamic-load-provider=configuration/load-metric=Z
 *
 * @author Radoslav Husar
 */
class LoadMetricResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static PathElement pathElement(String loadMetric) {
        return PathElement.pathElement("load-metric", loadMetric);
    }

    enum SharedAttribute implements org.jboss.as.clustering.controller.Attribute {
        WEIGHT("weight", ModelType.INT, new ModelNode(LoadMetric.DEFAULT_WEIGHT)),
        CAPACITY("capacity", ModelType.DOUBLE, new ModelNode(LoadMetric.DEFAULT_CAPACITY)),
        PROPERTY(ModelDescriptionConstants.PROPERTY),
        ;

        private final AttributeDefinition definition;

        SharedAttribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .setRestartAllServices()
                    .build();
        }

        SharedAttribute(String name) {
            this.definition = new PropertiesAttributeDefinition.Builder(name)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setRestartAllServices()
                    .setAttributeMarshaller(AttributeMarshaller.PROPERTIES_MARSHALLER_UNWRAPPED)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        TYPE("type", ModelType.STRING, null) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(false).setValidator(new EnumValidator<>(LoadMetricEnum.class));
            }
        },
        ;

        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(defaultValue == null)
                    .setDefaultValue(defaultValue)
                    .setRestartAllServices()
            ).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }

        @Override
        public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
            return builder;
        }
    }

    LoadMetricResourceDefinition() {
        super(WILDCARD_PATH, ModClusterExtension.SUBSYSTEM_RESOLVER.createChildResolver(WILDCARD_PATH));
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(SharedAttribute.class)
                .addAttributes(Attribute.class)
                ;

        new ReloadRequiredResourceRegistration(descriptor).register(registration);

        return registration;
    }

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder builder) {
        // Nothing to transform
    }
}
