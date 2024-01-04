/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.PathAddress;

/**
 * Factory for creating a {@link ResourceServiceConfigurator}.
 * @author Paul Ferraro
 */
public interface ResourceServiceConfiguratorFactory {

    /**
     * Creates a {@link ServiceConfigurator} for a resource
     * @param address the path address of this resource
     * @return a service configurator
     */
    ResourceServiceConfigurator createServiceConfigurator(PathAddress address);
}
