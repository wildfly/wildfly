/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.service.user;

/**
 * @author Paul Ferraro
 */
public interface LegacyDistributableUserManagementProviderFactory {
    /**
     * Fabricates a user management provider to use in the absence of proper application security domain configuration.
     * @return a user management provider
     */
    DistributableUserManagementProvider createUserManagementProvider();
}
