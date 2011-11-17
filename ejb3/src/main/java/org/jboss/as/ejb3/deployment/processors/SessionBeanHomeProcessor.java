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
package org.jboss.as.ejb3.deployment.processors;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

import javax.ejb.Handle;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentStartService;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ee.component.DependencyConfigurator;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.deployers.AbstractComponentConfigProcessor;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.EJBViewDescription;
import org.jboss.as.ejb3.component.interceptors.EjbMetadataInterceptorFactory;
import org.jboss.as.ejb3.component.interceptors.HomeRemoveInterceptor;
import org.jboss.as.ejb3.component.interceptors.SessionBeanHomeInterceptorFactory;
import org.jboss.as.ejb3.component.session.InvalidRemoveExceptionMethodInterceptor;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.ejb3.component.stateless.StatelessComponentDescription;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.server.deployment.reflect.DeploymentClassIndex;
import org.jboss.msc.service.ServiceBuilder;
import static org.jboss.as.ejb3.EjbMessages.MESSAGES;

/**
 * Processor that hooks up home interfaces for session beans
 *
 * @author Stuart Douglas
 */
public class SessionBeanHomeProcessor extends AbstractComponentConfigProcessor {

    @Override
    protected void processComponentConfig(final DeploymentUnit deploymentUnit, final DeploymentPhaseContext phaseContext, final CompositeIndex index, final ComponentDescription componentDescription) throws DeploymentUnitProcessingException {

        if (componentDescription instanceof SessionBeanComponentDescription) {
            final SessionBeanComponentDescription ejbComponentDescription = (SessionBeanComponentDescription) componentDescription;

            //check for EJB's with a local home interface
            if (ejbComponentDescription.getEjbLocalHomeView() != null) {
                final EJBViewDescription view = ejbComponentDescription.getEjbLocalHomeView();
                final EJBViewDescription ejbLocalView = ejbComponentDescription.getEjbLocalView();
                configureHome(phaseContext, componentDescription, ejbComponentDescription, view, ejbLocalView);
            }
            if (ejbComponentDescription.getEjbHomeView() != null) {
                final EJBViewDescription view = ejbComponentDescription.getEjbHomeView();
                final EJBViewDescription ejbRemoteView = ejbComponentDescription.getEjbRemoteView();
                configureHome(phaseContext, componentDescription, ejbComponentDescription, view, ejbRemoteView);
            }
        }
    }

