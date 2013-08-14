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
package org.jboss.as.messaging.deployment;

import static org.jboss.as.ee.structure.DeploymentType.EAR;
import static org.jboss.as.naming.deployment.ContextNames.BindInfo;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentNamingMode;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.ServiceInjectionSource;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceName;

/**
 * Processor responsible for binding JMS related resources to JNDI.
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2013 Red Hat Inc.
 */
public class MessagingJndiBindingProcessor implements DeploymentUnitProcessor {

    public static final String DEFAULT_JMS_CONNECTION_FACTORY = "java:comp/DefaultJMSConnectionFactory";

    /**
     * The DefaultJMSConnectionFactory lookup can be available in a EJB container, a Web container or an appclient container.
     *
     * Using the java:/JmsXA to find the default JMS connection factory would be confusing as this binding is reserved for pooled connection
     * factory that is not available inside an appclient container.
     *
     * This addition lookup name can be used either in a pooled-cf or a appclient's regular cf.
     */
    public static final String JBOSS_DEFAULT_JMS_CONNECTION_FACTORY_LOOKUP = "java:jboss/DefaultJMSConnectionFactory";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);

        if(moduleDescription == null) {
            return;
        }

        final BindInfo defaultConnectionFactoryBindInfo = ContextNames.bindInfoFor(JBOSS_DEFAULT_JMS_CONNECTION_FACTORY_LOOKUP);
        final ServiceName serviceName = defaultConnectionFactoryBindInfo.getBinderServiceName();

        // do not alias the default JMS connection factory if there is no entry defined by the messaging subsystem
        if (phaseContext.getServiceRegistry().getService(serviceName) == null) {
            return;
        }

        if (!DeploymentTypeMarker.isType(EAR, deploymentUnit)) {
            moduleDescription.getBindingConfigurations().add(new BindingConfiguration(DEFAULT_JMS_CONNECTION_FACTORY, new ServiceInjectionSource(serviceName)));
        }

        for(ComponentDescription component : moduleDescription.getComponentDescriptions()) {
            if(component.getNamingMode() == ComponentNamingMode.CREATE) {
                component.getBindingConfigurations().add(new BindingConfiguration(DEFAULT_JMS_CONNECTION_FACTORY, new ServiceInjectionSource(serviceName)));
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

}
