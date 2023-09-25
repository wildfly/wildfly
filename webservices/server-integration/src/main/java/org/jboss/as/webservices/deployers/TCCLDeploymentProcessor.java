/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.deployers;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.wsf.spi.classloading.ClassLoaderProvider;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A DUP that sets the context classloader
 *
 * @author alessio.soldano@jboss.com
 * @since 14-Jan-2011
 */
public abstract class TCCLDeploymentProcessor implements DeploymentUnitProcessor {

    @Override
    public final void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        ClassLoader origClassLoader = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(ClassLoaderProvider.getDefaultProvider().getServerIntegrationClassLoader());
            internalDeploy(phaseContext);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(origClassLoader);
        }
    }

    @Override
    public final void undeploy(final DeploymentUnit context) {
        ClassLoader origClassLoader = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(ClassLoaderProvider.getDefaultProvider().getServerIntegrationClassLoader());
            internalUndeploy(context);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(origClassLoader);
        }
    }

    public abstract void internalDeploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException;

    public abstract void internalUndeploy(final DeploymentUnit context);
}
