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
import org.jboss.as.controller.operations.validation.ModuleNameValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.mod_cluster.LoadMetricResourceDefinition.SharedAttribute;

/**
 * @author Radoslav Husar
 */
public class CustomLoadMetricResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static PathElement pathElement(String loadMetric) {
        return PathElement.pathElement("custom-load-metric", loadMetric);
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        CLASS("class", ModelType.STRING),
        MODULE("module", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder
                        .setDefaultValue(new ModelNode("org.wildfly.extension.mod_cluster"))
                        .setRequired(false)
                        .setValidator(ModuleNameValidator.INSTANCE)
                        ;
            }
        },
        ;

        private final AttributeDefinition definition;

        Attribute(String name, ModelType type) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(true)
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

    CustomLoadMetricResourceDefinition() {
        super(WILDCARD_PATH, ModClusterExtension.SUBSYSTEM_RESOLVER.createChildResolver(LoadMetricResourceDefinition.WILDCARD_PATH));
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
