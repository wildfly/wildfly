/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.infinispan.Cache;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.distribution.LocalizedCacheTopology;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.Listener.Observation;
import org.infinispan.notifications.cachelistener.annotation.TopologyChanged;
import org.infinispan.notifications.cachelistener.event.TopologyChangedEvent;
import org.infinispan.util.concurrent.BlockingManager;
import org.jboss.logging.Logger;

/**
 * Jakarta Enterprise Beans that establishes a stable topology.
 * @author Paul Ferraro
 */
@Stateless
@Remote(TopologyChangeListener.class)
public class TopologyChangeListenerBean implements TopologyChangeListener {

    private static final Logger logger = Logger.getLogger(TopologyChangeListenerBean.class);

    @Override
    public void establishTopology(String containerName, String cacheName, Set<String> desiredTopology, Duration timeout) throws TimeoutException {
        Cache<?, ?> cache = findCache(containerName, cacheName);
        if (cache == null) {
            throw new IllegalStateException(String.format("Cache %s.%s not found", containerName, cacheName));
        }
        DistributionManager dist = cache.getAdvancedCache().getDistributionManager();
        if (dist == null) return;
        Object listener = new TopologyChangeListener();
        synchronized (listener) {
            cache.addListener(listener);
            LocalizedCacheTopology topology = dist.getCacheTopology();
            Set<String> currentTopology = getMembers(topology);
            Instant now = Instant.now();
            Instant start = now;
            Instant stop = now.plus(timeout);
            try {
                while (!currentTopology.equals(desiredTopology) && now.isBefore(stop)) {
                    logger.infof("%s != %s, waiting for a topology change event. Current topology id = %d", desiredTopology, currentTopology, topology.getTopologyId());
                    long millis = Duration.between(Instant.now(), stop).toMillis();
                    if (millis > 0) {
                        listener.wait(millis);
                    }
                    topology = dist.getCacheTopology();
                    currentTopology = getMembers(topology);
                    now = Instant.now();
                }
                if (!currentTopology.equals(desiredTopology)) {
                    throw new TimeoutException(String.format("Cache %s/%s failed to establish topology %s within %s. Current view is: %s", containerName, cacheName, desiredTopology, timeout, currentTopology));
                }
                logger.infof("Cache %s/%s successfully established topology %s in %s. Topology id = %d", containerName, cacheName, desiredTopology, Duration.between(start, now), topology.getTopologyId());
            } catch (InterruptedException e) {
                logger.warnf("Cache %s/%s interrupted while establishing topology %s. Topology id = %d", containerName, cacheName, desiredTopology, topology.getTopologyId());
                Thread.currentThread().interrupt();
            } finally {
                cache.removeListener(listener);
            }
        }
    }

    private static Cache<?, ?> findCache(String containerName, String cacheName) {
        try {
            Context context = new InitialContext();
            try {
                EmbeddedCacheManager manager = (EmbeddedCacheManager) context.lookup("java:jboss/infinispan/container/" + containerName);
                return manager.cacheExists(cacheName) ? manager.getCache(cacheName) : null;
            } finally {
                context.close();
            }
        } catch (NamingException e) {
            return null;
        }
    }

    private static Set<String> getMembers(LocalizedCacheTopology topology) {
        return topology.getCurrentCH().getMembers().stream().map(Object::toString).collect(Collectors.toCollection(TreeSet::new));
    }

    @Listener(observation = Observation.POST)
    static class TopologyChangeListener implements Runnable {

        @TopologyChanged
        public CompletionStage<Void> topologyChanged(TopologyChangedEvent<?, ?> event) {
            BlockingManager blocking = GlobalComponentRegistry.componentOf(event.getCache().getCacheManager(), BlockingManager.class);
            blocking.asExecutor(this.getClass().getName()).execute(this);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void run() {
            synchronized (this) {
                this.notify();
            }
        }
    }
}
