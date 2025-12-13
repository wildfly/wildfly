/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import org.wildfly.clustering.server.expiration.Expiration;

/**
 * Encapsulates the configuration of a {@link BeanMetaDataFactory}.
 * @author Paul Ferraro
 */
public interface BeanMetaDataFactoryConfiguration extends Expiration {
    String getBeanName();
}
