/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Inc., and individual contributors as indicated
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
