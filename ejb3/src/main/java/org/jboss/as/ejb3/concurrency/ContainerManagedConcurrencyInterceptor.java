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

package org.jboss.as.ejb3.concurrency;

import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.logging.Logger;

import javax.ejb.LockType;
import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import static org.jboss.as.ejb3.EjbMessages.MESSAGES;
import static org.jboss.as.ejb3.EjbLogger.ROOT_LOGGER;
/**
 * @author Jaikiran Pai
 */
public class ContainerManagedConcurrencyInterceptor implements Interceptor {

    /**
     * Logger
     */
    private static final Logger logger = Logger.getLogger(ContainerManagedConcurrencyInterceptor.class);

    /**
     * A spec compliant {@link org.jboss.as.ejb3.concurrency.EJBReadWriteLock}
     */
    private final ReadWriteLock readWriteLock = new EJBReadWriteLock();

    private final LockableComponent lockableComponent;

    public ContainerManagedConcurrencyInterceptor(LockableComponent component) {
        if (component == null) {
            throw MESSAGES.componentIsNull(LockableComponent.class.getName());
        }
        this.lockableComponent = component;
    }

    protected LockableComponent getLockableComponent() {
        return this.lockableComponent;
    }

    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        final InvocationContext invocationContext = context.getInvocationContext();
        LockableComponent lockableComponent = this.getLockableComponent();
        // get the invoked method
        Method invokedMethod = invocationContext.getMethod();
        if (invokedMethod == null) {
            throw MESSAGES.invocationNotApplicableForMethodInvocation(invocationContext);
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

                ROOT_LOGGER.debug("Ignoring a negative @AccessTimeout value: " + accessTimeoutOnMethod.getValue() + " and timeout unit: "
                        + accessTimeoutOnMethod.getTimeUnit().name() + ". Will default to timeout value: " + defaultAccessTimeout.getValue()
                        + " and timeout unit: " + defaultAccessTimeout.getTimeUnit().name());
            } else {
                // use the explicit access timeout values specified on the method
                time = accessTimeoutOnMethod.getValue();
                unit = accessTimeoutOnMethod.getTimeUnit();
            }
        }
        // try getting the lock
        boolean success = lock.tryLock(time, unit);
        if (!success) {
            throw MESSAGES.concurrentAccessTimeoutException(invocationContext,time + unit.name());
        }
        try {
            // lock obtained. now proceed!
            return invocationContext.proceed();
        } finally {
            lock.unlock();
        }
    }

    private Lock getLock(LockableComponent lockableComponent, Method method) {
        LockType lockType = lockableComponent.getLockType(method);
        switch (lockType) {
            case READ:
                return readWriteLock.readLock();
            case WRITE:
                return readWriteLock.writeLock();
        }
        throw MESSAGES.failToObtainLockIllegalType(lockType,method,lockableComponent);
    }

}
