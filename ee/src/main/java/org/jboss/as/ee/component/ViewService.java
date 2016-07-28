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
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.ee.utils.DescriptorUtils;
import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.Interceptors;
import org.jboss.invocation.SimpleInterceptorFactoryContext;
import org.jboss.invocation.proxy.ProxyFactory;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.security.manager.WildFlySecurityManager;

import static org.jboss.as.ee.logging.EeLogger.ROOT_LOGGER;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ViewService implements Service<ComponentView> {

    private final InjectedValue<Component> componentInjector = new InjectedValue<Component>();
    private final Map<Method, InterceptorFactory> viewInterceptorFactories;
    private final Map<Method, InterceptorFactory> clientInterceptorFactories;
    private final InterceptorFactory clientPostConstruct;
    private final InterceptorFactory clientPreDestroy;
    private final ProxyFactory<?> proxyFactory;
    private final Class<?> viewClass;
    private final Set<Method> asyncMethods;
    private final ViewInstanceFactory viewInstanceFactory;
    private final Map<Class<?>, Object> privateData;
    private volatile ComponentView view;

    private volatile Interceptor clientPostConstructInterceptor;
    private volatile Interceptor clientPreDestroyInterceptor;
    private volatile Map<Method, Interceptor> clientInterceptors;


    public ViewService(final ViewConfiguration viewConfiguration) {
        viewClass = viewConfiguration.getViewClass();
        final ProxyFactory<?> proxyFactory = viewConfiguration.getProxyFactory();
        this.proxyFactory = proxyFactory;
        final List<Method> methods = proxyFactory.getCachedMethods();
        final int methodCount = methods.size();
        clientPostConstruct = Interceptors.getChainedInterceptorFactory(viewConfiguration.getClientPostConstructInterceptors());
        clientPreDestroy = Interceptors.getChainedInterceptorFactory(viewConfiguration.getClientPreDestroyInterceptors());
        final IdentityHashMap<Method, InterceptorFactory> viewInterceptorFactories = new IdentityHashMap<Method, InterceptorFactory>(methodCount);
        final IdentityHashMap<Method, InterceptorFactory> clientInterceptorFactories = new IdentityHashMap<Method, InterceptorFactory>(methodCount);
        for (final Method method : methods) {
            if (method.getName().equals("finalize") && method.getParameterTypes().length == 0) {
                viewInterceptorFactories.put(method, Interceptors.getTerminalInterceptorFactory());
            } else {
                viewInterceptorFactories.put(method, Interceptors.getChainedInterceptorFactory(viewConfiguration.getViewInterceptors(method)));
                clientInterceptorFactories.put(method, Interceptors.getChainedInterceptorFactory(viewConfiguration.getClientInterceptors(method)));
            }
        }
        this.viewInterceptorFactories = viewInterceptorFactories;
        this.clientInterceptorFactories = clientInterceptorFactories;
        this.asyncMethods = viewConfiguration.getAsyncMethods();
        if (viewConfiguration.getViewInstanceFactory() == null) {
            viewInstanceFactory = new DefaultViewInstanceFactory();
        } else {
            viewInstanceFactory = viewConfiguration.getViewInstanceFactory();
        }
        if(viewConfiguration.getPrivateData().isEmpty()) {
            privateData = Collections.emptyMap();
        } else {
            privateData = viewConfiguration.getPrivateData();
        }
    }

    public void start(final StartContext context) throws StartException {
        // Construct the view
        View view = new View(privateData);
        view.initializeInterceptors();
        this.view = view;

        final SimpleInterceptorFactoryContext factoryContext = new SimpleInterceptorFactoryContext();
        final Component component = view.getComponent();
        factoryContext.getContextData().put(Component.class, component);
        factoryContext.getContextData().put(ComponentView.class, view);

        clientPostConstructInterceptor = clientPostConstruct.create(factoryContext);
        clientPreDestroyInterceptor = clientPreDestroy.create(factoryContext);

        final Map<Method, InterceptorFactory> clientInterceptorFactories = ViewService.this.clientInterceptorFactories;
        clientInterceptors = new IdentityHashMap<Method, Interceptor>(clientInterceptorFactories.size());
        for (Method method : clientInterceptorFactories.keySet()) {
            clientInterceptors.put(method, clientInterceptorFactories.get(method).create(factoryContext));
        }


    }

    public void stop(final StopContext context) {
        view = null;
    }

    public Injector<Component> getComponentInjector() {
        return componentInjector;
    }

    public ComponentView getValue() throws IllegalStateException, IllegalArgumentException {
        return view;
    }

    class View implements ComponentView {

        private final Component component;
        private final Map<Method, Interceptor> viewInterceptors;
        private final Map<MethodDescription, Method> methods;
        private final Map<Class<?>, Object> privateData;

        View(final Map<Class<?>, Object> privateData) {
            this.privateData = privateData;
            component = componentInjector.getValue();
            //we need to build the view interceptor chain
            this.viewInterceptors = new IdentityHashMap<Method, Interceptor>();
            this.methods = new HashMap<MethodDescription, Method>();
        }

        void initializeInterceptors() {
            final SimpleInterceptorFactoryContext factoryContext = new SimpleInterceptorFactoryContext();
            final Map<Method, InterceptorFactory> viewInterceptorFactories = ViewService.this.viewInterceptorFactories;
            final Map<Method, Interceptor> viewEntryPoints = viewInterceptors;
            factoryContext.getContextData().put(Component.class, component);
            //we don't have this code in the constructor so we avoid passing around
            //a half constructed instance
            factoryContext.getContextData().put(ComponentView.class, this);

            for (Method method : viewInterceptorFactories.keySet()) {
                viewEntryPoints.put(method, viewInterceptorFactories.get(method).create(factoryContext));
                methods.put(new MethodDescription(method.getName(), DescriptorUtils.methodDescriptor(method)), method);
            }

        }

        public ManagedReference createInstance() throws Exception {
            return createInstance(Collections.<Object, Object>emptyMap());
        }

        public ManagedReference createInstance(Map<Object, Object> contextData) throws Exception {
            // view instance creation can lead to instantiating application component classes (like the MDB implementation class
            // or even the EJB implementation class of a no-interface view exposing bean). Such class initialization needs to
            // have the TCCL set to the component/application's classloader. @see https://issues.jboss.org/browse/WFLY-3989
            final ClassLoader oldTCCL = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
            try {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(component.getComponentClass());
                return viewInstanceFactory.createViewInstance(this, contextData);
            } finally {
                // reset the TCCL
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(oldTCCL);
            }
        }

        @Override
        public Object invoke(InterceptorContext interceptorContext) throws Exception {
            if(component instanceof BasicComponent) {
                ((BasicComponent) component).waitForComponentStart();
            }
            final Method method = interceptorContext.getMethod();
            final Interceptor interceptor = viewInterceptors.get(method);
            return interceptor.processInvocation(interceptorContext);
        }

        public Component getComponent() {
            return component;
        }

        @Override
        public Class<?> getProxyClass() {
            return proxyFactory.defineClass();
        }

        @Override
        public Class<?> getViewClass() {
            return viewClass;
        }

        @Override
        public Set<Method> getViewMethods() {
            return viewInterceptors.keySet();
        }

        @Override
        public Method getMethod(final String name, final String descriptor) {
            Method method = this.methods.get(new MethodDescription(name, descriptor));
            if (method == null) {
                throw EeLogger.ROOT_LOGGER.viewMethodNotFound(name, descriptor, viewClass, component.getComponentClass());
            }
            return method;
        }

        @Override
        public <T> T getPrivateData(final Class<T> clazz) {
            return (T) privateData.get(clazz);
        }

        @Override
        public boolean isAsynchronous(final Method method) {
            return asyncMethods.contains(method);
        }

        @Override
        public String toString() {
            return "Component view " + viewClass + " for component "
                    + component.getComponentClass();
        }

        private final class MethodDescription {
            private final String name;
            private final String descriptor;

            public MethodDescription(final String name, final String descriptor) {
                this.name = name;
                this.descriptor = descriptor;
            }

            @Override
            public boolean equals(final Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                final MethodDescription that = (MethodDescription) o;

                if (!descriptor.equals(that.descriptor)) return false;
                if (!name.equals(that.name)) return false;

                return true;
            }

            @Override
            public int hashCode() {
                int result = name.hashCode();
                result = 31 * result + descriptor.hashCode();
                return result;
            }
        }
    }

    private class DefaultViewInstanceFactory implements ViewInstanceFactory {

        public ManagedReference createViewInstance(final ComponentView componentView, final Map<Object, Object> contextData) throws Exception {

            final Object proxy;
            final Component component = componentView.getComponent();
            final ComponentClientInstance instance = new ComponentClientInstance();
            try {
                proxy = proxyFactory.newInstance(new ProxyInvocationHandler(clientInterceptors, instance, componentView));
            } catch (InstantiationException e) {
                InstantiationError error = new InstantiationError(e.getMessage());
                Throwable cause = e.getCause();
                if (cause != null) error.initCause(cause);
                throw error;
            } catch (IllegalAccessException e) {
                IllegalAccessError error = new IllegalAccessError(e.getMessage());
                Throwable cause = e.getCause();
                if (cause != null) error.initCause(cause);
                throw error;
            }

            InterceptorContext context = new InterceptorContext();
            context.putPrivateData(ComponentView.class, componentView);
            context.putPrivateData(Component.class, component);
            context.putPrivateData(ComponentClientInstance.class, instance);
            context.setContextData(new HashMap<String, Object>());
            for(Map.Entry<Object, Object> entry : contextData.entrySet()) {
                context.putPrivateData(entry.getKey(), entry.getValue());
            }
            clientPostConstructInterceptor.processInvocation(context);
            instance.constructionComplete();

            return new ManagedReference() {

                @Override
                public void release() {
                    try {
                        InterceptorContext interceptorContext = new InterceptorContext();
                        interceptorContext.putPrivateData(ComponentView.class, componentView);
                        interceptorContext.putPrivateData(Component.class, component);
                        clientPreDestroyInterceptor.processInvocation(interceptorContext);
                    } catch (Exception e) {
                        ROOT_LOGGER.preDestroyInterceptorFailure(e, component.getComponentClass());
                    }
                }

                @Override
                public Object getInstance() {
                    return proxy;
                }
            };
        }
    }
}

