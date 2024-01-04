/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.reactive;

import org.infinispan.factories.annotations.DefaultFactoryFor;
import org.infinispan.reactive.publisher.impl.ClusterPublisherManager;
import org.infinispan.reactive.publisher.impl.LocalPublisherManager;

/**
 * Because we override Infinispan's KeyPartition for non-tx invalidation-caches, we need to make sure the {@link org.infinispan.factories.PublisherManagerFactory.LOCAL_CLUSTER_PUBLISHER}
 * handles segmentation in the same way.
 * @author Paul Ferraro
 */
@DefaultFactoryFor(classes = { LocalPublisherManager.class, ClusterPublisherManager.class }, names = org.infinispan.factories.PublisherManagerFactory.LOCAL_CLUSTER_PUBLISHER)
public class PublisherManagerFactory extends org.infinispan.factories.PublisherManagerFactory {
    @Override
    public Object construct(String componentName) {
        if (componentName.equals(LOCAL_CLUSTER_PUBLISHER)) {
            return new LocalClusterPublisherManager<>();
        }
        return super.construct(componentName);
   }
}
