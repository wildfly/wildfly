/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.client;

/**
 * @author Paul Ferraro
 */
public interface RemoteCache<K, V> extends org.infinispan.client.hotrod.RemoteCache<K, V> {

    @Override
    RemoteCacheContainer getRemoteCacheContainer();
}
