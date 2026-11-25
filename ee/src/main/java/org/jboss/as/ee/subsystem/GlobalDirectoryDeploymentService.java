/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.subsystem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.ee.subsystem.GlobalDirectoryResourceDefinition.GlobalDirectory;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.server.moduleservice.ExternalModule;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * This service does the required steps to add a directory as a deployment dependency and as an {@link ExternalModule}.
 * <p>
 * Basically is used as an intermediate step in <code>GlobalDirectoryDependencyProcessor</code> to retrieve the global directories
 * supplied by the services installed with the corresponding ee/global-directory resource. Those services are consumed here
 * and for each supplied value an {@link ExternalModule} is created and added as system dependency to the current deployment.
 *
 * @author Yeray Borges
 */
public class GlobalDirectoryDeploymentService implements Service {
    private List<Supplier<GlobalDirectory>> globalDirectories;
    private final ExternalModule externalModuleService;
    private final ModuleSpecification moduleSpecification;
    private final ModuleLoader moduleLoader;
    private final ServiceRegistry serviceRegistry;
    private final ServiceTarget serviceTarget;

    public GlobalDirectoryDeploymentService(List<Supplier<GlobalDirectory>> globalDirectories, final ExternalModule externalModuleService, final ModuleSpecification moduleSpecification, final ModuleLoader moduleLoader, final ServiceRegistry serviceRegistry, final ServiceTarget serviceTarget) {
        this.globalDirectories = globalDirectories;
        this.moduleSpecification = moduleSpecification;
        this.externalModuleService = externalModuleService;
        this.moduleLoader = moduleLoader;
        this.serviceRegistry = serviceRegistry;
        this.serviceTarget = serviceTarget;
    }

    @Override
    public void start(StartContext context) throws StartException {
        final List<GlobalDirectory> dataSorted = new ArrayList<>();
        for (Supplier<GlobalDirectory> dataSupplier : globalDirectories) {
            GlobalDirectory data = dataSupplier.get();
            dataSorted.add(data);
        }

        //validate all exists
        for(GlobalDirectory globalDirectory : dataSorted) {
            if (!Files.exists(globalDirectory.getResolvedPath())) {
                throw EeLogger.ROOT_LOGGER.globalDirectoryDoNotExist(globalDirectory.getResolvedPath().toString(), globalDirectory.getName());
            }
        }

        //sort by name, it will allow setting the final deployment module dependencies in a deterministic way
        Collections.sort(dataSorted, Comparator.comparing(GlobalDirectory::getModuleName));
        synchronized (externalModuleService) {
            for (GlobalDirectory data : dataSorted) {
                Path resolvedPath = data.getResolvedPath();
                String moduleName = data.getModuleName();
                String moduleIdentifier = externalModuleService.addExternalModuleAsString(moduleName, resolvedPath.toString(), serviceRegistry, serviceTarget);
                moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, moduleIdentifier).setImportServices(true).build());
            }
        }
    }

    @Override
    public void stop(StopContext context) {

    }
}
