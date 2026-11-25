/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo;

import org.jboss.as.pojo.descriptor.BaseBeanFactory;
import org.jboss.as.pojo.descriptor.KernelDeploymentXmlDescriptor;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.filter.PathFilters;

import java.util.List;

/**
 * Check if we have any bean factories, as we need the POJO module api.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class KernelDeploymentModuleProcessor implements DeploymentUnitProcessor {

    private static final String POJO_MODULE = "org.jboss.as.pojo";

    /**
     * Add POJO module if we have any bean factories.
     *
     * @param phaseContext the deployment unit context
     * @throws org.jboss.as.server.deployment.DeploymentUnitProcessingException
     */
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        final List<KernelDeploymentXmlDescriptor> kdXmlDescriptors = unit.getAttachment(KernelDeploymentXmlDescriptor.ATTACHMENT_KEY);
        if (kdXmlDescriptors == null || kdXmlDescriptors.isEmpty())
            return;

        for (KernelDeploymentXmlDescriptor kdxd : kdXmlDescriptors) {
            if (kdxd.getBeanFactoriesCount() > 0) {
                final ModuleSpecification moduleSpecification = unit.getAttachment(Attachments.MODULE_SPECIFICATION);
                final ModuleLoader moduleLoader = Module.getBootModuleLoader();
                ModuleDependency dependency = ModuleDependency.Builder.of(moduleLoader, POJO_MODULE).build();
                PathFilter filter = PathFilters.isChildOf(BaseBeanFactory.class.getPackage().getName());
                dependency.addImportFilter(filter, true);
                dependency.addImportFilter(PathFilters.rejectAll(), false);
                moduleSpecification.addSystemDependency(dependency);
                return;
            }
        }
    }
}
