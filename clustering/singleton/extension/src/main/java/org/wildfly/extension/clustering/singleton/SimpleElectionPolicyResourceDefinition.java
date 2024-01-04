/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import org.jboss.as.clustering.controller.SimpleResourceDescriptorConfigurator;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Definition of a simple election policy resource.
 * @author Paul Ferraro
 */
public class SimpleElectionPolicyResourceDefinition extends ElectionPolicyResourceDefinition {

    static final String PATH_VALUE = "simple";
    static final PathElement PATH = pathElement(PATH_VALUE);

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        POSITION("position", ModelType.INT, ModelNode.ZERO),
        ;
        private final SimpleAttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .build();
        }

        @Override
        public SimpleAttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    SimpleElectionPolicyResourceDefinition() {
        super(PATH, SingletonExtension.SUBSYSTEM_RESOLVER.createChildResolver(PATH, WILDCARD_PATH), new SimpleResourceDescriptorConfigurator<>(Attribute.class), SimpleElectionPolicyServiceConfigurator::new);
    }
}
