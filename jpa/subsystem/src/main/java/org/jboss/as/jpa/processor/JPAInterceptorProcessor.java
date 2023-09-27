/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jpa.processor;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.jpa.interceptor.SBInvocationInterceptor;
import org.jboss.as.jpa.interceptor.SFSBCreateInterceptor;
import org.jboss.as.jpa.interceptor.SFSBDestroyInterceptor;
import org.jboss.as.jpa.interceptor.SFSBInvocationInterceptor;
import org.jboss.as.jpa.interceptor.SFSBPreCreateInterceptor;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

import static org.jboss.as.jpa.messages.JpaLogger.ROOT_LOGGER;

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
                ROOT_LOGGER.tracef("registering session bean Jakarta Interceptors for component '%s' in '%s'", component.getComponentName(), deploymentUnit.getName());
                registerSessionBeanInterceptors((SessionBeanComponentDescription) component, deploymentUnit);
            }
        }
    }

    // Register our listeners on SFSB that will be created
    private void registerSessionBeanInterceptors(SessionBeanComponentDescription componentDescription, final DeploymentUnit deploymentUnit) {
        // if it's a SFSB then setup appropriate Jakarta Interceptors
        if (componentDescription.isStateful()) {

            // first setup the post construct and pre destroy component Jakarta Interceptors
            componentDescription.getConfigurators().addFirst(new ComponentConfigurator() {
                @Override
                public void configure(DeploymentPhaseContext context, ComponentDescription description, ComponentConfiguration configuration) throws
                    DeploymentUnitProcessingException {
                    configuration.addPostConstructInterceptor(SFSBPreCreateInterceptor.FACTORY, InterceptorOrder.ComponentPostConstruct.JPA_SFSB_PRE_CREATE);
                    configuration.addPostConstructInterceptor(SFSBCreateInterceptor.FACTORY, InterceptorOrder.ComponentPostConstruct.JPA_SFSB_CREATE);
                    configuration.addPreDestroyInterceptor(SFSBDestroyInterceptor.FACTORY, InterceptorOrder.ComponentPreDestroy.JPA_SFSB_DESTROY);
                    configuration.addComponentInterceptor(SFSBInvocationInterceptor.FACTORY, InterceptorOrder.Component.JPA_SFSB_INTERCEPTOR, false);

                    //we need to serialized the entity manager state
                    configuration.getInterceptorContextKeys().add(SFSBInvocationInterceptor.CONTEXT_KEY);
                }
            });
        }
        // register interceptor on stateful/stateless SB with transactional entity manager.
        if ((componentDescription.isStateful() || componentDescription.isStateless())) {
            componentDescription.getConfigurators().add(new ComponentConfigurator() {
                @Override
                public void configure(DeploymentPhaseContext context, ComponentDescription description, ComponentConfiguration configuration) throws
                    DeploymentUnitProcessingException {
                    configuration.addComponentInterceptor(SBInvocationInterceptor.FACTORY, InterceptorOrder.Component.JPA_SESSION_BEAN_INTERCEPTOR, false);
                }
            });
        }
    }
}
