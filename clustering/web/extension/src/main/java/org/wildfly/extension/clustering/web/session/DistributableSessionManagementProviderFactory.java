/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.web.session;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.web.service.routing.RouteLocatorProvider;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementConfiguration;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementProvider;

/**
 * @author Paul Ferraro
 */
public interface DistributableSessionManagementProviderFactory {
    DistributableSessionManagementProvider createSessionManagementProvider(DistributableSessionManagementConfiguration<DeploymentUnit> configuration, BinaryServiceConfiguration cacheConfiguration, RouteLocatorProvider locatorProvider);
}
