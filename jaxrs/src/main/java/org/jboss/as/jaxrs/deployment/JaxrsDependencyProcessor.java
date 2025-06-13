/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.JACKSON_DATATYPE_JDK8;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.JACKSON_DATATYPE_JSR310;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.JAXB_API;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.JAXRS_API;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.JSON_API;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.MP_REST_CLIENT;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.RESTEASY_ATOM;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.RESTEASY_CDI;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.RESTEASY_CLIENT;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.RESTEASY_CLIENT_API;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.RESTEASY_CLIENT_MICROPROFILE;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.RESTEASY_CRYPTO;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.RESTEASY_JACKSON2;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.RESTEASY_JAXB;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.RESTEASY_CORE;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.RESTEASY_CORE_SPI;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.RESTEASY_JSAPI;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.RESTEASY_JSON_B_PROVIDER;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.RESTEASY_JSON_P_PROVIDER;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.RESTEASY_MULTIPART;
import static org.jboss.as.jaxrs.JaxrsSubsystemDefinition.RESTEASY_VALIDATOR;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.weld.WeldCapability;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.filter.PathFilters;
import org.jboss.vfs.VirtualFile;

/**
 * Deployment processor which adds a module dependencies for modules needed for Jakarta RESTful Web Services deployments.
 *
 * @author Stuart Douglas
 */
public class JaxrsDependencyProcessor implements DeploymentUnitProcessor {

    private static final String CLIENT_BUILDER = "META-INF/services/jakarta.ws.rs.client.ClientBuilder";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);

        final boolean deploymentBundlesClientBuilder = isClientBuilderInDeployment(deploymentUnit);

        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        addDependency(moduleSpecification, moduleLoader, JAXRS_API, false, false);
        addDependency(moduleSpecification, moduleLoader, JAXB_API, false, false);
        addDependency(moduleSpecification, moduleLoader, JSON_API, false, false);

        //we need to add these from all deployments, as they could be using the Jakarta RESTful Web Services client

        addDependency(moduleSpecification, moduleLoader, RESTEASY_ATOM, true, false);
        addDependency(moduleSpecification, moduleLoader, RESTEASY_CLIENT, true, deploymentBundlesClientBuilder);
        addDependency(moduleSpecification, moduleLoader, RESTEASY_CLIENT_API, true, deploymentBundlesClientBuilder);
        addDependency(moduleSpecification, moduleLoader, RESTEASY_CORE, true, deploymentBundlesClientBuilder);
        addDependency(moduleSpecification, moduleLoader, RESTEASY_CORE_SPI, true, deploymentBundlesClientBuilder);
        addDependency(moduleSpecification, moduleLoader, RESTEASY_CLIENT_MICROPROFILE, true, false);
        addDependency(moduleSpecification, moduleLoader, RESTEASY_JAXB, true, false);
        addDependency(moduleSpecification, moduleLoader, RESTEASY_JACKSON2, true, false);
        addDependency(moduleSpecification, moduleLoader, RESTEASY_JSON_P_PROVIDER, true, false);
        addDependency(moduleSpecification, moduleLoader, RESTEASY_JSON_B_PROVIDER, true, false);
        addDependency(moduleSpecification, moduleLoader, RESTEASY_JSAPI, true, false);
        addDependency(moduleSpecification, moduleLoader, RESTEASY_MULTIPART, true, false);
        addDependency(moduleSpecification, moduleLoader, RESTEASY_CRYPTO, true, false);
        addDependency(moduleSpecification, moduleLoader, JACKSON_DATATYPE_JDK8, true, false);
        addDependency(moduleSpecification, moduleLoader, JACKSON_DATATYPE_JSR310, true, false);

        // Add modules which were previously added with export="true" in the module itself. It's better to explicitly
        // add them here so they can be excluded vs exporting them in the module.

        // These were previously exported on the org.jboss.resteasy.resteasy-jackson2-provider.
        addDependency(moduleSpecification, moduleLoader, "com.fasterxml.jackson.core.jackson-annotations", true, false);
        addDependency(moduleSpecification, moduleLoader, "com.fasterxml.jackson.core.jackson-core", true, false);
        addDependency(moduleSpecification, moduleLoader, "com.fasterxml.jackson.core.jackson-databind", true, false);
        addDependency(moduleSpecification, moduleLoader, "com.fasterxml.jackson.jakarta.jackson-jakarta-json-provider", true, false);

        // These were perviously exported on the org.jboss.resteasy.resteasy-rxjava2 module.
        addDependency(moduleSpecification, moduleLoader, "org.reactivestreams", true, false);
        addDependency(moduleSpecification, moduleLoader, "io.reactivex.rxjava2.rxjava", true, false);

        // End add exported modules

        final CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
        if (support.hasCapability(WELD_CAPABILITY_NAME)) {
            final WeldCapability api = support.getOptionalCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class).orElse(null);
            if (api != null && api.isPartOfWeldDeployment(deploymentUnit)) {
                addDependency(moduleSpecification, moduleLoader, RESTEASY_CDI, true, false);
            }
        }
        if (support.hasCapability("org.wildfly.microprofile.config")) {
            addDependency(moduleSpecification, moduleLoader, MP_REST_CLIENT, true, false);
            addDependency(moduleSpecification, moduleLoader, "org.jboss.resteasy.microprofile.config", true, false);
        }
        // If bean-validation is available, add the support for the resteasy-validator
        if (support.hasCapability("org.wildfly.bean-validation")) {
            final ModuleDependency dep = ModuleDependency.Builder.of(moduleLoader, RESTEASY_VALIDATOR).setOptional(true).setExport(true).setImportServices(true).build();
            moduleSpecification.addSystemDependency(dep);
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
                               String moduleIdentifier, boolean optional, boolean deploymentBundlesClientBuilder) {
        ModuleDependency dependency = ModuleDependency.Builder.of(moduleLoader, moduleIdentifier).setOptional(optional).setImportServices(true).build();
        if(deploymentBundlesClientBuilder) {
            dependency.addImportFilter(PathFilters.is(CLIENT_BUILDER), false);
        }
        moduleSpecification.addSystemDependency(dependency);
    }
}
