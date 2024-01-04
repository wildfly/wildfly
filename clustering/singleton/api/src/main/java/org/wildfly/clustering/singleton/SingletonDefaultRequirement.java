/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton;

import org.wildfly.clustering.service.Requirement;

/**
 * Enumerates capability requirements for default singleton resources
 * @author Paul Ferraro
 */
public enum SingletonDefaultRequirement implements Requirement {
    /**
     * @deprecated Use {@link SingletonDefaultRequirement#POLICY} instead.
     */
    @Deprecated(forRemoval = true) SINGLETON_POLICY("org.wildfly.clustering.singleton.default-policy", org.wildfly.clustering.singleton.SingletonPolicy.class),
    POLICY("org.wildfly.clustering.default-singleton-policy", org.wildfly.clustering.singleton.service.SingletonPolicy.class),
    ;
    private final String name;
    private final Class<?> type;

    SingletonDefaultRequirement(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Class<?> getType() {
        return this.type;
    }
}
