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
package org.jboss.as.ejb3.component.entity;

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
import org.jboss.as.ejb3.component.entity.interceptors.EntityBeanAssociatingInterceptor;
import org.jboss.as.ejb3.component.entity.interceptors.EntityBeanEjbCreateMethodInterceptor;
import org.jboss.as.ejb3.component.entity.interceptors.EntityBeanIdentityInterceptorFactory;
import org.jboss.as.ejb3.component.entity.interceptors.EntityBeanIsIdenticalInterceptorFactory;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndexUtil;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.Interceptors;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.msc.service.ServiceBuilder;

/**
 * configurator that sets up interceptors for an EJB's object view
 *
 * @author Stuart Douglas
 */
public class EntityBeanObjectViewConfigurator implements ViewConfigurator {

    @Override
    public void configure(final DeploymentPhaseContext context, final ComponentConfiguration componentConfiguration, final ViewDescription description, final ViewConfiguration configuration) throws DeploymentUnitProcessingException {
        final DeploymentReflectionIndex index = context.getDeploymentUnit().getAttachment(org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX);

        configuration.addClientPostConstructInterceptor(getEjbCreateInterceptorFactory(), InterceptorOrder.ClientPostConstruct.INSTANCE_CREATE);
        configuration.addViewInterceptor(EntityBeanAssociatingInterceptor.FACTORY, InterceptorOrder.View.ASSOCIATING_INTERCEPTOR);

        for (final Method method : configuration.getProxyFactory().getCachedMethods()) {

            if (method.getName().equals("getPrimaryKey") && method.getParameterTypes().length == 0) {
                configuration.addClientInterceptor(method, ViewDescription.CLIENT_DISPATCHER_INTERCEPTOR_FACTORY, InterceptorOrder.Client.CLIENT_DISPATCHER);
                configuration.addViewInterceptor(method, EntityBeanInterceptors.GET_PRIMARY_KEY, InterceptorOrder.View.COMPONENT_DISPATCHER);
            } else if (method.getName().equals("remove") && method.getParameterTypes().length == 0) {
                configuration.addClientInterceptor(method, ViewDescription.CLIENT_DISPATCHER_INTERCEPTOR_FACTORY, InterceptorOrder.Client.CLIENT_DISPATCHER);
                Method remove = resolveRemoveMethod(componentConfiguration.getComponentClass(), index, componentConfiguration.getComponentName());
                configuration.addViewInterceptor(method, getEjbRemoveInterceptorFactory(remove), InterceptorOrder.View.COMPONENT_DISPATCHER);
            } else if (method.getName().equals("isIdentical") && method.getParameterTypes().length == 1 &&
                    (method.getParameterTypes()[0] == EJBLocalObject.class || method.getParameterTypes()[0] == EJBObject.class)) {
                configuration.addClientInterceptor(method, ViewDescription.CLIENT_DISPATCHER_INTERCEPTOR_FACTORY, InterceptorOrder.Client.CLIENT_DISPATCHER);
                configuration.addViewInterceptor(method, EntityBeanIsIdenticalInterceptorFactory.INSTANCE, InterceptorOrder.View.COMPONENT_DISPATCHER);
            } else if (method.getName().equals("getEJBLocalHome") && method.getParameterTypes().length == 0) {
                configuration.addClientInterceptor(method, ViewDescription.CLIENT_DISPATCHER_INTERCEPTOR_FACTORY, InterceptorOrder.Client.CLIENT_DISPATCHER);
                final EntityGetHomeInterceptorFactory factory = new EntityGetHomeInterceptorFactory();
                configuration.addViewInterceptor(method, factory, InterceptorOrder.View.COMPONENT_DISPATCHER);
                final EntityBeanComponentDescription entityBeanComponentDescription = (EntityBeanComponentDescription) componentConfiguration.getComponentDescription();
                componentConfiguration.getStartDependencies().add(new DependencyConfigurator<ComponentStartService>() {
                    @Override
                    public void configureDependency(final ServiceBuilder<?> serviceBuilder, final ComponentStartService service) throws DeploymentUnitProcessingException {
                        EjbHomeViewDescription ejbLocalHomeView = entityBeanComponentDescription.getEjbLocalHomeView();
                        if (ejbLocalHomeView == null) {
                            throw EjbLogger.ROOT_LOGGER.beanLocalHomeInterfaceIsNull(entityBeanComponentDescription.getComponentName());
                        }
                        serviceBuilder.addDependency(ejbLocalHomeView.getServiceName(), ComponentView.class, factory.getViewToCreate());
                    }
                });

            } else if (method.getName().equals("getEJBHome") && method.getParameterTypes().length == 0) {

                configuration.addClientInterceptor(method, ViewDescription.CLIENT_DISPATCHER_INTERCEPTOR_FACTORY, InterceptorOrder.Client.CLIENT_DISPATCHER);
                final EntityGetHomeInterceptorFactory factory = new EntityGetHomeInterceptorFactory();
                configuration.addViewInterceptor(method, factory, InterceptorOrder.View.COMPONENT_DISPATCHER);
                final EntityBeanComponentDescription entityBeanComponentDescription = (EntityBeanComponentDescription) componentConfiguration.getComponentDescription();
                componentConfiguration.getStartDependencies().add(new DependencyConfigurator<ComponentStartService>() {
                    @Override
                    public void configureDependency(final ServiceBuilder<?> serviceBuilder, final ComponentStartService service) throws DeploymentUnitProcessingException {
                        EjbHomeViewDescription ejbHomeView = entityBeanComponentDescription.getEjbHomeView();
                        if (ejbHomeView == null) {
                            throw EjbLogger.ROOT_LOGGER.beanHomeInterfaceIsNull(entityBeanComponentDescription.getComponentName());
                        }
                        serviceBuilder.addDependency(ejbHomeView.getServiceName(), ComponentView.class, factory.getViewToCreate());

                    }
                });

            } else if (method.getName().equals("getHandle") && method.getParameterTypes().length == 0) {
                //ignore, handled client side
            }  else if ((method.getName().equals("hashCode") && method.getParameterTypes().length == 0) || method.getName().equals("equals") && method.getParameterTypes().length == 1 && method.getParameterTypes()[0] == Object.class) {
                configuration.addClientInterceptor(method, EntityBeanIdentityInterceptorFactory.INSTANCE, InterceptorOrder.Client.EJB_EQUALS_HASHCODE);
            } else {

                final Method componentMethod = ClassReflectionIndexUtil.findMethod(index, componentConfiguration.getComponentClass(), MethodIdentifier.getIdentifierForMethod(method));
                if (componentMethod == null) {
                    handleNonBeanMethod(componentConfiguration, configuration, index, method);
                } else {
                    if(!Modifier.isPublic(componentMethod.getModifiers())) {
                        throw EjbLogger.ROOT_LOGGER.ejbBusinessMethodMustBePublic(componentMethod);
                    }
                    configuration.addViewInterceptor(method, new ImmediateInterceptorFactory(new ComponentDispatcherInterceptor(componentMethod)), InterceptorOrder.View.COMPONENT_DISPATCHER);
                    configuration.addClientInterceptor(method, ViewDescription.CLIENT_DISPATCHER_INTERCEPTOR_FACTORY, InterceptorOrder.Client.CLIENT_DISPATCHER);
                }

            }

        }

        configuration.addClientPostConstructInterceptor(Interceptors.getTerminalInterceptorFactory(), InterceptorOrder.ClientPostConstruct.TERMINAL_INTERCEPTOR);
        configuration.addClientPreDestroyInterceptor(Interceptors.getTerminalInterceptorFactory(), InterceptorOrder.ClientPreDestroy.TERMINAL_INTERCEPTOR);

    }

