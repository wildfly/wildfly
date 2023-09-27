/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.service;

/**
 * Identifies a requirement that provides a service.
 * @author Paul Ferraro
 */
public interface Requirement {
    /**
     * The base name of this requirement.
     * @return the requirement name.
     */
    String getName();

    /**
     * The value type of the service provided by this requirement.
     * @return a service value type
     */
    Class<?> getType();
}
