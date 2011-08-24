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

package org.jboss.ejb3.concurrency;

import org.jboss.ejb3.concurrency.spi.LockableComponent;
import org.jboss.logging.Logger;

import javax.ejb.AccessTimeout;
import javax.ejb.ConcurrentAccessTimeoutException;
import javax.ejb.LockType;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * @author Jaikiran Pai
 */
public abstract class ContainerManagedConcurrencyInterceptor {

    /**
     * Logger
     */
    private static final Logger logger = Logger.getLogger(org.jboss.ejb3.concurrency.ContainerManagedConcurrencyInterceptor.class);

    /**
     * A spec compliant {@link org.jboss.ejb3.concurrency.EJBReadWriteLock}
     */
    private ReadWriteLock readWriteLock = new EJBReadWriteLock();

    protected abstract LockableComponent getLockableComponent();

    @AroundInvoke
    public Object invoke(InvocationContext invocationContext) throws Exception {
        LockableComponent lockableComponent = this.getLockableComponent();
        // get the invoked method
        Method invokedMethod = invocationContext.getMethod();
        if (invokedMethod == null) {
            throw new IllegalArgumentException("Invocation context: " + invocationContext + " cannot be processed because it's not applicable for a method invocation");
        }
        // get the Lock applicable for this method
        Lock lock = getLock(lockableComponent, invokedMethod);
        // the default access timeout (will be used in the absence of any explicit access timeout value for the invoked method)
        AccessTimeout defaultAccessTimeout = lockableComponent.getDefaultAccessTimeout();
        // set to the default values
        long time = defaultAccessTimeout.value();
        TimeUnit unit = defaultAccessTimeout.unit();

        AccessTimeout accessTimeoutOnMethod = lockableComponent.getAccessTimeout(invokedMethod);
        if (accessTimeoutOnMethod != null) {
            if (accessTimeoutOnMethod.value() < 0) {
                // for any negative value of timeout, we just default to max timeout val and max timeout unit.
                // violation of spec! But we don't want to wait indefinitely.
                logger.debug("Ignoring a negative @AccessTimeout value: " + accessTimeoutOnMethod.value() + " and timeout unit: "
                        + accessTimeoutOnMethod.unit().name() + ". Will default to timeout value: " + defaultAccessTimeout.value()
                        + " and timeout unit: " + defaultAccessTimeout.unit().name());
            } else {
                // use the explicit access timeout values specified on the method
                time = accessTimeoutOnMethod.value();
                unit = accessTimeoutOnMethod.unit();
            }
        }
        // try getting the lock
        boolean success = lock.tryLock(time, unit);
        if (!success) {
            throw new ConcurrentAccessTimeoutException("EJB 3.1 PFD2 4.8.5.5.1 concurrent access timeout on " + invocationContext
                    + " - could not obtain lock within " + time + unit.name());
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
        throw new IllegalStateException("Illegal lock type " + lockType + " on " + method + " for component " + lockableComponent);
    }
}
