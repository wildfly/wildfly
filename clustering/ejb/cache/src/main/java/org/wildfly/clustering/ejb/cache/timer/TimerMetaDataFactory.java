/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.util.Map;

import org.wildfly.clustering.cache.CacheEntryCreator;
import org.wildfly.clustering.cache.CacheEntryRemover;
import org.wildfly.clustering.ejb.timer.TimerMetaData;

/**
 * @author Paul Ferraro
 * @param <I> the timer identifier type
 * @param <V> the timer metadata value type
 */
public interface TimerMetaDataFactory<I, V> extends ImmutableTimerMetaDataFactory<I, V>, CacheEntryCreator<I, V, Map.Entry<V, TimerIndex>>, CacheEntryRemover<I> {

    TimerMetaData createTimerMetaData(I id, V value);
}
