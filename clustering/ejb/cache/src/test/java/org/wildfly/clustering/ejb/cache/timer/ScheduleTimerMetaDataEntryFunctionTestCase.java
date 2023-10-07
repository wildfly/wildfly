/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map.Entry;
import java.util.UUID;

import org.mockito.Mockito;
import org.wildfly.clustering.ee.Key;
import org.wildfly.clustering.ee.cache.offset.OffsetValue;
import org.wildfly.clustering.ejb.timer.ScheduleTimerConfiguration;

/**
 * Schedule variant of Unit test for {@link TimerMetaDataEntryFunction}
 * @author Paul Ferraro
 */
public class ScheduleTimerMetaDataEntryFunctionTestCase extends AbstractScheduleTimerMetaDataEntryTestCase {

    public ScheduleTimerMetaDataEntryFunctionTestCase(Entry<ScheduleTimerConfiguration, Method> entry) {
        super(entry);
    }

    @Override
    public void accept(ScheduleTimerMetaDataEntry<UUID> entry) {
        OffsetValue<Duration> lastTimeoutOffset = OffsetValue.from(entry.getLastTimeout());

        MutableTimerMetaDataEntry<UUID> mutableEntry = new MutableTimerMetaDataEntry<>(entry, lastTimeoutOffset);

        this.updateState(mutableEntry);

        this.verifyOriginalState(entry);

        Key<String> key = Mockito.mock(Key.class);
        RemappableTimerMetaDataEntry<UUID> resultEntry = new TimerMetaDataEntryFunction<UUID>(lastTimeoutOffset).apply(key, entry);

        Mockito.verifyNoInteractions(key);

        this.verifyUpdatedState((ScheduleTimerMetaDataEntry<UUID>) resultEntry);
    }
}
