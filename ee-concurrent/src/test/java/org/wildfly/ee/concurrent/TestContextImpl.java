/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.ee.concurrent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * @author Eduardo Martins
 */
public class TestContextImpl implements TestContextConfiguration.TestContext {

    public static final ConcurrentHashMap<SetData, Boolean> allContexts = new ConcurrentHashMap<>();
    static final ThreadLocal<SetData> threadLocal = new ThreadLocal<>();

    private final Object object;

    public TestContextImpl(Object object) {
        this.object = object;
    }

    static SetData getCurrent() {
        return threadLocal.get();
    }

    public TestContextConfiguration.TestContext set() {
        SetData previous = getCurrent();
        SetData current = new SetData();
        current.testContext = this;
        current.countDownLatch = new CountDownLatch(1);
        allContexts.put(current, Boolean.TRUE);
        threadLocal.set(current);
        ResetContext resetContext = new ResetContext();
        resetContext.context = this;
        resetContext.previous = previous;
        resetContext.current = current;
        return resetContext;
    }

    public Object getObject() {
        return object;
    }

    static class SetData {
        TestContextImpl testContext;
        CountDownLatch countDownLatch;
    }

    static interface Context {
        Context set();
    }

    static class ResetContext implements TestContextConfiguration.TestContext {

        TestContextImpl context;
        SetData previous;
        SetData current;

        public TestContextConfiguration.TestContext set() {
            if (threadLocal.get() != current) {
                throw new IllegalStateException();
            }
            threadLocal.set(previous);
            allContexts.remove(current);
            current.countDownLatch.countDown();
            return context;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestContextImpl that = (TestContextImpl) o;

        if (!object.equals(that.object)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return object.hashCode();
    }

}
