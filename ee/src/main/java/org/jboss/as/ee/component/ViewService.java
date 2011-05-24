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

import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.Interceptors;
import org.jboss.invocation.SimpleInterceptorFactoryContext;
import org.jboss.invocation.proxy.ProxyFactory;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ViewService implements Service<ComponentView> {
    private static final Logger logger = Logger.getLogger(ViewService.class);
    private final InjectedValue<Component> componentInjector = new InjectedValue<Component>();
    private final Map<Method, InterceptorFactory> viewInterceptorFactories;
    private final Map<Method, InterceptorFactory> clientInterceptorFactories;
    private final InterceptorFactory viewPostConstruct;
    private final InterceptorFactory viewPreDestroy;
    private final InterceptorFactory clientPostConstruct;
    private final InterceptorFactory clientPreDestroy;
    private final ProxyFactory<?> proxyFactory;
    private final Set<Method> allowedMethods;
    private final Class<?> viewClass;
    private volatile ComponentView view;

    private static InterceptorFactory DESTROY_INTERCEPTOR = new ImmediateInterceptorFactory(new Interceptor() {
        public Object processInvocation(final InterceptorContext context) throws Exception {
            context.getPrivateData(ComponentViewInstance.class).destroy();
            return null;
        }
    });

    ViewService(final ViewConfiguration viewConfiguration) {
        viewClass = viewConfiguration.getViewClass();
        final ProxyFactory<?> proxyFactory = viewConfiguration.getProxyFactory();
        this.proxyFactory = proxyFactory;
        final Method[] methods = proxyFactory.getCachedMethods();
        final int methodCount = methods.length;
        viewPostConstruct = Interceptors.getChainedInterceptorFactory(viewConfiguration.getViewPostConstructInterceptors());
        viewPreDestroy = Interceptors.getChainedInterceptorFactory(viewConfiguration.getViewPreDestroyInterceptors());
        clientPostConstruct = Interceptors.getChainedInterceptorFactory(viewConfiguration.getClientPostConstructInterceptors());
        clientPreDestroy = Interceptors.getChainedInterceptorFactory(viewConfiguration.getClientPreDestroyInterceptors());
        final IdentityHashMap<Method, InterceptorFactory> viewInterceptorFactories = new IdentityHashMap<Method, InterceptorFactory>(methodCount);
        final IdentityHashMap<Method, InterceptorFactory> clientInterceptorFactories = new IdentityHashMap<Method, InterceptorFactory>(methodCount);
        for (Method method : methods) {
            if (method.getName().equals("finalize") && method.getParameterTypes().length == 0) {
                viewInterceptorFactories.put(method, DESTROY_INTERCEPTOR);
            } else {
                viewInterceptorFactories.put(method, Interceptors.getChainedInterceptorFactory(viewConfiguration.getViewInterceptors(method)));
                clientInterceptorFactories.put(method, Interceptors.getChainedInterceptorFactory(viewConfiguration.getClientInterceptors(method)));
            }
        }
        this.viewInterceptorFactories = viewInterceptorFactories;
        this.clientInterceptorFactories = clientInterceptorFactories;
        allowedMethods = Collections.unmodifiableSet(viewInterceptorFactories.keySet());
    }

    public void start(final StartContext context) throws StartException {
        // Construct the view
        view = new View();
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

        View() {
            component = componentInjector.getValue();
        }

        public ComponentViewInstance createInstance() {
            return createInstance(Collections.<Object, Object>emptyMap());
        }

        public ComponentViewInstance createInstance(Map<Object, Object> contextData) {
            final SimpleInterceptorFactoryContext factoryContext = new SimpleInterceptorFactoryContext();
            final Map<Method, InterceptorFactory> viewInterceptorFactories = ViewService.this.viewInterceptorFactories;
            final Map<Method, Interceptor> viewEntryPoints = new IdentityHashMap<Method, Interceptor>(viewInterceptorFactories.size());
            factoryContext.getContextData().put(Component.class, component);
            factoryContext.getContextData().put(ComponentView.class, this);
            factoryContext.getContextData().putAll(contextData);
            final Interceptor postConstructInterceptor = viewPostConstruct.create(factoryContext);

            for (Method method : viewInterceptorFactories.keySet()) {
                viewEntryPoints.put(method, viewInterceptorFactories.get(method).create(factoryContext));
            }
            final Interceptor preDestroyInterceptor = viewPreDestroy.create(factoryContext);

            final ComponentViewInstance instance = new ViewInstance(viewEntryPoints, preDestroyInterceptor);
            try {
                InterceptorContext context = new InterceptorContext();
                context.putPrivateData(ComponentView.class, this);
                context.putPrivateData(Component.class, component);
                postConstructInterceptor.processInvocation(context);
            } catch (Exception e) {
                // TODO: What is the best exception type to throw here?
                throw new RuntimeException("Failed to instantiate component view", e);
            }
            return instance;
        }

        public Component getComponent() {
            return component;
        }

        @Override
        public Class<?> getProxyClass() {
            return proxyFactory.defineClass();
        }

        class ViewInstance implements ComponentViewInstance {

            private final Map<Method, Interceptor> viewEntryPoints;
            private final Interceptor preDestroyInterceptor;

            ViewInstance(final Map<Method, Interceptor> viewEntryPoints, final Interceptor preDestroyInterceptor) {
                this.viewEntryPoints = viewEntryPoints;
                this.preDestroyInterceptor = preDestroyInterceptor;
            }

            public Component getComponent() {
                return component;
            }

            public Class<?> getViewClass() {
                return viewClass;
            }

            public Object createProxy() {
                final SimpleInterceptorFactoryContext factoryContext = new SimpleInterceptorFactoryContext();
                factoryContext.getContextData().put(Component.class, component);
                factoryContext.getContextData().put(ComponentView.class, View.this);
                factoryContext.getContextData().put(ComponentViewInstance.class, this);

                final Map<Method, InterceptorFactory> clientInterceptorFactories = ViewService.this.clientInterceptorFactories;
                final Map<Method, Interceptor> clientEntryPoints = new IdentityHashMap<Method, Interceptor>(clientInterceptorFactories.size());
                for (Method method : clientInterceptorFactories.keySet()) {
                    clientEntryPoints.put(method, clientInterceptorFactories.get(method).create(factoryContext));
                }
                final Interceptor postConstructInterceptor = clientPostConstruct.create(factoryContext);
                try {
                    Object object = proxyFactory.newInstance(new ProxyInvocationHandler(clientEntryPoints, component, View.this, this));
                    InterceptorContext interceptorContext = new InterceptorContext();
                    interceptorContext.putPrivateData(ComponentView.class, View.this);
                    interceptorContext.putPrivateData(ComponentViewInstance.class, this);
                    interceptorContext.putPrivateData(Component.class, component);
                    try {
                        postConstructInterceptor.processInvocation(interceptorContext);
                    } catch (Exception e) {
                        InstantiationException exception = new InstantiationException("Post-construct lifecycle failed");
                        exception.initCause(e);
                        throw exception;
                    }
                    return object;
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
            }

            public Collection<Method> allowedMethods() {
                return allowedMethods;
            }

            public Interceptor getEntryPoint(final Method method) throws IllegalArgumentException {
                Interceptor interceptor = viewEntryPoints.get(method);
                if (interceptor == null) {
                    throw new IllegalArgumentException("Invalid view entry point " + method);
                }
                return interceptor;
            }

            @Deprecated
            public boolean isAsynchronous(final Method method) throws IllegalArgumentException {
                return false;
            }

            public void destroy() {
                try {
                    InterceptorContext interceptorContext = new InterceptorContext();
                    interceptorContext.putPrivateData(ComponentView.class, View.this);
                    interceptorContext.putPrivateData(ComponentViewInstance.class, this);
                    interceptorContext.putPrivateData(Component.class, component);
                    preDestroyInterceptor.processInvocation(interceptorContext);
                } catch (Exception e) {
                    logger.warn("Exception while invoking pre-destroy interceptor for component class: " + this.getComponent().getComponentClass(), e);
                }
            }
        }
    }
}
