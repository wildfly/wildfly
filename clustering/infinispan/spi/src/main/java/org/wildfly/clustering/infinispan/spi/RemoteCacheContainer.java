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

package org.wildfly.clustering.infinispan.spi;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.commons.api.BasicCacheContainer;
import org.infinispan.commons.marshall.Marshaller;

/**
 * Exposes subset of Infinispan's {@link org.infinispan.client.hotrod.RemoteCacheManager} additionally exposing the name
 * of the remote cache container.
 *
 * @author Radoslav Husar
 */
public interface RemoteCacheContainer extends BasicCacheContainer {

    /**
     * Returns the name of this remote cache container.
     *
     * @return the remote cache container name
     */
    String getName();

    /**
     * Retrieves the configuration currently in use. The configuration object is immutable.
     *
     * @return configuration of the {@link org.infinispan.client.hotrod.RemoteCacheManager}
     */
    Configuration getConfiguration();

    /**
     * Retrieves a named cache from the system.
     *
     * @param forceReturnValue whether or not to implicitly FORCE_RETURN_VALUE for all calls
     * @return the default cache
     */
    <K, V> RemoteCache<K, V> getCache(boolean forceReturnValue);

    /**
     * Retrieves a named cache from the system.
     *
     * @param cacheName        name of cache to retrieve
     * @param forceReturnValue whether or not to implicitly FORCE_RETURN_VALUE for all calls
     * @return a named cache
     */
    <K, V> RemoteCache<K, V> getCache(String cacheName, boolean forceReturnValue);

    /**
     * Returns whether this cache container is started
     *
     * @return whether this cache container is started
     */
    boolean isStarted();

    /**
     * Switch remote cache manager to a different cluster, previously
     * declared via configuration. If the switch was completed successfully,
     * this method returns {@code true}, otherwise it returns {@code false}.
     *
     * @param clusterName name of the cluster to which to switch to
     * @return {@code true} if the cluster was switched, {@code false} otherwise
     */
    boolean switchToCluster(String clusterName);

    /**
     * Switch remote cache manager to a the default cluster, previously
     * declared via configuration. If the switch was completed successfully,
     * this method returns {@code true}, otherwise it returns {@code false}.
     *
     * @return {@code true} if the cluster was switched, {@code false} otherwise
     */
    boolean switchToDefaultCluster();

    /**
     * Returns the marshaller in use by this cache container.
     *
     * @return marshaller in use
     */
    Marshaller getMarshaller();
}
