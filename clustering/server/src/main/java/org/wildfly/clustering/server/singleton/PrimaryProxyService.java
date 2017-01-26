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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.dispatcher.CommandDispatcherException;
import org.wildfly.clustering.dispatcher.CommandResponse;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.server.logging.ClusteringServerLogger;

/**
 * Service that proxies the value from the primary node.
 * @author Paul Ferraro
 */
public class PrimaryProxyService<T> implements Service<T> {

    private final PrimaryProxyContext<T> context;

    private volatile boolean started = false;

    public PrimaryProxyService(PrimaryProxyContext<T> context) {
        this.context = context;
    }

    @Override
    public T getValue() {
        if (!this.started) {
            throw ClusteringServerLogger.ROOT_LOGGER.notStarted(this.context.getServiceName().getCanonicalName());
        }
        try {
            Map<Node, CommandResponse<Optional<T>>> responses = this.context.getCommandDispatcher().executeOnCluster(new SingletonValueCommand<T>());
            // Prune non-primary (i.e. null) results
            List<Map.Entry<Node, Optional<T>>> result = responses.entrySet().stream().map(entry -> {
                try {
                    return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue().get());
                } catch (ExecutionException e) {
                    throw new IllegalArgumentException(e);
                }
            }).filter(entry -> entry.getValue() != null).collect(Collectors.toList());
            // We expect only 1 result
            if (result.size() > 1) {
                // This would mean there are multiple primary nodes!
                throw ClusteringServerLogger.ROOT_LOGGER.multiplePrimaryProvidersDetected(this.context.getServiceName().getCanonicalName(), result.stream().map(Map.Entry::getKey).collect(Collectors.toList()));
            }
            return result.stream().findFirst().orElseThrow(() -> ClusteringServerLogger.ROOT_LOGGER.noResponseFromMaster(this.context.getServiceName().getCanonicalName())).getValue().orElse(null);
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
