/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.services.bootstrap;

import java.util.Collection;
import java.util.Collections;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.weld.spi.ModuleServicesProvider;
import org.jboss.modules.Module;
import org.jboss.weld.bootstrap.api.Service;

/**
 *
 * @author Martin Kouba
 */
public class JpaModuleServiceProvider implements ModuleServicesProvider {

    @Override
    public Collection<Service> getServices(DeploymentUnit rootDeploymentUnit, DeploymentUnit deploymentUnit, Module module, ResourceRoot resourceRoot) {
        return Collections.singleton(new WeldJpaInjectionServices(deploymentUnit));
    }

}
