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
package org.jboss.as.ejb3.component.session;

import java.lang.reflect.Method;

import javax.ejb.EJBException;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentStartService;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ee.component.DependencyConfigurator;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ejb3.component.interceptors.GetHomeInterceptorFactory;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.msc.service.ServiceBuilder;

/**
 * configurator that sets up interceptors for an EJB's object view
 *
 * @author Stuart Douglas
 */
public abstract class SessionBeanObjectViewConfigurator implements ViewConfigurator {

    @Override
    public void configure(final DeploymentPhaseContext context, final ComponentConfiguration componentConfiguration, final ViewDescription description, final ViewConfiguration configuration) throws DeploymentUnitProcessingException {
        //note that we don't have to handle all methods on the EJBObject, as some are handled client side

        for (final Method method : configuration.getProxyFactory().getCachedMethods()) {

            if (method.getName().equals("getPrimaryKey") && method.getParameterTypes().length == 0) {
                configuration.addClientInterceptor(method, ViewDescription.CLIENT_DISPATCHER_INTERCEPTOR_FACTORY, InterceptorOrder.Client.CLIENT_DISPATCHER);
                configuration.addViewInterceptor(method, PRIMARY_KEY_INTERCEPTOR, InterceptorOrder.View.COMPONENT_DISPATCHER);
            } else if (method.getName().equals("remove") && method.getParameterTypes().length == 0) {
                configuration.addClientInterceptor(method, ViewDescription.CLIENT_DISPATCHER_INTERCEPTOR_FACTORY, InterceptorOrder.Client.CLIENT_DISPATCHER);
                configuration.addViewInterceptor(method, getEjbRemoveInterceptorFactory(), InterceptorOrder.View.SESSION_REMOVE_INTERCEPTOR);
                configuration.addViewInterceptor(method, org.jboss.invocation.Interceptors.getTerminalInterceptorFactory(), InterceptorOrder.View.COMPONENT_DISPATCHER);

            } else if (method.getName().equals("getEJBLocalHome") && method.getParameterTypes().length == 0) {
                configuration.addClientInterceptor(method, ViewDescription.CLIENT_DISPATCHER_INTERCEPTOR_FACTORY, InterceptorOrder.Client.CLIENT_DISPATCHER);
                final GetHomeInterceptorFactory factory = new GetHomeInterceptorFactory();
                configuration.addViewInterceptor(method, factory, InterceptorOrder.View.COMPONENT_DISPATCHER);
                final SessionBeanComponentDescription componentDescription = (SessionBeanComponentDescription) componentConfiguration.getComponentDescription();
                componentConfiguration.getStartDependencies().add(new DependencyConfigurator<ComponentStartService>() {
                    @Override
                    public void configureDependency(final ServiceBuilder<?> serviceBuilder, final ComponentStartService service) throws DeploymentUnitProcessingException {
                        serviceBuilder.addDependency(componentDescription.getEjbLocalHomeView().getServiceName(), ComponentView.class, factory.getViewToCreate());
                    }
                });
            }else if (method.getName().equals("getEJBHome") && method.getParameterTypes().length == 0) {
                configuration.addClientInterceptor(method, ViewDescription.CLIENT_DISPATCHER_INTERCEPTOR_FACTORY, InterceptorOrder.Client.CLIENT_DISPATCHER);
                final GetHomeInterceptorFactory factory = new GetHomeInterceptorFactory();
                configuration.addViewInterceptor(method, factory, InterceptorOrder.View.COMPONENT_DISPATCHER);
                final SessionBeanComponentDescription componentDescription = (SessionBeanComponentDescription) componentConfiguration.getComponentDescription();
                componentConfiguration.getStartDependencies().add(new DependencyConfigurator<ComponentStartService>() {
                    @Override
                    public void configureDependency(final ServiceBuilder<?> serviceBuilder, final ComponentStartService service) throws DeploymentUnitProcessingException {
                        serviceBuilder.addDependency(componentDescription.getEjbHomeView().getServiceName(), ComponentView.class, factory.getViewToCreate());
                    }
                });
            }
        }
    }

    protected abstract InterceptorFactory getEjbRemoveInterceptorFactory();

    private static final InterceptorFactory PRIMARY_KEY_INTERCEPTOR = new ImmediateInterceptorFactory(new Interceptor() {
        @Override
        public Object processInvocation(final InterceptorContext context) throws Exception {
            throw new EJBException("Cannot call getPrimaryKey on a session bean");
        }
    });
}
