/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.stateful.sessionsynchronization;

import javax.ejb.ConcurrentAccessException;
import javax.ejb.EJBException;
import javax.ejb.SessionSynchronization;
import javax.ejb.Stateful;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Ryan Emerson
 */
@Stateful
public class SessionSynchBeanImpl implements SessionSynchronization {

    public static final AtomicInteger afterBegin = new AtomicInteger();
    public static final AtomicInteger afterCompletion = new AtomicInteger();

    private final AtomicInteger currentlyExecutedMethods = new AtomicInteger();
    private volatile String previousMethod = "";

    @Override
    public void afterBegin() throws EJBException {
        afterBegin.incrementAndGet();
        methodBlock("afterBegin");
    }

    @Override
    public void beforeCompletion() throws EJBException {
        methodBlock("beforeCompletion");
    }

    @Override
    public void afterCompletion(boolean committed) throws EJBException {
        afterCompletion.incrementAndGet();
        methodBlock("afterCompletion (" + committed + ")");
    }

    public void method1() {
        methodBlock("method1");
    }

    public void method2() {
        methodBlock("method2");
    }

    private void methodBlock(String methodName) {
        int i = currentlyExecutedMethods.getAndIncrement();
        if (i > 0)
            throw new ConcurrentAccessException(getExceptionMessage(i, methodName));

        try {
            Thread.sleep(5000);
        } catch (InterruptedException ignore) {
        }

        i = currentlyExecutedMethods.decrementAndGet();
        if (i > 0)
            throw new ConcurrentAccessException(getExceptionMessage(i, methodName));

        previousMethod = methodName;
    }

    private String getExceptionMessage(int count, String currentMethod) {
        String className = this.getClass().getSimpleName() + ".";
        return "Concurrent access detected: " + count + " method(s) were already in execution." + " Method 1 := "
                + className + previousMethod + ", Method 2 := " + className + currentMethod;
    }
}