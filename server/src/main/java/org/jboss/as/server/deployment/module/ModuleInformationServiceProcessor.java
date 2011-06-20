/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.server.deployment.module;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;

import java.util.List;

/**
 * DUP that registers a module information service for dependant scoped modules. This service allows other services
 * to examine and automatically add a modules services. This is used for resource adaptors, where all transitive deps
 * must be added automatically.
 * <p/>
 * This service also adds a next phase dep on all dependencies module information services.
 *
 * @author Stuart Douglas
 */
public class ModuleInformationServiceProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (deploymentUnit.hasAttachment(Attachments.OSGI_MANIFEST)) {
            return;
        }

        final ModuleSpecification moduleSpec = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleIdentifier moduleIdentifier = deploymentUnit.getAttachment(Attachments.MODULE_IDENTIFIER);
        final List<AdditionalModuleSpecification> additionalModules = deploymentUnit.getAttachmentList(Attachments.ADDITIONAL_MODULES);

        if (moduleIdentifier == null || moduleSpec == null) {
            return;
        }

        phaseContext.getServiceTarget().addService(ServiceModuleLoader.moduleInformationServiceName(moduleIdentifier),
                new ValueService<Object>(new ImmediateValue<Object>(moduleSpec)))
                .install();

        for (ModuleDependency dependency : moduleSpec.getAllDependencies()) {
            if (dependency.getIdentifier().getName().startsWith(ServiceModuleLoader.MODULE_PREFIX)) {
                phaseContext.addDependency(ServiceModuleLoader.moduleInformationServiceName(dependency.getIdentifier()), Attachments.MODULE_DEPENDENCY_INFORMATION);
            }
        }

        for (AdditionalModuleSpecification module : additionalModules) {
            phaseContext.getServiceTarget().addService(ServiceModuleLoader.moduleInformationServiceName(module.getModuleIdentifier()),
                    new ValueService<Object>(new ImmediateValue<Object>(module)))
                    .install();
        }

    }

    @Override
    public void undeploy(final DeploymentUnit context) {
    }
}
