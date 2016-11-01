/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.jaxrs.logging.JaxrsLogger.JAXRS_LOGGER;

import java.util.Arrays;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.managedbean.component.ManagedBeanComponentDescription;
import org.jboss.as.ee.weld.WeldDeploymentMarker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.Module;
import org.jboss.resteasy.util.GetRestful;

/**
 * Integrates JAX-RS with managed beans and EJB's
 *
 * @author Stuart Douglas
 */
public class JaxrsComponentDeployer implements DeploymentUnitProcessor {

    /**
     * We use hard coded class names to avoid a direct dependency on EJB
     *
     * This allows the use of JAX-RS in cut down servers without EJB
     *
     * Kinda yuck, but there is not really any alternative if we want don't want the dependency
     */
    private static final String SESSION_BEAN_DESCRIPTION_CLASS_NAME = "org.jboss.as.ejb3.component.session.SessionBeanComponentDescription";

    private static final String STATEFUL_SESSION_BEAN_DESCRIPTION_CLASS_NAME = "org.jboss.as.ejb3.component.stateful.StatefulComponentDescription";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        if (module == null) {
            return;
        }


        final ResteasyDeploymentData resteasy = deploymentUnit.getAttachment(JaxrsAttachments.RESTEASY_DEPLOYMENT_DATA);
        if (resteasy == null) {
            return;
        }
        // right now I only support resources
        if (!resteasy.isScanResources()) return;

        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        if (moduleDescription == null) {
            return;
        }

        final ClassLoader loader = module.getClassLoader();

        for (final ComponentDescription component : moduleDescription.getComponentDescriptions()) {
            Class<?> componentClass = null;
            try {
                componentClass = loader.loadClass(component.getComponentClassName());
            } catch (ClassNotFoundException e) {
                throw new DeploymentUnitProcessingException(e);
            }
            if (!GetRestful.isRootResource(componentClass)) continue;

            if (isInstanceOf(component, SESSION_BEAN_DESCRIPTION_CLASS_NAME)) {
                if (isInstanceOf(component, STATEFUL_SESSION_BEAN_DESCRIPTION_CLASS_NAME)) {
                    //using SFSB's as JAX-RS endpoints is not recommended, but if people really want to do it they can

                    JAXRS_LOGGER.debugf("Stateful session bean %s is being used as a JAX-RS endpoint, this is not recommended", component.getComponentName());
                    if (WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
                        //if possible just let CDI handle the integration
                        continue;
                    }
                }

                Class<?>[] jaxrsType = GetRestful.getSubResourceClasses(componentClass);
                final String jndiName;
                if (component.getViews().size() == 1) {
                    //only 1 view, just use the simple JNDI name
                    jndiName = "java:app/" + moduleDescription.getModuleName() + "/" + component.getComponentName();
                } else {
                    boolean found = false;
                    String foundType = null;
                    for (final ViewDescription view : component.getViews()) {
                        for (Class<?> subResource : jaxrsType) {
                            if (view.getViewClassName().equals(subResource.getName())) {
                                foundType = subResource.getName();
                                found = true;
                                break;
                            }
                        }
                        if (found) {
                            break;
                        }
                    }
                    if (!found) {
                        throw JAXRS_LOGGER.typeNameNotAnEjbView(Arrays.asList(jaxrsType), component.getComponentName());
                    }
                    jndiName = "java:app/" + moduleDescription.getModuleName() + "/" + component.getComponentName() + "!" + foundType;
                }

                JAXRS_LOGGER.debugf("Found JAX-RS Managed Bean: %s local jndi jaxRsTypeName: %s", component.getComponentClassName(), jndiName);
                StringBuilder buf = new StringBuilder();
                buf.append(jndiName).append(";").append(component.getComponentClassName()).append(";").append("true");

                resteasy.getScannedJndiComponentResources().add(buf.toString());
                // make sure its removed from list
                resteasy.getScannedResourceClasses().remove(component.getComponentClassName());
            } else if (component instanceof ManagedBeanComponentDescription) {
                String jndiName = "java:app/" + moduleDescription.getModuleName() + "/" + component.getComponentName();

                JAXRS_LOGGER.debugf("Found JAX-RS Managed Bean: %s local jndi name: %s", component.getComponentClassName(), jndiName);
                StringBuilder buf = new StringBuilder();
                buf.append(jndiName).append(";").append(component.getComponentClassName()).append(";").append("true");

                resteasy.getScannedJndiComponentResources().add(buf.toString());
                // make sure its removed from list
                resteasy.getScannedResourceClasses().remove(component.getComponentClassName());
            }
        }
    }

    private boolean isInstanceOf(ComponentDescription component, String className) {
        Class<?> c = component.getClass();
        while (c != Object.class && c != null) {
            if(c.getName().equals(className)) {
                return true;
            }
            c = c.getSuperclass();
        }
        return false;
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }

}