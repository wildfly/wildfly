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

package org.jboss.as.ee.naming;

import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.naming.service.NamingStoreService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.Values;

/**
 * Deployment processor that deploys a naming context for the current application.
 *
 * @author John E. Bailey
 */
public class ApplicationContextProcessor implements DeploymentUnitProcessor {

    /**
     * Add a ContextService for this module.
     *
     * @param phaseContext the deployment unit context
     * @throws org.jboss.as.server.deployment.DeploymentUnitProcessingException
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (deploymentUnit.getParent() != null) {
            return;
        }
        EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);

        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();

        final ServiceName applicationContextServiceName = ContextNames.contextServiceNameOfApplication(moduleDescription.getApplicationName());
        final NamingStoreService contextService = new NamingStoreService();
        serviceTarget.addService(applicationContextServiceName, contextService).install();

        final BinderService applicationNameBinder = new BinderService("AppName");
        serviceTarget.addService(applicationContextServiceName.append("AppName"), applicationNameBinder)
                .addDependency(applicationContextServiceName, ServiceBasedNamingStore.class, applicationNameBinder.getNamingStoreInjector())
                .addInjection(applicationNameBinder.getManagedObjectInjector(), new ValueManagedReferenceFactory(Values.immediateValue(moduleDescription.getApplicationName())))
                .install();

        deploymentUnit.putAttachment(Attachments.APPLICATION_CONTEXT_CONFIG, applicationContextServiceName);
    }

    public void undeploy(DeploymentUnit context) {

    }
}
