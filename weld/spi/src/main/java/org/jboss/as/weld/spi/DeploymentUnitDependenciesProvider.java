/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.spi;

import java.util.Set;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.service.ServiceName;

/**
 * Allows to add service dependencies for a deployment unit.
 *
 * @author Martin Kouba
 */
public interface DeploymentUnitDependenciesProvider {

    /**
     *
     * @param deploymentUnit
     * @return the set of dependencies for the given deployment unit
     */
    Set<ServiceName> getDependencies(DeploymentUnit deploymentUnit);

}
