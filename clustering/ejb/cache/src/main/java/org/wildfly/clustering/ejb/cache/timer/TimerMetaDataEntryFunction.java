/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.time.Duration;

import org.wildfly.clustering.cache.function.RemappingFunction;
import org.wildfly.clustering.function.Supplier;
import org.wildfly.clustering.server.offset.Offset;
import org.wildfly.clustering.server.offset.OffsetValue;

/**
 * @author Paul Ferraro
 * @param <C> the timer context type
 */
public class TimerMetaDataEntryFunction<C> extends RemappingFunction<RemappableTimerMetaDataEntry<C>, java.util.function.Supplier<Offset<Duration>>> {

    public TimerMetaDataEntryFunction(OffsetValue<Duration> lastTimeoutDelta) {
        super(lastTimeoutDelta::getOffset);
    }

    TimerMetaDataEntryFunction(Offset<Duration> lastTimeoutOffsetDelta) {
        super(Supplier.of(lastTimeoutOffsetDelta));
    }

    Offset<Duration> getOffset() {
        return super.getOperand().get();
    }
}
