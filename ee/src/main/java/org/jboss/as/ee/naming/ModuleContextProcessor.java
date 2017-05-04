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

import java.util.HashSet;
import java.util.Set;

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

import static org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION;
import static org.jboss.as.ee.naming.Attachments.MODULE_CONTEXT_CONFIG;
import static org.jboss.as.server.deployment.Attachments.SETUP_ACTIONS;

/**
 * Deployment processor that deploys a naming context for the current module.
 *
 * @author John E. Bailey
 * @author Eduardo Martins
 */
public class ModuleContextProcessor implements DeploymentUnitProcessor {

    /**
     * Add a ContextService for this module.
     *
     * @param phaseContext the deployment unit context
     * @throws org.jboss.as.server.deployment.DeploymentUnitProcessingException
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            return;
        }
        EEModuleDescription moduleDescription = deploymentUnit.getAttachment(EE_MODULE_DESCRIPTION);
        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();

        final ServiceName appContextServiceName = ContextNames.contextServiceNameOfApplication(moduleDescription.getApplicationName());
        final ServiceName moduleContextServiceName = ContextNames.contextServiceNameOfModule(moduleDescription.getApplicationName(), moduleDescription.getModuleName());
        final NamingStoreService contextService = new NamingStoreService(true);
        serviceTarget.addService(moduleContextServiceName, contextService).install();

        final BinderService moduleNameBinder = new BinderService("ModuleName");
        final ServiceName moduleNameServiceName = moduleContextServiceName.append("ModuleName");
        serviceTarget.addService(moduleNameServiceName, moduleNameBinder)
                .addInjection(moduleNameBinder.getManagedObjectInjector(), new ValueManagedReferenceFactory(Values.immediateValue(moduleDescription.getModuleName())))
                .addDependency(moduleContextServiceName, ServiceBasedNamingStore.class, moduleNameBinder.getNamingStoreInjector())
                .install();
        deploymentUnit.addToAttachmentList(org.jboss.as.server.deployment.Attachments.JNDI_DEPENDENCIES, moduleNameServiceName);

        deploymentUnit.putAttachment(MODULE_CONTEXT_CONFIG, moduleContextServiceName);

        final InjectedEENamespaceContextSelector selector = new InjectedEENamespaceContextSelector();
        phaseContext.addDependency(appContextServiceName, NamingStore.class, selector.getAppContextInjector());
        phaseContext.addDependency(moduleContextServiceName, NamingStore.class, selector.getModuleContextInjector());
        phaseContext.addDependency(moduleContextServiceName, NamingStore.class, selector.getCompContextInjector());
        phaseContext.addDependency(ContextNames.JBOSS_CONTEXT_SERVICE_NAME, NamingStore.class, selector.getJbossContextInjector());
        phaseContext.addDependency(ContextNames.EXPORTED_CONTEXT_SERVICE_NAME, NamingStore.class, selector.getExportedContextInjector());
        phaseContext.addDependency(ContextNames.GLOBAL_CONTEXT_SERVICE_NAME, NamingStore.class, selector.getGlobalContextInjector());

        moduleDescription.setNamespaceContextSelector(selector);

        final Set<ServiceName> serviceNames = new HashSet<ServiceName>();
        serviceNames.add(appContextServiceName);
        serviceNames.add(moduleContextServiceName);
        serviceNames.add(ContextNames.JBOSS_CONTEXT_SERVICE_NAME);
        serviceNames.add(ContextNames.GLOBAL_CONTEXT_SERVICE_NAME);

        // add the arquillian setup action, so the module namespace is available in arquillian tests
        final JavaNamespaceSetup setupAction = new JavaNamespaceSetup(selector, deploymentUnit.getServiceName());
        deploymentUnit.addToAttachmentList(SETUP_ACTIONS, setupAction);
        deploymentUnit.addToAttachmentList(org.jboss.as.ee.component.Attachments.WEB_SETUP_ACTIONS, setupAction);
        deploymentUnit.putAttachment(Attachments.JAVA_NAMESPACE_SETUP_ACTION, setupAction);
    }

    public void undeploy(DeploymentUnit deploymentUnit) {
        deploymentUnit.removeAttachment(Attachments.JAVA_NAMESPACE_SETUP_ACTION);
        deploymentUnit.getAttachmentList(org.jboss.as.ee.component.Attachments.WEB_SETUP_ACTIONS).removeIf(setupAction -> setupAction instanceof JavaNamespaceSetup);
        deploymentUnit.getAttachmentList(SETUP_ACTIONS).removeIf(setupAction -> setupAction instanceof JavaNamespaceSetup);
        deploymentUnit.removeAttachment(MODULE_CONTEXT_CONFIG);
    }
}
