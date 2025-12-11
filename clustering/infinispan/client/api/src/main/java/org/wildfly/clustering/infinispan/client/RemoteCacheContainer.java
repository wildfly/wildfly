/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.client;

import org.infinispan.client.hotrod.jmx.RemoteCacheManagerMXBean;

/**
 * Extends Infinispan's {@link org.wildfly.clustering.infinispan.client.client.hotrod.RemoteCacheContainer} additionally exposing the name of the
 * remote cache container, an administration utility, and a mechanism for configuring near caching per remote cache.
 *
 * @author Radoslav Husar
 * @author Paul Ferraro
 */
public interface RemoteCacheContainer extends org.infinispan.client.hotrod.RemoteCacheContainer, RemoteCacheManagerMXBean {

    /**
     * Returns the name of this remote cache container.
     *
     * @return the remote cache container name
     */
    String getName();
}
