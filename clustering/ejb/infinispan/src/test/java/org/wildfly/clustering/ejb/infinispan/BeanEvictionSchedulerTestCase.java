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
import static org.junit.Assert.*;

import org.jboss.as.clustering.infinispan.invoker.Evictor;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.ejb.Batch;
import org.wildfly.clustering.ejb.Batcher;
import org.wildfly.clustering.ejb.Bean;
import org.wildfly.clustering.ejb.BeanPassivationConfiguration;

public class BeanEvictionSchedulerTestCase {
    @SuppressWarnings("rawtypes")
    @Test
    public void test() throws Exception {
        String name = "bean";
        String evictedBeanId = "evicted";
        String activeBeanId = "active";
        CommandDispatcherFactory dispatcherFactory = mock(CommandDispatcherFactory.class);
        CommandDispatcher<BeanEvictionContext<String>> dispatcher = mock(CommandDispatcher.class);
        Batcher batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);
        Evictor<String> evictor = mock(Evictor.class);
        PassivationConfiguration<Bean<Object, String, Object>> config = mock(PassivationConfiguration.class);
        BeanPassivationConfiguration passivationConfig = mock(BeanPassivationConfiguration.class);
        ArgumentCaptor<Command> capturedCommand = ArgumentCaptor.forClass(Command.class);
        ArgumentCaptor<BeanEvictionContext> capturedContext = ArgumentCaptor.forClass(BeanEvictionContext.class);

        when(dispatcherFactory.createCommandDispatcher(same(name), (BeanEvictionContext<String>) capturedContext.capture())).thenReturn(dispatcher);
        when(config.getConfiguration()).thenReturn(passivationConfig);
        when(passivationConfig.getMaxSize()).thenReturn(1);

        try (Scheduler<String> scheduler = new BeanEvictionScheduler<>(name, batcher, evictor, dispatcherFactory, config)) {
            BeanEvictionContext<String> context = capturedContext.getValue();

            assertSame(scheduler, context);
            
            scheduler.schedule(evictedBeanId);

            verifyZeroInteractions(dispatcher);

            scheduler.schedule(activeBeanId);

            verify(dispatcher).submitOnCluster(capturedCommand.capture());

            when(batcher.startBatch()).thenReturn(batch);

            capturedCommand.getValue().execute(context);

            verify(evictor).evict(evictedBeanId);
            verify(evictor, never()).evict(activeBeanId);
            verify(batch).close();
        }

        verify(dispatcher).close();
    }
}
