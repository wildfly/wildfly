/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.util.Map;

import org.wildfly.clustering.ee.Creator;
import org.wildfly.clustering.ee.Remover;
import org.wildfly.clustering.ejb.timer.TimerMetaData;

/**
 * @author Paul Ferraro
 * @param <I> the timer identifier type
 * @param <V> the timer metadata value type
 */
public interface TimerMetaDataFactory<I, V> extends ImmutableTimerMetaDataFactory<I, V>, Creator<I, V, Map.Entry<V, TimerIndex>>, Remover<I> {

    TimerMetaData createTimerMetaData(I id, V value);
}
