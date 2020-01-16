/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.ee.structure;

import static org.jboss.as.ee.subsystem.EeCapabilities.EE_GLOBAL_DIRECTORY_CAPABILITY_NAME;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.subsystem.GlobalDirectoryDeploymentService;
import org.jboss.as.ee.subsystem.GlobalDirectoryResourceDefinition;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DelegatingSupplier;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.server.moduleservice.ExternalModule;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;

/**
 * A deployment processor that prepares the global directories that are going to be used as dependencies in a deployment.
 * <p>
 * It finds which services were installed under ee/global-directory resource using its capability name and installs a
 * {@link GlobalDirectoryDeploymentService} to consume the supplied GlobalDirectory values.
 * <p>
 * This deployment processor ensures that the {@link GlobalDirectoryDeploymentService} installed for this deployment is up before moving
 * to the next phase.
 *
 * @author Yeray Borges
 */
public class GlobalDirectoryDependencyProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ServiceName depUnitServiceName = deploymentUnit.getServiceName();

        final DeploymentUnit parent = deploymentUnit.getParent();
        final DeploymentUnit topLevelDeployment = parent == null ? deploymentUnit : parent;
        final ExternalModule externalModuleService = topLevelDeployment.getAttachment(Attachments.EXTERNAL_MODULE_SERVICE);
        final ModuleLoader moduleLoader = deploymentUnit.getAttachment(Attachments.SERVICE_MODULE_LOADER);
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final CapabilityServiceSupport capabilitySupport = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);

        final ServiceRegistry serviceRegistry = phaseContext.getServiceRegistry();
        final ServiceTarget target = phaseContext.getServiceTarget();
        final ServiceTarget externalServiceTarget = deploymentUnit.getAttachment(Attachments.EXTERNAL_SERVICE_TARGET);

        final ServiceName allDirReadyServiceName = depUnitServiceName.append("directory-services-ready");
        final ServiceBuilder<?> allDirReadyServiceBuilder = target.addService(allDirReadyServiceName);
        final List<Supplier<GlobalDirectoryResourceDefinition.GlobalDirectory>> allDirReadySuppliers = new ArrayList<>();

        final ServiceName csName = capabilitySupport.getCapabilityServiceName(EE_GLOBAL_DIRECTORY_CAPABILITY_NAME);
        List<ServiceName> serviceNames = serviceRegistry.getServiceNames();
        for (ServiceName serviceName : serviceNames) {
            if (csName.isParentOf(serviceName)) {
                Supplier<GlobalDirectoryResourceDefinition.GlobalDirectory> pathRequirement = allDirReadyServiceBuilder.requires(serviceName);
                allDirReadySuppliers.add(pathRequirement);
            }
        }

        if (!allDirReadySuppliers.isEmpty()) {
            GlobalDirectoryDeploymentService globalDirDepService = new GlobalDirectoryDeploymentService(allDirReadySuppliers, externalModuleService, moduleSpecification, moduleLoader, serviceRegistry, externalServiceTarget);

            allDirReadyServiceBuilder.requires(phaseContext.getPhaseServiceName());
            allDirReadyServiceBuilder.setInstance(globalDirDepService)
                    .install();

            phaseContext.requires(allDirReadyServiceName, new DelegatingSupplier());
        }
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
        ServiceController<?> requiredService = deploymentUnit.getServiceRegistry().getService(deploymentUnit.getServiceName().append("directory-services-ready"));
        if (requiredService != null) {
            requiredService.setMode(ServiceController.Mode.REMOVE);
        }
    }
}
