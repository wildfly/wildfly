/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import org.wildfly.clustering.ejb.bean.BeanExpiration;

/**
 * Encapsulates the configuration of a {@link BeanMetaDataFactory}.
 * @author Paul Ferraro
 */
public interface BeanMetaDataFactoryConfiguration {
    String getBeanName();
    BeanExpiration getExpiration();
}
