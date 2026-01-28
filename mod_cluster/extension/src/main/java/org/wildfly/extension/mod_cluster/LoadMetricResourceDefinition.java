/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.mod_cluster;

import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ReloadRequiredResourceRegistrar;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modcluster.load.metric.LoadMetric;
import org.wildfly.subsystem.resource.PropertiesAttributeDefinition;

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
                    .setRestartAllServices()
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
                return builder.setAllowExpression(false).setValidator(EnumValidator.create(LoadMetricEnum.class));
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

        new ReloadRequiredResourceRegistrar(descriptor).register(registration);

        return registration;
    }

}
