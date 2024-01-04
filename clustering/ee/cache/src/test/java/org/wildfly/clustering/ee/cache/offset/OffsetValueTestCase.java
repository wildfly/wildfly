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
 * Unit test for {@link OffsetValue}.
 * @author Paul Ferraro
 */
public class OffsetValueTestCase {

    @Test
    public void duration() {
        Duration forward = Duration.ofSeconds(1);
        Duration backward = Duration.ofSeconds(-1);

        // Test from zero duration
        OffsetValue<Duration> value = OffsetValue.from(Duration.ZERO);

        Assert.assertTrue(value.getOffset().isZero());
        Assert.assertSame(Duration.ZERO, value.getBasis());
        Assert.assertSame(Duration.ZERO, value.get());
        Assert.assertSame(backward, value.getOffset().apply(backward));
        Assert.assertSame(Duration.ZERO, value.getOffset().apply(Duration.ZERO));
        Assert.assertSame(forward, value.getOffset().apply(forward));

        value.set(forward);

        Assert.assertFalse(value.getOffset().isZero());
        Assert.assertSame(Duration.ZERO, value.getBasis());
        Assert.assertEquals(forward, value.get());
        Assert.assertEquals(Duration.ZERO, value.getOffset().apply(backward));
        Assert.assertEquals(forward, value.getOffset().apply(Duration.ZERO));

        OffsetValue<Duration> rebaseValue = value.rebase();

        Assert.assertTrue(rebaseValue.getOffset().isZero());
        Assert.assertEquals(forward, rebaseValue.getBasis());
        Assert.assertEquals(forward, rebaseValue.get());
        Assert.assertEquals(Duration.ZERO, rebaseValue.getOffset().apply(Duration.ZERO));
        Assert.assertEquals(forward, rebaseValue.getOffset().apply(forward));

        value.set(backward);

        Assert.assertFalse(value.getOffset().isZero());
        Assert.assertSame(Duration.ZERO, value.getBasis());
        Assert.assertEquals(backward, value.get());
        Assert.assertEquals(backward, value.getOffset().apply(Duration.ZERO));
        Assert.assertEquals(Duration.ZERO, value.getOffset().apply(forward));

        // Verify rebase offset value reflects change in basis, but with unchanged offset
        Assert.assertTrue(rebaseValue.getOffset().isZero());
        Assert.assertEquals(value.get(), rebaseValue.getBasis());
        Assert.assertEquals(value.get(), rebaseValue.get());
        Assert.assertEquals(Duration.ZERO, rebaseValue.getOffset().apply(Duration.ZERO));
        Assert.assertEquals(forward, rebaseValue.getOffset().apply(forward));

        // Test from positive duration
        value = OffsetValue.from(forward);

        Assert.assertTrue(value.getOffset().isZero());
        Assert.assertSame(forward, value.getBasis());
        Assert.assertSame(forward, value.get());
        Assert.assertSame(backward, value.getOffset().apply(backward));
        Assert.assertSame(Duration.ZERO, value.getOffset().apply(Duration.ZERO));
        Assert.assertEquals(forward, value.getOffset().apply(forward));

        value.set(Duration.ZERO);

        Assert.assertFalse(value.getOffset().isZero());
        Assert.assertSame(forward, value.getBasis());
        Assert.assertSame(Duration.ZERO, value.get());
        Assert.assertEquals(backward, value.getOffset().apply(Duration.ZERO));
        Assert.assertEquals(Duration.ZERO, value.getOffset().apply(forward));

        // Test negative duration
        value = OffsetValue.from(backward);

        Assert.assertTrue(value.getOffset().isZero());
        Assert.assertSame(backward, value.getBasis());
        Assert.assertSame(backward, value.get());
        Assert.assertSame(backward, value.getOffset().apply(backward));
        Assert.assertSame(Duration.ZERO, value.getOffset().apply(Duration.ZERO));
        Assert.assertEquals(forward, value.getOffset().apply(forward));

        value.set(Duration.ZERO);

        Assert.assertFalse(value.getOffset().isZero());
        Assert.assertSame(backward, value.getBasis());
        Assert.assertSame(Duration.ZERO, value.get());
        Assert.assertEquals(Duration.ZERO, value.getOffset().apply(backward));
        Assert.assertEquals(forward, value.getOffset().apply(Duration.ZERO));
    }

    @Test
    public void instant() {
        Duration forward = Duration.ofSeconds(1);
        Duration backward = Duration.ofSeconds(-1);
        Instant present = Instant.now();
        Instant past = present.plus(backward);
        Instant future = present.plus(forward);

        OffsetValue<Instant> value = OffsetValue.from(present);

        Assert.assertTrue(value.getOffset().isZero());
        Assert.assertSame(present, value.getBasis());
        Assert.assertSame(present, value.get());
        Assert.assertSame(past, value.getOffset().apply(past));
        Assert.assertSame(present, value.getOffset().apply(present));
        Assert.assertSame(future, value.getOffset().apply(future));

        value.set(future);

        Assert.assertFalse(value.getOffset().isZero());
        Assert.assertEquals(present, value.getBasis());
        Assert.assertEquals(future, value.get());
        Assert.assertEquals(present, value.getOffset().apply(past));
        Assert.assertEquals(future, value.getOffset().apply(present));

        OffsetValue<Instant> rebaseValue = value.rebase();

        Assert.assertTrue(rebaseValue.getOffset().isZero());
        Assert.assertEquals(future, rebaseValue.getBasis());
        Assert.assertEquals(future, rebaseValue.get());
        Assert.assertSame(past, rebaseValue.getOffset().apply(past));
        Assert.assertSame(present, rebaseValue.getOffset().apply(present));
        Assert.assertSame(future, rebaseValue.getOffset().apply(future));

        value.set(past);

        Assert.assertFalse(value.getOffset().isZero());
        Assert.assertEquals(present, value.getBasis());
        Assert.assertEquals(past, value.get());
        Assert.assertEquals(past, value.getOffset().apply(present));
        Assert.assertEquals(present, value.getOffset().apply(future));

        // Verify rebase offset value reflects change in basis, but with unchanged offset
        Assert.assertTrue(rebaseValue.getOffset().isZero());
        Assert.assertEquals(past, rebaseValue.getBasis());
        Assert.assertEquals(past, rebaseValue.get());
        Assert.assertSame(past, rebaseValue.getOffset().apply(past));
        Assert.assertSame(present, rebaseValue.getOffset().apply(present));
        Assert.assertSame(future, rebaseValue.getOffset().apply(future));
    }
}
