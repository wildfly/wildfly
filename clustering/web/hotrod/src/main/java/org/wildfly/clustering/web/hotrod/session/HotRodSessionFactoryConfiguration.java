/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.hotrod.session;

import org.wildfly.clustering.ee.hotrod.HotRodConfiguration;

/**
 * Encapsulates the configuration of a {@link HotRodSessionFactory}.
 * @author Paul Ferraro
 */
public interface HotRodSessionFactoryConfiguration extends HotRodConfiguration {
    /**
     * Returns the size of the thread pool used for processing expiration events from the remote Infinispan cluster.
     * @return
     */
    int getExpirationThreadPoolSize();
}
