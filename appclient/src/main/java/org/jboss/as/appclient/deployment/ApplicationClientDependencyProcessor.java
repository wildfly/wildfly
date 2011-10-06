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
package org.jboss.as.appclient.deployment;

import java.io.File;
import java.util.Collections;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.AdditionalModuleSpecification;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ResourceLoader;
import org.jboss.modules.ResourceLoaderSpec;
import org.jboss.modules.ResourceLoaders;
import org.jboss.modules.filter.PathFilters;

/**
 * DUP that builds a module out of app client additional classes
 *
 * @author Stuart Douglas
 */
public class ApplicationClientDependencyProcessor implements DeploymentUnitProcessor {

    public static final ModuleIdentifier APP_CLIENT_MODULE_ID = ModuleIdentifier.create("deployment.appclient.additionalClassPath");
    public static ModuleIdentifier CORBA_ID = ModuleIdentifier.create("org.omg.api");

    private final String additionalClassPath;

    public ApplicationClientDependencyProcessor(final String additionalClassPath) {
        this.additionalClassPath = additionalClassPath;
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();


        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader loader = deploymentUnit.getAttachment(Attachments.SERVICE_MODULE_LOADER);

        moduleSpecification.addSystemDependency(new ModuleDependency(loader, CORBA_ID, false, true, true));


        Boolean activate = deploymentUnit.getAttachment(AppClientAttachments.START_APP_CLIENT);
        if (activate == null || !activate) {
            return;
        }

        final ModuleDependency dependency = new ModuleDependency(loader, APP_CLIENT_MODULE_ID, false, true, true);
        dependency.addImportFilter(PathFilters.acceptAll(), true);
        moduleSpecification.addSystemDependency(dependency);

        final AdditionalModuleSpecification specification = new AdditionalModuleSpecification(APP_CLIENT_MODULE_ID, Collections.<ResourceRoot>emptySet());
        final String[] parts = additionalClassPath.split(File.pathSeparator);
        for(final String part : parts) {
            final ResourceLoader resource = ResourceLoaders.createFileResourceLoader(part, new File(part));
            specification.addResourceLoader(ResourceLoaderSpec.createResourceLoaderSpec(resource));

        }
        deploymentUnit.addToAttachmentList(Attachments.ADDITIONAL_MODULES, specification);
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }
}
