/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import static org.wildfly.extension.clustering.singleton.SimpleElectionPolicyResourceDefinition.Attribute.POSITION;

import org.jboss.as.clustering.controller.SimpleResourceDescriptorConfigurator;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.singleton.election.SingletonElectionPolicy;

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
        super(PATH, SingletonExtension.SUBSYSTEM_RESOLVER.createChildResolver(PATH, WILDCARD_PATH), new SimpleResourceDescriptorConfigurator<>(Attribute.class));
    }

    @Override
    public SingletonElectionPolicy resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        int position = POSITION.resolveModelAttribute(context, model).asInt();
        return SingletonElectionPolicy.position(position);
    }
}
