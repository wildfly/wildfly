/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.deployers.ds.processors;

import java.util.List;
import java.util.Locale;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.jboss.vfs.VirtualFile;

/**
 * Configures dependencies for JDBC driver deployments.
 */
public final class JdbcDriverDeploymentProcessor implements DeploymentUnitProcessor {

    private static final String JAR_SUFFIX = ".jar";
    private static final String JDBC_DRIVER_CONFIG_FILE = "META-INF/services/java.sql.Driver";
    private static final String JDK_SECURITY_JGSS_ID = "jdk.security.jgss";
    private static final String JDK_NET = "jdk.net";

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final String deploymentName = deploymentUnit.getName().toLowerCase(Locale.ENGLISH);
        if (!deploymentName.endsWith(JAR_SUFFIX)) {
            return;
        }

        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);

        boolean javaSqlDriverDetected = false;
        final List<ResourceRoot> resourceRoots = DeploymentUtils.allResourceRoots(deploymentUnit);
        for (ResourceRoot resourceRoot : resourceRoots) {
            final VirtualFile deploymentRoot = resourceRoot.getRoot();
            if (deploymentRoot.getChild(JDBC_DRIVER_CONFIG_FILE).exists())  {
                javaSqlDriverDetected = true;
                break;
            }
        }

        if (javaSqlDriverDetected) {
            moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, JDK_SECURITY_JGSS_ID).build());
            moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, JDK_NET).build());
        }
    }

}
