/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ejb.bean;

/**
 * Configuration for bean passivation.
 * @author Paul Ferraro
 */
public interface BeanPassivationConfiguration {
    /**
     * Returns the maximum number of bean instances to retain in memory at a given time.
     * @return the maximum number of bean instances to retain in memory at a given time, or null if passivation is disabled.
     */
    Integer getMaxActiveBeans();
}
