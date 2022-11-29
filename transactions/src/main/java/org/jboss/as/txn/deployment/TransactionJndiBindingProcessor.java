/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.txn.deployment;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.UserTransaction;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentNamingMode;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.naming.ManagedReferenceInjector;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.txn.service.TransactionSynchronizationRegistryService;
import org.jboss.as.txn.service.UserTransactionBindingService;
import org.jboss.as.txn.service.UserTransactionAccessControlService;
import org.jboss.as.txn.service.UserTransactionService;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * Processor responsible for binding transaction related resources to JNDI.
 * </p>
 * Unlike other resource injections this binding happens for all eligible components,
 * regardless of the presence of the {@link jakarta.annotation.Resource} annotation.
 *
 * @author Stuart Douglas
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class TransactionJndiBindingProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if(DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            return;
        }
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        if(moduleDescription == null) {
            return;
        }
        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
        // bind to module
        final ServiceName moduleContextServiceName = ContextNames.contextServiceNameOfModule(moduleDescription.getApplicationName(),moduleDescription.getModuleName());
        bindServices(deploymentUnit, serviceTarget, moduleContextServiceName);
        if (!DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            // bind to each component
            for (ComponentDescription component : moduleDescription.getComponentDescriptions()) {
                if (component.getNamingMode() == ComponentNamingMode.CREATE) {
                    final ServiceName compContextServiceName = ContextNames.contextServiceNameOfComponent(moduleDescription.getApplicationName(), moduleDescription.getModuleName(), component.getComponentName());
                    bindServices(deploymentUnit, serviceTarget, compContextServiceName);
                }
            }
        }
    }

    /**
     * Binds the java:comp/UserTransaction service and the java:comp/TransactionSynchronizationRegistry
     *
     * @param deploymentUnit The deployment unit
     * @param serviceTarget The service target
     * @param contextServiceName The service name of the context to bind to
     */
    private void bindServices(DeploymentUnit deploymentUnit, ServiceTarget serviceTarget, ServiceName contextServiceName) {

        final ServiceName userTransactionServiceName = contextServiceName.append("UserTransaction");
        final UserTransactionBindingService userTransactionBindingService = new UserTransactionBindingService("UserTransaction");
        serviceTarget.addService(userTransactionServiceName, userTransactionBindingService)
            .addDependency(UserTransactionAccessControlService.SERVICE_NAME, UserTransactionAccessControlService.class,userTransactionBindingService.getUserTransactionAccessControlServiceInjector())
            .addDependency(UserTransactionService.INTERNAL_SERVICE_NAME, UserTransaction.class,
                    new ManagedReferenceInjector<UserTransaction>(userTransactionBindingService.getManagedObjectInjector()))
            .addDependency(contextServiceName, ServiceBasedNamingStore.class, userTransactionBindingService.getNamingStoreInjector())
            .install();
        final Map<ServiceName, Set<ServiceName>> jndiComponentDependencies = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.COMPONENT_JNDI_DEPENDENCIES);
        Set<ServiceName> jndiDependencies = jndiComponentDependencies.get(contextServiceName);
        if (jndiDependencies == null) {
            jndiComponentDependencies.put(contextServiceName, jndiDependencies = new HashSet<>());
        }
        jndiDependencies.add(userTransactionServiceName);

        final ServiceName transactionSynchronizationRegistryName = contextServiceName.append("TransactionSynchronizationRegistry");
        BinderService transactionSyncBinderService = new BinderService("TransactionSynchronizationRegistry");
        serviceTarget.addService(transactionSynchronizationRegistryName, transactionSyncBinderService)
            .addDependency(TransactionSynchronizationRegistryService.INTERNAL_SERVICE_NAME, TransactionSynchronizationRegistry.class,
                    new ManagedReferenceInjector<TransactionSynchronizationRegistry>(transactionSyncBinderService.getManagedObjectInjector()))
            .addDependency(contextServiceName, ServiceBasedNamingStore.class, transactionSyncBinderService.getNamingStoreInjector())
            .install();
        jndiDependencies.add(transactionSynchronizationRegistryName);
    }
}
