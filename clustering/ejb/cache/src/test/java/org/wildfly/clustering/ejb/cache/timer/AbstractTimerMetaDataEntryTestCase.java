/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.ejb.timer.TimerConfiguration;

/**
 * Abstract unit test for validating timer metadata entries.
 * @author Paul Ferraro
 */
public abstract class AbstractTimerMetaDataEntryTestCase<E extends RemappableTimerMetaDataEntry<UUID>> implements Consumer<E>, Function<UUID, E> {

    private final UUID context = UUID.randomUUID();
    private final TimerConfiguration config;
    private final Duration originalLastTimeout = Duration.ZERO;

    private final Duration updatedLastTimeout = Duration.ofSeconds(1);

    AbstractTimerMetaDataEntryTestCase(TimerConfiguration config) {
        this.config = config;
    }

    @Test
    public void test() {
        E entry = this.apply(this.context);

        // Verify defaults
        this.verifyDefaultState(entry);

        // Apply original state
        entry.setLastTimeout(this.originalLastTimeout);

        this.verifyOriginalState(entry);

        this.accept(entry);
    }

    void updateState(TimerMetaDataEntry<UUID> entry) {
        entry.setLastTimeout(this.updatedLastTimeout);
    }

    void verifyDefaultState(E entry) {
        Assert.assertEquals(this.config.getStart(), entry.getStart());
        Assert.assertEquals(this.context, entry.getContext());
        Assert.assertNull(entry.getLastTimeout());
    }

    void verifyOriginalState(E entry) {
        Assert.assertEquals(this.config.getStart(), entry.getStart());
        Assert.assertEquals(this.context, entry.getContext());
        Assert.assertEquals(this.originalLastTimeout, entry.getLastTimeout());
    }

    void verifyUpdatedState(E entry) {
        Assert.assertEquals(this.config.getStart(), entry.getStart());
        Assert.assertEquals(this.context, entry.getContext());
        Assert.assertEquals(this.updatedLastTimeout, entry.getLastTimeout());
    }
}
