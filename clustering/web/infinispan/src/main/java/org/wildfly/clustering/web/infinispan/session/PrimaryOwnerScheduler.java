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

package org.wildfly.clustering.web.infinispan.session;

import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherException;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.ee.Invoker;
import org.wildfly.clustering.ee.cache.retry.RetryingInvoker;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.infinispan.spi.distribution.Key;
import org.wildfly.clustering.marshalling.spi.Marshallability;
import org.wildfly.clustering.web.cache.session.Scheduler;
import org.wildfly.clustering.web.infinispan.logging.InfinispanWebLogger;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;
import org.wildfly.common.function.ExceptionSupplier;

/**
 * {@link Scheduler} decorator that executes schedule and cancellation commands on the primary owner of a given session.
 * @author Paul Ferraro
 */
public class PrimaryOwnerScheduler implements Scheduler {
    private static final Invoker INVOKER = new RetryingInvoker(Duration.ZERO, Duration.ofMillis(10), Duration.ofMillis(100));

    private final Function<Key<String>, Node> primaryOwnerLocator;
    private final CommandDispatcher<Scheduler> dispatcher;

    public <C extends Marshallability, L> PrimaryOwnerScheduler(CommandDispatcherFactory dispatcherFactory, String name, Scheduler scheduler, Function<Key<String>, Node> primaryOwnerLocator) {
        this.dispatcher = dispatcherFactory.createCommandDispatcher(name, scheduler);
        this.primaryOwnerLocator = primaryOwnerLocator;
    }

    @Override
    public void schedule(String sessionId, ImmutableSessionMetaData metaData) {
        try {
            this.executeOnPrimaryOwner(sessionId, new ScheduleSchedulerCommand(sessionId, metaData));
        } catch (Exception e) {
            InfinispanWebLogger.ROOT_LOGGER.failedToScheduleSession(e, sessionId);
        }
    }

    @Override
    public void cancel(String sessionId) {
        try {
            this.executeOnPrimaryOwner(sessionId, new CancelSchedulerCommand(sessionId));
        } catch (Exception e) {
            InfinispanWebLogger.ROOT_LOGGER.failedToCancelSession(e, sessionId);
        }
    }

    private void executeOnPrimaryOwner(String sessionId, Command<Void, Scheduler> command) throws CommandDispatcherException {
        Function<Key<String>, Node> primaryOwnerLocator = this.primaryOwnerLocator;
        CommandDispatcher<Scheduler> dispatcher = this.dispatcher;
        ExceptionSupplier<CompletionStage<Void>, CommandDispatcherException> action = new ExceptionSupplier<CompletionStage<Void>, CommandDispatcherException>() {
            @Override
            public CompletionStage<Void> get() throws CommandDispatcherException {
                Node node = primaryOwnerLocator.apply(new Key<>(sessionId));
                // This should only go remote following a failover
                return dispatcher.executeOnMember(command, node);
            }
        };
        INVOKER.invoke(action).toCompletableFuture().join();
    }

    @Override
    public void close() {
        this.dispatcher.close();
    }
}
