/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.timer;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;

/**
 * Provides timer management service installation mechanics for a component.
 * @author Paul Ferraro
 */
public interface TimerManagementProvider {
    <I> CapabilityServiceConfigurator getTimerManagerFactoryServiceConfigurator(TimerManagerFactoryConfiguration<I> config);
}
