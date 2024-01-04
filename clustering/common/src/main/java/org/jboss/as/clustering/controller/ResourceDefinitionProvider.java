/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Provides a {@link ResourceDefinition} and handles its registration.
 * @author Paul Ferraro
 */
public interface ResourceDefinitionProvider extends ManagementRegistrar<ManagementResourceRegistration> {

    /**
     * The registration path of the provided resource definition.
     * @return a path element
     */
    PathElement getPathElement();
}
