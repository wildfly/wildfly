/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.bean;

import java.time.Duration;
import java.time.Instant;

import org.junit.Assert;
import org.junit.Test;

/**
 * Validates BeanExpirationMetaData logic.
 * @author Paul Ferraro
 */
public class BeanExpirationMetaDataTestCase {

    @Test
    public void nullTimeout() {
        BeanExpirationMetaData metaData = new BeanExpirationMetaData() {

            @Override
            public Duration getTimeout() {
                return null;
            }

            @Override
            public Instant getLastAccessTime() {
                return Instant.now().plus(Duration.ofHours(1));
            }
        };
        Assert.assertFalse(metaData.isExpired());
        Assert.assertTrue(metaData.isImmortal());
    }

    @Test
    public void negativeTimeout() {
        BeanExpirationMetaData metaData = new BeanExpirationMetaData() {

            @Override
            public Duration getTimeout() {
                return Duration.ofSeconds(-1);
            }

            @Override
            public Instant getLastAccessTime() {
                return Instant.now().plus(Duration.ofHours(1));
            }
        };
        Assert.assertFalse(metaData.isExpired());
        Assert.assertTrue(metaData.isImmortal());
    }

    @Test
    public void zeroTimeout() {
        BeanExpirationMetaData metaData = new BeanExpirationMetaData() {

            @Override
            public Duration getTimeout() {
                return Duration.ZERO;
            }

            @Override
            public Instant getLastAccessTime() {
                return Instant.now().plus(Duration.ofHours(1));
            }
        };
        Assert.assertTrue(metaData.isExpired());
        Assert.assertFalse(metaData.isImmortal());
    }

    @Test
    public void expired() {
        BeanExpirationMetaData metaData = new BeanExpirationMetaData() {

            @Override
            public Duration getTimeout() {
                return Duration.ofMinutes(1);
            }

            @Override
            public Instant getLastAccessTime() {
                return Instant.now().minus(Duration.ofHours(1));
            }
        };
        Assert.assertTrue(metaData.isExpired());
        Assert.assertFalse(metaData.isImmortal());
    }

    @Test
    public void notYetExpired() {
        BeanExpirationMetaData metaData = new BeanExpirationMetaData() {

            @Override
            public Duration getTimeout() {
                return Duration.ofHours(1);
            }

            @Override
            public Instant getLastAccessTime() {
                return Instant.now();
            }
        };
        Assert.assertFalse(metaData.isExpired());
        Assert.assertFalse(metaData.isImmortal());
    }
}
