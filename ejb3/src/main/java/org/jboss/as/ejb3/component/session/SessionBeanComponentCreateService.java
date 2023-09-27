/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.session;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import jakarta.ejb.LockType;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ejb3.PrimitiveClassLoaderUtil;
import org.jboss.as.ejb3.component.EJBBusinessMethod;
import org.jboss.as.ejb3.component.EJBComponentCreateService;
import org.jboss.as.ejb3.concurrency.AccessTimeoutDetails;
import org.jboss.as.ejb3.deployment.ApplicationExceptions;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.metadata.ejb.spec.MethodInterfaceType;
import org.jboss.msc.value.InjectedValue;

/**
 * User: jpai
 */
public abstract class SessionBeanComponentCreateService extends EJBComponentCreateService {

    private final Map<String, LockType> beanLevelLockType;
    private final Map<EJBBusinessMethod, LockType> methodApplicableLockTypes;
    private final Map<String, AccessTimeoutDetails> beanLevelAccessTimeout;
    private final Map<EJBBusinessMethod, AccessTimeoutDetails> methodApplicableAccessTimeouts;

    private final InjectedValue<ExecutorService> asyncExecutorService = new InjectedValue<ExecutorService>();

    /**
     * Construct a new instance.
     *
     * @param componentConfiguration the component configuration
     */
    public SessionBeanComponentCreateService(final ComponentConfiguration componentConfiguration, final ApplicationExceptions ejbJarConfiguration) {
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

        final Map<MethodIdentifier, AccessTimeoutDetails> methodAccessTimeouts = sessionBeanComponentDescription.getMethodApplicableAccessTimeouts();
        if (methodAccessTimeouts == null) {
            this.methodApplicableAccessTimeouts = Collections.emptyMap();
        } else {
            final Map<EJBBusinessMethod, AccessTimeoutDetails> accessTimeouts = new HashMap<EJBBusinessMethod, AccessTimeoutDetails>();
            for (Map.Entry<MethodIdentifier, AccessTimeoutDetails> entry : methodAccessTimeouts.entrySet()) {
                final MethodIdentifier ejbMethodDescription = entry.getKey();
                final EJBBusinessMethod ejbMethod = this.getEJBBusinessMethod(ejbMethodDescription);
                accessTimeouts.put(ejbMethod, entry.getValue());
            }
            this.methodApplicableAccessTimeouts = Collections.unmodifiableMap(accessTimeouts);
        }

        if (sessionBeanComponentDescription.getScheduleMethods() != null) {
            for (Method method : sessionBeanComponentDescription.getScheduleMethods().keySet()) {
                processTxAttr(sessionBeanComponentDescription, MethodInterfaceType.Timer, method);
            }
        }
        if (sessionBeanComponentDescription.getTimeoutMethod() != null) {
            this.processTxAttr(sessionBeanComponentDescription, MethodInterfaceType.Timer,
                    sessionBeanComponentDescription.getTimeoutMethod());
        }
    }

    public Map<String, LockType> getBeanLockType() {
        return this.beanLevelLockType;
    }

    public Map<EJBBusinessMethod, LockType> getMethodApplicableLockTypes() {
        return this.methodApplicableLockTypes;
    }

    public Map<EJBBusinessMethod, AccessTimeoutDetails> getMethodApplicableAccessTimeouts() {
        return this.methodApplicableAccessTimeouts;
    }

    public Map<String, AccessTimeoutDetails> getBeanAccessTimeout() {
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

    public InjectedValue<ExecutorService> getAsyncExecutorService() {
        return asyncExecutorService;
    }
}
