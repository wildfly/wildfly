/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.coarse;

import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ee.cache.offset.OffsetValue;
import org.wildfly.clustering.web.cache.session.metadata.InvalidatableSessionMetaData;

/**
 * @author Paul Ferraro
 */
public class DefaultSessionMetaDataTestCase extends DefaultImmutableSessionMetaDataTestCase {

    private final MutableSessionMetaDataEntry entry;
    private final Mutator mutator;
    private final InvalidatableSessionMetaData metaData;

    public DefaultSessionMetaDataTestCase() {
        this(Mockito.mock(MutableSessionMetaDataEntry.class), Mockito.mock(Mutator.class));
    }

    private DefaultSessionMetaDataTestCase(MutableSessionMetaDataEntry entry, Mutator mutator) {
        this(entry, mutator, new DefaultSessionMetaData(entry, mutator));
    }

    private DefaultSessionMetaDataTestCase(MutableSessionMetaDataEntry entry, Mutator mutator, InvalidatableSessionMetaData metaData) {
        super(entry, metaData);
        this.entry = entry;
        this.mutator = mutator;
        this.metaData = metaData;
    }

    @Override
    public void testCreationTime() {
        super.testCreationTime();
        Mockito.verifyNoInteractions(this.mutator);
    }

    @Override
    public void testLastAccessStartTime() {
        super.testLastAccessStartTime();
        Mockito.verifyNoInteractions(this.mutator);
    }

    @Override
    public void testLastAccessEndTime() {
        super.testLastAccessEndTime();
        Mockito.verifyNoInteractions(this.mutator);
    }

    @Override
    public void testTimeout() {
        super.testTimeout();
        Mockito.verifyNoInteractions(this.mutator);
    }

    @Test
    public void setLastAccess() {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minus(Duration.ofMillis(500));
        OffsetValue<Instant> lastAccessStartTime = Mockito.mock(OffsetValue.class);
        OffsetValue<Instant> lastAccessEndTime = Mockito.mock(OffsetValue.class);

        ArgumentCaptor<Instant> lastAccessStartTimeCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> lastAccessEndTimeCaptor = ArgumentCaptor.forClass(Instant.class);

        doReturn(lastAccessStartTime).when(this.entry).getLastAccessStartTime();
        doReturn(lastAccessEndTime).when(this.entry).getLastAccessEndTime();

        doNothing().when(lastAccessStartTime).set(lastAccessStartTimeCaptor.capture());
        doNothing().when(lastAccessEndTime).set(lastAccessEndTimeCaptor.capture());

        this.metaData.setLastAccess(startTime, endTime);

        Instant normalizedStartTime = lastAccessStartTimeCaptor.getValue();
        Instant normalizedEndTime = lastAccessEndTimeCaptor.getValue();

        // Verify millisecond precision
        Assert.assertEquals(0, normalizedStartTime.getNano() % Duration.ofMillis(1).getNano());
        Assert.assertEquals(startTime.toEpochMilli(), normalizedStartTime.toEpochMilli());

        // Verify second precision
        Duration lastAccessDuration = Duration.between(normalizedStartTime, normalizedEndTime);
        Assert.assertEquals(1, lastAccessDuration.getSeconds());
        Assert.assertEquals(0, lastAccessDuration.getNano());

        Mockito.verifyNoInteractions(this.mutator);
    }

    @Test
    public void setTimeout() {
        Duration timeout = Duration.ofHours(1);

        this.metaData.setTimeout(timeout);

        Mockito.verify(this.entry).setTimeout(timeout);

        Mockito.verifyNoInteractions(this.mutator);
    }

    @Test
    public void invalidate() {
        Assert.assertTrue(this.metaData.isValid());

        this.metaData.invalidate();

        Mockito.verifyNoInteractions(this.entry);
        Mockito.verifyNoInteractions(this.mutator);

        Assert.assertFalse(this.metaData.isValid());
    }

    @Test
    public void close() {
        this.metaData.close();

        Mockito.verifyNoInteractions(this.entry);
        Mockito.verify(this.mutator).mutate();
    }
}
