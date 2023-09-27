/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Registration interface for child resource definitions.
 * @author Paul Ferraro
 */
public interface ChildResourceRegistrar<R extends ManagementResourceRegistration> {
    /**
     * Registers this child resource, returning the new registration
     * @param parent the parent registration
     * @return the child resource registration
     */
    R register(R parent);
}
