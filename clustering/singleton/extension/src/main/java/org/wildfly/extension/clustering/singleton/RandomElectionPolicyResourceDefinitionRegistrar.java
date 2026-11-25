/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import org.jboss.as.controller.OperationContext;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.singleton.election.SingletonElectionPolicy;

/**
 * Registers a resource definition for a random election policy.
 * @author Paul Ferraro
 */
public class RandomElectionPolicyResourceDefinitionRegistrar extends ElectionPolicyResourceDefinitionRegistrar {

    RandomElectionPolicyResourceDefinitionRegistrar() {
        super(ElectionPolicyResourceRegistration.RANDOM);
    }

    @Override
    public SingletonElectionPolicy resolve(OperationContext context, ModelNode model) {
        return SingletonElectionPolicy.random();
    }
}
