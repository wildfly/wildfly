/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
            viewConfiguration.addViewInterceptor(method, new UserInterceptorFactory(weaved(serverInterceptorsAroundInvoke), weaved(serverInterceptorsAroundTimeout)), InterceptorOrder.View.USER_APP_SPECIFIC_CONTAINER_INTERCEPTORS);
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
