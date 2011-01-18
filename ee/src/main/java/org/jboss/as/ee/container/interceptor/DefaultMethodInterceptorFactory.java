/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.container.interceptor;

import java.lang.reflect.Method;
import java.util.List;
import org.jboss.as.ee.container.BeanContainerConfiguration;
import org.jboss.as.ee.container.injection.ResourceInjection;
import org.jboss.as.server.deployment.DeploymentUnit;

/**
 * The default MethodInterceptorFactory instance.  Assumes all configurations use AroundInvoke methods.
 *
 * @author John Bailey
 */
public class DefaultMethodInterceptorFactory implements InterceptorFactory {

    public MethodInterceptor createMethodInterceptor(DeploymentUnit deploymentUnit, ClassLoader classLoader, MethodInterceptorConfiguration configuration, List<ResourceInjection> injections) throws Exception {
        final Class<?> interceptorClass = classLoader.loadClass(configuration.getInterceptorClassName());
        final Method aroundInvokeMethod;
        if(configuration.acceptsInvocationContext()) {
            aroundInvokeMethod = interceptorClass.getMethod(configuration.getMethodName(), javax.interceptor.InvocationContext.class);
        } else {
            aroundInvokeMethod = interceptorClass.getMethod(configuration.getMethodName());
        }
        return AroundInvokeInterceptor.create(interceptorClass, aroundInvokeMethod, configuration.getMethodFilter(), injections);
    }

    public LifecycleInterceptor createLifecycleInterceptor(final DeploymentUnit deploymentUnit, final ClassLoader classLoader, final BeanContainerConfiguration beanContainerConfig, final LifecycleInterceptorConfiguration configuration) throws Exception {
        final Class<?> interceptorClass = classLoader.loadClass(beanContainerConfig.getBeanClass());
        final Method lifecycleMethod = interceptorClass.getMethod(configuration.getMethodName());
        return new LifeCycleMethodInterceptor(lifecycleMethod);
    }
}
