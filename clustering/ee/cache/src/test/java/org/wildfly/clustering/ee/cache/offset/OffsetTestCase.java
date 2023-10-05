/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.offset;

import java.time.Duration;
import java.time.Instant;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link Offset}.
 * @author Paul Ferraro
 */
public class OffsetTestCase {

    @Test
    public void duration() {
        Duration forward = Duration.ofSeconds(1);
        Duration backward = Duration.ofSeconds(-1);

        // Test zero offset
        Offset<Duration> offset = Offset.forDuration(Duration.ZERO);

        Assert.assertSame(backward, offset.apply(backward));
        Assert.assertSame(Duration.ZERO, offset.apply(Duration.ZERO));
        Assert.assertSame(forward, offset.apply(forward));

        // Test positive offset
        offset = Offset.forDuration(forward);

        Assert.assertEquals(Duration.ZERO, offset.apply(backward));
        Assert.assertEquals(forward, offset.apply(Duration.ZERO));

        // Test negative offset
        offset = Offset.forDuration(backward);

        Assert.assertEquals(backward, offset.apply(Duration.ZERO));
        Assert.assertEquals(Duration.ZERO, offset.apply(forward));
    }

    @Test
    public void instant() {
        Duration forward = Duration.ofSeconds(1);
        Duration backward = Duration.ofSeconds(-1);
        Instant present = Instant.now();
        Instant past = present.plus(backward);
        Instant future = present.plus(forward);

        // Test zero offset
        Offset<Instant> offset = Offset.forInstant(Duration.ZERO);

        Assert.assertSame(past, offset.apply(past));
        Assert.assertSame(present, offset.apply(present));
        Assert.assertSame(future, offset.apply(future));

        // Test positive offset
        offset = Offset.forInstant(forward);

        Assert.assertEquals(present, offset.apply(past));
        Assert.assertEquals(future, offset.apply(present));

        // Test negative offset
        offset = Offset.forInstant(backward);

        Assert.assertEquals(past, offset.apply(present));
        Assert.assertEquals(present, offset.apply(future));
    }
}
