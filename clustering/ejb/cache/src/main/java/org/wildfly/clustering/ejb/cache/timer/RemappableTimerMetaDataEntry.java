/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.time.Duration;
import java.util.function.Supplier;

import org.wildfly.clustering.ee.cache.function.Remappable;
import org.wildfly.clustering.ee.cache.offset.Offset;

/**
 * @author Paul Ferraro
 */
public interface RemappableTimerMetaDataEntry<C> extends TimerMetaDataEntry<C>, Remappable<RemappableTimerMetaDataEntry<C>, Supplier<Offset<Duration>>> {
}
