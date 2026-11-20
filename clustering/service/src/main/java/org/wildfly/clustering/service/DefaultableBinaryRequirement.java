/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.service;


/**
 * Identifies a requirement that provides a service and can reference some default requirement.
 * @author Paul Ferraro
 * @deprecated Superseded by {@link org.wildfly.service.descriptor.BinaryServiceDescriptor#of(String, org.wildfly.service.descriptor.UnaryServiceDescriptor)}.
 */
@Deprecated(forRemoval = true)
public interface DefaultableBinaryRequirement extends BinaryRequirement {
    UnaryRequirement getDefaultRequirement();

    @Override
    default Class<?> getType() {
        return this.getDefaultRequirement().getType();
    }

    @Override
    default String resolve(String parent, String child) {
        return (child != null) ? BinaryRequirement.super.resolve(parent, child) : this.getDefaultRequirement().resolve(parent);
    }
}
