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
package org.wildfly.as.concurrent.context;

import javax.enterprise.concurrent.ManagedTaskListener;

/**
 * A ContextConfiguration can be used to create new Context instances, for different objects.
 *
 * @author Eduardo Martins
 */
public interface ContextConfiguration {

    /**
     * Creates a new Context for the specified task.
     *
     * @param task
     * @return
     */
    Context newTaskContext(Object task);

    /**
     * Creates a new Context for the specified listener.
     *
     * @param listener
     * @return
     */
    Context newManagedTaskListenerContext(ManagedTaskListener listener);

    /**
     * Creates a new Context for the specified object instance, to be used by a reflection proxy.
     *
     * @param instance
     * @return
     */
    Context newContextualProxyContext(Object instance);

    /**
     * Creates a new Context for the specified  task, to be used by a manageable thread.
     *
     * @param task
     * @return
     */
    Context newManageableThreadContext(Runnable task);

}
