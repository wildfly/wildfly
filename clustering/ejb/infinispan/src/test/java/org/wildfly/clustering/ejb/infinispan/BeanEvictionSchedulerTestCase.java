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

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.infinispan.Evictor;
import org.wildfly.clustering.ee.infinispan.TransactionBatch;
import org.wildfly.clustering.ejb.Bean;
import org.wildfly.clustering.ejb.BeanPassivationConfiguration;

public class BeanEvictionSchedulerTestCase {
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    @org.junit.Ignore
    public void test() throws Exception {
        String name = "bean";
        String evictedBeanId = "evicted";
        String activeBeanId = "active";
        CommandDispatcherFactory dispatcherFactory = mock(CommandDispatcherFactory.class);
        CommandDispatcher<BeanGroupEvictionContext<String>> dispatcher = mock(CommandDispatcher.class);
        Batcher<TransactionBatch> batcher = mock(Batcher.class);
        TransactionBatch batch = mock(TransactionBatch.class);
        Evictor<String> evictor = mock(Evictor.class);
        PassivationConfiguration<Bean<String, Object>> config = mock(PassivationConfiguration.class);
        BeanPassivationConfiguration passivationConfig = mock(BeanPassivationConfiguration.class);
        ArgumentCaptor<Command> capturedCommand = ArgumentCaptor.forClass(Command.class);
        ArgumentCaptor<BeanGroupEvictionContext> capturedContext = ArgumentCaptor.forClass(BeanGroupEvictionContext.class);

        when(dispatcherFactory.createCommandDispatcher(same(name), (BeanGroupEvictionContext<String>) capturedContext.capture())).thenReturn(dispatcher);
        when(config.getConfiguration()).thenReturn(passivationConfig);
        when(passivationConfig.getMaxSize()).thenReturn(1);

        try (Scheduler<String> scheduler = new BeanGroupEvictionScheduler<>(name, batcher, evictor, dispatcherFactory, config)) {
            BeanGroupEvictionContext<String> context = capturedContext.getValue();

            assertSame(scheduler, context);

            scheduler.schedule(evictedBeanId);

            verifyZeroInteractions(dispatcher);

            scheduler.schedule(activeBeanId);

            verify(dispatcher).submitOnCluster(capturedCommand.capture());

            when(batcher.createBatch()).thenReturn(batch);

            capturedCommand.getValue().execute(context);

            verify(evictor).evict(evictedBeanId);
            verify(evictor, never()).evict(activeBeanId);
            verify(batch).close();
        }

        verify(dispatcher).close();
    }
}
