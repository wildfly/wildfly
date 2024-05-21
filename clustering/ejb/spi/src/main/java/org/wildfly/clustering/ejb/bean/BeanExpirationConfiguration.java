/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.bean;

import org.wildfly.clustering.server.expiration.ExpirationConfiguration;

/**
 * Encapsulates the expiration configuration for a bean.
 * @author Paul Ferraro
 *
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public interface BeanExpirationConfiguration<K, V extends BeanInstance<K>> extends BeanExpiration, ExpirationConfiguration<V> {
}
