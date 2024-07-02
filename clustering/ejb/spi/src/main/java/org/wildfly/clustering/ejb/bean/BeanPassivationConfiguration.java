/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ejb.bean;

import java.util.OptionalInt;

/**
 * Configuration for bean passivation.
 * @author Paul Ferraro
 */
public interface BeanPassivationConfiguration {
    /**
     * When present, returns the maximum number of bean instances to retain in memory at a given time.
     * @return when present, the maximum number of bean instances to retain in memory at a given time, or empty if passivation is disabled.
     */
    OptionalInt getMaxActiveBeans();
}
