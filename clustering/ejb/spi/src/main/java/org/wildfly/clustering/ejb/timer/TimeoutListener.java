/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.timer;

import java.util.concurrent.ExecutionException;

/**
 * A listener to be invoked on timeout of a timer.
 * @author Paul Ferraro
 * @param <I> the timer identifier type
 */
public interface TimeoutListener<I> {

    void timeout(TimerManager<I> manager, Timer<I> timer) throws ExecutionException;
}
