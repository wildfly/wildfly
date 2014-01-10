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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jboss.as.clustering.concurrent.Scheduler;
import org.junit.Test;
import org.wildfly.clustering.ejb.Batch;
import org.wildfly.clustering.ejb.Batcher;
import org.wildfly.clustering.ejb.Bean;
import org.wildfly.clustering.ejb.RemoveListener;
import org.wildfly.clustering.ejb.Time;

public class BeanExpirationSchedulerTestCase {
    @Test
    public void testImmortal() throws InterruptedException {
        Batcher batcher = mock(Batcher.class);
        BeanRemover<String, Object> remover = mock(BeanRemover.class);
        Bean<Object, String, Object> bean = mock(Bean.class);
        ExpirationConfiguration<Object> config = mock(ExpirationConfiguration.class);
        RemoveListener<Object> listener = mock(RemoveListener.class);
        String beanId = "immortal";

        when(config.getExecutor()).thenReturn(Executors.newSingleThreadScheduledExecutor());
        when(bean.getId()).thenReturn(beanId);
        when(bean.isExpired()).thenReturn(false);

        // Fun fact: the EJB specification allows a timeout value of 0, so only negative timeouts are treated as immortal
        when(config.getTimeout()).thenReturn(new Time(-1, TimeUnit.SECONDS));
        when(config.getRemoveListener()).thenReturn(listener);

        try (Scheduler<Bean<Object, String, Object>> scheduler = new BeanExpirationScheduler<>(batcher, remover, config)) {
            scheduler.schedule(bean);

            Thread.sleep(1000);
        }

        verify(batcher, never()).startBatch();
        verify(remover, never()).remove(beanId, listener);
    }

    @Test
    public void testExpire() throws InterruptedException {
        Batcher batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);
        BeanRemover<String, Object> remover = mock(BeanRemover.class);
        Bean<Object, String, Object> bean = mock(Bean.class);
        ExpirationConfiguration<Object> config = mock(ExpirationConfiguration.class);
        RemoveListener<Object> listener = mock(RemoveListener.class);
        String beanId = "expiring";

        when(config.getExecutor()).thenReturn(Executors.newSingleThreadScheduledExecutor());
        when(batcher.startBatch()).thenReturn(batch);

        when(bean.isExpired()).thenReturn(false);

        when(config.getTimeout()).thenReturn(new Time(1, TimeUnit.MILLISECONDS));
        when(config.getRemoveListener()).thenReturn(listener);
        
        when(bean.getId()).thenReturn(beanId);
        
        try (Scheduler<Bean<Object, String, Object>> scheduler = new BeanExpirationScheduler<>(batcher, remover, config)) {
            scheduler.schedule(bean);

            Thread.sleep(1000);
        }

        verify(remover).remove(beanId, listener);
        verify(batch).close();
    }

    @Test
    public void testCancel() throws InterruptedException {
        Batcher batcher = mock(Batcher.class);
        BeanRemover<String, Object> remover = mock(BeanRemover.class);
        Bean<Object, String, Object> bean = mock(Bean.class);
        ExpirationConfiguration<Object> config = mock(ExpirationConfiguration.class);
        RemoveListener<Object> listener = mock(RemoveListener.class);
        String beanId = "canceled";

        when(config.getExecutor()).thenReturn(Executors.newSingleThreadScheduledExecutor());
        when(bean.isExpired()).thenReturn(false);

        when(config.getTimeout()).thenReturn(new Time(1, TimeUnit.MINUTES));
        when(config.getRemoveListener()).thenReturn(listener);
        
        when(bean.getId()).thenReturn(beanId);
        
        try (Scheduler<Bean<Object, String, Object>> scheduler = new BeanExpirationScheduler<>(batcher, remover, config)) {
            scheduler.schedule(bean);

            Thread.sleep(1000);

            scheduler.cancel(bean);
            scheduler.schedule(bean);
        }

        verify(remover, never()).remove(beanId, listener);
        verify(batcher, never()).startBatch();
    }
}
