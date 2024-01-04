/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.service;


/**
 * Identifies a requirement that provides a service.
 * Includes a binary function for resolving its name.
 * @author Paul Ferraro
 */
public interface BinaryRequirement extends Requirement {

    default String resolve(String parent, String child) {
        return String.join(".", this.getName(), parent, child);
    }
}
