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
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherException;
import org.wildfly.clustering.ee.Invoker;
import org.wildfly.clustering.ee.cache.retry.RetryingInvoker;
import org.wildfly.clustering.ee.infinispan.logging.Logger;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.spi.dispatcher.CommandDispatcherFactory;
import org.wildfly.common.function.ExceptionSupplier;

/**
 * Scheduler decorator that schedules/cancels a given object on the primary owner.
 * @author Paul Ferraro
 */
public class PrimaryOwnerScheduler<I, K, M> implements org.wildfly.clustering.ee.Scheduler<I, M> {
    private static final Invoker INVOKER = new RetryingInvoker(Duration.ZERO, Duration.ofMillis(10), Duration.ofMillis(100));

    private final Function<K, Node> primaryOwnerLocator;
    private final Function<I, K> keyFactory;
    private final CommandDispatcher<Scheduler<I, M>> dispatcher;
    private final BiFunction<I, M, ScheduleCommand<I, M>> scheduleCommandFactory;

    public <C, L> PrimaryOwnerScheduler(CommandDispatcherFactory dispatcherFactory, String name, Scheduler<I, M> scheduler, Function<K, Node> primaryOwnerLocator, Function<I, K> keyFactory) {
        this(dispatcherFactory, name, scheduler, primaryOwnerLocator, keyFactory, ScheduleWithTransientMetaDataCommand::new);
    }

    public <C, L> PrimaryOwnerScheduler(CommandDispatcherFactory dispatcherFactory, String name, Scheduler<I, M> scheduler, Function<K, Node> primaryOwnerLocator, Function<I, K> keyFactory, BiFunction<I, M, ScheduleCommand<I, M>> scheduleCommandFactory) {
        this.dispatcher = dispatcherFactory.createCommandDispatcher(name, scheduler, keyFactory.apply(null).getClass().getClassLoader());
        this.primaryOwnerLocator = primaryOwnerLocator;
        this.keyFactory = keyFactory;
        this.scheduleCommandFactory = scheduleCommandFactory;
    }

    @Override
    public void schedule(I id, M metaData) {
        try {
            this.executeOnPrimaryOwner(id, this.scheduleCommandFactory.apply(id, metaData));
        } catch (Exception e) {
            Logger.ROOT_LOGGER.failedToSchedule(e, id);
        }
    }

    @Override
    public void cancel(I id) {
        try {
            this.executeOnPrimaryOwner(id, new CancelCommand<>(id)).toCompletableFuture().join();
        } catch (Exception e) {
            Logger.ROOT_LOGGER.failedToCancel(e, id);
        }
    }

    private CompletionStage<Void> executeOnPrimaryOwner(I id, Command<Void, Scheduler<I, M>> command) throws CommandDispatcherException {
        K key = this.keyFactory.apply(id);
        Function<K, Node> primaryOwnerLocator = this.primaryOwnerLocator;
        CommandDispatcher<Scheduler<I, M>> dispatcher = this.dispatcher;
        ExceptionSupplier<CompletionStage<Void>, CommandDispatcherException> action = new ExceptionSupplier<CompletionStage<Void>, CommandDispatcherException>() {
            @Override
            public CompletionStage<Void> get() throws CommandDispatcherException {
                Node node = primaryOwnerLocator.apply(key);
                // This should only go remote following a failover
                return dispatcher.executeOnMember(command, node);
            }
        };
        return INVOKER.invoke(action);
    }

    @Override
    public void close() {
        this.dispatcher.close();
        this.dispatcher.getContext().close();
    }
}
