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
package org.jboss.as.jpa.processor;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.jpa.container.SFSBXPCMap;
import org.jboss.as.jpa.interceptor.SBInvocationInterceptor;
import org.jboss.as.jpa.interceptor.SFSBCreateInterceptor;
import org.jboss.as.jpa.interceptor.SFSBDestroyInterceptor;
import org.jboss.as.jpa.interceptor.SFSBInvocationInterceptor;
import org.jboss.as.jpa.interceptor.SFSBPreCreateInterceptor;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

/**
 * @author Stuart Douglas
 */
public class JPAInterceptorProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        for (ComponentDescription component : moduleDescription.getComponentDescriptions()) {
            if (component instanceof SessionBeanComponentDescription) {
                registerSessionBeanInterceptors((SessionBeanComponentDescription) component, deploymentUnit);
            }
        }
    }

    // Register our listeners on SFSB that will be created
    private void registerSessionBeanInterceptors(SessionBeanComponentDescription componentDescription, final DeploymentUnit deploymentUnit) {
        // if it's a SFSB then setup appropriate interceptors
        if (componentDescription.isStateful()) {
            // first setup the post construct and pre destroy component interceptors
            componentDescription.getConfigurators().addFirst(new ComponentConfigurator() {
                @Override
                public void configure(DeploymentPhaseContext context, ComponentDescription description, ComponentConfiguration configuration) throws
                        DeploymentUnitProcessingException {
                    final SFSBXPCMap map = SFSBXPCMap.getXpcMap(deploymentUnit);
                    configuration.addPostConstructInterceptor(SFSBPreCreateInterceptor.FACTORY, InterceptorOrder.ComponentPostConstruct.JPA_SFSB_PRE_CREATE);
                    configuration.addPostConstructInterceptor(new SFSBCreateInterceptor.Factory(map), InterceptorOrder.ComponentPostConstruct.JPA_SFSB_CREATE);
                    configuration.addPreDestroyInterceptor(new SFSBDestroyInterceptor.Factory(map), InterceptorOrder.ComponentPreDestroy.JPA_SFSB_DESTROY);
                }
            });

            Iterable<ViewDescription> views = componentDescription.getViews();
            for (ViewDescription view : views) {
                view.getConfigurators().addFirst(new ViewConfigurator() {
                    @Override
                    public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration configuration) throws
                            DeploymentUnitProcessingException {
                        configuration.addViewInterceptor(SFSBInvocationInterceptor.FACTORY, InterceptorOrder.View.JPA_SFSB_INTERCEPTOR);
                    }
                });
            }
        }
        // register interceptor on stateful/stateless SB with transactional entity manager.
        if ((componentDescription.isStateful() || componentDescription.isStateless())) {
            componentDescription.getConfigurators().add(new ComponentConfigurator() {
                @Override
                public void configure(DeploymentPhaseContext context, ComponentDescription description, ComponentConfiguration configuration) throws
                        DeploymentUnitProcessingException {
                    configuration.addComponentInterceptor(SBInvocationInterceptor.FACTORY, InterceptorOrder.Component.JPA_SESSION_BEAN_INTERCEPTOR, false);
                    if(description.isTimerServiceApplicable()) {
                        configuration.addTimeoutInterceptor(SBInvocationInterceptor.FACTORY, InterceptorOrder.Component.JPA_SESSION_BEAN_INTERCEPTOR);
                    }
                }
            });
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
    }
}
