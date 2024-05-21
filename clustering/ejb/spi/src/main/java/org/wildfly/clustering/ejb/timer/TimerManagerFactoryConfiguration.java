/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.timer;

import org.wildfly.clustering.server.manager.ManagerConfiguration;

/**
 * Encapsulates the configuration of a {@link TimerManagerFactory}.
 * @author Paul Ferraro
 * @param <I> the timer identifier type
 */
public interface TimerManagerFactoryConfiguration<I> extends ManagerConfiguration<I> {

    TimerServiceConfiguration getTimerServiceConfiguration();
    TimerRegistry<I> getRegistry();
    boolean isPersistent();
}
