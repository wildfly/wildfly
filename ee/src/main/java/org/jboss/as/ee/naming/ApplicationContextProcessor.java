/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
        applicationNameBinder.getManagedObjectInjector().inject(new ValueManagedReferenceFactory(moduleDescription.getApplicationName()));
        serviceTarget.addService(appNameServiceName, applicationNameBinder)
                .addDependency(applicationContextServiceName, ServiceBasedNamingStore.class, applicationNameBinder.getNamingStoreInjector())
                .install();
        deploymentUnit.addToAttachmentList(org.jboss.as.server.deployment.Attachments.JNDI_DEPENDENCIES, appNameServiceName);
        deploymentUnit.putAttachment(Attachments.APPLICATION_CONTEXT_CONFIG, applicationContextServiceName);

        // add a setup action for the naming context setup, this may be needed by CDI extensions that does java:app lookups
        final InjectedEENamespaceContextSelector selector = new InjectedEENamespaceContextSelector();
        phaseContext.requires(applicationContextServiceName, selector.getAppContextSupplier());
        phaseContext.requires(ContextNames.JBOSS_CONTEXT_SERVICE_NAME, selector.getJbossContextSupplier());
        phaseContext.requires(ContextNames.EXPORTED_CONTEXT_SERVICE_NAME, selector.getExportedContextSupplier());
        phaseContext.requires(ContextNames.GLOBAL_CONTEXT_SERVICE_NAME, selector.getGlobalContextSupplier());
        final JavaNamespaceSetup setupAction = new JavaNamespaceSetup(selector, deploymentUnit.getServiceName());
        deploymentUnit.putAttachment(Attachments.JAVA_NAMESPACE_SETUP_ACTION, setupAction);
    }

    public void undeploy(DeploymentUnit deploymentUnit) {
        deploymentUnit.removeAttachment(Attachments.APPLICATION_CONTEXT_CONFIG);
        deploymentUnit.removeAttachment(Attachments.JAVA_NAMESPACE_SETUP_ACTION);
    }
}
