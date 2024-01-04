/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ejb.bean;

/**
 * Exposes statistics for cached beans.
 * @author Paul Ferraro
 */
public interface BeanStatistics {
    /**
     * Returns the number of beans that are actively cached.
     * @return a number of beans
     */
    int getActiveCount();

    /**
     * Returns the number of passivated beans accessible to the cache.
     * @return a number of beans
     */
    int getPassiveCount();
}
