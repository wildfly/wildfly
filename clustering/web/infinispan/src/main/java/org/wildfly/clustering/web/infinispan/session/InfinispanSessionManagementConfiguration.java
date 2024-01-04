/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.session;

import org.wildfly.clustering.ee.infinispan.InfinispanCacheConfiguration;
import org.wildfly.clustering.web.session.DistributableSessionManagementConfiguration;

/**
 * Configuration of an Infinispan session management provider.
 * @author Paul Ferraro
 */
public interface InfinispanSessionManagementConfiguration<M> extends DistributableSessionManagementConfiguration<M>, InfinispanCacheConfiguration {
}
