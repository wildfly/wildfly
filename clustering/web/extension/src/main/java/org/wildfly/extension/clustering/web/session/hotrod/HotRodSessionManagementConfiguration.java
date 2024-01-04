/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.session.hotrod;

import org.wildfly.clustering.ee.hotrod.RemoteCacheConfiguration;
import org.wildfly.clustering.web.session.DistributableSessionManagementConfiguration;

/**
 * Configuration of an {@link HotRodSessionManagementProvider}.
 * @author Paul Ferraro
 */
public interface HotRodSessionManagementConfiguration<M> extends DistributableSessionManagementConfiguration<M>, RemoteCacheConfiguration {
    /**
     * Returns the size of the thread pool used for processing expiration events from the remote Infinispan cluster.
     * @return
     */
    int getExpirationThreadPoolSize();
}
