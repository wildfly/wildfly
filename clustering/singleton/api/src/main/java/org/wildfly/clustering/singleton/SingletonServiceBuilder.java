/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.singleton;

import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.singleton.service.SingletonServiceConfigurator;

/**
 * Builds a singleton service.
 * @author Paul Ferraro
 * @param <T> the singleton service value type
 * @deprecated Replaced by {@link SingletonServiceConfigurator}.
 */
@Deprecated(forRemoval = true)
public interface SingletonServiceBuilder<T> extends Builder<T> {

    /**
     * Defines the minimum number of members required before a singleton election will take place.
     * @param quorum the quorum required for electing a primary singleton provider
     * @return a reference to this builder
     */
    SingletonServiceBuilder<T> requireQuorum(int quorum);

    /**
     * Defines the policy for electing a primary singleton provider.
     * @param policy an election policy
     * @return a reference to this builder
     */
    SingletonServiceBuilder<T> electionPolicy(SingletonElectionPolicy policy);

    /**
     * Defines a listener to trigger following the election of a primary singleton provider.
     * @param listener an election listener
     * @return a reference to this builder
     */
    SingletonServiceBuilder<T> electionListener(SingletonElectionListener listener);
}
