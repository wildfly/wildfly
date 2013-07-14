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

import javax.enterprise.concurrent.ManagedTaskListener;
import javax.enterprise.concurrent.Trigger;
import java.lang.reflect.InvocationHandler;
import java.util.concurrent.Callable;

/**
 * A ContextConfiguration can be used to create contextual objects, which are proxies which delegate the to another object, with a specific invocation context set.
 *
 * @author Eduardo Martins
 */
public interface ContextConfiguration {

    /**
     * Creates a new contextual Callable, which when invoked, will delegate to the specified Callable with the current invocation context.
     *
     * @param callable
     * @param <V>
     * @return
     */
    <V> Callable<V> newContextualCallable(Callable<V> callable);

    /**
     * Creates a new contextual Runnable, which when invoked, will delegate to the specified Runnable with the current invocation context.
     *
     * @param runnable
     * @return
     */
    Runnable newContextualRunnable(Runnable runnable);

    /**
     * Creates a new contextual ManagedTaskListener, which when invoked, will delegate to the specified ManagedTaskListener with the current invocation context.
     *
     * @param listener
     * @return
     */
    ManagedTaskListener newContextualManagedTaskListener(ManagedTaskListener listener);

    /**
     * Creates a new contextual InvocationHandler, which when invoked, will delegate to the specified object instance with the current invocation context.
     *
     * @param instance
     * @return
     */
    InvocationHandler newContextualInvocationHandler(Object instance);

    /**
     * Creates a new contextual Trigger, which when invoked, will delegate to the specified Trigger with the current invocation context.
     *
     * @param trigger
     * @return
     */
    Trigger newContextualTrigger(Trigger trigger);

    /**
     * Creates a new contextual Runnable for a ManageableThread, which when invoked, will delegate to the specified Runnable with the invocation context set. All Runnables created with method run with the same invocation context.
     *
     * @param runnable
     * @return
     */
    Runnable newManageableThreadContextualRunnable(Runnable runnable);

}
