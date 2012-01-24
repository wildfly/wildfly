/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jaxrs.deployment;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.weld.WeldDeploymentMarker;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

/**
 * Deployment processor which adds a module dependencies for modules needed for JAX-RS deployments.
 *
 * @author Stuart Douglas
 */
public class JaxrsDependencyProcessor implements DeploymentUnitProcessor {

    public static ModuleIdentifier RESTEASY_ATOM = ModuleIdentifier.create("org.jboss.resteasy.resteasy-atom-provider");
    public static ModuleIdentifier RESTEASY_CDI = ModuleIdentifier.create("org.jboss.resteasy.resteasy-cdi");
    public static ModuleIdentifier RESTEASY_JAXRS = ModuleIdentifier.create("org.jboss.resteasy.resteasy-jaxrs");
    public static ModuleIdentifier RESTEASY_JAXB = ModuleIdentifier.create("org.jboss.resteasy.resteasy-jaxb-provider");
    public static ModuleIdentifier RESTEASY_JACKSON = ModuleIdentifier.create("org.jboss.resteasy.resteasy-jackson-provider");
    public static ModuleIdentifier RESTEASY_JSAPI = ModuleIdentifier.create("org.jboss.resteasy.resteasy-jsapi");
    public static ModuleIdentifier RESTEASY_MULTIPART = ModuleIdentifier.create("org.jboss.resteasy.resteasy-multipart-provider");
    public static ModuleIdentifier RESTEASY_YAML = ModuleIdentifier.create("org.jboss.resteasy.resteasy-yaml-provider");
    public static ModuleIdentifier JAXB_API = ModuleIdentifier.create("javax.xml.bind.api");

    /**
     * We include this so that jackson annotations will be available, otherwise they will be ignored which leads
     * to confusing behaviour.
     *
     */
    public static ModuleIdentifier JACKSON_CORE_ASL = ModuleIdentifier.create("org.codehaus.jackson.jackson-core-asl");

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);

        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        addDepdenency(moduleSpecification, moduleLoader, JAXB_API);

        if (!JaxrsDeploymentMarker.isJaxrsDeployment(deploymentUnit)) {
            return;
        }
        addDepdenency(moduleSpecification, moduleLoader, RESTEASY_ATOM);
        addDepdenency(moduleSpecification, moduleLoader, RESTEASY_JAXRS);
        addDepdenency(moduleSpecification, moduleLoader, RESTEASY_JAXB);
        addDepdenency(moduleSpecification, moduleLoader, RESTEASY_JACKSON);
        addDepdenency(moduleSpecification, moduleLoader, RESTEASY_JSAPI);
        addDepdenency(moduleSpecification, moduleLoader, RESTEASY_MULTIPART);
        addDepdenency(moduleSpecification, moduleLoader, RESTEASY_YAML);
        addDepdenency(moduleSpecification, moduleLoader, JACKSON_CORE_ASL);

        if (WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
            addDepdenency(moduleSpecification, moduleLoader, RESTEASY_CDI);
        }

    }

    private void addDepdenency(ModuleSpecification moduleSpecification, ModuleLoader moduleLoader,
                               ModuleIdentifier moduleIdentifier) {
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, moduleIdentifier, false, false, true));
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }
}
