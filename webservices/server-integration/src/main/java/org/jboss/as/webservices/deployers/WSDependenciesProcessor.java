/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.deployers;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;

/**
 * A DUP that sets the WS dependencies
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class WSDependenciesProcessor implements DeploymentUnitProcessor {

    private static final String JBOSSWS_API = "org.jboss.ws.api";
    private static final String JBOSSWS_SPI = "org.jboss.ws.spi";
    private static final String[] JAVAEE_APIS = {
            "jakarta.xml.ws.api",
            "jakarta.xml.soap.api"
    };

    private final boolean addJBossWSDependencies;

    public WSDependenciesProcessor(boolean addJBossWSDependencies) {
        this.addJBossWSDependencies = addJBossWSDependencies;
    }

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        final ModuleSpecification moduleSpec = unit.getAttachment(Attachments.MODULE_SPECIFICATION);

        if (addJBossWSDependencies) {
            moduleSpec.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, JBOSSWS_API).setExport(true).setImportServices(true).build());
            moduleSpec.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, JBOSSWS_SPI).setExport(true).setImportServices(true).build());
        }
        for (String api : JAVAEE_APIS) {
            moduleSpec.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, api).setImportServices(true).build());
        }
    }
}
