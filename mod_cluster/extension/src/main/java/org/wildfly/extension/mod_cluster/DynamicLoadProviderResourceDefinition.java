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
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modcluster.load.impl.DynamicLoadBalanceFactorProvider;

/**
 * Definition for resource at address /subsystem=modcluster/proxy=X/load-provider=dynamic
 *
 * @author Radoslav Husar
 */
public class DynamicLoadProviderResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

    public static final PathElement PATH = PathElement.pathElement("load-provider", "dynamic");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        DECAY("decay", ModelType.DOUBLE, new ModelNode((double) DynamicLoadBalanceFactorProvider.DEFAULT_DECAY_FACTOR)),
        HISTORY("history", ModelType.INT, new ModelNode(DynamicLoadBalanceFactorProvider.DEFAULT_HISTORY)),
        INITIAL_LOAD("initial-load", ModelType.INT, ModelNode.ZERO) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder simpleAttributeDefinitionBuilder) {
                return simpleAttributeDefinitionBuilder.setValidator(new IntRangeValidator(-1, 100, true, true));
            }
        },
        ;

        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
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

    DynamicLoadProviderResourceDefinition() {
        super(PATH, ModClusterExtension.SUBSYSTEM_RESOLVER.createChildResolver(PATH));
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                ;

        new LoadMetricResourceDefinition().register(registration);
        new CustomLoadMetricResourceDefinition().register(registration);

        new ReloadRequiredResourceRegistrar(descriptor).register(registration);

        return registration;
    }

}
