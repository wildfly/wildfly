/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
