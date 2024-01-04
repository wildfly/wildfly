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
 */
public interface TimerMetaDataFactory<I, V, C> extends ImmutableTimerMetaDataFactory<I, V, C>, Creator<I, V, Map.Entry<V, TimerIndex>>, Remover<I> {

    TimerMetaData createTimerMetaData(I id, V value);
}
