/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.bean;

import java.util.function.Supplier;

/**
 * Encapsulates the configuration of a bean manager.
 * @author Paul Ferraro
 *
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public interface BeanManagerConfiguration<K, V extends BeanInstance<K>> {
    Supplier<K> getIdentifierFactory();
    String getBeanName();
    BeanExpirationConfiguration<K, V> getExpiration();
}
