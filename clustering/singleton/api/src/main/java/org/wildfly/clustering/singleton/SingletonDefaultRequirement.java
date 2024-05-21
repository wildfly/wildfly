/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton;

import org.wildfly.clustering.service.Requirement;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;

/**
 * Enumerates capability requirements for default singleton resources
 * @author Paul Ferraro
 */
@Deprecated(forRemoval = true)
public enum SingletonDefaultRequirement implements Requirement {
    /**
     * @deprecated Use {@link org.wildfly.clustering.singleton.SingletonPolicy#DEFAULT_SERVICE_DESCRIPTOR} instead.
     */
    @Deprecated(forRemoval = true) SINGLETON_POLICY(org.wildfly.clustering.singleton.SingletonPolicy.DEFAULT_SERVICE_DESCRIPTOR),
    /**
     * @deprecated Use {@link org.wildfly.clustering.singleton.service.SingletonPolicy#DEFAULT_SERVICE_DESCRIPTOR} instead.
     */
    @Deprecated POLICY(org.wildfly.clustering.singleton.service.SingletonPolicy.DEFAULT_SERVICE_DESCRIPTOR),
    ;
    private final NullaryServiceDescriptor<?> descriptor;

    SingletonDefaultRequirement(NullaryServiceDescriptor<?> descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public String getName() {
        return this.descriptor.getName();
    }

    @Override
    public Class<?> getType() {
        return this.descriptor.getType();
    }
}
