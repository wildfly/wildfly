/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.stateful.cache;

import org.jboss.as.ejb3.component.stateful.StatefulSessionComponent;
import org.wildfly.clustering.ejb.bean.BeanInstance;

/**
 * A removable stateful session bean instance.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 */
public interface StatefulSessionBeanInstance<K> extends BeanInstance<K> {
    @Override
    default String getName() {
        return this.getComponent().getComponentName();
    }

    /**
     * Returns the component of this bean instance.
     * @return the component of this bean instance.
     */
    StatefulSessionComponent getComponent();

    /**
     * Indicates that this bean instance was removed from its cache.
     */
    void removed();
}
