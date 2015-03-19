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
import org.jboss.as.ee.weld.WeldDeploymentMarker;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

/**
 * Deployment processor which adds a module dependencies for modules needed for JAX-RS deployments.
 *
 * @author Stuart Douglas
 */
public class JaxrsDependencyProcessor implements DeploymentUnitProcessor {

    public static final ModuleIdentifier RESTEASY_ATOM = ModuleIdentifier.create("org.jboss.resteasy.resteasy-atom-provider");
    public static final ModuleIdentifier RESTEASY_CDI = ModuleIdentifier.create("org.jboss.resteasy.resteasy-cdi");
    public static final ModuleIdentifier RESTEASY_CRYPTO = ModuleIdentifier.create("org.jboss.resteasy.resteasy-crypto");
    public static final ModuleIdentifier RESTEASY_VALIDATOR_11 = ModuleIdentifier.create("org.jboss.resteasy.resteasy-validator-provider-11");
    public static final ModuleIdentifier RESTEASY_JAXRS = ModuleIdentifier.create("org.jboss.resteasy.resteasy-jaxrs");
    public static final ModuleIdentifier RESTEASY_JAXB = ModuleIdentifier.create("org.jboss.resteasy.resteasy-jaxb-provider");
    public static final ModuleIdentifier RESTEASY_JACKSON2 = ModuleIdentifier.create("org.jboss.resteasy.resteasy-jackson2-provider");
    public static final ModuleIdentifier RESTEASY_JSON_P_PROVIDER = ModuleIdentifier.create("org.jboss.resteasy.resteasy-json-p-provider");
    //public static final ModuleIdentifier RESTEASY_JETTISON = ModuleIdentifier.create("org.jboss.resteasy.resteasy-jettison-provider");
    public static final ModuleIdentifier RESTEASY_JSAPI = ModuleIdentifier.create("org.jboss.resteasy.resteasy-jsapi");
    public static final ModuleIdentifier RESTEASY_MULTIPART = ModuleIdentifier.create("org.jboss.resteasy.resteasy-multipart-provider");
    public static final ModuleIdentifier RESTEASY_YAML = ModuleIdentifier.create("org.jboss.resteasy.resteasy-yaml-provider");
    public static final ModuleIdentifier JAXB_API = ModuleIdentifier.create("javax.xml.bind.api");
    public static final ModuleIdentifier JSON_API = ModuleIdentifier.create("javax.json.api");
    public static final ModuleIdentifier JAXRS_API = ModuleIdentifier.create("javax.ws.rs.api");

    /**
     * We include this so that jackson annotations will be available, otherwise they will be ignored which leads
     * to confusing behaviour.
     *
     */
    public static final ModuleIdentifier JACKSON_CORE_ASL = ModuleIdentifier.create("org.codehaus.jackson.jackson-core-asl");

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);

        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        addDependency(moduleSpecification, moduleLoader, JAXRS_API, false);
        addDependency(moduleSpecification, moduleLoader, JAXB_API, false);
        addDependency(moduleSpecification, moduleLoader, JSON_API, false);

        //we need to add these from all deployments, as they could be using the JAX-RS client

        addDependency(moduleSpecification, moduleLoader, RESTEASY_ATOM, true);
        addDependency(moduleSpecification, moduleLoader, RESTEASY_VALIDATOR_11, true);
        addDependency(moduleSpecification, moduleLoader, RESTEASY_JAXRS, true);
        addDependency(moduleSpecification, moduleLoader, RESTEASY_JAXB, true);
        addDependency(moduleSpecification, moduleLoader, RESTEASY_JACKSON2, true);
        addDependency(moduleSpecification, moduleLoader, RESTEASY_JSON_P_PROVIDER, true);
        //addDependency(moduleSpecification, moduleLoader, RESTEASY_JETTISON);
        addDependency(moduleSpecification, moduleLoader, RESTEASY_JSAPI, true);
        addDependency(moduleSpecification, moduleLoader, RESTEASY_MULTIPART, true);
        addDependency(moduleSpecification, moduleLoader, RESTEASY_YAML, true);
        addDependency(moduleSpecification, moduleLoader, JACKSON_CORE_ASL, true);
        addDependency(moduleSpecification, moduleLoader, RESTEASY_CRYPTO, true);

        if (WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
            addDependency(moduleSpecification, moduleLoader, RESTEASY_CDI, true);
        }

    }

    private void addDependency(ModuleSpecification moduleSpecification, ModuleLoader moduleLoader,
                               ModuleIdentifier moduleIdentifier, boolean optional) {
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, moduleIdentifier, optional, false, true, false));
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }
}
