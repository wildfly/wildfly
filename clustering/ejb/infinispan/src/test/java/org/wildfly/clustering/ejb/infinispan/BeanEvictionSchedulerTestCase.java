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
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutorService;

import org.jboss.as.clustering.concurrent.Scheduler;
import org.jboss.as.clustering.infinispan.invoker.Evictor;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.wildfly.clustering.ejb.Batch;
import org.wildfly.clustering.ejb.Batcher;
import org.wildfly.clustering.ejb.Bean;
import org.wildfly.clustering.ejb.BeanPassivationConfiguration;

public class BeanEvictionSchedulerTestCase {
    @Test
    public void test() {
        String evictedBeanId = "evicted";
        String activeBeanId = "active";
        Bean<Object, String, Object> evictedBean = mock(Bean.class);
        Bean<Object, String, Object> activeBean = mock(Bean.class);
        Batcher batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);
        Evictor<String> evictor = mock(Evictor.class);
        ExecutorService executor = mock(ExecutorService.class);
        PassivationConfiguration<Bean<Object, String, Object>> config = mock(PassivationConfiguration.class);
        BeanPassivationConfiguration passivationConfig = mock(BeanPassivationConfiguration.class);
        ArgumentCaptor<Runnable> capturedTask = ArgumentCaptor.forClass(Runnable.class);

        when(config.getExecutor()).thenReturn(executor);
        when(config.getConfiguration()).thenReturn(passivationConfig);
        when(passivationConfig.getMaxSize()).thenReturn(1);

        try (Scheduler<Bean<Object, String, Object>> scheduler = new BeanEvictionScheduler<>(batcher, evictor, config)) {
            when(evictedBean.getId()).thenReturn(evictedBeanId);
            when(activeBean.getId()).thenReturn(activeBeanId);

            scheduler.schedule(evictedBean);

            verifyZeroInteractions(executor);

            scheduler.schedule(activeBean);

            verify(executor).execute(capturedTask.capture());
            
            when(batcher.startBatch()).thenReturn(batch);
            
            capturedTask.getValue().run();
            
            verify(evictor).evict(evictedBeanId);
            verify(evictor, never()).evict(activeBeanId);
            verify(batch).close();
        }
    }
}
