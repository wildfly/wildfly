/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.hotrod.sso;

import org.infinispan.client.hotrod.RemoteCache;

/**
 * @author Paul Ferraro
 */
public interface HotRodSSOManagerFactoryConfiguration {
    <K, V> RemoteCache<K, V> getRemoteCache();
}
