/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.mail.extension;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.filter.PathFilters;

/**
 * @author Stuart Douglas
 */
public class MailDependenciesProcessor implements DeploymentUnitProcessor {

    private static final String MAIL_API = "jakarta.mail.api";
    private static final String ACTIVATION_API = "jakarta.activation.api";
    private static final String ANGUS_MAIL_IMPL = "org.eclipse.angus.mail";
    private static final String ANGUS_ACTIVATION_IMPL = "org.eclipse.angus.activation";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        final ModuleSpecification moduleSpec = unit.getAttachment(Attachments.MODULE_SPECIFICATION);

        moduleSpec.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, MAIL_API).setImportServices(true).build());
        moduleSpec.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, ACTIVATION_API).setImportServices(true).build());
        ModuleDependency angusMailModDep = ModuleDependency.Builder.of(moduleLoader, ANGUS_MAIL_IMPL).setImportServices(true).build();
        angusMailModDep.addImportFilter(PathFilters.getMetaInfFilter(), true);
        moduleSpec.addSystemDependency(angusMailModDep);
        moduleSpec.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, ANGUS_ACTIVATION_IMPL).setImportServices(true).build());
    }
}
