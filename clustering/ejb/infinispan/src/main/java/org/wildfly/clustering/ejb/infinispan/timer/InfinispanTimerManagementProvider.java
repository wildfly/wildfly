/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.wildfly.clustering.ejb.timer.TimerManagementProvider;
import org.wildfly.clustering.ejb.timer.TimerManagerFactoryConfiguration;

/**
 * @author Paul Ferraro
 */
public class InfinispanTimerManagementProvider implements TimerManagementProvider {

    private final InfinispanTimerManagementConfiguration configuration;

    public InfinispanTimerManagementProvider(InfinispanTimerManagementConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public <I> CapabilityServiceConfigurator getTimerManagerFactoryServiceConfigurator(TimerManagerFactoryConfiguration<I> configuration) {
        return new InfinispanTimerManagerFactoryServiceConfigurator<>(this.configuration, configuration);
    }
}
