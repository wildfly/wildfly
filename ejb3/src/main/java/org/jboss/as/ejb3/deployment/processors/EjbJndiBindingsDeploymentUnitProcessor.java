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

package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ee.component.AbstractComponentDescription;
import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.BindingDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.ServiceBindingSourceDescription;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentMarker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceName;

import java.util.Collection;
import java.util.Set;

/**
 * Sets up JNDI bindings for each of the views exposed by a {@link SessionBeanComponentDescription session bean}
 *
 * @author Jaikiran Pai
 */
public class EjbJndiBindingsDeploymentUnitProcessor implements DeploymentUnitProcessor {

    /**
     * Logger
     */
    private static final Logger logger = Logger.getLogger(EjbJndiBindingsDeploymentUnitProcessor.class);

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        // Only process EJB deployments
        if (!EjbDeploymentMarker.isEjbDeployment(deploymentUnit)) {
            return;
        }

        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final Collection<AbstractComponentDescription> componentDescriptions = eeModuleDescription.getComponentDescriptions();
        if (componentDescriptions == null || componentDescriptions.isEmpty()) {
            return;
        }
        for (AbstractComponentDescription componentDescription : componentDescriptions) {
            // process only EJB session beans
            if (componentDescription instanceof SessionBeanComponentDescription) {
                this.setupJNDIBindings((SessionBeanComponentDescription) componentDescription, deploymentUnit);
            }
        }
    }


    /**
     * Sets up jndi bindings for each of the views exposed by the passed <code>sessionBean</code>
     *
     * @param sessionBean    The session bean
     * @param deploymentUnit The deployment unit containing the session bean
     */
    private void setupJNDIBindings(SessionBeanComponentDescription sessionBean, DeploymentUnit deploymentUnit) {

        Set<String> views = sessionBean.getViewClassNames();
        if (views == null || views.isEmpty()) {
            logger.info("No jndi bindings will be created for EJB: " + sessionBean.getEJBName() + " since no views are exposed");
            return;
        }

        // In case of EJB bindings, appname == .ear file name (if it's an .ear deployment)
        String applicationName = this.getEarName(deploymentUnit);
        String globalJNDIBaseName = "java:global/" + (applicationName != null ? applicationName + "/" : "") + sessionBean.getModuleName() + "/" + sessionBean.getEJBName();
        String appJNDIBaseName = "java:app/" + sessionBean.getModuleName() + "/" + sessionBean.getEJBName();
        String moduleJNDIBaseName = "java:module/" + sessionBean.getEJBName();

        // the base ServiceName which will be used to create the ServiceName(s) for each of the view bindings
        ServiceName baseServiceName = deploymentUnit.getServiceName().append("component").append(sessionBean.getComponentName());

        // now create the bindings for each view under the java:global, java:app and java:module namespaces
        for (String viewClassName : views) {
            final BindingDescription globalBinding = new BindingDescription();
            globalBinding.setAbsoluteBinding(true);
            String globalJNDIName = globalJNDIBaseName + "!" + viewClassName;
            globalBinding.setBindingName(globalJNDIName);
            globalBinding.setBindingType(viewClassName);
            globalBinding.setReferenceSourceDescription(new ServiceBindingSourceDescription(baseServiceName.append("VIEW").append(viewClassName)));
            // add the binding to the component description
            sessionBean.getBindings().add(globalBinding);
            logger.debug("Added java:global jndi binding at " + globalJNDIName + " for view: " + viewClassName + " of session bean: " + sessionBean.getEJBName());

            // java:app bindings
            final BindingDescription appBinding = new BindingDescription();
            appBinding.setAbsoluteBinding(true);
            String appJNDIName = appJNDIBaseName + "!" + viewClassName;
            appBinding.setBindingName(appJNDIName);
            appBinding.setBindingType(viewClassName);
            appBinding.setReferenceSourceDescription(new ServiceBindingSourceDescription(baseServiceName.append("VIEW").append(viewClassName)));
            // add the binding to the component description
            sessionBean.getBindings().add(appBinding);
            logger.debug("Added java:app jndi binding at " + appJNDIName + " for view: " + viewClassName + " of session bean: " + sessionBean.getEJBName());

            // java:module bindings
            final BindingDescription moduleBinding = new BindingDescription();
            moduleBinding.setAbsoluteBinding(true);
            String moduleJNDIName = moduleJNDIBaseName + "!" + viewClassName;
            moduleBinding.setBindingName(moduleJNDIName);
            moduleBinding.setBindingType(viewClassName);
            moduleBinding.setReferenceSourceDescription(new ServiceBindingSourceDescription(baseServiceName.append("VIEW").append(viewClassName)));
            // add the binding to the component description
            sessionBean.getBindings().add(moduleBinding);
            logger.debug("Added java:module jndi binding at " + moduleJNDIName + " for view: " + viewClassName + " of session bean: " + sessionBean.getEJBName());

        }

        // EJB3.1 spec, section 4.4.1 Global JNDI Access states:
        // In addition to the previous requirements, if the bean exposes only one of the
        // applicable client interfaces(or alternatively has only a no-interface view), the container
        // registers an entry for that view with the following syntax :
        //
        // java:global[/<app-name>]/<module-name>/<bean-name>
        //
        // Note that this also applies to java:app and java:module bindings
        // as can be seen by the examples in 4.4.2.1
        if (views.size() == 1) {
            final BindingDescription globalBinding = new BindingDescription();
            globalBinding.setAbsoluteBinding(true);
            globalBinding.setBindingName(globalJNDIBaseName);
            String viewClassName = views.iterator().next();
            globalBinding.setBindingType(viewClassName);
            globalBinding.setReferenceSourceDescription(new ServiceBindingSourceDescription(baseServiceName.append("VIEW").append(viewClassName)));
            // add the binding to the component description
            sessionBean.getBindings().add(globalBinding);
            logger.debug("Added java:global jndi binding at " + globalJNDIBaseName + " for view: " + viewClassName + " of session bean: " + sessionBean.getEJBName());

            // java:app bindings
            final BindingDescription appBinding = new BindingDescription();
            appBinding.setAbsoluteBinding(true);
            appBinding.setBindingName(appJNDIBaseName);
            appBinding.setBindingType(viewClassName);
            appBinding.setReferenceSourceDescription(new ServiceBindingSourceDescription(baseServiceName.append("VIEW").append(viewClassName)));
            // add the binding to the component description
            sessionBean.getBindings().add(appBinding);
            logger.debug("Added java:app jndi binding at " + appJNDIBaseName + " for view: " + viewClassName + " of session bean: " + sessionBean.getEJBName());

            // java:module bindings
            final BindingDescription moduleBinding = new BindingDescription();
            moduleBinding.setAbsoluteBinding(true);
            moduleBinding.setBindingName(moduleJNDIBaseName);
            moduleBinding.setBindingType(viewClassName);
            moduleBinding.setReferenceSourceDescription(new ServiceBindingSourceDescription(baseServiceName.append("VIEW").append(viewClassName)));
            // add the binding to the component description
            sessionBean.getBindings().add(moduleBinding);
            logger.debug("Added java:module jndi binding at " + moduleJNDIBaseName + " for view: " + viewClassName + " of session bean: " + sessionBean.getEJBName());
        }

    }

    /**
     * Returns the name (stripped off the .ear suffix) of the top level .ear deployment for the passed <code>deploymentUnit</code>.
     * Returns null if the passed <code>deploymentUnit</code> doesn't belong to a .ear deployment.
     *
     * @param deploymentUnit
     */
    private String getEarName(DeploymentUnit deploymentUnit) {
        DeploymentUnit parentDU = deploymentUnit.getParent();
        if (parentDU == null) {
            String duName = deploymentUnit.getName();
            if (duName.endsWith(".ear")) {
                return duName.substring(0, duName.length() - ".ear".length());
            }
            return null;
        }
        // traverse to top level DU
        while (parentDU.getParent() != null) {
            parentDU = parentDU.getParent();
        }
        String duName = parentDU.getName();
        if (duName.endsWith(".ear")) {
            return duName.substring(0, duName.length() - ".ear".length());
        }
        return null;
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}
