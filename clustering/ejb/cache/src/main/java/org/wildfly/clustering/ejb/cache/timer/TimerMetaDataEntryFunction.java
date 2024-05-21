/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.time.Duration;
import java.util.function.Supplier;

import org.wildfly.clustering.cache.function.RemappingFunction;
import org.wildfly.clustering.server.offset.Offset;
import org.wildfly.clustering.server.offset.OffsetValue;
import org.wildfly.common.function.Functions;

/**
 * @author Paul Ferraro
 * @param <C> the timer context type
 */
public class TimerMetaDataEntryFunction<C> extends RemappingFunction<RemappableTimerMetaDataEntry<C>, Supplier<Offset<Duration>>> {

    public TimerMetaDataEntryFunction(OffsetValue<Duration> lastTimeoutDelta) {
        super(lastTimeoutDelta::getOffset);
    }

    TimerMetaDataEntryFunction(Offset<Duration> lastTimeoutOffsetDelta) {
        super(Functions.constantSupplier(lastTimeoutOffsetDelta));
    }

    Offset<Duration> getOffset() {
        return super.getOperand().get();
    }
}
