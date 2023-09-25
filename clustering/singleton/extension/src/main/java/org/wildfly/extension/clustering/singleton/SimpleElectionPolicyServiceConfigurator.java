/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import static org.wildfly.extension.clustering.singleton.SimpleElectionPolicyResourceDefinition.Attribute.POSITION;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.singleton.SingletonElectionPolicy;
import org.wildfly.clustering.singleton.election.SimpleSingletonElectionPolicy;

/**
 * Builds a service that provides a simple election policy.
 * @author Paul Ferraro
 */
public class SimpleElectionPolicyServiceConfigurator extends ElectionPolicyServiceConfigurator {

    private volatile int position;

    public SimpleElectionPolicyServiceConfigurator(PathAddress policyAddress) {
        super(policyAddress);
    }

    @Override
    public SingletonElectionPolicy get() {
        return new SimpleSingletonElectionPolicy(this.position);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.position = POSITION.resolveModelAttribute(context, model).asInt();
        return super.configure(context, model);
    }
}
