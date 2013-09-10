/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.cache.simple;

import java.security.AccessController;
import java.util.concurrent.ThreadFactory;

import org.jboss.as.ejb3.cache.Cache;
import org.jboss.as.ejb3.cache.CacheFactory;
import org.jboss.as.ejb3.cache.Identifiable;
import org.jboss.as.ejb3.cache.StatefulObjectFactory;
import org.jboss.as.ejb3.component.stateful.StatefulTimeoutInfo;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.value.Value;
import org.jboss.threads.JBossThreadFactory;
import org.wildfly.clustering.ejb.IdentifierFactory;
import org.wildfly.clustering.ejb.PassivationListener;
import org.wildfly.security.manager.GetAccessControlContextAction;

/**
 * Service that provides a simple {@link CacheFactory}.
 *
 * @author Paul Ferraro
 *
 * @param <K> the cache key type
 * @param <V> the cache value type
 */
public class SimpleCacheFactoryService<K, V extends Identifiable<K>> extends AbstractService<CacheFactory<K, V>> implements CacheFactory<K, V> {
    private final Value<ServerEnvironment> environment;
    private final StatefulTimeoutInfo timeout;

    public SimpleCacheFactoryService(Value<ServerEnvironment> environment, StatefulTimeoutInfo timeout) {
        this.environment = environment;
        this.timeout = timeout;
    }

    @Override
    public CacheFactory<K, V> getValue() {
        return this;
    }

    private static ThreadFactory createThreadFactory() {
        return new JBossThreadFactory(new ThreadGroup(SimpleCache.class.getSimpleName()), Boolean.FALSE, null, "%G - %t", null, null, AccessController.doPrivileged(GetAccessControlContextAction.getInstance()));
    }

    @Override
    public Cache<K, V> createCache(IdentifierFactory<K> identifierFactory, StatefulObjectFactory<V> factory, PassivationListener<V> passivationListener) {
        return new SimpleCache<>(factory, identifierFactory, this.timeout, this.environment.getValue(), createThreadFactory());
    }
}
