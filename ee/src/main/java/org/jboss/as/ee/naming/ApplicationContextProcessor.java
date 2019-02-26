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
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.naming.NamingStore;
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

import static org.jboss.as.server.deployment.Attachments.SETUP_ACTIONS;

/**
 * Deployment processor that deploys a naming context for the current application.
 *
 * @author John E. Bailey
 * @author Eduardo Martins
 */
public class ApplicationContextProcessor implements DeploymentUnitProcessor {

    /**
     * Add a ContextService for this module.
     *
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (deploymentUnit.getParent() != null) {
            return;
        }
        EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
        final ServiceName applicationContextServiceName = ContextNames.contextServiceNameOfApplication(moduleDescription.getApplicationName());
        final NamingStoreService contextService = new NamingStoreService(true);
        serviceTarget.addService(applicationContextServiceName, contextService).install();
        final ServiceName appNameServiceName = applicationContextServiceName.append("AppName");
        final BinderService applicationNameBinder = new BinderService("AppName");
        applicationNameBinder.getManagedObjectInjector().inject(new ValueManagedReferenceFactory(Values.immediateValue(moduleDescription.getApplicationName())));
        serviceTarget.addService(appNameServiceName, applicationNameBinder)
                .addDependency(applicationContextServiceName, ServiceBasedNamingStore.class, applicationNameBinder.getNamingStoreInjector())
                .install();
        deploymentUnit.addToAttachmentList(org.jboss.as.server.deployment.Attachments.JNDI_DEPENDENCIES, appNameServiceName);
        deploymentUnit.putAttachment(Attachments.APPLICATION_CONTEXT_CONFIG, applicationContextServiceName);

        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            final InjectedEENamespaceContextSelector selector = new InjectedEENamespaceContextSelector();
            phaseContext.addDependency(applicationContextServiceName, NamingStore.class, selector.getAppContextInjector());
            phaseContext.addDependency(applicationContextServiceName, NamingStore.class, selector.getModuleContextInjector());
            phaseContext.addDependency(applicationContextServiceName, NamingStore.class, selector.getCompContextInjector());
            phaseContext.addDependency(ContextNames.JBOSS_CONTEXT_SERVICE_NAME, NamingStore.class, selector.getJbossContextInjector());
            phaseContext.addDependency(ContextNames.EXPORTED_CONTEXT_SERVICE_NAME, NamingStore.class, selector.getExportedContextInjector());
            phaseContext.addDependency(ContextNames.GLOBAL_CONTEXT_SERVICE_NAME, NamingStore.class, selector.getGlobalContextInjector());
            moduleDescription.setNamespaceContextSelector(selector);
            final JavaNamespaceSetup setupAction = new JavaNamespaceSetup(selector, deploymentUnit.getServiceName());
            deploymentUnit.addToAttachmentList(SETUP_ACTIONS, setupAction);
            deploymentUnit.putAttachment(Attachments.JAVA_NAMESPACE_SETUP_ACTION, setupAction);
        }
    }

    public void undeploy(DeploymentUnit deploymentUnit) {
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            deploymentUnit.removeAttachment(Attachments.JAVA_NAMESPACE_SETUP_ACTION);
            deploymentUnit.getAttachmentList(SETUP_ACTIONS).removeIf(setupAction -> setupAction instanceof JavaNamespaceSetup);
        }
        deploymentUnit.removeAttachment(Attachments.APPLICATION_CONTEXT_CONFIG);
    }
}
