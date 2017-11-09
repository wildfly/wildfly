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

package org.jboss.as.ejb3.component.singleton;

import org.jboss.as.ejb3.concurrency.AccessTimeoutDetails;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;

import javax.ejb.LockType;
import javax.interceptor.InvocationContext;
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
