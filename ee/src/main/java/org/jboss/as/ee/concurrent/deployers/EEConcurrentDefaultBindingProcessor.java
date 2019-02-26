/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.ee.concurrent.deployers;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentNamingMode;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.LookupInjectionSource;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

import static org.jboss.as.ee.structure.DeploymentType.APPLICATION_CLIENT;
import static org.jboss.as.ee.structure.DeploymentType.EAR;
import static org.jboss.as.ee.structure.DeploymentType.WAR;

/**
 * Processor responsible for binding the default EE concurrency resources to the naming context of EE modules/components.
 *
 * @author Eduardo Martins
 */
public class EEConcurrentDefaultBindingProcessor implements DeploymentUnitProcessor {

    public static final String DEFAULT_CONTEXT_SERVICE_JNDI_NAME = "DefaultContextService";
    public static final String DEFAULT_MANAGED_EXECUTOR_SERVICE_JNDI_NAME = "DefaultManagedExecutorService";
    public static final String DEFAULT_MANAGED_SCHEDULED_EXECUTOR_SERVICE_JNDI_NAME = "DefaultManagedScheduledExecutorService";
    public static final String DEFAULT_MANAGED_THREAD_FACTORY_JNDI_NAME = "DefaultManagedThreadFactory";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        if(moduleDescription == null) {
            return;
        }
        final String contextService = moduleDescription.getDefaultResourceJndiNames().getContextService();
        if(contextService != null) {
            addBinding(contextService, DEFAULT_CONTEXT_SERVICE_JNDI_NAME, moduleDescription, deploymentUnit);
        }
        final String managedExecutorService = moduleDescription.getDefaultResourceJndiNames().getManagedExecutorService();
        if(managedExecutorService != null) {
            addBinding(managedExecutorService, DEFAULT_MANAGED_EXECUTOR_SERVICE_JNDI_NAME, moduleDescription, deploymentUnit);
        }
        final String managedScheduledExecutorService = moduleDescription.getDefaultResourceJndiNames().getManagedScheduledExecutorService();
        if(managedScheduledExecutorService != null) {
            addBinding(managedScheduledExecutorService, DEFAULT_MANAGED_SCHEDULED_EXECUTOR_SERVICE_JNDI_NAME, moduleDescription, deploymentUnit);
        }
        final String managedThreadFactory = moduleDescription.getDefaultResourceJndiNames().getManagedThreadFactory();
        if(managedThreadFactory != null) {
            addBinding(managedThreadFactory, DEFAULT_MANAGED_THREAD_FACTORY_JNDI_NAME, moduleDescription, deploymentUnit);
        }
    }

    private void addBinding(String source, String bindingName, EEModuleDescription moduleDescription, DeploymentUnit deploymentUnit) {
        final LookupInjectionSource injectionSource = new LookupInjectionSource(source);
        if (DeploymentTypeMarker.isType(EAR, deploymentUnit)) {
            moduleDescription.getBindingConfigurations().add(new BindingConfiguration("java:app/"+bindingName, injectionSource));
        } else {
            if (DeploymentTypeMarker.isType(WAR, deploymentUnit)) {
                moduleDescription.getBindingConfigurations().add(new BindingConfiguration("java:module/"+bindingName, injectionSource));
            } else {
                if (DeploymentTypeMarker.isType(APPLICATION_CLIENT, deploymentUnit)) {
                    moduleDescription.getBindingConfigurations().add(new BindingConfiguration("java:comp/"+bindingName, injectionSource));
                } else {
                    for(ComponentDescription componentDescription : moduleDescription.getComponentDescriptions()) {
                        if(componentDescription.getNamingMode() == ComponentNamingMode.CREATE) {
                            componentDescription.getBindingConfigurations().add(new BindingConfiguration("java:comp/"+bindingName, injectionSource));
                        }
                    }
                }
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

}
