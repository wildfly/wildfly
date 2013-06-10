/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ee.concurrent.deployers;

import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ServiceInjectionSource;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ee.concurrent.ConcurrentContextInterceptor;
import org.jboss.as.ee.concurrent.handle.ClassLoaderContextHandleFactory;
import org.jboss.as.ee.concurrent.handle.ConcurrentContextHandleFactory;
import org.jboss.as.ee.concurrent.handle.NamingContextHandleFactory;
import org.jboss.as.ee.concurrent.handle.SecurityContextHandleFactory;
import org.jboss.as.ee.concurrent.service.ConcurrentServiceNames;
import org.jboss.as.ee.concurrent.service.ContextServiceService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.InterceptorFactory;

import java.util.List;

/**
 * The {@link org.jboss.as.server.deployment.DeploymentUnitProcessor} which sets up the default concurrent service for each EE component in the deployment unit.
 *
 * @author Eduardo Martins
 */
public class EEConcurrentDefaultContextServiceProcessor extends EEConcurrentAbstractProcessor {

    @Override
    void installComponentConfigurator(ComponentDescription componentDescription) {
        final ComponentConfigurator componentConfigurator = new ComponentConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                // add the interceptor which manages the concurrent context
                final ConcurrentContextInterceptor interceptor = new ConcurrentContextInterceptor(configuration);
                final InterceptorFactory interceptorFactory = new ImmediateInterceptorFactory(interceptor);
                configuration.addPostConstructInterceptor(interceptorFactory, InterceptorOrder.ComponentPostConstruct.CONCURRENT_CONTEXT);
                configuration.addPreDestroyInterceptor(interceptorFactory, InterceptorOrder.ComponentPreDestroy.CONCURRENT_CONTEXT);
                if (description.isPassivationApplicable()) {
                    configuration.addPrePassivateInterceptor(interceptorFactory, InterceptorOrder.ComponentPassivation.CONCURRENT_CONTEXT);
                    configuration.addPostActivateInterceptor(interceptorFactory, InterceptorOrder.ComponentPassivation.CONCURRENT_CONTEXT);
                }
                configuration.addComponentInterceptor(interceptorFactory, InterceptorOrder.Component.CONCURRENT_CONTEXT, false);
                // add default context handle factories
                configuration.getAllChainedContextHandleFactory().add(ClassLoaderContextHandleFactory.INSTANCE);
                configuration.getAllChainedContextHandleFactory().add(SecurityContextHandleFactory.INSTANCE);
                configuration.getAllChainedContextHandleFactory().add(new NamingContextHandleFactory(configuration.getNamespaceContextSelector(), context.getDeploymentUnit().getServiceName()));
                configuration.getAllChainedContextHandleFactory().add(ConcurrentContextHandleFactory.INSTANCE);
            }
        };
        componentDescription.getConfigurators().add(componentConfigurator);
    }

    @Override
    void addBindingsConfigurations(String bindingNamePrefix, List<BindingConfiguration> bindingConfigurations) {
        bindingConfigurations.add(new BindingConfiguration(bindingNamePrefix + "DefaultContextService", new ServiceInjectionSource(ConcurrentServiceNames.DEFAULT_CONTEXT_SERVICE_SERVICE_NAME, ContextServiceService.SERVICE_VALUE_TYPE)));
    }

}
