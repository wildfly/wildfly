/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton;

import org.wildfly.clustering.service.DefaultableUnaryRequirement;
import org.wildfly.clustering.service.Requirement;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * @author Paul Ferraro
 */
@Deprecated(forRemoval = true)
public enum SingletonRequirement implements DefaultableUnaryRequirement {
    /**
     * @deprecated Use {@link org.wildfly.clustering.singleton.SingletonPolicy#SERVICE_DESCRIPTOR} instead.
     */
    @Deprecated(forRemoval = true) SINGLETON_POLICY(org.wildfly.clustering.singleton.SingletonPolicy.SERVICE_DESCRIPTOR, SingletonDefaultRequirement.SINGLETON_POLICY),
    /**
     * @deprecated Use {@link org.wildfly.clustering.singleton.service.SingletonPolicy#SERVICE_DESCRIPTOR} instead.
     */
    @Deprecated POLICY(org.wildfly.clustering.singleton.service.SingletonPolicy.SERVICE_DESCRIPTOR, SingletonDefaultRequirement.POLICY),
    ;
    private final UnaryServiceDescriptor<?> descriptor;
    private final Requirement defaultRequirement;

    SingletonRequirement(UnaryServiceDescriptor<?> descriptor, Requirement defaultRequirement) {
        this.descriptor = descriptor;
        this.defaultRequirement = defaultRequirement;
    }

    @Override
    public String getName() {
        return this.descriptor.getName();
    }

    @Override
    public Requirement getDefaultRequirement() {
        return this.defaultRequirement;
    }
}
