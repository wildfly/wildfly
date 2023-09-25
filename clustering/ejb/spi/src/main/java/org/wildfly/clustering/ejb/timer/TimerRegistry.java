/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.timer;

/**
 * Exposes the mechanism for registering arbitrary timers with the system, e.g. management model.
 * @author Paul Ferraro
 * @param <I> the timer identifier type
 */
public interface TimerRegistry<I> {

    void register(I id);

    void unregister(I id);
}
