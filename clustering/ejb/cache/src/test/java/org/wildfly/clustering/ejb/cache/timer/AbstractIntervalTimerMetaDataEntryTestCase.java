/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.wildfly.clustering.ejb.timer.IntervalTimerConfiguration;
import org.wildfly.clustering.ejb.timer.TimerType;

/**
 * Abstract unit test for validating interval-based timer metadata entries.
 * @author Paul Ferraro
 */
@RunWith(Parameterized.class)
public abstract class AbstractIntervalTimerMetaDataEntryTestCase extends AbstractTimerMetaDataEntryTestCase<IntervalTimerMetaDataEntry<UUID>> {

    @Parameters
    public static Iterable<IntervalTimerConfiguration> parameters() {
        Instant start = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        return List.of(new IntervalTimerConfiguration() {
            @Override
            public Instant getStart() {
                return start;
            }
        }, new IntervalTimerConfiguration() {
            @Override
            public Instant getStart() {
                return start;
            }

            @Override
            public Duration getInterval() {
                return Duration.ofSeconds(10);
            }
        });
    }

    private final IntervalTimerConfiguration config;

    AbstractIntervalTimerMetaDataEntryTestCase(IntervalTimerConfiguration config) {
        super(config);
        this.config = config;
    }

    @Override
    public IntervalTimerMetaDataEntry<UUID> apply(UUID context) {
        return new IntervalTimerMetaDataEntry<>(context, this.config);
    }

    @Override
    void verifyDefaultState(IntervalTimerMetaDataEntry<UUID> entry) {
        super.verifyDefaultState(entry);
        this.verifyState(entry);
    }

    @Override
    void verifyOriginalState(IntervalTimerMetaDataEntry<UUID> entry) {
        super.verifyOriginalState(entry);
        this.verifyState(entry);
    }

    @Override
    void verifyUpdatedState(IntervalTimerMetaDataEntry<UUID> entry) {
        super.verifyUpdatedState(entry);
        this.verifyState(entry);
    }

    private void verifyState(IntervalTimerMetaDataEntry<UUID> entry) {
        Assert.assertSame(TimerType.INTERVAL, entry.getType());
        Assert.assertSame(DefaultTimeoutMatcher.INSTANCE, entry.getTimeoutMatcher());
        Assert.assertEquals(this.config.getInterval(), entry.getInterval());
    }
}
