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

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.jaxrs.JaxrsMessages;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.deployment.component.WebComponentDescription;
import org.jboss.modules.Module;
import org.jboss.resteasy.util.GetRestful;

import static org.jboss.as.jaxrs.JaxrsLogger.JAXRS_LOGGER;

/**
 * Integrates JAX-RS with other component types such as managed beans and EJB's
 * <p/>
 * This is not needed if beans.xml is present, as in this case the integration is handed by the more general
 * integration with CDI.
 *
 * @author Stuart Douglas
 */
public class JaxrsComponentDeployer implements DeploymentUnitProcessor {

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

            if (component instanceof WebComponentDescription) {
                continue;
            }
            if (component instanceof SessionBeanComponentDescription) {
                Class jaxrsType = GetRestful.getSubResourceClass(componentClass);
                final String jndiName;
                if(component.getViews().size() == 1) {
                    //only 1 view, just use the simple JNDI name
                    jndiName = "java:app/" + moduleDescription.getModuleName() + "/" + componentClass.getSimpleName();
                } else {
                    final String jaxRsTypeName = jaxrsType.getName();
                    boolean found = false;
                    for(final ViewDescription view : component.getViews()) {
                        if(view.getViewClassName().equals(jaxRsTypeName)) {
                            found = true;
                            break;
                        }
                    }
                    if(!found) {
                        throw JaxrsMessages.MESSAGES.typeNameNotAnEjbView(jaxRsTypeName, component.getComponentName());
                    }
                    jndiName = "java:app/" + moduleDescription.getModuleName() + "/" + componentClass.getSimpleName() + "!" + jaxRsTypeName;
                }

                JAXRS_LOGGER.debugf("Found JAX-RS Managed Bean: %s local jndi jaxRsTypeName: %s", component.getComponentClassName(), jndiName);
                StringBuilder buf = new StringBuilder();
                buf.append(jndiName).append(";").append(component.getComponentClassName()).append(";").append("true");

                resteasy.getScannedJndiComponentResources().add(buf.toString());
                // make sure its removed from list
                resteasy.getScannedResourceClasses().remove(component.getComponentClassName());
            } else {

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

    @Override
    public void undeploy(DeploymentUnit context) {

    }

}