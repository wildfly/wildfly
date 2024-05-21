/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.time.Duration;
import java.util.UUID;

import org.mockito.Mockito;
import org.wildfly.clustering.cache.Key;
import org.wildfly.clustering.ejb.timer.IntervalTimerConfiguration;
import org.wildfly.clustering.server.offset.OffsetValue;

/**
 * Interval variant of Unit test for {@link TimerMetaDataEntryFunction}
 * @author Paul Ferraro
 */
public class IntervalTimerMetaDataEntryFunctionTestCase extends AbstractIntervalTimerMetaDataEntryTestCase {

    public IntervalTimerMetaDataEntryFunctionTestCase(IntervalTimerConfiguration config) {
        super(config);
    }

    @Override
    public void accept(IntervalTimerMetaDataEntry<UUID> entry) {
        OffsetValue<Duration> lastTimeoutOffset = OffsetValue.from(entry.getLastTimeout());

        MutableTimerMetaDataEntry<UUID> mutableEntry = new MutableTimerMetaDataEntry<>(entry, lastTimeoutOffset);

        this.updateState(mutableEntry);

        this.verifyOriginalState(entry);

        Key<String> key = Mockito.mock(Key.class);
        RemappableTimerMetaDataEntry<UUID> resultEntry = new TimerMetaDataEntryFunction<UUID>(lastTimeoutOffset).apply(key, entry);

        Mockito.verifyNoInteractions(key);

        this.verifyUpdatedState((IntervalTimerMetaDataEntry<UUID>) resultEntry);
    }
}
