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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.dispatcher.CommandDispatcherException;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.server.logging.ClusteringServerLogger;

/**
 * Service that proxies the value from the primary node.
 * @author Paul Ferraro
 */
@Deprecated
public class PrimaryProxyService<T> implements Service<T> {

    private final Supplier<PrimaryProxyContext<T>> contextFactory;

    private volatile boolean started = false;

    public PrimaryProxyService(Supplier<PrimaryProxyContext<T>> contextFactory) {
        this.contextFactory = contextFactory;
    }

    @Override
    public T getValue() {
        PrimaryProxyContext<T> context = this.contextFactory.get();
        if (!this.started) {
            throw ClusteringServerLogger.ROOT_LOGGER.notStarted(context.getServiceName().getCanonicalName());
        }
        try {
            Map<Node, CompletionStage<Optional<T>>> responses = context.getCommandDispatcher().executeOnGroup(new SingletonValueCommand<>());
            // Prune non-primary (i.e. null) results
            Map<Node, Optional<T>> results = new HashMap<>();
            try {
                for (Map.Entry<Node, CompletionStage<Optional<T>>> entry : responses.entrySet()) {
                    try {
                        Optional<T> response = entry.getValue().toCompletableFuture().join();
                        if (response != null) {
                            results.put(entry.getKey(), response);
                        }
                    } catch (CancellationException e) {
                        // Ignore
                    }
                }
            } catch (CompletionException e) {
                throw new IllegalArgumentException(e);
            }
            // We expect only 1 result
            if (results.size() > 1) {
                // This would mean there are multiple primary nodes!
                throw ClusteringServerLogger.ROOT_LOGGER.multiplePrimaryProvidersDetected(context.getServiceName().getCanonicalName(), results.keySet());
            }
            Iterator<Optional<T>> values = results.values().iterator();
            if (!values.hasNext()) {
                throw ClusteringServerLogger.ROOT_LOGGER.noResponseFromMaster(context.getServiceName().getCanonicalName());
            }
            return values.next().orElse(null);
        } catch (CommandDispatcherException e) {
            throw new IllegalArgumentException(e);
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
