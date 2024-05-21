/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.bean;

import org.wildfly.clustering.cache.infinispan.embedded.EmbeddedCacheConfiguration;
import org.wildfly.clustering.ejb.cache.bean.BeanMetaDataFactoryConfiguration;

/**
 * Encapsulate the configuration for an {@link InfinispanBeanMetaDataFactory}.
 * @author Paul Ferraro
 */
public interface InfinispanBeanMetaDataFactoryConfiguration extends BeanMetaDataFactoryConfiguration, EmbeddedCacheConfiguration {
}
