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

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ee.component.ViewBindingInjectionSource;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ejb3.component.EJBViewDescription;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentMarker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.logging.Logger;

import java.util.Collection;

/**
 * Sets up JNDI bindings for each of the views exposed by a {@link SessionBeanComponentDescription session bean}
 *
 * @author Jaikiran Pai
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
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
        final Collection<ComponentDescription> componentDescriptions = eeModuleDescription.getComponentDescriptions();
        if (componentDescriptions == null || componentDescriptions.isEmpty()) {
            return;
        }
        for (ComponentDescription componentDescription : componentDescriptions) {
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
    private void setupJNDIBindings(SessionBeanComponentDescription sessionBean, DeploymentUnit deploymentUnit) throws DeploymentUnitProcessingException {
        final Collection<ViewDescription> views = sessionBean.getViews();
        if (views == null || views.isEmpty()) {
            logger.info("No jndi bindings will be created for EJB: " + sessionBean.getEJBName() + " since no views are exposed");
            return;
        }

        // In case of EJB bindings, appname == .ear file name (if it's an .ear deployment)
        final String applicationName = this.getEarName(deploymentUnit);
        final String globalJNDIBaseName = "java:global/" + (applicationName != null ? applicationName + "/" : "") + sessionBean.getModuleName() + "/" + sessionBean.getEJBName();
        final String appJNDIBaseName = "java:app/" + sessionBean.getModuleName() + "/" + sessionBean.getEJBName();
        final String moduleJNDIBaseName = "java:module/" + sessionBean.getEJBName();

        // the base ServiceName which will be used to create the ServiceName(s) for each of the view bindings
        final StringBuilder jndiBindingsLogMessage = new StringBuilder();
        jndiBindingsLogMessage.append("JNDI bindings for session bean named " + sessionBean.getEJBName() + " in deployment unit " + deploymentUnit + " are as follows:\n\n");

        // now create the bindings for each view under the java:global, java:app and java:module namespaces
        EJBViewDescription ejbViewDescription = null;
        for (ViewDescription viewDescription : views) {
            ejbViewDescription = (EJBViewDescription) viewDescription;
            if (!ejbViewDescription.hasJNDIBindings()) continue;

            final String viewClassName = ejbViewDescription.getViewClassName();

            // java:global bindings
            final String globalJNDIName = globalJNDIBaseName + "!" + viewClassName;
            registerBinding(viewDescription, globalJNDIName);
            logBinding(jndiBindingsLogMessage, globalJNDIName);

            // java:app bindings
            final String appJNDIName = appJNDIBaseName + "!" + viewClassName;
            registerBinding(viewDescription, appJNDIName);
            logBinding(jndiBindingsLogMessage, appJNDIName);

            // java:module bindings
            final String moduleJNDIName = moduleJNDIBaseName + "!" + viewClassName;
            registerBinding(viewDescription, moduleJNDIName);
            logBinding(jndiBindingsLogMessage, moduleJNDIName);
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
            final EJBViewDescription viewDescription = (EJBViewDescription) views.iterator().next();
            if (ejbViewDescription.hasJNDIBindings()) {

                // java:global binding
                registerBinding(viewDescription, globalJNDIBaseName);
                logBinding(jndiBindingsLogMessage, globalJNDIBaseName);

                // java:app binding
                registerBinding(viewDescription, appJNDIBaseName);
                logBinding(jndiBindingsLogMessage, appJNDIBaseName);

                // java:module binding
                registerBinding(viewDescription, moduleJNDIBaseName);
                logBinding(jndiBindingsLogMessage, moduleJNDIBaseName);
            }
        }

        // log the jndi bindings
        logger.info(jndiBindingsLogMessage);
    }

    private void registerBinding(final ViewDescription viewDescription, final String jndiName) {
        final InjectionSource moduleBindingSource = new ViewBindingInjectionSource(viewDescription.getServiceName());
        final BindingConfiguration moduleBinding = new BindingConfiguration(jndiName, moduleBindingSource);
        addBindingConfiguration(viewDescription, moduleBinding);
    }

    private void logBinding(final StringBuilder jndiBindingsLogMessage, final String jndiName) {
        jndiBindingsLogMessage.append("\t");
        jndiBindingsLogMessage.append(jndiName);
        jndiBindingsLogMessage.append("\n");
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

    /**
     * Add the passed {@link BindingConfiguration} to the {@link ViewDescription viewDescription}
     *
     * @param viewDescription      The view description
     * @param bindingConfiguration The binding configuration
     */
    private void addBindingConfiguration(final ViewDescription viewDescription, final BindingConfiguration bindingConfiguration) {
        viewDescription.getConfigurators().add(new ViewConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                configuration.getBindingConfigurations().add(bindingConfiguration);
            }
        });
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}
