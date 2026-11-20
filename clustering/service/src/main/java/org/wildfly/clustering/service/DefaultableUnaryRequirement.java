/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.service;

/**
 * Identifies a requirement that provides a service and can reference some default requirement.
 * @author Paul Ferraro
 * @deprecated Superseded by {@link org.wildfly.service.descriptor.UnaryServiceDescriptor#of(String, org.wildfly.service.descriptor.NullaryServiceDescriptor)}.
 */
@Deprecated(forRemoval = true)
public interface DefaultableUnaryRequirement extends UnaryRequirement {
    Requirement getDefaultRequirement();

    @Override
    default Class<?> getType() {
        return this.getDefaultRequirement().getType();
    }

    @Override
    default String resolve(String name) {
        return (name != null) ? UnaryRequirement.super.resolve(name) : this.getDefaultRequirement().getName();
    }
}
