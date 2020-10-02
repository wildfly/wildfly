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
