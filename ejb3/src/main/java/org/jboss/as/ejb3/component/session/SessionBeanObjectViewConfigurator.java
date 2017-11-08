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
import java.lang.reflect.Modifier;

import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentStartService;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ee.component.DependencyConfigurator;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.interceptors.ComponentDispatcherInterceptor;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ee.component.serialization.WriteReplaceInterface;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EjbHomeViewDescription;
import org.jboss.as.ejb3.component.interceptors.GetHomeInterceptorFactory;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndexUtil;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.Interceptors;
import org.jboss.invocation.proxy.MethodIdentifier;
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
        final DeploymentReflectionIndex index = context.getDeploymentUnit().getAttachment(Attachments.REFLECTION_INDEX);
        for (final Method method : configuration.getProxyFactory().getCachedMethods()) {

            if (method.getName().equals("getPrimaryKey") && method.getParameterTypes().length == 0) {
                configuration.addClientInterceptor(method, ViewDescription.CLIENT_DISPATCHER_INTERCEPTOR_FACTORY, InterceptorOrder.Client.CLIENT_DISPATCHER);
                configuration.addViewInterceptor(method, PRIMARY_KEY_INTERCEPTOR, InterceptorOrder.View.COMPONENT_DISPATCHER);
            } else if (method.getName().equals("remove") && method.getParameterTypes().length == 0) {
                handleRemoveMethod(componentConfiguration, configuration, index, method);

            } else if (method.getName().equals("getEJBLocalHome") && method.getParameterTypes().length == 0) {
                configuration.addClientInterceptor(method, ViewDescription.CLIENT_DISPATCHER_INTERCEPTOR_FACTORY, InterceptorOrder.Client.CLIENT_DISPATCHER);
                final GetHomeInterceptorFactory factory = new GetHomeInterceptorFactory();
                configuration.addViewInterceptor(method, factory, InterceptorOrder.View.COMPONENT_DISPATCHER);
                final SessionBeanComponentDescription componentDescription = (SessionBeanComponentDescription) componentConfiguration.getComponentDescription();
                componentConfiguration.getStartDependencies().add(new DependencyConfigurator<ComponentStartService>() {
                    @Override
                    public void configureDependency(final ServiceBuilder<?> serviceBuilder, final ComponentStartService service) throws DeploymentUnitProcessingException {
                        EjbHomeViewDescription ejbLocalHomeView = componentDescription.getEjbLocalHomeView();
                        if (ejbLocalHomeView == null) {
                            throw EjbLogger.ROOT_LOGGER.beanLocalHomeInterfaceIsNull(componentDescription.getComponentName());
                        }
                        serviceBuilder.addDependency(ejbLocalHomeView.getServiceName(), ComponentView.class, factory.getViewToCreate());

                    }
                });
            } else if (method.getName().equals("getEJBHome") && method.getParameterTypes().length == 0) {
                configuration.addClientInterceptor(method, ViewDescription.CLIENT_DISPATCHER_INTERCEPTOR_FACTORY, InterceptorOrder.Client.CLIENT_DISPATCHER);
                final GetHomeInterceptorFactory factory = new GetHomeInterceptorFactory();
                configuration.addViewInterceptor(method, factory, InterceptorOrder.View.COMPONENT_DISPATCHER);
                final SessionBeanComponentDescription componentDescription = (SessionBeanComponentDescription) componentConfiguration.getComponentDescription();
                componentConfiguration.getStartDependencies().add(new DependencyConfigurator<ComponentStartService>() {
                    @Override
                    public void configureDependency(final ServiceBuilder<?> serviceBuilder, final ComponentStartService service) throws DeploymentUnitProcessingException {
                        EjbHomeViewDescription ejbHomeView = componentDescription.getEjbHomeView();
                        if (ejbHomeView == null) {
                            throw EjbLogger.ROOT_LOGGER.beanHomeInterfaceIsNull(componentDescription.getComponentName());
                        }
                        serviceBuilder.addDependency(ejbHomeView.getServiceName(), ComponentView.class, factory.getViewToCreate());

                    }
                });
            } else if (method.getName().equals("getHandle") && method.getParameterTypes().length == 0) {
                //ignore, handled client side
            } else if (method.getName().equals("isIdentical") && method.getParameterTypes().length == 1 && (method.getParameterTypes()[0].equals(EJBObject.class) || method.getParameterTypes()[0].equals(EJBLocalObject.class))) {

                handleIsIdenticalMethod(componentConfiguration, configuration, index, method);
            }  else {
                final Method componentMethod = ClassReflectionIndexUtil.findMethod(index, componentConfiguration.getComponentClass(), MethodIdentifier.getIdentifierForMethod(method));

                if (componentMethod != null) {
                    if(!Modifier.isPublic(componentMethod.getModifiers())) {
                        throw EjbLogger.ROOT_LOGGER.ejbBusinessMethodMustBePublic(componentMethod);
                    }
                    configuration.addViewInterceptor(method, new ImmediateInterceptorFactory(new ComponentDispatcherInterceptor(componentMethod)), InterceptorOrder.View.COMPONENT_DISPATCHER);
                    configuration.addClientInterceptor(method, ViewDescription.CLIENT_DISPATCHER_INTERCEPTOR_FACTORY, InterceptorOrder.Client.CLIENT_DISPATCHER);
                } else if(method.getDeclaringClass() != Object.class && method.getDeclaringClass() != WriteReplaceInterface.class) {
                    throw EjbLogger.ROOT_LOGGER.couldNotFindViewMethodOnEjb(method, description.getViewClassName(), componentConfiguration.getComponentName());
                }
            }
        }

        configuration.addClientPostConstructInterceptor(Interceptors.getTerminalInterceptorFactory(), InterceptorOrder.ClientPostConstruct.TERMINAL_INTERCEPTOR);
        configuration.addClientPreDestroyInterceptor(Interceptors.getTerminalInterceptorFactory(), InterceptorOrder.ClientPreDestroy.TERMINAL_INTERCEPTOR);

    }

    protected abstract void handleIsIdenticalMethod(final ComponentConfiguration componentConfiguration, final ViewConfiguration configuration, final DeploymentReflectionIndex index, final Method method);

    protected abstract void handleRemoveMethod(final ComponentConfiguration componentConfiguration, final ViewConfiguration configuration, final DeploymentReflectionIndex index, final Method method) throws DeploymentUnitProcessingException;


    private static final InterceptorFactory PRIMARY_KEY_INTERCEPTOR = new ImmediateInterceptorFactory(new Interceptor() {
        @Override
        public Object processInvocation(final InterceptorContext context) throws Exception {
            throw EjbLogger.ROOT_LOGGER.cannotCallGetPKOnSessionBean();
        }
    });

}
