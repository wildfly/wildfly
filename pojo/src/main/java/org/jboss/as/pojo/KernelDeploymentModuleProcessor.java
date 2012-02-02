/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
import org.jboss.modules.ModuleIdentifier;
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

    private ModuleIdentifier POJO_MODULE = ModuleIdentifier.create("org.jboss.as.pojo");

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
                ModuleDependency dependency = new ModuleDependency(moduleLoader, POJO_MODULE, false, false, false, false);
                PathFilter filter = PathFilters.isChildOf(BaseBeanFactory.class.getPackage().getName());
                dependency.addImportFilter(filter, true);
                dependency.addImportFilter(PathFilters.rejectAll(), false);
                moduleSpecification.addSystemDependency(dependency);
                return;
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}
