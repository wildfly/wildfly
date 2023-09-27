/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.spi;

import java.util.Collection;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.modules.Module;
import org.jboss.weld.bootstrap.api.Service;

/**
 * Provides services which should be added to all bean deployment archves of a specific EE module.
 *
 * @author Martin Kouba
 * @see Service
 * @see BeanDeploymentArchiveServicesProvider
 */
public interface ModuleServicesProvider {

    /**
     *
     * @param deploymentUnit
     * @return the services for the given deployment unit
     */
    Collection<Service> getServices(DeploymentUnit rootDeploymentUnit, DeploymentUnit deploymentUnit, Module module, ResourceRoot resourceRoot);

}
