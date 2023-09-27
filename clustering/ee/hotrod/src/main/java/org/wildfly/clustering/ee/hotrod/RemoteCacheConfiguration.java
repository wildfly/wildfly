/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.hotrod;

/**
 * Configuration identifying a remote cache.
 * @author Paul Ferraro
 */
public interface RemoteCacheConfiguration {
    String getContainerName();

    String getConfigurationName();
}
