/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton;

/**
 * @author Paul Ferraro
 */
public interface SingletonStatus {
    /**
     * Indicates whether this member is the primary provider of the singleton.
     * @return true, if this member is the primary provider of the singleton, false otherwise.
     */
    boolean isPrimaryProvider();
}
