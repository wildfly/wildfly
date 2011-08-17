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

package org.jboss.as.ejb3.component.session;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ejb3.PrimitiveClassLoaderUtil;
import org.jboss.as.ejb3.component.EJBBusinessMethod;
import org.jboss.as.ejb3.component.EJBComponentCreateService;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.deployment.EjbJarConfiguration;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.Interceptors;
import org.jboss.invocation.proxy.MethodIdentifier;

import javax.ejb.AccessTimeout;
import javax.ejb.LockType;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * User: jpai
 */
public abstract class SessionBeanComponentCreateService extends EJBComponentCreateService {

    private final Map<String, LockType> beanLevelLockType;

    private final Map<EJBBusinessMethod, LockType> methodApplicableLockTypes;

    private final Map<String, AccessTimeout> beanLevelAccessTimeout;

    private final Map<EJBBusinessMethod, AccessTimeout> methodApplicableAccessTimeouts;

    private final Map<Method, InterceptorFactory> timeoutInterceptors;
    private final Method timeoutMethod;

    /**
     * Construct a new instance.
     *
     * @param componentConfiguration the component configuration
     */
    public SessionBeanComponentCreateService(final ComponentConfiguration componentConfiguration, final EjbJarConfiguration ejbJarConfiguration) {
        super(componentConfiguration, ejbJarConfiguration);

        final SessionBeanComponentDescription sessionBeanComponentDescription = (SessionBeanComponentDescription) componentConfiguration.getComponentDescription();
        this.beanLevelLockType = sessionBeanComponentDescription.getBeanLevelLockType();
        Map<MethodIdentifier, LockType> methodLocks = sessionBeanComponentDescription.getMethodApplicableLockTypes();
        if (methodLocks == null) {
            this.methodApplicableLockTypes = Collections.emptyMap();
        } else {
            final Map<EJBBusinessMethod, LockType> locks = new HashMap<EJBBusinessMethod, LockType>();
            for (Map.Entry<MethodIdentifier, LockType> entry : methodLocks.entrySet()) {
                final MethodIdentifier ejbMethodDescription = entry.getKey();
                final EJBBusinessMethod ejbMethod = this.getEJBBusinessMethod(ejbMethodDescription);
                locks.put(ejbMethod, entry.getValue());
            }
            this.methodApplicableLockTypes = Collections.unmodifiableMap(locks);
        }

        this.beanLevelAccessTimeout = sessionBeanComponentDescription.getBeanLevelAccessTimeout();

        final Map<MethodIdentifier, AccessTimeout> methodAccessTimeouts = sessionBeanComponentDescription.getMethodApplicableAccessTimeouts();
        if (methodAccessTimeouts == null) {
            this.methodApplicableAccessTimeouts = Collections.emptyMap();
        } else {
            final Map<EJBBusinessMethod, AccessTimeout> accessTimeouts = new HashMap();
            for (Map.Entry<MethodIdentifier, AccessTimeout> entry : methodAccessTimeouts.entrySet()) {
                final MethodIdentifier ejbMethodDescription = entry.getKey();
                final EJBBusinessMethod ejbMethod = this.getEJBBusinessMethod(ejbMethodDescription);
                accessTimeouts.put(ejbMethod, entry.getValue());
            }
            this.methodApplicableAccessTimeouts = Collections.unmodifiableMap(accessTimeouts);
        }
        if (sessionBeanComponentDescription.isTimerServiceApplicable()) {
            Map<Method, InterceptorFactory> timeoutInterceptors = new IdentityHashMap<Method, InterceptorFactory>();
            for (Method method : componentConfiguration.getDefinedComponentMethods()) {
                timeoutInterceptors.put(method, Interceptors.getChainedInterceptorFactory(componentConfiguration.getAroundTimeoutInterceptors(method)));
            }
            this.timeoutInterceptors = timeoutInterceptors;
        } else {
            timeoutInterceptors = null;
        }
        this.timeoutMethod = sessionBeanComponentDescription.getTimeoutMethod();

        if(this.timeoutMethod != null) {
            processTxAttr(sessionBeanComponentDescription, MethodIntf.BEAN, this.timeoutMethod);
        }
        if(sessionBeanComponentDescription.getScheduleMethods() != null) {
            for(Method method : sessionBeanComponentDescription.getScheduleMethods().keySet()) {
            processTxAttr(sessionBeanComponentDescription, MethodIntf.BEAN, method);
            }
        }

    }

    public Map<String, LockType> getBeanLockType() {
        return this.beanLevelLockType;
    }

    public Map<EJBBusinessMethod, LockType> getMethodApplicableLockTypes() {
        return this.methodApplicableLockTypes;
    }

    public Map<EJBBusinessMethod, AccessTimeout> getMethodApplicableAccessTimeouts() {
        return this.methodApplicableAccessTimeouts;
    }

    public Map<String, AccessTimeout> getBeanAccessTimeout() {
        return this.beanLevelAccessTimeout;
    }

    private EJBBusinessMethod getEJBBusinessMethod(final MethodIdentifier method) {
        final ClassLoader classLoader = this.getComponentClass().getClassLoader();
        final String methodName = method.getName();
        final String[] types = method.getParameterTypes();
        if (types == null || types.length == 0) {
            return new EJBBusinessMethod(methodName);
        }
        Class<?>[] paramTypes = new Class<?>[types.length];
        int i = 0;
        for (String type : types) {
            try {
                paramTypes[i++] = PrimitiveClassLoaderUtil.loadClass(type, classLoader);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return new EJBBusinessMethod(methodName, paramTypes);
    }

    public Map<Method, InterceptorFactory> getTimeoutInterceptors() {
        return timeoutInterceptors;
    }

    public Method getTimeoutMethod() {
        return timeoutMethod;
    }
}
