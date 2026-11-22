/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.service;

/**
 * Identifies a requirement that provides a service.
 * Includes a unary function for resolving its name.
 * @author Paul Ferraro
 * @deprecated Superseded by {@link org.wildfly.service.descriptor.UnaryServiceDescriptor}.
 */
@Deprecated(forRemoval = true)
public interface UnaryRequirement extends Requirement {

    default String resolve(String name) {
        return String.join(".", this.getName(), name);
    }
}
