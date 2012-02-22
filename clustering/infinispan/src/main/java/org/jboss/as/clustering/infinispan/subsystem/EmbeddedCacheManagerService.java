/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStarted;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStopped;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStartedEvent;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStoppedEvent;
import org.jboss.as.clustering.infinispan.DefaultEmbeddedCacheManager;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.as.clustering.msc.AsynchronousService;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.Value;

/**
 * @author Paul Ferraro
 */
@Listener
public class EmbeddedCacheManagerService extends AsynchronousService<EmbeddedCacheManager> {

    private static final Logger log = Logger.getLogger(EmbeddedCacheManagerService.class.getPackage().getName());
    private static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append(InfinispanExtension.SUBSYSTEM_NAME);

    public static ServiceName getServiceName(String name) {
        return (name != null) ? SERVICE_NAME.append(name) : SERVICE_NAME;
    }

    private final Value<EmbeddedCacheManagerConfiguration> config;
    private volatile EmbeddedCacheManager container;

    public EmbeddedCacheManagerService(Value<EmbeddedCacheManagerConfiguration> config) {
        this.config = config;
    }

    @Override
    public EmbeddedCacheManager getValue() throws IllegalStateException, IllegalArgumentException {
        return this.container;
    }

    @Override
    protected void start() {
        EmbeddedCacheManagerConfiguration config = this.config.getValue();
        this.container = new DefaultEmbeddedCacheManager(config.getGlobalConfiguration(), config.getDefaultCache());
        this.container.addListener(this);
        this.container.start();
        log.debugf("%s cache container started", config.getName());
    }

    @Override
    protected void stop() {
        if ((this.container != null) && this.container.getStatus().allowInvocations()) {
            this.container.stop();
            this.container.removeListener(this);
            log.debugf("%s cache container stopped", this.config.getValue().getName());
        }
    }

    @CacheStarted
    public void cacheStarted(CacheStartedEvent event) {
        InfinispanLogger.ROOT_LOGGER.cacheStarted(event.getCacheName(), this.config.getValue().getName());
    }

    @CacheStopped
    public void cacheStopped(CacheStoppedEvent event) {
        InfinispanLogger.ROOT_LOGGER.cacheStopped(event.getCacheName(), this.config.getValue().getName());
    }
}
