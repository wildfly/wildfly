/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import org.wildfly.clustering.ejb.bean.Bean;
import org.wildfly.clustering.ejb.bean.BeanInstance;

/**
 * A {@link Bean} that allows associating a bean instance.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public interface MutableBean<K, V extends BeanInstance<K>> extends Bean<K, V> {

    /**
     * Associates this bean with an instance.
     * @param instance
     */
    void setInstance(V instance);
}
