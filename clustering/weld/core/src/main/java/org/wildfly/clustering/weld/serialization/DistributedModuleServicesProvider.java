/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.serialization;

import java.util.Collection;
import java.util.Collections;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.weld.spi.ModuleServicesProvider;
import org.jboss.modules.Module;
import org.jboss.weld.bootstrap.api.Service;
import org.kohsuke.MetaInfServices;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(ModuleServicesProvider.class)
public class DistributedModuleServicesProvider implements ModuleServicesProvider {

    @Override
    public Collection<Service> getServices(DeploymentUnit rootDeploymentUnit, DeploymentUnit deploymentUnit, Module module, ResourceRoot resourceRoot) {
        return Collections.singleton(new DistributedContextualStore(deploymentUnit.getName()));
    }
}
