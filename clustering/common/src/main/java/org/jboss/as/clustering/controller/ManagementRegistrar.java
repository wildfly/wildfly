/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

/**
 * Implemented by a management artifact that can register itself.
 * This allows a management object to encapsulates specific registration details (e.g. resource aliases) from the parent resource.
 * @author Paul Ferraro
 */
public interface ManagementRegistrar<R> {
    /**
     * Registers this object with a resource.
     * @param registration a registration for a management resource
     */
    void register(R registration);
}
