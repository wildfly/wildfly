/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.deployment;

import java.util.List;

import org.wildfly.clustering.web.service.session.DistributableSessionManagementProvider;

/**
 * Configuration of a distributable web deployment.
 * @author Paul Ferraro
 */
public interface DistributableWebDeploymentConfiguration {
    /**
     * References the name of a session management provider.
     * @return a session management provider name
     */
    String getSessionManagementName();

    /**
     * Returns a deployment-specific session management provider.
     * @return a session management provider
     */
    DistributableSessionManagementProvider getSessionManagementProvider();

    /**
     * Returns a list of immutable session attribute classes.
     * @return a list of class names
     */
    List<String> getImmutableClasses();
}
