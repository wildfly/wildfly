/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.singleton;

import org.jboss.as.ejb3.concurrency.AccessTimeoutDetails;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;

import jakarta.ejb.LockType;
import jakarta.interceptor.InvocationContext;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import static org.jboss.as.ejb3.logging.EjbLogger.ROOT_LOGGER;
/**
 * @author Jaikiran Pai
 */
class ContainerManagedConcurrencyInterceptor implements Interceptor {


    private final SingletonComponent lockableComponent;

    private final Map<Method, Method> viewMethodToComponentMethodMap;

    public ContainerManagedConcurrencyInterceptor(SingletonComponent component, Map<Method, Method> viewMethodToComponentMethodMap) {
        this.viewMethodToComponentMethodMap = viewMethodToComponentMethodMap;
        if (component == null) {
            throw EjbLogger.ROOT_LOGGER.componentIsNull(SingletonComponent.class.getName());
        }
        this.lockableComponent = component;
    }

    protected SingletonComponent getLockableComponent() {
        return this.lockableComponent;
    }

    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        final InvocationContext invocationContext = context.getInvocationContext();
        SingletonComponent lockableComponent = this.getLockableComponent();
        // get the invoked method
        Method method = invocationContext.getMethod();
        if (method == null) {
            throw EjbLogger.ROOT_LOGGER.invocationNotApplicableForMethodInvocation(invocationContext);
        }
        Method invokedMethod = viewMethodToComponentMethodMap.get(method);
        if(invokedMethod == null) {
            invokedMethod = method;
        }
        // get the Lock applicable for this method
        Lock lock = getLock(lockableComponent, invokedMethod);
        // the default access timeout (will be used in the absence of any explicit access timeout value for the invoked method)
        AccessTimeoutDetails defaultAccessTimeout = lockableComponent.getDefaultAccessTimeout();
        // set to the default values
        long time = defaultAccessTimeout.getValue();
        TimeUnit unit = defaultAccessTimeout.getTimeUnit();

        AccessTimeoutDetails accessTimeoutOnMethod = lockableComponent.getAccessTimeout(invokedMethod);
        if (accessTimeoutOnMethod != null) {
            if (accessTimeoutOnMethod.getValue() < 0) {
                // for any negative value of timeout, we just default to max timeout val and max timeout unit.
                // violation of spec! But we don't want to wait indefinitely.

                if (ROOT_LOGGER.isDebugEnabled()) {
                    ROOT_LOGGER.debug("Ignoring a negative @AccessTimeout value: " + accessTimeoutOnMethod.getValue()
                            + " and timeout unit: " + accessTimeoutOnMethod.getTimeUnit().name()
                            + ". Will default to timeout value: " + defaultAccessTimeout.getValue() + " and timeout unit: "
                            + defaultAccessTimeout.getTimeUnit().name());
                }
            } else {
                // use the explicit access timeout values specified on the method
                time = accessTimeoutOnMethod.getValue();
                unit = accessTimeoutOnMethod.getTimeUnit();
            }
        }
        // try getting the lock
        boolean success = lock.tryLock(time, unit);
        if (!success) {
            throw EjbLogger.ROOT_LOGGER.concurrentAccessTimeoutException(lockableComponent.getComponentName(), time + unit.name());
        }
        try {
            // lock obtained. now proceed!
            return invocationContext.proceed();
        } finally {
            lock.unlock();
        }
    }

    private Lock getLock(SingletonComponent lockableComponent, Method method) {
        LockType lockType = lockableComponent.getLockType(method);
        switch (lockType) {
            case READ:
                return lockableComponent.getLock().readLock();
            case WRITE:
                return lockableComponent.getLock().writeLock();
        }
        throw EjbLogger.ROOT_LOGGER.failToObtainLockIllegalType(lockType, method, lockableComponent);
    }

}
