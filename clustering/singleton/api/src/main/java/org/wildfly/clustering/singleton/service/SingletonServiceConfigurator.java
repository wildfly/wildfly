/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.service;

import org.wildfly.clustering.singleton.SingletonElectionListener;
import org.wildfly.clustering.singleton.SingletonElectionPolicy;

/**
 * Extension of {@link ImmutableSingletonServiceConfigurator} for customizing singleton service behavior.
 * @author Paul Ferraro
 * @deprecated Superseded by {@link SingletonServiceTarget}.
 */
@Deprecated(forRemoval = true)
public interface SingletonServiceConfigurator extends ImmutableSingletonServiceConfigurator {

    /**
     * Defines the minimum number of members required before a singleton election will take place.
     * @param quorum the quorum required for electing a primary singleton provider
     * @return a reference to this configurator
     */
    SingletonServiceConfigurator requireQuorum(int quorum);

    /**
     * Defines the policy for electing a primary singleton provider.
     * @param policy an election policy
     * @return a reference to this configurator
     */
    SingletonServiceConfigurator electionPolicy(SingletonElectionPolicy policy);

    /**
     * Defines a listener to trigger following the election of a primary singleton provider.
     * @param listener an election listener
     * @return a reference to this configurator
     */
    SingletonServiceConfigurator electionListener(SingletonElectionListener listener);
}