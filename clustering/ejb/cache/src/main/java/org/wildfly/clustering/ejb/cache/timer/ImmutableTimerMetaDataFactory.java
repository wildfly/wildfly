/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import org.wildfly.clustering.ee.Locator;
import org.wildfly.clustering.ejb.timer.ImmutableTimerMetaData;

/**
 * @author Paul Ferraro
 * @param <I> the timer identifier type
 * @param <V> the timer metadata value type
 */
public interface ImmutableTimerMetaDataFactory<I, V> extends Locator<I, V> {

    ImmutableTimerMetaData createImmutableTimerMetaData(V value);
}
