/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.service;

/**
 * Identifies a requirement that provides a service.
 * Includes a unary function for resolving its name.
 * @author Paul Ferraro
 */
public interface UnaryRequirement extends Requirement {

    default String resolve(String name) {
        return String.join(".", this.getName(), name);
    }
}
