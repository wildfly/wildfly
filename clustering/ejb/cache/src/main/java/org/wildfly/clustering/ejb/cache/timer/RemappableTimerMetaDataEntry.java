/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.time.Duration;
import java.util.function.Supplier;

import org.wildfly.clustering.cache.function.Remappable;
import org.wildfly.clustering.server.offset.Offset;

/**
 * @author Paul Ferraro
 * @param <C> the timer context type
 */
public interface RemappableTimerMetaDataEntry<C> extends TimerMetaDataEntry<C>, Remappable<RemappableTimerMetaDataEntry<C>, Supplier<Offset<Duration>>> {
}
