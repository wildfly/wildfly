/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import java.io.IOException;
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
import org.wildfly.clustering.server.GroupMember;

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
            throw SingletonLogger.ROOT_LOGGER.notStarted(context.getServiceName().getCanonicalName());
        }
        try {
            Map<GroupMember, CompletionStage<Optional<T>>> responses = context.getCommandDispatcher().dispatchToGroup(SingletonValueCommand.getInstance());
            // Prune non-primary (i.e. null) results
            Map<GroupMember, Optional<T>> results = new HashMap<>();
            try {
                for (Map.Entry<GroupMember, CompletionStage<Optional<T>>> entry : responses.entrySet()) {
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
                throw SingletonLogger.ROOT_LOGGER.multiplePrimaryProvidersDetected(context.getServiceName().getCanonicalName(), results.keySet());
            }
            Iterator<Optional<T>> values = results.values().iterator();
            if (!values.hasNext()) {
                throw SingletonLogger.ROOT_LOGGER.noResponseFromPrimary(context.getServiceName().getCanonicalName());
            }
            return values.next().orElse(null);
        } catch (IOException e) {
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
