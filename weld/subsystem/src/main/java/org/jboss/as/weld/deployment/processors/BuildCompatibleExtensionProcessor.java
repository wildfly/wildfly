/*
 * JBoss, Home of Professional Open Source
 * Copyright 2022, Red Hat Inc., and individual contributors as indicated
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

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.deployment.WeldPortableExtensions;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.wildfly.security.manager.WildFlySecurityManager;

import static org.jboss.as.weld.util.Reflections.loadClass;

/**
 * This processor adds the Weld LiteExtensionTranslator if CDI Lite BuildCompatibleExtensions are
 * seen from the root deployment. This uses a static block hack to lookup the
 * org.jboss.weld.lite.extension.translator.BuildCompatibleExtensionLoader#getBuildCompatibleExtensions
 * method used to determine if the deployment has BuildCompatibleExtensions. When Weld 5
 * can be used directly in main this would be replaced with a direct call.
 */
public class BuildCompatibleExtensionProcessor implements DeploymentUnitProcessor {
    private static final Logger log = Logger.getLogger(BuildCompatibleExtensionProcessor.class.getPackage().getName());
    private static final String BUILD_COMPATIBLE_EXTENSION_LOADER = "org.jboss.weld.lite.extension.translator.BuildCompatibleExtensionLoader";
    private static final String LITE_EXTENSION_TRANSLATOR = "org.jboss.weld.lite.extension.translator.LiteExtensionTranslator";

    private static final Method getBuildCompatibleExtensions;
    static {
        Method method = null;
        try {
            // See if the BuildCompatibleExtensionLoader can be loaded (EE10 WFP)
            @SuppressWarnings("unchecked")
            Class<?> clazz = BuildCompatibleExtensionProcessor.class.getClassLoader().loadClass(BUILD_COMPATIBLE_EXTENSION_LOADER);
            method = clazz.getDeclaredMethod("getBuildCompatibleExtensions");
            log.debugf("Found getBuildCompatibleExtensions for %s", BUILD_COMPATIBLE_EXTENSION_LOADER);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            // ignore
            log.debugf(e, "Cannot find getBuildCompatibleExtensions method in %s", BUILD_COMPATIBLE_EXTENSION_LOADER);
        }
        getBuildCompatibleExtensions = method;
    }

    /**
     * Reflectively calls the BuildCompatibleExtensionLoader#getBuildCompatibleExtensions to see
     * if BuildCompatibleExtensions existing in the current deployment. This requies that the
     * thread context class loader is set to the deployment unit module ClassLoader.
     * @return true if getBuildCompatibleExtensions returns a list > 0, false otherwise
     * @throws DeploymentUnitProcessingException - on reflection invocation failure
     */
    static boolean hasBuildCompatibleExtensions() throws DeploymentUnitProcessingException {
        if (getBuildCompatibleExtensions != null) {
            try {
                List extensions = (List) getBuildCompatibleExtensions.invoke(null);
                return extensions.size() > 0;
            } catch (ReflectiveOperationException e) {
                throw new DeploymentUnitProcessingException(e);
            }
        }
        return false;
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (deploymentUnit.getParent() != null) {
            return; // only run for top-level deployments
        }

        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        final ClassLoader classLoader = module.getClassLoader();
        final ClassLoader oldCl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            // Have to set the deployment module class loader as the build compatible extension
            // loader and translator use the TCCL to search for BuildCompatibleExtensions using
            // SerivceLoader
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(module.getClassLoader());
            if(hasBuildCompatibleExtensions()) {
                // there are build compatible extensions visible, register the translator
                WeldPortableExtensions.getPortableExtensions(deploymentUnit)
                        .tryRegisterExtension(loadClass(LITE_EXTENSION_TRANSLATOR, classLoader),
                                              deploymentUnit);
                log.debugf("Registered %s for %s", LITE_EXTENSION_TRANSLATOR, deploymentUnit.getName());
            }
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(oldCl);
        }
    }
}
