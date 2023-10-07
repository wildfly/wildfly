/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import org.wildfly.clustering.ee.Locator;
import org.wildfly.clustering.ejb.timer.ImmutableTimerMetaData;

/**
 * @author Paul Ferraro
 */
public interface ImmutableTimerMetaDataFactory<I, V, C> extends Locator<I, V> {

    ImmutableTimerMetaData createImmutableTimerMetaData(V value);
}
