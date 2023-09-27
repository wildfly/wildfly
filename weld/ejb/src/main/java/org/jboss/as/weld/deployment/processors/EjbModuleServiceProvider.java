/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.deployment.processors;

import java.util.Collection;
import java.util.Collections;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.weld.services.bootstrap.WeldEjbInjectionServices;
import org.jboss.as.weld.spi.ModuleServicesProvider;
import org.jboss.modules.Module;
import org.jboss.weld.bootstrap.api.Service;

/**
 *
 * @author Martin Kouba
 */
public class EjbModuleServiceProvider implements ModuleServicesProvider {

    @Override
    public Collection<Service> getServices(DeploymentUnit rootDeploymentUnit, DeploymentUnit deploymentUnit, Module module, ResourceRoot resourceRoot) {
        if (resourceRoot == null) {
            return Collections.emptySet();
        }
        return Collections.singleton(new WeldEjbInjectionServices(rootDeploymentUnit.getServiceRegistry(),
                rootDeploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION),
                rootDeploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_APPLICATION_DESCRIPTION), resourceRoot.getRoot(), module,
                DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)));
    }

}
