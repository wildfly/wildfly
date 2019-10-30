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

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.ee.component.interceptors.ComponentDispatcherInterceptor;
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
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.invocation.proxy.ProxyFactory;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;

import static java.lang.reflect.Modifier.ABSTRACT;
import static java.lang.reflect.Modifier.PUBLIC;
import static java.lang.reflect.Modifier.STATIC;
import static org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX;

/**
 * A description of a view.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class ViewDescription {

    //JVM bridge method flag
    public static final int BRIDGE = 0x0040;

    private final String viewClassName;
    private final ComponentDescription componentDescription;
    private final List<String> viewNameParts = new ArrayList<String>();
    private final Set<String> bindingNames = new HashSet<String>();
    private final Deque<ViewConfigurator> configurators = new ArrayDeque<ViewConfigurator>();
    private boolean serializable;
    private boolean useWriteReplace;

    /**
     * Construct a new instance.
     *
     * @param componentDescription the associated component description
     * @param viewClassName        the view class name
     */
    public ViewDescription(final ComponentDescription componentDescription, final String viewClassName) {
        this(componentDescription, viewClassName, true);
    }

    /**
     * Construct a new instance.
     *
     * @param componentDescription        the associated component description
     * @param viewClassName               the view class name
     * @param defaultConfiguratorRequired
     */
    public ViewDescription(final ComponentDescription componentDescription, final String viewClassName, final boolean defaultConfiguratorRequired) {
        this.componentDescription = componentDescription;
        this.viewClassName = viewClassName;
        if (defaultConfiguratorRequired) {
            configurators.addFirst(DefaultConfigurator.INSTANCE);
        }
        configurators.addFirst(ViewBindingConfigurator.INSTANCE);
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
     *
     * @param viewClass              view class
     * @param componentConfiguration component config
     * @param proxyFactory           proxy factory
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

    /**
     * Create the injection source
     *
     * @param serviceName     The view service name
     * @param viewClassLoader
     */
    protected InjectionSource createInjectionSource(final ServiceName serviceName, Value<ClassLoader> viewClassLoader, boolean appclient) {
        return new ViewBindingInjectionSource(serviceName);
    }

    public static final ImmediateInterceptorFactory CLIENT_DISPATCHER_INTERCEPTOR_FACTORY = new ImmediateInterceptorFactory(new Interceptor() {
        public Object processInvocation(final InterceptorContext context) throws Exception {
            ComponentView view = context.getPrivateData(ComponentView.class);
            return view.invoke(context);
        }
    });

    private static class DefaultConfigurator implements ViewConfigurator {

        public static final DefaultConfigurator INSTANCE = new DefaultConfigurator();

        public void configure(final DeploymentPhaseContext context, final ComponentConfiguration componentConfiguration, final ViewDescription description, final ViewConfiguration configuration) throws DeploymentUnitProcessingException {
            // Create method indexes
            final DeploymentReflectionIndex reflectionIndex = context.getDeploymentUnit().getAttachment(REFLECTION_INDEX);
            final List<Method> methods = configuration.getProxyFactory().getCachedMethods();
            for (final Method method : methods) {
                MethodIdentifier methodIdentifier = MethodIdentifier.getIdentifierForMethod(method);
                Method componentMethod = ClassReflectionIndexUtil.findMethod(reflectionIndex, componentConfiguration.getComponentClass(), methodIdentifier);

                if (componentMethod == null && method.getDeclaringClass().isInterface() && (method.getModifiers() & (ABSTRACT | PUBLIC | STATIC)) == PUBLIC) {
                    // no component method and the interface method is defaulted, so we really do want to invoke on the interface method
                    componentMethod = method;
                }
                if (componentMethod != null) {

                    if ((BRIDGE & componentMethod.getModifiers()) != 0) {
                        Method other = findRealMethodForBridgeMethod(componentMethod, componentConfiguration, reflectionIndex, methodIdentifier);
                        //try and find the non-bridge method to delegate to
                        if(other != null) {
                                componentMethod = other;
                        }
                    }

                    configuration.addViewInterceptor(method, new ImmediateInterceptorFactory(new ComponentDispatcherInterceptor(componentMethod)), InterceptorOrder.View.COMPONENT_DISPATCHER);
                    configuration.addClientInterceptor(method, CLIENT_DISPATCHER_INTERCEPTOR_FACTORY, InterceptorOrder.Client.CLIENT_DISPATCHER);
                    configuration.getViewToComponentMethodMap().put(method, componentMethod);
                }
            }

            configuration.addClientPostConstructInterceptor(Interceptors.getTerminalInterceptorFactory(), InterceptorOrder.ClientPostConstruct.TERMINAL_INTERCEPTOR);
            configuration.addClientPreDestroyInterceptor(Interceptors.getTerminalInterceptorFactory(), InterceptorOrder.ClientPreDestroy.TERMINAL_INTERCEPTOR);
        }

        private Method findRealMethodForBridgeMethod(final Method componentMethod, final ComponentConfiguration componentConfiguration, final DeploymentReflectionIndex reflectionIndex, final MethodIdentifier methodIdentifier) {
            final ClassReflectionIndex classIndex = reflectionIndex.getClassIndex(componentMethod.getDeclaringClass()); //the non-bridge method will be on the same class as the bridge method
            final Collection<Method> methods = classIndex.getAllMethods(componentMethod.getName(), componentMethod.getParameterTypes().length);
            for(final Method method : methods) {
                if ((BRIDGE & method.getModifiers()) == 0) {
                    if(componentMethod.getReturnType().isAssignableFrom(method.getReturnType())) {
                        boolean ok = true;
                        for(int i = 0; i < method.getParameterTypes().length; ++i) {
                            if(!componentMethod.getParameterTypes()[i].isAssignableFrom(method.getParameterTypes()[i])) {
                                ok = false;
                                break;
                            }
                        }
                        if(ok) {
                            return method;
                        }
                    }
                }
            }
            return null;
        }
    }


    private static class ViewBindingConfigurator implements ViewConfigurator {

        public static final ViewBindingConfigurator INSTANCE = new ViewBindingConfigurator();

        @Override
        public void configure(final DeploymentPhaseContext context, final ComponentConfiguration componentConfiguration, final ViewDescription description, final ViewConfiguration configuration) throws DeploymentUnitProcessingException {
            boolean appclient = context.getDeploymentUnit().getAttachment(Attachments.EE_MODULE_DESCRIPTION).isAppClient();
            // Create view bindings
            final List<BindingConfiguration> bindingConfigurations = configuration.getBindingConfigurations();
            for (String bindingName : description.getBindingNames()) {
                bindingConfigurations.add(new BindingConfiguration(bindingName, description.createInjectionSource(description.getServiceName(), Values.immediateValue(componentConfiguration.getModuleClassLoader()), appclient)));
            }
        }
    }

    public boolean isSerializable() {
        return serializable;
    }

    public void setSerializable(final boolean serializable) {
        this.serializable = serializable;
    }

    public boolean isUseWriteReplace() {
        return useWriteReplace;
    }

    public void setUseWriteReplace(final boolean useWriteReplace) {
        this.useWriteReplace = useWriteReplace;
    }

    @Override
    public String toString() {
        return "View of type " + viewClassName + " for " + componentDescription;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final ViewDescription that = (ViewDescription) o;

        //compare the component description based on ==
        if (componentDescription != that.componentDescription)
            return false;
        if (viewClassName != null ? !viewClassName.equals(that.viewClassName) : that.viewClassName != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = viewClassName != null ? viewClassName.hashCode() : 0;
        result = 31 * result + (componentDescription != null ? componentDescription.hashCode() : 0);
        return result;
    }
}
