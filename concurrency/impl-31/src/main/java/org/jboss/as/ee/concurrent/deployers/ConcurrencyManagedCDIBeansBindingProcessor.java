/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent.deployers;

import org.glassfish.concurro.cdi.ConcurrencyManagedCDIBeans;
import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ee.concurrent.ConcurroConcurrencyAttachments;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;


/**
 * Processor responsible for binding the {@link ConcurrencyManagedCDIBeans} instance to JNDI.
 *
 * @author Eduardo Martins
 */
public class ConcurrencyManagedCDIBeansBindingProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (deploymentUnit.getParent() != null) {
            return;
        }
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        if (moduleDescription == null) {
            return;
        }
        final ConcurrencyManagedCDIBeans concurrencyManagedCDIBeans = new ConcurrencyManagedCDIBeans();
        deploymentUnit.putAttachment(ConcurroConcurrencyAttachments.CONCURRENCY_MANAGED_CDI_BEANS, concurrencyManagedCDIBeans);
        moduleDescription.getBindingConfigurations().add(new BindingConfiguration(ConcurrencyManagedCDIBeans.JNDI_NAME, new InjectionSource() {
            @Override
            public void getResourceValue(ResolutionContext resolutionContext, ServiceBuilder<?> serviceBuilder, DeploymentPhaseContext phaseContext, Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
                injector.inject(new ValueManagedReferenceFactory(concurrencyManagedCDIBeans));
            }
        }));
    }
}
