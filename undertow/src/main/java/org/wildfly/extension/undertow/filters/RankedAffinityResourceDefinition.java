/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.filters;

import org.jboss.as.clustering.controller.SimpleResourceDescriptorConfigurator;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.undertow.Constants;

/**
 * Affinity resource configuring ranked affinity handling which requires a delimiter to be specified.
 *
 * @author Radoslav Husar
 */
public class RankedAffinityResourceDefinition extends AffinityResourceDefinition {

    public static final PathElement PATH = pathElement(Constants.RANKED);

    public enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        DELIMITER(Constants.DELIMITER, ModelType.STRING, new ModelNode(".")),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setRequired(defaultValue == null)
                    .setAllowExpression(true)
                    .setDefaultValue(defaultValue)
                    .setRestartAllServices()
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    public RankedAffinityResourceDefinition() {
        super(PATH, new SimpleResourceDescriptorConfigurator<>(Attribute.class));
    }
}
