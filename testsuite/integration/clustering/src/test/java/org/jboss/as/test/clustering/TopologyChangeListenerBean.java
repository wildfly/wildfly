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
import java.util.TreeSet;

import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.infinispan.Cache;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.TopologyChanged;
import org.infinispan.notifications.cachelistener.event.TopologyChangedEvent;
import org.infinispan.remoting.transport.Address;
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
    public static final long TIMEOUT = 15000;

    @Override
    public void establishTopology(String containerName, String cacheName, String... nodes) throws InterruptedException {
        Set<String> expectedMembers = new TreeSet<>();
        if (nodes != null) {
            for (String name: nodes) {
                expectedMembers.add(name + "/" + containerName);
            }
        }
        ServiceRegistry registry = CurrentServiceContainer.getServiceContainer();
        ServiceName name = ServiceName.JBOSS.append("infinispan", containerName, cacheName);
        Cache<?, ?> cache = ServiceContainerHelper.findValue(registry, name);
        if (cache == null) {
            throw new IllegalStateException(String.format("Failed to locate %s", name));
        }
        cache.addListener(this);
        try
        {
            long start = System.currentTimeMillis();
            long now = start;
            long timeout = start + TIMEOUT;
            synchronized (this) {
                Set<String> members = getMembers(cache);
                while (!expectedMembers.equals(members)) {
                    System.out.println(String.format("%s != %s, waiting for a topology change event...", expectedMembers, members));
                    this.wait(timeout - now);
                    now = System.currentTimeMillis();
                    if (now >= timeout) {
                        throw new InterruptedException(String.format("Cache %s/%s failed to establish view %s within %d ms.  Current view is: %s", containerName, cacheName, expectedMembers, TIMEOUT, members));
                    }
                    members = getMembers(cache);
                }
                System.out.println(String.format("Cache %s/%s successfully established view %s within %d ms.", containerName, cacheName, expectedMembers, now - start));
            }
        } finally {
            cache.removeListener(this);
        }
    }

    private static Set<String> getMembers(Cache<?, ?> cache) {
        Set<String> members = new TreeSet<>();
        for (Address address: cache.getAdvancedCache().getComponentRegistry().getStateTransferManager().getCacheTopology().getMembers()) {
            members.add(address.toString());
        }
        return members;
    }

    @TopologyChanged
    public void topologyChanged(TopologyChangedEvent<?, ?> event) {
        synchronized (this) {
            this.notify();
        }
    }
}