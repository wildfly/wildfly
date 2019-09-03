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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;

import org.junit.Test;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ejb.RemoveListener;

public class BeanExpirationSchedulerTestCase {
    @Test
    public void testImmortal() throws InterruptedException {
        Batcher<TransactionBatch> batcher = mock(Batcher.class);
        BeanFactory<String, Object> factory = mock(BeanFactory.class);
        ExpirationConfiguration<Object> config = mock(ExpirationConfiguration.class);
        RemoveListener<Object> listener = mock(RemoveListener.class);
        BeanEntry<String> entry = mock(BeanEntry.class);
        String beanId = "immortal";

        when(config.getExecutor()).thenReturn(Executors.newSingleThreadScheduledExecutor());

        // Fun fact: the EJB specification allows a timeout value of 0, so only negative timeouts are treated as immortal
        when(config.getTimeout()).thenReturn(Duration.ofMinutes(-1L));
        when(config.getRemoveListener()).thenReturn(listener);
        when(entry.getLastAccessedTime()).thenReturn(Instant.now());

        try (Scheduler<String> scheduler = new BeanExpirationScheduler<>(batcher, factory, config)) {
            scheduler.schedule(beanId, entry);

            Thread.sleep(1000);
        }

        verify(batcher, never()).createBatch();
        verify(factory, never()).remove(beanId, listener);
    }

    @Test
    public void testExpire() throws InterruptedException {
        Batcher<TransactionBatch> batcher = mock(Batcher.class);
        TransactionBatch batch = mock(TransactionBatch.class);
        BeanFactory<String, Object> factory = mock(BeanFactory.class);
        ExpirationConfiguration<Object> config = mock(ExpirationConfiguration.class);
        RemoveListener<Object> listener = mock(RemoveListener.class);
        BeanEntry<String> entry = mock(BeanEntry.class);
        String beanId = "expiring";
        Duration timeout = Duration.ofMillis(10L);

        when(config.getExecutor()).thenReturn(Executors.newSingleThreadScheduledExecutor());
        when(batcher.createBatch()).thenReturn(batch);

        when(config.getTimeout()).thenReturn(timeout);
        when(config.getRemoveListener()).thenReturn(listener);
        when(entry.getLastAccessedTime()).thenReturn(Instant.now());
        when(factory.findValue(beanId)).thenReturn(entry);
        when(entry.isExpired(same(timeout))).thenReturn(true);

        try (Scheduler<String> scheduler = new BeanExpirationScheduler<>(batcher, factory, config)) {
            scheduler.schedule(beanId, entry);

            Thread.sleep(1000);
        }

        verify(factory).remove(beanId, listener);
        verify(batch).close();
    }

    @Test
    public void testNotFound() throws InterruptedException {
        Batcher<TransactionBatch> batcher = mock(Batcher.class);
        TransactionBatch batch = mock(TransactionBatch.class);
        BeanFactory<String, Object> factory = mock(BeanFactory.class);
        ExpirationConfiguration<Object> config = mock(ExpirationConfiguration.class);
        RemoveListener<Object> listener = mock(RemoveListener.class);
        BeanEntry<String> entry = mock(BeanEntry.class);
        String beanId = "expiring";
        Duration timeout = Duration.ofMillis(10L);

        when(config.getExecutor()).thenReturn(Executors.newSingleThreadScheduledExecutor());
        when(batcher.createBatch()).thenReturn(batch);

        when(config.getTimeout()).thenReturn(timeout);
        when(config.getRemoveListener()).thenReturn(listener);
        when(entry.getLastAccessedTime()).thenReturn(Instant.now());
        when(factory.findValue(beanId)).thenReturn(null);

        try (Scheduler<String> scheduler = new BeanExpirationScheduler<>(batcher, factory, config)) {
            scheduler.schedule(beanId, entry);

            Thread.sleep(1000);
        }

        verify(factory, never()).remove(beanId, listener);
        verify(batch).close();
    }

    @Test
    public void testNotExpired() throws InterruptedException {
        Batcher<TransactionBatch> batcher = mock(Batcher.class);
        TransactionBatch batch = mock(TransactionBatch.class);
        BeanFactory<String, Object> factory = mock(BeanFactory.class);
        ExpirationConfiguration<Object> config = mock(ExpirationConfiguration.class);
        RemoveListener<Object> listener = mock(RemoveListener.class);
        BeanEntry<String> entry = mock(BeanEntry.class);
        String beanId = "expiring";
        Duration timeout = Duration.ofMillis(10L);

        when(config.getExecutor()).thenReturn(Executors.newSingleThreadScheduledExecutor());
        when(batcher.createBatch()).thenReturn(batch);

        when(config.getTimeout()).thenReturn(Duration.ofMillis(1L));
        when(config.getRemoveListener()).thenReturn(listener);
        when(entry.getLastAccessedTime()).thenReturn(Instant.now(), Instant.now().plus(Duration.ofMinutes(1)));
        when(factory.findValue(beanId)).thenReturn(entry);
        when(entry.isExpired(same(timeout))).thenReturn(false);

        try (Scheduler<String> scheduler = new BeanExpirationScheduler<>(batcher, factory, config)) {
            scheduler.schedule(beanId, entry);

            Thread.sleep(1000);
        }

        verify(factory, never()).remove(beanId, listener);
        verify(batch).close();
    }

    @Test
    public void testCancel() throws InterruptedException {
        Batcher<TransactionBatch> batcher = mock(Batcher.class);
        BeanFactory<String, Object> factory = mock(BeanFactory.class);
        ExpirationConfiguration<Object> config = mock(ExpirationConfiguration.class);
        RemoveListener<Object> listener = mock(RemoveListener.class);
        BeanEntry<String> entry = mock(BeanEntry.class);
        String beanId = "canceled";
        Duration timeout = Duration.ofMinutes(1L);

        when(config.getExecutor()).thenReturn(Executors.newSingleThreadScheduledExecutor());

        when(config.getTimeout()).thenReturn(timeout);
        when(config.getRemoveListener()).thenReturn(listener);
        when(entry.getLastAccessedTime()).thenReturn(Instant.now());

        try (Scheduler<String> scheduler = new BeanExpirationScheduler<>(batcher, factory, config)) {
            scheduler.schedule(beanId, entry);

            Thread.sleep(1000);

            scheduler.cancel(beanId);
        }

        verify(factory, never()).remove(beanId, listener);
        verify(batcher, never()).createBatch();
    }
}
