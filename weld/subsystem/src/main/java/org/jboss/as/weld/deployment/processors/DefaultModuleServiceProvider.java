/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.deployment.processors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.ee.structure.EJBAnnotationPropertyReplacement;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.weld.discovery.WeldClassFileServices;
import org.jboss.as.weld.services.bootstrap.WeldResourceInjectionServices;
import org.jboss.as.weld.spi.ModuleServicesProvider;
import org.jboss.modules.Module;
import org.jboss.weld.bootstrap.api.Service;

/**
 *
 * @author Martin Kouba
 */
public class DefaultModuleServiceProvider implements ModuleServicesProvider {

    @Override
    public Collection<Service> getServices(DeploymentUnit rootDeploymentUnit, DeploymentUnit deploymentUnit, Module module, ResourceRoot resourceRoot) {
        List<Service> services = new ArrayList<>();

        // ResourceInjectionServices
        // TODO I'm not quite sure we should use rootDeploymentUnit here
        services.add(new WeldResourceInjectionServices(rootDeploymentUnit.getServiceRegistry(),
                rootDeploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION),
                EJBAnnotationPropertyReplacement.propertyReplacer(deploymentUnit),
                module, DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)));

        // ClassFileServices
        final CompositeIndex index = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        if (index != null) {
            services.add(new WeldClassFileServices(index, module.getClassLoader()));
        }
        return services;
    }

}
