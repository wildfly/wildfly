/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.mod_cluster;

import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ReloadRequiredResourceRegistrar;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Definition for resource at address /subsystem=modcluster/proxy=X/load-provider=simple
 *
 * @author Radoslav Husar
 */
public class SimpleLoadProviderResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

    public static final PathElement PATH = PathElement.pathElement("load-provider", "simple");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        FACTOR("factor", ModelType.INT, new ModelNode(1), new IntRangeValidator(0, 100, true, true)),
        ;

        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue, ParameterValidator validator) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(defaultValue == null)
                    .setDefaultValue(defaultValue)
                    .setValidator(validator)
                    .setRestartAllServices()
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    SimpleLoadProviderResourceDefinition() {
        super(PATH, ModClusterExtension.SUBSYSTEM_RESOLVER.createChildResolver(PATH));
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                ;

        new ReloadRequiredResourceRegistrar(descriptor).register(registration);

        return registration;
    }

}
