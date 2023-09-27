/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.bean;

/**
 * A distributable bean instance.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 */
public interface BeanInstance<K> {

    /**
     * Returns the unique identifier of this bean instance.
     * @return a unique identifier
     */
    K getId();

    /**
     * Invoked prior to serializing this bean instance for the purpose of replication or persistence.
     */
    void prePassivate();

    /**
     * Invoked after deserializing this bean instance following replication or persistence.
     */
    void postActivate();
}
