/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.remote;

import org.jboss.as.controller.RequirementServiceTarget;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.deployment.DeploymentRepositoryService;
import org.jboss.as.server.suspend.SuspendableActivityRegistry;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.ejb.remote.ModuleAvailabilityRegistrarProvider;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrar;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;
import java.util.List;
/**
 * Used to install a ModuleAvailabilityRegistrar service to support module availability updates sent
 * to remote EJB clients.
 *
 * @author Richard Achmatowicz
 */
public class ModuleAvailabilityRegistrarServiceInstaller implements ServiceInstaller {
    @Override
    public ServiceController<?> install(RequirementServiceTarget target) {
        ServiceDependency<DeploymentRepository> deploymentRepository = ServiceDependency.on(DeploymentRepositoryService.SERVICE_NAME);
        ServiceDependency<ServiceProviderRegistrar<Object, GroupMember>> serviceProviderRegistrar = ServiceDependency.on(ModuleAvailabilityRegistrarProvider.MODULE_AVAILABILITY_REGISTRAR_SERVICE_PROVIDER_REGISTRAR);
        ServiceDependency<SuspendableActivityRegistry> activityRegistry = ServiceDependency.on(SuspendableActivityRegistry.SERVICE_DESCRIPTOR);
        // NOTE: choose the correct builder to avoid service installation issues (need a supplier builder here)
        return ServiceInstaller.builder(() -> new ModuleAvailabilityRegistrarService(activityRegistry, serviceProviderRegistrar, deploymentRepository))
                // this service performs blocking operations
                .blocking()
                // and depends in start()/stop() methods
                .onStart(ModuleAvailabilityRegistrarService::start)
                .onStop(ModuleAvailabilityRegistrarService::stop)
                .requires(List.of(deploymentRepository, serviceProviderRegistrar, activityRegistry))
                // doesn't need a name but give it one anyway for debugging
                .provides(ServiceName.parse("org.jboss.as.ejb3.remote.module-availability-registrar-service"))
                // TODO: this service ideally needs to ON_DEMAND and made avaioable upon depoyment of one or more EJBs
                .asActive()
                .build()
                .install(target);
    }
}
