/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import static org.mockito.Mockito.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.wildfly.clustering.ejb.timer.ImmutableTimerMetaData;
import org.wildfly.clustering.ejb.timer.TimerConfiguration;
import org.wildfly.clustering.ejb.timer.TimerType;
import org.wildfly.clustering.marshalling.Marshaller;

/**
 * Unit test for {@link DefaultImmutableTimerMetaData}.
 * @author Paul Ferraro
 */
public class DefaultImmutableTimerMetaDataTestCase {

    private final TimerMetaDataConfiguration<Object> config = mock(TimerMetaDataConfiguration.class);
    private final ImmutableTimerMetaDataEntry<Object> entry = mock(ImmutableTimerMetaDataEntry.class);
    private final Marshaller<Object, Object> marshaller = mock(Marshaller.class);
    private ImmutableTimerMetaData metaData;

    @Before
    public void init() {
        doReturn(this.marshaller).when(this.config).getMarshaller();
        doReturn(true).when(this.config).isPersistent();

        this.metaData = new DefaultImmutableTimerMetaData<>(this.config, this.entry);
    }

    @Test
    public void getType() {
        for (TimerType type : EnumSet.allOf(TimerType.class)) {
            doReturn(type).when(this.entry).getType();

            Assert.assertSame(type, this.metaData.getType());
        }
    }

    @Test
    public void getContext() throws IOException {
        UUID marshalledContext = UUID.randomUUID();
        UUID context = UUID.randomUUID();

        doReturn(marshalledContext).when(this.entry).getContext();
        doReturn(context).when(this.marshaller).read(marshalledContext);

        Assert.assertSame(context, this.metaData.getContext());
    }

    @Test
    public void getTimeoutMatcher() {
        Predicate<Method> matcher = method -> true;

        doReturn(matcher).when(this.entry).getTimeoutMatcher();

        Assert.assertSame(matcher, this.metaData.getTimeoutMatcher());
    }

    @Test
    public void isPersistent() {
        Assert.assertTrue(this.metaData.isPersistent());
    }

    @Test
    public void getConfiguration() {
        Assert.assertSame(this.entry, this.metaData.getConfiguration(TimerConfiguration.class));
    }

    @Test
    public void getLastTimeout() {
        Instant start = Instant.now();

        doReturn(null).when(this.entry).getLastTimeout();
        doReturn(start).when(this.entry).getStart();

        Optional<Instant> result = this.metaData.getLastTimeout();

        Assert.assertFalse(result.isPresent());

        Duration lastTimeout = Duration.ofSeconds(10);

        doReturn(lastTimeout).when(this.entry).getLastTimeout();

        result = this.metaData.getLastTimeout();

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(lastTimeout, Duration.between(start, result.get()));
    }

    @Test
    public void getNextTimeout() {
        Instant start = Instant.now();

        doReturn(null).when(this.entry).getLastTimeout();
        doReturn(start).when(this.entry).getStart();

        Optional<Instant> result = this.metaData.getNextTimeout();

        Assert.assertTrue(result.isPresent());
        Assert.assertSame(start, result.get());

        Duration lastTimeoutDuration = Duration.ofSeconds(10);
        Instant lastTimeout = start.plus(lastTimeoutDuration);

        doReturn(lastTimeoutDuration).when(this.entry).getLastTimeout();
        doReturn(null).when(this.entry).apply(lastTimeout);

        result = this.metaData.getNextTimeout();

        Assert.assertFalse(result.isPresent());

        Instant nextTimeout = start.plus(Duration.ofMinutes(1));

        doReturn(nextTimeout).when(this.entry).apply(lastTimeout);

        result = this.metaData.getNextTimeout();

        Assert.assertTrue(result.isPresent());
        Assert.assertSame(nextTimeout, result.get());
    }
}
