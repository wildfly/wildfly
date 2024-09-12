/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.service;

import org.jboss.msc.Service;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.wildfly.clustering.singleton.election.SingletonElectionListener;
import org.wildfly.clustering.singleton.election.SingletonElectionPolicy;

/**
 * A service builder for singleton service installation.
 * @author Paul Ferraro
 */
public interface SingletonServiceBuilder<T> extends ServiceBuilder<T> {

    @Override
    SingletonServiceController<T> install();

    @Override
    SingletonServiceBuilder<T> setInitialMode(Mode mode);

    @Override
    SingletonServiceBuilder<T> setInstance(Service service);

    @Override
    SingletonServiceBuilder<T> addListener(LifecycleListener listener);

    /**
     * Defines the minimum number of members required before a singleton election will take place.
     * @param quorum the quorum required for electing a primary singleton provider
     * @return a reference to this configurator
     */
    SingletonServiceBuilder<T> requireQuorum(int quorum);

    /**
     * Defines the policy for electing a primary singleton provider.
     * @param policy an election policy
     * @return a reference to this configurator
     */
    SingletonServiceBuilder<T> withElectionPolicy(SingletonElectionPolicy policy);

    /**
     * Defines a listener to trigger following the election of a primary singleton provider.
     * @param listener an election listener
     * @return a reference to this configurator
     */
    SingletonServiceBuilder<T> withElectionListener(SingletonElectionListener listener);
}
