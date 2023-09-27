/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.timer;

import java.util.concurrent.ExecutionException;

import org.wildfly.clustering.ee.Batch;

/**
 * A listener to be invoked on timeout of a timer.
 * @author Paul Ferraro
 * @param <I> the timer identifier type
 * @param <B> the batch type
 */
public interface TimeoutListener<I, B extends Batch> {

    void timeout(TimerManager<I, B> manager, Timer<I> timer) throws ExecutionException;
}
