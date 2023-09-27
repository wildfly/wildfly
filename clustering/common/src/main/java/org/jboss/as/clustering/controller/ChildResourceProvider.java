/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.Set;

import org.jboss.as.controller.registry.Resource;

/**
 * Provides child resources.
 * @author Paul Ferraro
 */
public interface ChildResourceProvider {
    /**
     * Returns a child resource with the specified name.
     * @param name a resource name
     * @return a resource
     */
    Resource getChild(String name);

    /**
     * Returns the complete set of child resource names.
     * @return a set of resource names
     */
    Set<String> getChildren();
}