/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton;

import org.wildfly.clustering.service.DefaultableUnaryRequirement;
import org.wildfly.clustering.service.Requirement;

/**
 * @author Paul Ferraro
 */
public enum SingletonRequirement implements DefaultableUnaryRequirement {
    /**
     * @deprecated Use {@link SingletonRequirement#POLICY} instead.
     */
    @Deprecated(forRemoval = true) SINGLETON_POLICY("org.wildfly.clustering.singleton.policy", SingletonDefaultRequirement.SINGLETON_POLICY),
    POLICY("org.wildfly.clustering.singleton-policy", SingletonDefaultRequirement.POLICY),
    ;
    private final String name;
    private final Requirement defaultRequirement;

    SingletonRequirement(String name, Requirement defaultRequirement) {
        this.name = name;
        this.defaultRequirement = defaultRequirement;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Requirement getDefaultRequirement() {
        return this.defaultRequirement;
    }
}
