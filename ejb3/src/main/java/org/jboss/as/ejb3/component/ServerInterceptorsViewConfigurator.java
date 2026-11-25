/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ee.component.interceptors.UserInterceptorFactory;
import org.jboss.as.ejb3.interceptor.server.ServerInterceptorCache;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.invocation.Interceptors;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class ServerInterceptorsViewConfigurator implements ViewConfigurator {

    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];

    public static final ServerInterceptorsViewConfigurator INSTANCE = new ServerInterceptorsViewConfigurator();

    private ServerInterceptorsViewConfigurator() {
    }

    @Override
    public void configure(final DeploymentPhaseContext deploymentPhaseContext, final ComponentConfiguration componentConfiguration, final ViewDescription viewDescription, final ViewConfiguration viewConfiguration) throws DeploymentUnitProcessingException {
        final ComponentDescription componentDescription = componentConfiguration.getComponentDescription();
        if (!(componentDescription instanceof EJBComponentDescription)) {
            return;
        }
        final EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) componentDescription;
        final ServerInterceptorCache serverInterceptorCache = ejbComponentDescription.getServerInterceptorCache();
        if(serverInterceptorCache == null){
            return;
        }
        final List<InterceptorFactory> serverInterceptorsAroundInvoke = serverInterceptorCache.getServerInterceptorsAroundInvoke();
        final List<InterceptorFactory> serverInterceptorsAroundTimeout;
        if (ejbComponentDescription.isTimerServiceRequired()) {
            serverInterceptorsAroundTimeout = serverInterceptorCache.getServerInterceptorsAroundTimeout();
        } else {
            serverInterceptorsAroundTimeout = new ArrayList<>();
        }
        final List<Method> viewMethods = viewConfiguration.getProxyFactory().getCachedMethods();
        for (final Method method : viewMethods) {
            viewConfiguration.addViewInterceptor(method, new UserInterceptorFactory(weaved(serverInterceptorsAroundInvoke), weaved(serverInterceptorsAroundTimeout)), InterceptorOrder.View.USER_SPECIFIC_SERVER_INTERCEPTORS);
        }
    }

    private InterceptorFactory weaved(final Collection<InterceptorFactory> interceptorFactories) {
        return new InterceptorFactory() {
            @Override
            public Interceptor create(InterceptorFactoryContext context) {
                final Interceptor[] interceptors = new Interceptor[interceptorFactories.size()];
                final Iterator<InterceptorFactory> factories = interceptorFactories.iterator();
                for (int i = 0; i < interceptors.length; i++) {
                    interceptors[i] = factories.next().create(context);
                }
                return Interceptors.getWeavedInterceptor(interceptors);
            }
        };
    }
}
