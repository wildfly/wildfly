/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;
import java.util.function.Predicate;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.ejb.timer.TimerType;
import org.wildfly.clustering.server.offset.OffsetValue;

/**
 * @author Paul Ferraro
 */
public class MutableTimerMetaDataEntryTestCase {

    private final ImmutableTimerMetaDataEntry<UUID> entry = mock(ImmutableTimerMetaDataEntry.class);
    private final OffsetValue<Duration> lastTimeoutValue = mock(OffsetValue.class);

    private final TimerMetaDataEntry<UUID> subject = new MutableTimerMetaDataEntry<>(this.entry, this.lastTimeoutValue);

    @Test
    public void getType() {
        for (TimerType type : EnumSet.allOf(TimerType.class)) {
            doReturn(type).when(this.entry).getType();

            verifyNoInteractions(this.lastTimeoutValue);

            Assert.assertSame(type, this.subject.getType());
        }
    }

    @Test
    public void getContext() {
        UUID expected = UUID.randomUUID();

        doReturn(expected).when(this.entry).getContext();

        verifyNoInteractions(this.lastTimeoutValue);

        Assert.assertSame(expected, this.subject.getContext());
    }

    @Test
    public void getStart() {
        Instant expected = Instant.now();

        doReturn(expected).when(this.entry).getStart();

        verifyNoInteractions(this.entry);

        Assert.assertSame(expected, this.subject.getStart());
    }

    @Test
    public void getTimeoutMatcher() {
        Predicate<Method> expected = method -> true;

        doReturn(expected).when(this.entry).getTimeoutMatcher();

        verifyNoInteractions(this.entry);

        Assert.assertSame(expected, this.subject.getTimeoutMatcher());
    }

    @Test
    public void getLastTimeout() {
        Duration expected = Duration.ofSeconds(10);

        doReturn(expected).when(this.lastTimeoutValue).get();

        verifyNoInteractions(this.entry);

        Assert.assertSame(expected, this.subject.getLastTimeout());
    }

    @Test
    public void setLastTimeout() {
        Duration timeout = Duration.ofSeconds(10);

        this.subject.setLastTimeout(timeout);

        verify(this.lastTimeoutValue).set(timeout);
        verifyNoInteractions(this.entry);
    }

    @Test
    public void apply() {
        Instant now = Instant.now();
        Instant expected = now.plus(Duration.ofMinutes(1));

        doReturn(expected).when(this.entry).apply(now);

        verifyNoInteractions(this.lastTimeoutValue);

        Assert.assertSame(expected, this.subject.apply(now));
    }
}
