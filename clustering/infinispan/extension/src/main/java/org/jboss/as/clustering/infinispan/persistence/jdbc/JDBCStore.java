/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.persistence.jdbc;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Predicate;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;

import org.infinispan.commons.configuration.ConfiguredBy;
import org.infinispan.commons.executors.BlockingResource;
import org.infinispan.commons.util.IntSet;
import org.infinispan.persistence.jdbc.stringbased.JdbcStringBasedStore;
import org.infinispan.persistence.spi.InitializationContext;
import org.infinispan.persistence.spi.MarshallableEntry;
import org.jboss.logging.Logger;
import org.reactivestreams.Publisher;

/**
 * Custom JDBC cache store implementation that executes all publisher actions on the caller thread.
 * @author Paul Ferraro
 */
@ConfiguredBy(JDBCStoreConfiguration.class)
public class JDBCStore<K, V> extends JdbcStringBasedStore<K, V> {
    private static final Logger LOGGER = Logger.getLogger(JDBCStore.class);
    private static final Runnable INTERRUPT = () -> Thread.currentThread().interrupt();

    private Executor executor;

    @Override
    public CompletionStage<Void> start(InitializationContext context) {
        this.executor = context.getBlockingManager().asExecutor(this.getClass().getSimpleName());
        return super.start(context);
    }

    @Override
    public Publisher<MarshallableEntry<K, V>> publishEntries(IntSet segments, Predicate<? super K> filter, boolean includeValues) {
        return this.blocking(this.tableOperations.publishEntries(this.connectionFactory::getConnection, this.connectionFactory::releaseConnection, segments, filter, includeValues));
    }

    @Override
    public Publisher<K> publishKeys(IntSet segments, Predicate<? super K> filter) {
        return this.blocking(this.tableOperations.publishKeys(this.connectionFactory::getConnection, this.connectionFactory::releaseConnection, segments, filter));
    }

    private <T> Flowable<T> blocking(Flowable<T> flowable) {
        if (Thread.currentThread().getThreadGroup() instanceof BlockingResource) return flowable;

        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
        // Allocate single blocking thread from pool for running queued tasks
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                // Continue executing queued tasks until interruption
                while (!Thread.interrupted()) {
                    try {
                        queue.take().run();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Throwable e) {
                        LOGGER.warn(e.getLocalizedMessage(), e);
                    }
                }
            }
        });
        // Scheduler facade that queues tasks
        Scheduler scheduler = Schedulers.from(new Executor() {
            @Override
            public void execute(Runnable task) {
                try {
                    queue.put(task);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, false, true);
        // Execute subscribe, unsubscribe, observer, and finally actions on same blocking thread
        // Conclude with interrupt task to signal completion
        return flowable.subscribeOn(scheduler).unsubscribeOn(scheduler).observeOn(scheduler).doAfterTerminate(() -> queue.put(INTERRUPT));
    }
}
