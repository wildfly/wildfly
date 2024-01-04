/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.stateful.cache;

import org.wildfly.clustering.ejb.bean.BeanInstance;

/**
 * A removable stateful session bean instance.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 */
public interface StatefulSessionBeanInstance<K> extends BeanInstance<K> {

    /**
     * Indicates that this bean instance was removed from its cache.
     */
    void removed();
}
