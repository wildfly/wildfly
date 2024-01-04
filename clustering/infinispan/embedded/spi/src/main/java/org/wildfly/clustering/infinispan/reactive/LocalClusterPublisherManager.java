/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.reactive;

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
