/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.managedbean.processors;

import static org.jboss.as.ee.subsystem.EeSubsystemRootResource.GLASSFISH_EL;
import static org.jboss.as.ee.subsystem.EeSubsystemRootResource.JSON_API;
import static org.jboss.as.ee.subsystem.EeSubsystemRootResource.WILDFLY_NAMING;

import org.jboss.as.ee.concurrent.ConcurrencyImplementation;
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
 * Deployment processor which adds the Jakarta EE APIs to EE deployments
 * <p/>
 *
 * @author John E. Bailey
 * @author Jason T. Greene
 * @author Stuart Douglas
 */
public class JavaEEDependencyProcessor implements DeploymentUnitProcessor {

    private static String JBOSS_INVOCATION_ID = "org.jboss.invocation";
    private static String JBOSS_AS_EE = "org.jboss.as.ee";

    private static final String[] JAVA_EE_API_MODULES = {
            "jakarta.annotation.api",
            "jakarta.enterprise.concurrent.api",
            "jakarta.interceptor.api",
            JSON_API,
            "jakarta.json.bind.api",
            "jakarta.resource.api",
            "javax.rmi.api",
            "jakarta.xml.bind.api",
            GLASSFISH_EL
    };


    /**
     * Add the EE APIs as a dependency to all deployments
     *
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     *
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);

        final ModuleLoader moduleLoader = Module.getBootModuleLoader();

        //add jboss-invocation classes needed by the proxies
        ModuleDependency invocation = ModuleDependency.Builder.of(moduleLoader, JBOSS_INVOCATION_ID).build();
        invocation.addImportFilter(PathFilters.is("org/jboss/invocation/proxy/classloading"), true);
        invocation.addImportFilter(PathFilters.acceptAll(), false);
        moduleSpecification.addSystemDependency(invocation);

        ModuleDependency ee = ModuleDependency.Builder.of(moduleLoader, JBOSS_AS_EE).build();
        ee.addImportFilter(PathFilters.is("org/jboss/as/ee/component/serialization"), true);
        ee.addImportFilter(PathFilters.is("org/jboss/as/ee/concurrent"), true);
        ee.addImportFilter(PathFilters.is("org/jboss/as/ee/concurrent/handle"), true);
        ee.addImportFilter(PathFilters.acceptAll(), false);
        moduleSpecification.addSystemDependency(ee);

        // add dep for naming permission
        moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, WILDFLY_NAMING).build());

        //we always add all Jakarta EE API modules, as the platform spec requires them to always be available
        //we do not just add the javaee.api module, as this breaks excludes

        for (String moduleName : JAVA_EE_API_MODULES) {
            moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, moduleName).setOptional(true).setImportServices(true).build());
        }

        // adds the Concurrency Implementation module
        moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, ConcurrencyImplementation.INSTANCE.getJBossModuleName()).setOptional(true).setImportServices(true).build());
    }
}