    private void configureHome(final DeploymentPhaseContext phaseContext, final ComponentDescription componentDescription, final SessionBeanComponentDescription ejbComponentDescription, final EJBViewDescription homeView, final EJBViewDescription ejbObjectView) {
        homeView.getConfigurators().add(new ViewConfigurator() {

            @Override
            public void configure(final DeploymentPhaseContext context, final ComponentConfiguration componentConfiguration, final ViewDescription description, final ViewConfiguration configuration) throws DeploymentUnitProcessingException {

                configuration.addClientPostConstructInterceptor(org.jboss.invocation.Interceptors.getTerminalInterceptorFactory(), InterceptorOrder.ClientPostConstruct.TERMINAL_INTERCEPTOR);
                configuration.addClientPreDestroyInterceptor(org.jboss.invocation.Interceptors.getTerminalInterceptorFactory(), InterceptorOrder.ClientPreDestroy.TERMINAL_INTERCEPTOR);

                final DeploymentClassIndex classIndex = phaseContext.getDeploymentUnit().getAttachment(Attachments.CLASS_INDEX);

                //loop over methods looking for create methods:
                for (Method method : configuration.getProxyFactory().getCachedMethods()) {
                    if (method.getName().startsWith("create")) {
                        //we have a create method
                        if (ejbObjectView == null) {
                            throw MESSAGES.invalidEjbLocalInterface(componentDescription.getComponentName());
                        }
                        final ViewDescription createdView = ejbObjectView;

                        Method initMethod = resolveInitMethod(ejbComponentDescription, method);
                        final SessionBeanHomeInterceptorFactory factory = new SessionBeanHomeInterceptorFactory(initMethod);
                        //add a dependency on the view to create
                        componentConfiguration.getStartDependencies().add(new DependencyConfigurator<ComponentStartService>() {
                            @Override
                            public void configureDependency(final ServiceBuilder<?> serviceBuilder, final ComponentStartService service) throws DeploymentUnitProcessingException {
                                serviceBuilder.addDependency(createdView.getServiceName(), ComponentView.class, factory.getViewToCreate());
                            }
                        });
                        //add the interceptor
                        configuration.addClientInterceptor(method, ViewDescription.CLIENT_DISPATCHER_INTERCEPTOR_FACTORY, InterceptorOrder.Client.CLIENT_DISPATCHER);
                        configuration.addViewInterceptor(method, factory, InterceptorOrder.View.HOME_METHOD_INTERCEPTOR);

                    } else if (method.getName().equals("getEJBMetaData") && method.getParameterTypes().length == 0) {

                        final Class<?> ejbObjectClass;
                        try {
                            ejbObjectClass = classIndex.classIndex(ejbObjectView.getViewClassName()).getModuleClass();
                        } catch (ClassNotFoundException e) {
                            throw new DeploymentUnitProcessingException("Could not load view class for " + componentDescription.getComponentName(), e);
                        }
                        final EjbMetadataInterceptorFactory factory = new EjbMetadataInterceptorFactory(ejbObjectClass, configuration.getViewClass(), null, true, componentDescription instanceof StatelessComponentDescription);

                        //add a dependency on the view to create
                        componentConfiguration.getStartDependencies().add(new DependencyConfigurator<ComponentStartService>() {
                            @Override
                            public void configureDependency(final ServiceBuilder<?> serviceBuilder, final ComponentStartService service) throws DeploymentUnitProcessingException {
                                serviceBuilder.addDependency(configuration.getViewServiceName(), ComponentView.class, factory.getHomeView());
                            }
                        });
                        //add the interceptor
                        configuration.addClientInterceptor(method, ViewDescription.CLIENT_DISPATCHER_INTERCEPTOR_FACTORY, InterceptorOrder.Client.CLIENT_DISPATCHER);
                        configuration.addViewInterceptor(method, factory, InterceptorOrder.View.HOME_METHOD_INTERCEPTOR);

                    } else if (method.getName().equals("remove") && method.getParameterTypes().length == 1 && method.getParameterTypes()[0] == Object.class) {
                        configuration.addClientInterceptor(method, ViewDescription.CLIENT_DISPATCHER_INTERCEPTOR_FACTORY, InterceptorOrder.Client.CLIENT_DISPATCHER);
                        configuration.addViewInterceptor(method, InvalidRemoveExceptionMethodInterceptor.FACTORY, InterceptorOrder.View.INVALID_METHOD_EXCEPTION);
                    } else if (method.getName().equals("remove") && method.getParameterTypes().length == 1 && method.getParameterTypes()[0] == Handle.class) {
                        configuration.addClientInterceptor(method, ViewDescription.CLIENT_DISPATCHER_INTERCEPTOR_FACTORY, InterceptorOrder.Client.CLIENT_DISPATCHER);
                        configuration.addViewInterceptor(method, HomeRemoveInterceptor.FACTORY, InterceptorOrder.View.HOME_METHOD_INTERCEPTOR);                    }

                }
            }

        });
    }


    private Method resolveInitMethod(final EJBComponentDescription description, final Method method) throws DeploymentUnitProcessingException {
        if (description instanceof StatelessComponentDescription) {
            return null;
        } else if (description instanceof StatefulComponentDescription) {
            return resolveStatefulInitMethod((StatefulComponentDescription) description, method);
        } else {
            throw MESSAGES.localHomeNotAllow(description);
        }
    }


    private Method resolveStatefulInitMethod(final StatefulComponentDescription description, final Method method) throws DeploymentUnitProcessingException {

        //for a SFSB we need to resolve the corresponding init method for this create method

        Method initMethod = null;
        //first we try and resolve methods that have additiona resolution data associated with them
        for (Map.Entry<Method, String> entry : description.getInitMethods().entrySet()) {
            String name = entry.getValue();
            Method init = entry.getKey();
            if (name != null) {
                if (Arrays.equals(init.getParameterTypes(), method.getParameterTypes())) {
                    if (init.getName().equals(name)) {
                        initMethod = init;
                    }
                }
            }
        }
        //now try and resolve the init methods with no additional resolution data
        if (initMethod == null) {
            for (Map.Entry<Method, String> entry : description.getInitMethods().entrySet()) {
                Method init = entry.getKey();
                if (entry.getValue() == null) {
                    if (Arrays.equals(init.getParameterTypes(), method.getParameterTypes())) {
                        initMethod = init;
                        break;
                    }
                }
            }
        }
        if (initMethod == null) {
            throw MESSAGES.failToCallEjbCreateForHomeInterface(method,description.getEJBClassName());
        }
        return initMethod;
    }

}
