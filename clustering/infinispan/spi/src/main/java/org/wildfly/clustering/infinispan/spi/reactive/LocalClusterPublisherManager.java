/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.infinispan.spi.reactive;

import java.lang.reflect.Field;

import org.infinispan.configuration.cache.Configuration;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.reactive.publisher.impl.LocalClusterPublisherManagerImpl;

/**
 * Overrides Infinispan's {@link org.infinispan.factories.PublisherManagerFactory.LOCAL_CLUSTER_PUBLISHER} to align its segmentation logic with WildFly's key partitioner for non-tx invalidation caches.
 * @author Paul Ferraro
 */
@Scope(Scopes.NAMED_CACHE)
public class LocalClusterPublisherManager<K, V> extends LocalClusterPublisherManagerImpl<K, V> {
    @Inject Configuration configuration;

    @Override
    public void start() {
        try {
            Field field = LocalClusterPublisherManagerImpl.class.getDeclaredField("maxSegment");
            field.setAccessible(true);
            try {
                field.set(this, this.configuration.clustering().hash().numSegments());
            } finally {
                field.setAccessible(false);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
