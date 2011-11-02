/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.clustering.web.infinispan;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStopped;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStoppedEvent;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.jboss.as.clustering.CoreGroupCommunicationService;
import org.jboss.as.clustering.lock.SharedLocalYieldingClusterLockManager;
import org.jgroups.Channel;

import static org.jboss.as.clustering.web.infinispan.InfinispanWebLogger.ROOT_LOGGER;
import static org.jboss.as.clustering.web.infinispan.InfinispanWebMessages.MESSAGES;

/**
 * @author Vladimir Blagojevic
 * @author Paul Ferraro
 */
@Listener
public class DefaultLockManagerSource implements LockManagerSource {
    /** The scope assigned to our group communication service */
    public static final Short SCOPE_ID = Short.valueOf((short) 222);
    /** The service name of the group communication service */
    public static final String SERVICE_NAME = "HTTPSESSIONOWNER";

    // Store LockManagers in static map so they can be shared across DCMs
    static final Map<String, LockManagerEntry> lockManagers = new HashMap<String, LockManagerEntry>();

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.web.tomcat.service.session.distributedcache.ispn.LockManagerSource#getLockManager(org.infinispan.Cache)
     */
    @Override
    public SharedLocalYieldingClusterLockManager getLockManager(Cache<?, ?> cache) {
        if (!cache.getConfiguration().getCacheMode().isClustered()) return null;

        EmbeddedCacheManager container = (EmbeddedCacheManager) cache.getCacheManager();
        String containerName = container.getGlobalConfiguration().getCacheManagerName();

        synchronized (lockManagers) {
            LockManagerEntry entry = lockManagers.get(containerName);

            if (entry == null) {
                JGroupsTransport transport = (JGroupsTransport) cache.getAdvancedCache().getRpcManager().getTransport();

                entry = new LockManagerEntry(transport.getChannel());

                debug("Started lock manager for \"%s\" container", containerName);

                container.addListener(entry);

                lockManagers.put(containerName, entry);
            }

            String cacheName = cache.getName();

            debug("Registering \"%s\" cache with lock manager for \"%s\" container", cacheName, containerName);

            entry.addCache(cacheName);

            return entry.getLockManager();
        }
    }

    @Listener
    public static class LockManagerEntry {
        private final SharedLocalYieldingClusterLockManager lockManager;
        private final CoreGroupCommunicationService service;
        private final Set<String> caches = new HashSet<String>();

        LockManagerEntry(Channel channel) {
            this.service = new CoreGroupCommunicationService();
            this.service.setChannel(channel);
            this.service.setScopeId(SCOPE_ID);

            try {
                this.service.start();
            } catch (Exception e) {
                throw MESSAGES.errorStartingGroupCommunications(e, channel.getClusterName());
            }

            this.lockManager = new SharedLocalYieldingClusterLockManager(SERVICE_NAME, this.service, this.service);

            try {
                this.lockManager.start();
            } catch (Exception e) {
                this.service.stop();
                throw MESSAGES.errorStartingLockManager(e, channel.getClusterName());
            }
        }

        SharedLocalYieldingClusterLockManager getLockManager() {
            return this.lockManager;
        }

        synchronized void addCache(String cacheName) {
            this.caches.add(cacheName);
        }

        synchronized boolean removeCache(String cacheName) {
            this.caches.remove(cacheName);

            boolean empty = this.caches.isEmpty();

            if (empty) {
                try {
                    this.lockManager.stop();
                } catch (Exception e) {
                    ROOT_LOGGER.warn(e.getMessage(), e);
                }
                try {
                    this.service.stop();
                } catch (Exception e) {
                    ROOT_LOGGER.warn(e.getMessage(), e);
                }
            }

            return empty;
        }

        @CacheStopped
        public void stopped(CacheStoppedEvent event) {
            EmbeddedCacheManager container = event.getCacheManager();
            String containerName = container.getGlobalConfiguration().getCacheManagerName();

            synchronized (lockManagers) {
                LockManagerEntry entry = lockManagers.get(containerName);

                if (entry != null) {
                    String cacheName = event.getCacheName();

                    debug("Deregistering \"%s\" cache from lock manager for \"%s\" container", cacheName, containerName);

                    // Returns true if this was the last cache
                    if (entry.removeCache(cacheName)) {
                        debug("Stopped lock manager for \"%s\" container", containerName);

                        lockManagers.remove(containerName);

                        container.removeListener(entry);
                    }
                }
            }
        }
    }

    static void debug(String message, Object... args) {
        ROOT_LOGGER.debugf(message, args);
    }
}
