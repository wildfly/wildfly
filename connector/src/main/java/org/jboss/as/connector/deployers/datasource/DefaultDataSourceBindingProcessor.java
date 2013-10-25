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
package org.jboss.as.connector.deployers.datasource;

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
 * Processor responsible for binding the default datasource to the naming context of EE modules/components.
 *
 * @author Eduardo Martins
 */
public class DefaultDataSourceBindingProcessor implements DeploymentUnitProcessor {

    public static final String COMP_DEFAULT_DATASOURCE_JNDI_NAME = "java:comp/DefaultDataSource";
    public static final String MODULE_DEFAULT_DATASOURCE_JNDI_NAME = "java:module/DefaultDataSource";

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
        final String defaultDataSource = moduleDescription.getDefaultResourceJndiNames().getDataSource();
        if(defaultDataSource == null) {
            return;
        }
        final LookupInjectionSource injectionSource = new LookupInjectionSource(defaultDataSource);
        if (DeploymentTypeMarker.isType(WAR, deploymentUnit)) {
            moduleDescription.getBindingConfigurations().add(new BindingConfiguration(MODULE_DEFAULT_DATASOURCE_JNDI_NAME, injectionSource));
        } else {
            if (DeploymentTypeMarker.isType(APPLICATION_CLIENT, deploymentUnit)) {
                moduleDescription.getBindingConfigurations().add(new BindingConfiguration(COMP_DEFAULT_DATASOURCE_JNDI_NAME, injectionSource));
            }
            for(ComponentDescription componentDescription : moduleDescription.getComponentDescriptions()) {
                if(componentDescription.getNamingMode() == ComponentNamingMode.CREATE) {
                    componentDescription.getBindingConfigurations().add(new BindingConfiguration(COMP_DEFAULT_DATASOURCE_JNDI_NAME, injectionSource));
                }
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

}