    /**
     * Handle method found on the view that is not implemented by the entity bean impl.  By default it will do nothing with it.
     *
     * @param componentConfiguration
     * @param configuration
     * @param index
     * @param method
     */
    protected void handleNonBeanMethod(final ComponentConfiguration componentConfiguration, final ViewConfiguration configuration, final DeploymentReflectionIndex index, final Method method) throws DeploymentUnitProcessingException {
        if(method.getDeclaringClass() != Object.class && method.getDeclaringClass() != WriteReplaceInterface.class) {
            throw EjbLogger.ROOT_LOGGER.couldNotFindViewMethodOnEjb(method, configuration.getViewClass().getName(), componentConfiguration.getComponentName());
        }
    }

    protected InterceptorFactory getEjbCreateInterceptorFactory() {
        return EntityBeanEjbCreateMethodInterceptor.INSTANCE;
    }

    protected InterceptorFactory getEjbRemoveInterceptorFactory(final Method remove) {
        //for BMP beans we just invoke the ejb remove method as normal, and allow the ejb remove
        //method to handle the actual removal
        return new ImmediateInterceptorFactory(new ComponentDispatcherInterceptor(remove));
    }

    private Method resolveRemoveMethod(final Class<?> componentClass, final DeploymentReflectionIndex index, final String ejbName) throws DeploymentUnitProcessingException {

        Class<?> clazz = componentClass;
        while (clazz != Object.class) {
            final ClassReflectionIndex classIndex = index.getClassIndex(clazz);
            Method ret = classIndex.getMethod(Void.TYPE, "ejbRemove");
            if (ret != null) {
                return ret;
            }
            clazz = clazz.getSuperclass();
        }
        throw EjbLogger.ROOT_LOGGER.failToResolveEjbRemoveForInterface(ejbName);
    }
}
