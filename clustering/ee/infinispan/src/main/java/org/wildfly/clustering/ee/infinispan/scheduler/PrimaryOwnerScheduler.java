/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.ee.infinispan.scheduler;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherException;
import org.wildfly.clustering.ee.Invoker;
import org.wildfly.clustering.ee.Scheduler;
import org.wildfly.clustering.ee.cache.retry.RetryingInvoker;
import org.wildfly.clustering.ee.infinispan.logging.Logger;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;
import org.wildfly.common.function.ExceptionSupplier;

/**
 * Scheduler decorator that schedules/cancels a given object on the primary owner.
 * @author Paul Ferraro
 */
public class PrimaryOwnerScheduler<I, K, M> implements Scheduler<I, M>, Function<CompletionStage<Collection<I>>, Stream<I>> {
    private static final Invoker INVOKER = new RetryingInvoker(Duration.ZERO, Duration.ofMillis(10), Duration.ofMillis(100));

    private final Function<K, Node> primaryOwnerLocator;
    private final Function<I, K> keyFactory;
    private final CommandDispatcher<CacheEntryScheduler<I, M>> dispatcher;
    private final BiFunction<I, M, ScheduleCommand<I, M>> scheduleCommandFactory;

    public <C, L> PrimaryOwnerScheduler(CommandDispatcherFactory dispatcherFactory, String name, CacheEntryScheduler<I, M> scheduler, Function<K, Node> primaryOwnerLocator, Function<I, K> keyFactory) {
        this(dispatcherFactory, name, scheduler, primaryOwnerLocator, keyFactory, ScheduleWithTransientMetaDataCommand::new);
    }

    public <C, L> PrimaryOwnerScheduler(CommandDispatcherFactory dispatcherFactory, String name, CacheEntryScheduler<I, M> scheduler, Function<K, Node> primaryOwnerLocator, Function<I, K> keyFactory, BiFunction<I, M, ScheduleCommand<I, M>> scheduleCommandFactory) {
        this.dispatcher = dispatcherFactory.createCommandDispatcher(name, scheduler, keyFactory.apply(null).getClass().getClassLoader());
        this.primaryOwnerLocator = primaryOwnerLocator;
        this.keyFactory = keyFactory;
        this.scheduleCommandFactory = scheduleCommandFactory;
    }

    @Override
    public void schedule(I id, M metaData) {
        try {
            this.executeOnPrimaryOwner(id, this.scheduleCommandFactory.apply(id, metaData));
        } catch (CommandDispatcherException e) {
            Logger.ROOT_LOGGER.failedToSchedule(e, id);
        }
    }

    @Override
    public void cancel(I id) {
        try {
            this.executeOnPrimaryOwner(id, new CancelCommand<>(id)).toCompletableFuture().join();
        } catch (CommandDispatcherException | CompletionException e) {
            Logger.ROOT_LOGGER.failedToCancel(e, id);
        } catch (CancellationException e) {
            // Ignore
        }
    }

    @Override
    public boolean contains(I id) {
        try {
            return this.executeOnPrimaryOwner(id, new ContainsCommand<>(id)).toCompletableFuture().join();
        } catch (CommandDispatcherException | CompletionException e) {
            Logger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
            return false;
        } catch (CancellationException e) {
            return false;
        }
    }

    private <R> CompletionStage<R> executeOnPrimaryOwner(I id, Command<R, CacheEntryScheduler<I, M>> command) throws CommandDispatcherException {
        K key = this.keyFactory.apply(id);
        Function<K, Node> primaryOwnerLocator = this.primaryOwnerLocator;
        CommandDispatcher<CacheEntryScheduler<I, M>> dispatcher = this.dispatcher;
        ExceptionSupplier<CompletionStage<R>, CommandDispatcherException> action = new ExceptionSupplier<>() {
            @Override
            public CompletionStage<R> get() throws CommandDispatcherException {
                Node node = primaryOwnerLocator.apply(key);
                // This should only go remote following a failover
                return dispatcher.executeOnMember(command, node);
            }
        };
        return INVOKER.invoke(action);
    }

    @Override
    public Stream<I> stream() {
        try {
            Map<Node, CompletionStage<Collection<I>>> results = this.dispatcher.executeOnGroup(new EntriesCommand<>());
            return results.isEmpty() ? Stream.empty() : results.values().stream().map(this).flatMap(Function.identity()).distinct();
        } catch (CommandDispatcherException e) {
            return Stream.empty();
        }
    }

    @Override
    public Stream<I> apply(CompletionStage<Collection<I>> stage) {
        try {
            return stage.toCompletableFuture().join().stream();
        } catch (CompletionException e) {
            Logger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
            return Stream.empty();
        } catch (CancellationException e) {
            return Stream.empty();
        }
    }

    @Override
    public void close() {
        this.dispatcher.close();
        this.dispatcher.getContext().close();
    }
}
