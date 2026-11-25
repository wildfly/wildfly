/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import org.wildfly.clustering.ejb.bean.BeanInstance;

/**
 * A bean group with the ability to mutate.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public interface MutableBeanGroup<K, V extends BeanInstance<K>> extends BeanGroup<K, V>, Runnable {

}
