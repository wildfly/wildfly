/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.remote;

import org.jboss.as.controller.RequirementServiceTarget;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.deployment.DeploymentRepositoryService;
import org.wildfly.clustering.ejb.remote.EjbClientServicesProvider;
import org.jboss.as.server.suspend.SuspendableActivityRegistry;
import org.jboss.msc.service.ServiceController;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrar;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

import java.util.List;

/**
 * Used to install a ModuleAvailabilityRegistrar service to support module avalability updates sent
 * to remote EJB clients.
 *
 * @author Richard Achmatowicz
 */
public class ModuleAvailabilityRegistrarServiceInstaller implements ServiceInstaller {
    @Override
    public ServiceController<?> install(RequirementServiceTarget target) {
        ServiceDependency<DeploymentRepository> deploymentRepository = ServiceDependency.on(DeploymentRepositoryService.SERVICE_NAME);
        ServiceDependency<ServiceProviderRegistrar<Object, GroupMember>> serviceProviderRegistrar = ServiceDependency.on(EjbClientServicesProvider.MODULE_AVAILABILITY_REGISTRAR_SERVICE_PROVIDER_REGISTRAR);
        ServiceDependency<SuspendableActivityRegistry> activityRegistry = ServiceDependency.on(SuspendableActivityRegistry.SERVICE_DESCRIPTOR);
        return ServiceInstaller.builder(new ModuleAvailabilityRegistrarService(activityRegistry.get(), serviceProviderRegistrar.get(), deploymentRepository.get()))
                .requires(List.of(deploymentRepository, serviceProviderRegistrar, activityRegistry))
                .build()
                .install(target);
    }
}