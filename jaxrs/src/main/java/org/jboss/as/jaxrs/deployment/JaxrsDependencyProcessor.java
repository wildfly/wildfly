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

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;

import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.JACKSON_CORE_ASL;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.JACKSON_DATATYPE_JDK8;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.JACKSON_DATATYPE_JSR310;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.JAXB_API;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.JAXRS_API;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.JSON_API;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.MP_REST_CLIENT;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.RESTEASY_ATOM;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.RESTEASY_CDI;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.RESTEASY_CRYPTO;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.RESTEASY_JACKSON2;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.RESTEASY_JAXB;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.RESTEASY_JAXRS;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.RESTEASY_JSAPI;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.RESTEASY_JSON_B_PROVIDER;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.RESTEASY_JSON_P_PROVIDER;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.RESTEASY_MULTIPART;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.RESTEASY_VALIDATOR;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.RESTEASY_VALIDATOR_11;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.RESTEASY_YAML;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.weld.WeldCapability;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.filter.PathFilters;
import org.jboss.vfs.VirtualFile;

/**
 * Deployment processor which adds a module dependencies for modules needed for JAX-RS deployments.
 *
 * @author Stuart Douglas
 */
public class JaxrsDependencyProcessor implements DeploymentUnitProcessor {

    private static final String CLIENT_BUILDER = "META-INF/services/javax.ws.rs.client.ClientBuilder";
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);

        boolean deploymentBundlesClientBuilder = isClientBuilderInDeployment(deploymentUnit);

        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        addDependency(moduleSpecification, moduleLoader, JAXRS_API, false, false);
        addDependency(moduleSpecification, moduleLoader, JAXB_API, false, false);
        addDependency(moduleSpecification, moduleLoader, JSON_API, false, false);

        //we need to add these from all deployments, as they could be using the JAX-RS client

        addDependency(moduleSpecification, moduleLoader, RESTEASY_ATOM, true, false);
        addDependency(moduleSpecification, moduleLoader, RESTEASY_VALIDATOR_11, true, false);
        addDependency(moduleSpecification, moduleLoader, RESTEASY_VALIDATOR, true, false);
        addDependency(moduleSpecification, moduleLoader, RESTEASY_JAXRS, true, deploymentBundlesClientBuilder);
        addDependency(moduleSpecification, moduleLoader, RESTEASY_JAXB, true, false);
        addDependency(moduleSpecification, moduleLoader, RESTEASY_JACKSON2, true, false);
        addDependency(moduleSpecification, moduleLoader, RESTEASY_JSON_P_PROVIDER, true, false);
        addDependency(moduleSpecification, moduleLoader, RESTEASY_JSON_B_PROVIDER, true, false);
        //addDependency(moduleSpecification, moduleLoader, RESTEASY_JETTISON);
        addDependency(moduleSpecification, moduleLoader, RESTEASY_JSAPI, true, false);
        addDependency(moduleSpecification, moduleLoader, RESTEASY_MULTIPART, true, false);
        addDependency(moduleSpecification, moduleLoader, RESTEASY_YAML, true, false);
        addDependency(moduleSpecification, moduleLoader, JACKSON_CORE_ASL, true, false);
        addDependency(moduleSpecification, moduleLoader, RESTEASY_CRYPTO, true, false);
        addDependency(moduleSpecification, moduleLoader, JACKSON_DATATYPE_JDK8, true, false);
        addDependency(moduleSpecification, moduleLoader, JACKSON_DATATYPE_JSR310, true, false);
        addDependency(moduleSpecification, moduleLoader, MP_REST_CLIENT, true, false);

        final CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
        if (support.hasCapability(WELD_CAPABILITY_NAME)) {
            final WeldCapability api = support.getOptionalCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class).get();
            if (api.isPartOfWeldDeployment(deploymentUnit)) {
                addDependency(moduleSpecification, moduleLoader, RESTEASY_CDI, true, false);
            }
        }
    }

    private boolean isClientBuilderInDeployment(DeploymentUnit deploymentUnit) {
        ResourceRoot root = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        List<ResourceRoot> roots = new ArrayList<>(deploymentUnit.getAttachmentList(Attachments.RESOURCE_ROOTS));
        roots.add(root);
        for(ResourceRoot r : roots) {
            VirtualFile file  = r.getRoot().getChild(CLIENT_BUILDER);
            if(file.exists()) {
                return true;
            }
        }

        return false;
    }

    private void addDependency(ModuleSpecification moduleSpecification, ModuleLoader moduleLoader,
                               ModuleIdentifier moduleIdentifier, boolean optional, boolean deploymentBundelesClientBuilder) {
        ModuleDependency dependency = new ModuleDependency(moduleLoader, moduleIdentifier, optional, false, true, false);
        if(deploymentBundelesClientBuilder) {
            dependency.addImportFilter(PathFilters.is(CLIENT_BUILDER), false);
        }
        moduleSpecification.addSystemDependency(dependency);
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }
}
