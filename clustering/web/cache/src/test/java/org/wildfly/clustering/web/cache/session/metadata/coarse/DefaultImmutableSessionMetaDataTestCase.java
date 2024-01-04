/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.coarse;

import static org.mockito.Mockito.doReturn;

import java.time.Duration;
import java.time.Instant;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.wildfly.clustering.ee.cache.offset.OffsetValue;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * @author Paul Ferraro
 */
public class DefaultImmutableSessionMetaDataTestCase {

    private final ImmutableSessionMetaDataEntry entry;
    private final ImmutableSessionMetaData metaData;

    public DefaultImmutableSessionMetaDataTestCase() {
        this(Mockito.mock(ImmutableSessionMetaDataEntry.class));
    }

    private DefaultImmutableSessionMetaDataTestCase(ImmutableSessionMetaDataEntry entry) {
        this(entry, new DefaultImmutableSessionMetaData(entry));
    }

    DefaultImmutableSessionMetaDataTestCase(ImmutableSessionMetaDataEntry entry, ImmutableSessionMetaData metaData) {
        this.entry = entry;
        this.metaData = metaData;
    }

    @Test
    public void testCreationTime() {
        Instant expected = Instant.now();

        doReturn(expected).when(this.entry).getCreationTime();

        Instant result = this.metaData.getCreationTime();

        Assert.assertSame(expected, result);
    }

    @Test
    public void testLastAccessStartTime() {
        Instant expected = Instant.now();
        OffsetValue<Instant> lastAccessStartTime = Mockito.mock(OffsetValue.class);

        doReturn(lastAccessStartTime).when(this.entry).getLastAccessStartTime();
        doReturn(expected).when(lastAccessStartTime).get();

        Instant result = this.metaData.getLastAccessStartTime();

        Assert.assertSame(expected, result);
    }

    @Test
    public void testLastAccessEndTime() {
        Instant expected = Instant.now();
        OffsetValue<Instant> lastAccessEndTime = Mockito.mock(OffsetValue.class);

        doReturn(lastAccessEndTime).when(this.entry).getLastAccessEndTime();
        doReturn(expected).when(lastAccessEndTime).get();

        Instant result = this.metaData.getLastAccessEndTime();

        Assert.assertEquals(expected, result);
        Assert.assertEquals(expected, this.metaData.getLastAccessTime());
    }

    @Test
    public void testTimeout() {
        Duration expected = Duration.ofMinutes(60);

        doReturn(expected).when(this.entry).getTimeout();

        Duration result = this.metaData.getTimeout();

        Assert.assertSame(expected, result);
    }
}
