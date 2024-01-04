/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import org.jboss.as.controller.PathAddress;
import org.wildfly.clustering.singleton.SingletonElectionPolicy;
import org.wildfly.clustering.singleton.election.RandomSingletonElectionPolicy;

/**
 * @author Paul Ferraro
 */
public class RandomElectionPolicyServiceConfigurator extends ElectionPolicyServiceConfigurator {

    public RandomElectionPolicyServiceConfigurator(PathAddress policyAddress) {
        super(policyAddress);
    }

    @Override
    public SingletonElectionPolicy get() {
        return new RandomSingletonElectionPolicy();
    }
}
