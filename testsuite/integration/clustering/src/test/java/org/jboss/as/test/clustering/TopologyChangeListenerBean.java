/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.clustering;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.TopologyChanged;
import org.infinispan.notifications.cachelistener.event.TopologyChangedEvent;
import org.infinispan.statetransfer.StateTransferManager;
import org.infinispan.topology.CacheTopology;
import org.jboss.as.clustering.msc.ServiceContainerHelper;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * EJB that establishes a stable topology.
 * @author Paul Ferraro
 */
@Stateless
@Remote(TopologyChangeListener.class)
@Listener(sync = false)
public class TopologyChangeListenerBean implements TopologyChangeListener {

    @Override
    public void establishTopology(String containerName, String cacheName, long timeout, String... nodes) throws InterruptedException {
        Set<String> expectedMembers = Stream.of(nodes).sorted().collect(Collectors.toSet());
        ServiceRegistry registry = CurrentServiceContainer.getServiceContainer();
        ServiceName name = ServiceName.JBOSS.append("infinispan", containerName);
        EmbeddedCacheManager cacheContainer = ServiceContainerHelper.findValue(registry, name);
        if (cacheContainer == null) {
            throw new IllegalStateException(String.format("Failed to locate %s", name));
        }
        Cache<?, ?> cache = cacheContainer.getCache(cacheName);
        if (cache == null) {
            throw new IllegalStateException(String.format("Cache %s not found", cacheName));
        }
        cache.addListener(this);
        try
        {
            long start = System.currentTimeMillis();
            long now = start;
            long endTime = start + timeout;
            synchronized (this) {
                StateTransferManager transfer = cache.getAdvancedCache().getComponentRegistry().getStateTransferManager();
                CacheTopology topology = transfer.getCacheTopology();
                Set<String> members = getMembers(topology);
                while (!expectedMembers.equals(members)) {
                    System.out.println(String.format("%s != %s, waiting for a topology change event. Current topology id = %d", expectedMembers, members, topology.getTopologyId()));
                    this.wait(endTime - now);
                    now = System.currentTimeMillis();
                    if (now >= endTime) {
                        throw new InterruptedException(String.format("Cache %s/%s failed to establish view %s within %d ms.  Current view is: %s", containerName, cacheName, expectedMembers, timeout, members));
                    }
                    topology = transfer.getCacheTopology();
                    members = getMembers(topology);
                }
                System.out.println(String.format("Cache %s/%s successfully established view %s within %d ms. Topology id = %d", containerName, cacheName, expectedMembers, now - start, topology.getTopologyId()));
            }
        } finally {
            cache.removeListener(this);
        }
    }

    private static Set<String> getMembers(CacheTopology topology) {
        return topology.getCurrentCH().getMembers().stream().map(address -> address.toString()).sorted().collect(Collectors.toSet());
    }

    @TopologyChanged
    public void topologyChanged(TopologyChangedEvent<?, ?> event) {
        if (!event.isPre()) {
            synchronized (this) {
                this.notify();
            }
        }
    }
}