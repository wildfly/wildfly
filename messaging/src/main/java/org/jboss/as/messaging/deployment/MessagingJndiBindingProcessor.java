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

import static org.jboss.as.messaging.BinderServiceUtil.installAliasBinderService;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentNamingMode;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * Processor responsible for binding JMS related resources to JNDI.
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2013 Red Hat Inc.
 */
public class MessagingJndiBindingProcessor implements DeploymentUnitProcessor {

    public static final String DEFAULT_JMS_CONNECTION_FACTORY = "DefaultJMSConnectionFactory";

    /* Use a pooled connection factory as the default cf */
    public static final String JBOSS_DEFAULT_JMS_CONNECTION_FACTORY_LOOKUP = "java:/JmsXA";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);

        if(moduleDescription == null) {
            return;
        }

        // do not alias the default JMS connection factory if there is no entry defined by the messaging subsystem
        if (phaseContext.getServiceRegistry().getService(ContextNames.bindInfoFor(JBOSS_DEFAULT_JMS_CONNECTION_FACTORY_LOOKUP).getBinderServiceName()) == null) {
            return;
        }

        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
        if(DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            final ServiceName moduleContextServiceName = ContextNames.contextServiceNameOfModule(moduleDescription.getApplicationName(),moduleDescription.getModuleName());
            bindAliasService(deploymentUnit, serviceTarget, moduleContextServiceName, JBOSS_DEFAULT_JMS_CONNECTION_FACTORY_LOOKUP);
        }

        for(ComponentDescription component : moduleDescription.getComponentDescriptions()) {
            if(component.getNamingMode() == ComponentNamingMode.CREATE) {
                final ServiceName compContextServiceName = ContextNames.contextServiceNameOfComponent(moduleDescription.getApplicationName(),moduleDescription.getModuleName(),component.getComponentName());
                bindAliasService(deploymentUnit, serviceTarget, compContextServiceName, JBOSS_DEFAULT_JMS_CONNECTION_FACTORY_LOOKUP);
            }
        }
    }

    /**
     * Binds the java:comp/DefaultJMSConnectionFactory by aliasing the connection factory lookup
     * flagged as default by the messaging subsystem.
     *
     * @param deploymentUnit The deployment unit
     * @param serviceTarget The service target
     * @param contextServiceName The service name of the context to bind to
     * @param connectionFactoryLookup the JNDI lookup of the default connection factory
     *                                that will be aliased to.
     *
     */
    private void bindAliasService(DeploymentUnit deploymentUnit, ServiceTarget serviceTarget, ServiceName contextServiceName, String connectionFactoryLookup) {
        final ServiceName defaultJMSConnectionFactoryServiceName = contextServiceName.append(DEFAULT_JMS_CONNECTION_FACTORY);

        installAliasBinderService(serviceTarget,
                contextServiceName,
                connectionFactoryLookup,
                defaultJMSConnectionFactoryServiceName,
                DEFAULT_JMS_CONNECTION_FACTORY);

        deploymentUnit.addToAttachmentList(org.jboss.as.server.deployment.Attachments.JNDI_DEPENDENCIES, defaultJMSConnectionFactoryServiceName);
    }


    @Override
    public void undeploy(DeploymentUnit context) {
    }

}
