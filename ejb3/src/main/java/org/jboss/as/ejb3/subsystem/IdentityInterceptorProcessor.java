/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.subsystem;

import static org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.EJBViewDescription;
import org.jboss.as.ejb3.security.IdentityInterceptor;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.invocation.ImmediateInterceptorFactory;

import java.util.Collection;

/**
 * A {@link DeploymentUnitProcessor} that adds interceptors to EJB deployments that will always
 * runAs the current {@code SecurityIdentity} to activate any outflow identities.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
class IdentityInterceptorProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        registerIdentityInterceptor(deploymentUnit);
    }

    private static void registerIdentityInterceptor(final DeploymentUnit deploymentUnit) {
        // this Interceptor will get used when invoking local EJBs
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(EE_MODULE_DESCRIPTION);
        if (eeModuleDescription == null) {
            return;
        }
        final Collection<ComponentDescription> componentDescriptions = eeModuleDescription.getComponentDescriptions();
        if (componentDescriptions == null || componentDescriptions.isEmpty()) {
            return;
        }
        for (ComponentDescription componentDescription : componentDescriptions) {
            if (componentDescription instanceof EJBComponentDescription) {
                for (ViewDescription view : componentDescription.getViews()) {
                    final EJBViewDescription ejbView = (EJBViewDescription) view;
                    ejbView.getConfigurators().add(new ViewConfigurator() {
                        @Override
                        public void configure(final DeploymentPhaseContext context, final ComponentConfiguration componentConfiguration,
                                              final ViewDescription description, final ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                            configuration.addClientInterceptor(new ImmediateInterceptorFactory(new IdentityInterceptor()), InterceptorOrder.Client.SECURITY_IDENTITY);
                        }
                    });
                }
            }
        }
    }
}
