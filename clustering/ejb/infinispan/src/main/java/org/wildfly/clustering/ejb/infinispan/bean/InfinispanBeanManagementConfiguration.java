/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.bean;

import org.wildfly.clustering.ee.infinispan.InfinispanCacheConfiguration;
import org.wildfly.clustering.ejb.bean.BeanManagementConfiguration;

/**
 * Configuration of an Infinispan-based bean management provider.
 * @author Paul Ferraro
 */
public interface InfinispanBeanManagementConfiguration extends InfinispanCacheConfiguration, BeanManagementConfiguration {
}
