/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.server.singleton;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherException;
import org.wildfly.clustering.dispatcher.CommandResponse;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.provider.ServiceProviderRegistration;
import org.wildfly.clustering.server.logging.ClusteringServerLogger;

/**
 * Service that proxies the value from the primary node.
 * @author Paul Ferraro
 */
public class PrimaryProxyService<T> implements Service<T> {

    private final Supplier<CommandDispatcher<SingletonContext<T>>> dispatcher;
    private final Supplier<ServiceProviderRegistration<ServiceName>> registration;
    private final int quorum;

    private volatile boolean started = false;

    public PrimaryProxyService(PrimaryProxyContext<T> context) {
        this.dispatcher = context.getCommandDispatcher();
        this.registration = context.getServiceProviderRegistration();
        this.quorum = context.getQuorum();
    }

    @Override
    public T getValue() {
        CommandDispatcher<SingletonContext<T>> dispatcher = this.dispatcher.get();
        ServiceProviderRegistration<ServiceName> registration = this.registration.get();
        try {
            List<Map.Entry<Node, Optional<T>>> result = Collections.emptyList();
            while (result.isEmpty()) {
                if (!this.started) {
                    throw ClusteringServerLogger.ROOT_LOGGER.notStarted(registration.getService().getCanonicalName());
                }
                Map<Node, CommandResponse<Optional<T>>> responses = dispatcher.executeOnCluster(new SingletonValueCommand<T>());
                // Prune non-primary (i.e. null) results
                result = responses.entrySet().stream().map(entry -> {
                    try {
                        return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue().get());
                    } catch (ExecutionException e) {
                        throw new IllegalArgumentException(e);
                    }
                }).filter(entry -> entry.getValue() != null).collect(Collectors.toList());
                // We expect only 1 result
                if (result.size() > 1) {
                    // This would mean there are multiple primary nodes!
                    throw ClusteringServerLogger.ROOT_LOGGER.multiplePrimaryProvidersDetected(registration.getService().getCanonicalName(), result.stream().map(entry -> entry.getKey()).collect(Collectors.toList()));
                }
                if (result.isEmpty()) {
                    ClusteringServerLogger.ROOT_LOGGER.noResponseFromMaster(registration.getService().getCanonicalName());
                    // Verify whether there is no primary node because a quorum was not reached during the last election
                    if (registration.getProviders().size() < this.quorum) {
                        throw ClusteringServerLogger.ROOT_LOGGER.notStarted(registration.getService().getCanonicalName());
                    }
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException();
                    }
                    // Otherwise, we're in the midst of a new election, so just try again
                    Thread.yield();
                }
            }
            return result.get(0).getValue().orElse(null);
        } catch (CommandDispatcherException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void start(StartContext context) {
        this.started = true;
    }

    @Override
    public void stop(StopContext context) {
        this.started = false;
    }
}
