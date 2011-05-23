/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.component;

import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndexUtil;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.Interceptors;
import org.jboss.invocation.proxy.ProxyFactory;
import org.jboss.msc.service.ServiceName;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX;

/**
 * A description of a view.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class ViewDescription {
    private final String viewClassName;
    private final ComponentDescription componentDescription;
    private final List<String> viewNameParts = new ArrayList<String>();
    private final Set<String> bindingNames = new HashSet<String>();
    private final Deque<ViewConfigurator> configurators = new ArrayDeque<ViewConfigurator>();

    /**
     * Construct a new instance.
     *
     * @param componentDescription the associated component description
     * @param viewClassName        the view class name
     */
    public ViewDescription(final ComponentDescription componentDescription, final String viewClassName) {
        this.componentDescription = componentDescription;
        this.viewClassName = viewClassName;
        configurators.addFirst(new DefaultConfigurator());
    }

    /**
     * Get the view's class name.
     *
     * @return the class name
     */
    public String getViewClassName() {
        return viewClassName;
    }

    /**
     * Get the associated component description.
     *
     * @return the component description
     */
    public ComponentDescription getComponentDescription() {
        return componentDescription;
    }

    /**
     * Get the strings to append to the view base name.  The view base name is the component base name
     * followed by {@code "VIEW"} followed by these strings.
     *
     * @return the list of strings
     */
    public List<String> getViewNameParts() {
        return viewNameParts;
    }

    /**
     * Get the service name for this view.
     *
     * @return the service name
     */
    public ServiceName getServiceName() {
        //TODO: need to set viewNameParts somewhere
        if (!viewNameParts.isEmpty()) {
            return componentDescription.getServiceName().append("VIEW").append(viewNameParts.toArray(new String[viewNameParts.size()]));
        } else {
            return componentDescription.getServiceName().append("VIEW").append(viewClassName);
        }
    }

    /**
     * Creates view configuration. Allows for extensibility in EE sub components.
     * @param viewClass view class
     * @param componentConfiguration component config
     * @param proxyFactory proxy factory
     * @return new view configuration
     */
    public ViewConfiguration createViewConfiguration(final Class<?> viewClass, final ComponentConfiguration componentConfiguration, final ProxyFactory<?> proxyFactory) {
        return new ViewConfiguration(viewClass, componentConfiguration, getServiceName(), proxyFactory);
    }

    /**
     * Get the set of binding names for this view.
     *
     * @return the set of binding names
     */
    public Set<String> getBindingNames() {
        return bindingNames;
    }

    /**
     * Get the configurators for this view.
     *
     * @return the configurators
     */
    public Deque<ViewConfigurator> getConfigurators() {
        return configurators;
    }

    static final ImmediateInterceptorFactory CLIENT_DISPATCHER_INTERCEPTOR_FACTORY = new ImmediateInterceptorFactory(new Interceptor() {
        public Object processInvocation(final InterceptorContext context) throws Exception {
            ComponentViewInstance viewInstance = context.getPrivateData(ComponentViewInstance.class);
            return viewInstance.getEntryPoint(context.getMethod()).processInvocation(context);
        }
    });

    private static class DefaultConfigurator implements ViewConfigurator {

        public void configure(final DeploymentPhaseContext context, final ComponentConfiguration componentConfiguration, final ViewDescription description, final ViewConfiguration configuration) throws DeploymentUnitProcessingException {
            // Create method indexes
            DeploymentReflectionIndex reflectionIndex = context.getDeploymentUnit().getAttachment(REFLECTION_INDEX);
            ClassReflectionIndex<?> index = reflectionIndex.getClassIndex(componentConfiguration.getComponentClass());
            Method[] methods = configuration.getProxyFactory().getCachedMethods();
            for (Method method : methods) {
                final Method componentMethod = ClassReflectionIndexUtil.findRequiredMethod(reflectionIndex, index, method);
                configuration.addViewInterceptor(method,new ImmediateInterceptorFactory(new ComponentDispatcherInterceptor(componentMethod)), InterceptorOrder.View.COMPONENT_DISPATCHER);
                configuration.addClientInterceptor(method, CLIENT_DISPATCHER_INTERCEPTOR_FACTORY, InterceptorOrder.Client.CLIENT_DISPATCHER);
            }

            configuration.addViewPostConstructInterceptor(Interceptors.getTerminalInterceptorFactory(), InterceptorOrder.ViewPostConstruct.TERMINAL_INTERCEPTOR);
            configuration.addViewPreDestroyInterceptor(Interceptors.getTerminalInterceptorFactory(), InterceptorOrder.ViewPreDestroy.TERMINAL_INTERCEPTOR);

            configuration.addClientPostConstructInterceptor(Interceptors.getTerminalInterceptorFactory(), InterceptorOrder.ClientPostConstruct.TERMINAL_INTERCEPTOR);
            configuration.addClientPreDestroyInterceptor(Interceptors.getTerminalInterceptorFactory(), InterceptorOrder.ClientPreDestroy.TERMINAL_INTERCEPTOR);

            // Create view bindings
            final List<BindingConfiguration> bindingConfigurations = configuration.getBindingConfigurations();
            List<String> viewNameParts = description.getViewNameParts();
            ///TODO: viewNameParts should be always populated
            final ServiceName serviceName;
            if (!viewNameParts.isEmpty()) {
                serviceName = context.getDeploymentUnit().getServiceName().append("component").append(componentConfiguration.getComponentName()).append("VIEW").append(viewNameParts.toArray(new String[viewNameParts.size()]));
            } else {
                serviceName = context.getDeploymentUnit().getServiceName().append("component").append(componentConfiguration.getComponentName()).append("VIEW").append(description.getViewClassName());
            }
            for (String bindingName : description.getBindingNames()) {
                bindingConfigurations.add(new BindingConfiguration(bindingName, new ViewBindingInjectionSource(serviceName)));
            }
        }
    }

    private static class ComponentDispatcherInterceptor implements Interceptor {

        private final Method componentMethod;

        public ComponentDispatcherInterceptor(final Method componentMethod) {
            this.componentMethod = componentMethod;
        }

        public Object processInvocation(final InterceptorContext context) throws Exception {
            ComponentInstance componentInstance = context.getPrivateData(ComponentInstance.class);
            if (componentInstance == null) {
                throw new IllegalStateException("No component instance associated");
            }
            Method oldMethod = context.getMethod();
            try {
                context.setMethod(componentMethod);
                context.setTarget(componentInstance.getInstance());
                return componentInstance.getInterceptor(componentMethod).processInvocation(context);
            } finally {
                context.setMethod(oldMethod);
                context.setTarget(null);
            }
        }
    }
}
