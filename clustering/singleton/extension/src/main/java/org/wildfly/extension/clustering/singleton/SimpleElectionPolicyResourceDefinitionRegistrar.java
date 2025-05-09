/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.singleton.election.SingletonElectionPolicy;
import org.wildfly.subsystem.resource.ResourceDescriptor;

/**
 * Registers a resource definition for a simple election policy.
 * @author Paul Ferraro
 */
public class SimpleElectionPolicyResourceDefinitionRegistrar extends ElectionPolicyResourceDefinitionRegistrar {

    static final AttributeDefinition POSITION = new SimpleAttributeDefinitionBuilder("position", ModelType.INT)
            .setAllowExpression(true)
            .setRequired(false)
            .setDefaultValue(ModelNode.ZERO)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    SimpleElectionPolicyResourceDefinitionRegistrar() {
        super(ElectionPolicyResourceRegistration.SIMPLE);
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder).addAttributes(List.of(POSITION));
    }

    @Override
    public SingletonElectionPolicy resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        int position = POSITION.resolveModelAttribute(context, model).asInt();
        return SingletonElectionPolicy.position(position);
    }
}
