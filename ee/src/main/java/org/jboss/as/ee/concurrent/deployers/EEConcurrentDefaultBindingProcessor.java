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

import static org.jboss.as.ee.structure.DeploymentType.EAR;
import static org.jboss.as.ee.structure.DeploymentType.WAR;

/**
 * Processor responsible for binding the default EE concurrency resources to the naming context of EE modules/components.
 *
 * @author Eduardo Martins
 */
public class EEConcurrentDefaultBindingProcessor implements DeploymentUnitProcessor {

    public static final String COMP_DEFAULT_CONTEXT_SERVICE_JNDI_NAME = "java:comp/DefaultContextService";
    public static final String MODULE_DEFAULT_CONTEXT_SERVICE_JNDI_NAME = "java:module/DefaultContextService";
    public static final String COMP_DEFAULT_MANAGED_EXECUTOR_SERVICE_JNDI_NAME = "java:comp/DefaultManagedExecutorService";
    public static final String MODULE_DEFAULT_MANAGED_EXECUTOR_SERVICE_JNDI_NAME = "java:module/DefaultManagedExecutorService";
    public static final String COMP_DEFAULT_MANAGED_SCHEDULED_EXECUTOR_SERVICE_JNDI_NAME = "java:comp/DefaultManagedScheduledExecutorService";
    public static final String MODULE_DEFAULT_MANAGED_SCHEDULED_EXECUTOR_SERVICE_JNDI_NAME = "java:module/DefaultManagedScheduledExecutorService";
    public static final String COMP_DEFAULT_MANAGED_THREAD_FACTORY_JNDI_NAME = "java:comp/DefaultManagedThreadFactory";
    public static final String MODULE_DEFAULT_MANAGED_THREAD_FACTORY_JNDI_NAME = "java:module/DefaultManagedThreadFactory";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (DeploymentTypeMarker.isType(EAR, deploymentUnit)) {
            return;
        }
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        if(moduleDescription == null) {
            return;
        }
        final String contextService = moduleDescription.getDefaultResourceJndiNames().getContextService();
        final String managedExecutorService = moduleDescription.getDefaultResourceJndiNames().getManagedExecutorService();
        final String managedScheduledExecutorService = moduleDescription.getDefaultResourceJndiNames().getManagedScheduledExecutorService();
        final String managedThreadFactory = moduleDescription.getDefaultResourceJndiNames().getManagedThreadFactory();
        if (DeploymentTypeMarker.isType(WAR, deploymentUnit)) {
            if(contextService != null) {
                moduleDescription.getBindingConfigurations().add(new BindingConfiguration(MODULE_DEFAULT_CONTEXT_SERVICE_JNDI_NAME, new LookupInjectionSource(contextService)));
            }
            if(managedExecutorService != null) {
                moduleDescription.getBindingConfigurations().add( new BindingConfiguration(MODULE_DEFAULT_MANAGED_EXECUTOR_SERVICE_JNDI_NAME, new LookupInjectionSource(managedExecutorService)));
            }
            if(managedScheduledExecutorService != null) {
                moduleDescription.getBindingConfigurations().add(new BindingConfiguration(MODULE_DEFAULT_MANAGED_SCHEDULED_EXECUTOR_SERVICE_JNDI_NAME, new LookupInjectionSource(managedScheduledExecutorService)));
            }
            if(managedThreadFactory != null) {
                moduleDescription.getBindingConfigurations().add(new BindingConfiguration(MODULE_DEFAULT_MANAGED_THREAD_FACTORY_JNDI_NAME,new LookupInjectionSource(managedThreadFactory)));
            }
        } else {
            for(ComponentDescription componentDescription : moduleDescription.getComponentDescriptions()) {
                if(componentDescription.getNamingMode() == ComponentNamingMode.CREATE) {
                    if(contextService != null) {
                        componentDescription.getBindingConfigurations().add(new BindingConfiguration(COMP_DEFAULT_CONTEXT_SERVICE_JNDI_NAME, new LookupInjectionSource(contextService)));
                    }
                    if(managedExecutorService != null) {
                        componentDescription.getBindingConfigurations().add( new BindingConfiguration(COMP_DEFAULT_MANAGED_EXECUTOR_SERVICE_JNDI_NAME, new LookupInjectionSource(managedExecutorService)));
                    }
                    if(managedScheduledExecutorService != null) {
                        componentDescription.getBindingConfigurations().add(new BindingConfiguration(COMP_DEFAULT_MANAGED_SCHEDULED_EXECUTOR_SERVICE_JNDI_NAME, new LookupInjectionSource(managedScheduledExecutorService)));
                    }
                    if(managedThreadFactory != null) {
                        componentDescription.getBindingConfigurations().add(new BindingConfiguration(COMP_DEFAULT_MANAGED_THREAD_FACTORY_JNDI_NAME,new LookupInjectionSource(managedThreadFactory)));
                    }
                }
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

}
