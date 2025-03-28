/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent.deployers;

import org.glassfish.concurro.cdi.ConcurrencyManagedCDIBeans;
import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.DeploymentDescriptorEnvironment;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.ResourceInjectionTarget;
import org.jboss.as.ee.component.deployers.AbstractDeploymentDescriptorBindingsProcessor;
import org.jboss.as.ee.concurrent.ConcurroConcurrencyAttachments;
import org.jboss.as.ee.resource.definition.ResourceDefinitionDescriptorProcessor;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.metadata.javaee.spec.ContextServiceMetaData;
import org.jboss.metadata.javaee.spec.ContextServicesMetaData;
import org.jboss.metadata.javaee.spec.Environment;
import org.jboss.metadata.javaee.spec.ManagedExecutorMetaData;
import org.jboss.metadata.javaee.spec.ManagedExecutorsMetaData;
import org.jboss.metadata.javaee.spec.ManagedScheduledExecutorMetaData;
import org.jboss.metadata.javaee.spec.ManagedScheduledExecutorsMetaData;
import org.jboss.metadata.javaee.spec.ManagedThreadFactoriesMetaData;
import org.jboss.metadata.javaee.spec.ManagedThreadFactoryMetaData;
import org.jboss.metadata.javaee.spec.RemoteEnvironment;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The {@link ResourceDefinitionDescriptorProcessor} that provides qualifiers to the {@link org.glassfish.concurro.cdi.ConcurrencyManagedCDIBeans} instance.
 * @author emmartins
 */
public class ConcurrencyManagedCDIBeansDescriptorProcessor extends AbstractDeploymentDescriptorBindingsProcessor {

    @Override
    protected List<BindingConfiguration> processDescriptorEntries(DeploymentUnit deploymentUnit, DeploymentDescriptorEnvironment deploymentDescriptorEnvironment, ResourceInjectionTarget resourceInjectionTarget, ComponentDescription componentDescription, ClassLoader classLoader, DeploymentReflectionIndex deploymentReflectionIndex, EEApplicationClasses applicationClasses) throws DeploymentUnitProcessingException {
        DeploymentUnit rootUnit = deploymentUnit;
        while (rootUnit.getParent() != null) {
            rootUnit = rootUnit.getParent();
        }
        final ConcurrencyManagedCDIBeans concurrencyManagedCDIBeans = rootUnit.getAttachment(ConcurroConcurrencyAttachments.CONCURRENCY_MANAGED_CDI_BEANS);
        final RemoteEnvironment remoteEnvironment = deploymentDescriptorEnvironment.getEnvironment();
        if (remoteEnvironment instanceof Environment) {
            final Environment environment = (Environment) remoteEnvironment;
            final ContextServicesMetaData contextServicesMetaData = environment.getContextServices();
            if (contextServicesMetaData != null) {
                for(ContextServiceMetaData metaData : contextServicesMetaData) {
                    addDefinition(concurrencyManagedCDIBeans, ConcurrencyManagedCDIBeans.Type.CONTEXT_SERVICE, metaData.getQualifier(), metaData.getName());
                }
            }
            final ManagedExecutorsMetaData managedExecutorsMetaData = environment.getManagedExecutors();
            if (managedExecutorsMetaData != null) {
                for(ManagedExecutorMetaData metaData : managedExecutorsMetaData) {
                    addDefinition(concurrencyManagedCDIBeans, ConcurrencyManagedCDIBeans.Type.MANAGED_EXECUTOR_SERVICE, metaData.getQualifier(), metaData.getName());
                }
            }
            final ManagedScheduledExecutorsMetaData managedScheduledExecutorMetaData = environment.getManagedScheduledExecutors();
            if (managedScheduledExecutorMetaData != null) {
                for(ManagedScheduledExecutorMetaData metaData : managedScheduledExecutorMetaData) {
                    addDefinition(concurrencyManagedCDIBeans, ConcurrencyManagedCDIBeans.Type.MANAGED_SCHEDULED_EXECUTOR_SERVICE, metaData.getQualifier(), metaData.getName());
                }
            }
            final ManagedThreadFactoriesMetaData managedThreadFactoryMetaData = environment.getManagedThreadFactories();
            if (managedThreadFactoryMetaData != null) {
                for(ManagedThreadFactoryMetaData metaData : managedThreadFactoryMetaData) {
                    addDefinition(concurrencyManagedCDIBeans, ConcurrencyManagedCDIBeans.Type.MANAGED_THREAD_FACTORY, metaData.getQualifier(), metaData.getName());
                }
            }
        }
        // nothing else todo here
        return Collections.emptyList();
    }

    private void addDefinition(ConcurrencyManagedCDIBeans concurrencyManagedCDIBeans, ConcurrencyManagedCDIBeans.Type type, List<String> qualifierList, String name ) {
        Set<String> qualifierSet = qualifierList == null || (qualifierList.size() == 1 && qualifierList.get(0).isEmpty()) ? Collections.emptySet() : new HashSet<>(qualifierList);
        concurrencyManagedCDIBeans.addDefinition(type,  qualifierSet, name);
    }
}
