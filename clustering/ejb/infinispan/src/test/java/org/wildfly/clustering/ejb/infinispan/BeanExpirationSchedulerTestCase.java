/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.ejb.infinispan;

import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;

import org.junit.Test;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.Scheduler;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ejb.RemoveListener;
import org.wildfly.clustering.group.Group;

/**
 * Unit test for {@link BeanExpirationScheduler}.
 * @author Paul Ferraro
 */
public class BeanExpirationSchedulerTestCase {
    @Test
    public void testImmortal() throws InterruptedException {
        Group group = mock(Group.class);
        Batcher<TransactionBatch> batcher = mock(Batcher.class);
        BeanFactory<String, Object> factory = mock(BeanFactory.class);
        ExpirationConfiguration<Object> config = mock(ExpirationConfiguration.class);
        RemoveListener<Object> listener = mock(RemoveListener.class);
        BeanEntry<String> entry = mock(BeanEntry.class);
        BeanRemover<String, Object> remover = mock(BeanRemover.class);
        String beanId = "immortal";

        when(group.isSingleton()).thenReturn(true);

        // Fun fact: the EJB specification allows a timeout value of 0, so only negative timeouts are treated as immortal
        when(config.getTimeout()).thenReturn(Duration.ofMinutes(-1L));
        when(config.getRemoveListener()).thenReturn(listener);
        when(entry.getLastAccessedTime()).thenReturn(Instant.now());

        try (Scheduler<String, ImmutableBeanEntry<String>> scheduler = new BeanExpirationScheduler<>(group, batcher, factory, config, remover, Duration.ZERO)) {
            scheduler.schedule(beanId, entry);

            Thread.sleep(500);
        }

        verify(batcher, never()).createBatch();
        verify(remover, never()).remove(beanId, listener);
    }

    @Test
    public void testExpire() throws InterruptedException {
        Group group = mock(Group.class);
        Batcher<TransactionBatch> batcher = mock(Batcher.class);
        TransactionBatch batch = mock(TransactionBatch.class);
        BeanFactory<String, Object> factory = mock(BeanFactory.class);
        ExpirationConfiguration<Object> config = mock(ExpirationConfiguration.class);
        RemoveListener<Object> listener = mock(RemoveListener.class);
        BeanEntry<String> entry = mock(BeanEntry.class);
        BeanRemover<String, Object> remover = mock(BeanRemover.class);
        String beanId = "expiring";
        Duration timeout = Duration.ofMillis(10L);

        when(group.isSingleton()).thenReturn(true);
        when(batcher.createBatch()).thenReturn(batch);

        when(config.getTimeout()).thenReturn(timeout);
        when(config.getRemoveListener()).thenReturn(listener);
        when(entry.getLastAccessedTime()).thenReturn(Instant.now());
        when(remover.remove(beanId, listener)).thenReturn(true);

        try (Scheduler<String, ImmutableBeanEntry<String>> scheduler = new BeanExpirationScheduler<>(group, batcher, factory, config, remover, Duration.ZERO)) {
            scheduler.schedule(beanId, entry);

            Thread.sleep(500);
        }

        verify(batch).close();
    }

    @Test
    public void testNotYetExpired() throws InterruptedException {
        Group group = mock(Group.class);
        Batcher<TransactionBatch> batcher = mock(Batcher.class);
        TransactionBatch batch = mock(TransactionBatch.class);
        BeanFactory<String, Object> factory = mock(BeanFactory.class);
        ExpirationConfiguration<Object> config = mock(ExpirationConfiguration.class);
        RemoveListener<Object> listener = mock(RemoveListener.class);
        BeanEntry<String> entry = mock(BeanEntry.class);
        BeanRemover<String, Object> remover = mock(BeanRemover.class);
        String beanId = "not-expired";
        Duration timeout = Duration.ofMillis(10L);

        when(batcher.createBatch()).thenReturn(batch);

        when(config.getTimeout()).thenReturn(Duration.ofMillis(1L));
        when(config.getRemoveListener()).thenReturn(listener);
        when(entry.getLastAccessedTime()).thenReturn(Instant.now().plus(Duration.ofMinutes(1)));
        when(factory.findValue(beanId)).thenReturn(entry);
        when(entry.isExpired(same(timeout))).thenReturn(false);

        try (Scheduler<String, ImmutableBeanEntry<String>> scheduler = new BeanExpirationScheduler<>(group, batcher, factory, config, remover, Duration.ZERO)) {
            scheduler.schedule(beanId, entry);

            Thread.sleep(500);
        }

        verify(remover, never()).remove(beanId, listener);
        verify(batch, never()).close();
    }

    @Test
    public void testCancel() throws InterruptedException {
        Group group = mock(Group.class);
        Batcher<TransactionBatch> batcher = mock(Batcher.class);
        BeanFactory<String, Object> factory = mock(BeanFactory.class);
        ExpirationConfiguration<Object> config = mock(ExpirationConfiguration.class);
        RemoveListener<Object> listener = mock(RemoveListener.class);
        BeanEntry<String> entry = mock(BeanEntry.class);
        BeanRemover<String, Object> remover = mock(BeanRemover.class);
        String beanId = "canceled";
        Duration timeout = Duration.ofMinutes(1L);

        when(config.getTimeout()).thenReturn(timeout);
        when(config.getRemoveListener()).thenReturn(listener);
        when(entry.getLastAccessedTime()).thenReturn(Instant.now());

        try (Scheduler<String, ImmutableBeanEntry<String>> scheduler = new BeanExpirationScheduler<>(group, batcher, factory, config, remover, Duration.ZERO)) {
            scheduler.schedule(beanId, entry);

            Thread.sleep(500);

            scheduler.cancel(beanId);
        }

        verify(remover, never()).remove(beanId, listener);
        verify(batcher, never()).createBatch();
    }
}
