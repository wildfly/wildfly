/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.infinispan;

/**
 * General configuration that identifies an Infinispan cache.
 * @author Paul Ferraro
 */
public interface InfinispanCacheConfiguration {
    String getContainerName();
    String getCacheName();
}
