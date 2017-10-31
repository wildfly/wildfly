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
package org.jboss.as.weld.deployment.processors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.enterprise.inject.spi.Extension;

import org.jboss.as.ee.weld.WeldDeploymentMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.PrivateSubDeploymentMarker;
import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.as.weld.deployment.WeldPortableExtensions;
import org.jboss.modules.Module;
import org.jboss.vfs.VFSUtils;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Deployment processor that loads CDI portable extensions.
 *
 * @author Stuart Douglas
 * @author Ales Justin
 */
public class WeldPortableExtensionProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        // for war modules we require a beans.xml to load portable extensions
        if (PrivateSubDeploymentMarker.isPrivate(deploymentUnit)) {
            if (!WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
                return;
            }
        } else {
            // if any deployments have a beans.xml we need to load portable extensions
            // even if this one does not.
            if (!WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
                return;
            }
        }

        WeldPortableExtensions extensions = WeldPortableExtensions.getPortableExtensions(deploymentUnit);

        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        ClassLoader oldCl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(module.getClassLoader());
            loadAttachments(module, deploymentUnit, extensions);

        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(oldCl);
        }
    }

    private void loadAttachments(Module module, DeploymentUnit deploymentUnit, WeldPortableExtensions extensions) throws DeploymentUnitProcessingException {
        // now load extensions
        try {
            Enumeration<URL> resources = module.getClassLoader().getResources("META-INF/services/" + Extension.class.getName());
            final List<String> services = new ArrayList<>();
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                final InputStream stream = resource.openStream();
                try {
                    final BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final int commentIdx = line.indexOf('#');
                        final String className;
                        if (commentIdx == -1) {
                            className = line.trim();
                        } else {
                            className = line.substring(0, commentIdx).trim();
                        }
                        if (className.length() == 0) {
                            continue;
                        }
                        services.add(className);
                    }
                } finally {
                    VFSUtils.safeClose(stream);
                }
            }
            for (String service : services) {
                final Class<Extension> extensionClass = loadExtension(service, module.getClassLoader());
                if (extensionClass == null) {
                    continue;
                }
                extensions.tryRegisterExtension(extensionClass, deploymentUnit);
            }
        } catch (IOException e) {
            throw new DeploymentUnitProcessingException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Class<Extension> loadExtension(String serviceClassName, final ClassLoader loader) throws DeploymentUnitProcessingException {
        try {
            return (Class<Extension>) loader.loadClass(serviceClassName);
        } catch (Exception e) {
            WeldLogger.DEPLOYMENT_LOGGER.couldNotLoadPortableExceptionClass(serviceClassName, e);
        } catch (LinkageError e) {
            WeldLogger.DEPLOYMENT_LOGGER.couldNotLoadPortableExceptionClass(serviceClassName, e);
        }
        return null;
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
        if (deploymentUnit.getParent() == null) {
            deploymentUnit.removeAttachment(WeldPortableExtensions.ATTACHMENT_KEY);
        }
    }

}
