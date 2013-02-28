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

package org.jboss.as.util.security;

import java.security.PrivilegedAction;

/**
 * A security action to create a thread.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class CreateThreadAction implements PrivilegedAction<Thread> {

    private final ThreadGroup group;
    private final Runnable target;
    private final String name;
    private final long stackSize;

    /**
     * Construct a new instance.
     *
     * @param name the name of the thread (may not be {@code null})
     */
    public CreateThreadAction(final String name) {
        this(null, null, name, 0L);
    }

    /**
     * Construct a new instance.
     *
     * @param group the thread group to use
     * @param name the name of the thread (may not be {@code null})
     */
    public CreateThreadAction(final ThreadGroup group, final String name) {
        this(group, null, name, 0L);
    }

    /**
     * Construct a new instance.
     *
     * @param target the runnable target
     * @param name the name of the thread (may not be {@code null})
     */
    public CreateThreadAction(final Runnable target, final String name) {
        this(null, target, name, 0L);
    }

    /**
     * Construct a new instance.
     *
     * @param group the thread group to use
     * @param target the runnable target
     * @param name the name of the thread (may not be {@code null})
     */
    public CreateThreadAction(final ThreadGroup group, final Runnable target, final String name) {
        this(group, target, name, 0L);
    }

    /**
     * Construct a new instance.
     *
     * @param group the thread group to use
     * @param target the runnable target
     * @param name the name of the thread (may not be {@code null})
     * @param stackSize the stack size to use
     */
    public CreateThreadAction(final ThreadGroup group, final Runnable target, final String name, final long stackSize) {
        this.group = group;
        this.target = target;
        this.name = name;
        this.stackSize = stackSize;
    }

    public Thread run() {
        return new Thread(group, target, name, stackSize);
    }
}
