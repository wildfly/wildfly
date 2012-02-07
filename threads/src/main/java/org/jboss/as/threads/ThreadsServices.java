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

package org.jboss.as.threads;

import org.jboss.msc.service.ServiceName;

/**
 * Utilities related to threa management services.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ThreadsServices {

    private ThreadsServices() {
    }

    public static final ServiceName THREAD = ServiceName.JBOSS.append("thread");
    public static final ServiceName FACTORY = THREAD.append("factory");
    public static final ServiceName EXECUTOR = THREAD.append("executor");

    /**
     * Standard implementation of {@link HandoffExecutorResolver} -- a {@link HandoffExecutorResolver.SimpleResolver} with a base service name
     * of {@link #EXECUTOR}.
     */
    public static final HandoffExecutorResolver STANDARD_HANDOFF_EXECUTOR_RESOLVER = new HandoffExecutorResolver.SimpleResolver(ThreadsServices.EXECUTOR);

    /**
     * Standard implementation of {@link ThreadFactoryResolver} -- a {@link ThreadFactoryResolver.SimpleResolver} with a base service name
     * of {@link #EXECUTOR}.
     */
    public static final ThreadFactoryResolver STANDARD_THREAD_FACTORY_RESOLVER = new ThreadFactoryResolver.SimpleResolver(ThreadsServices.FACTORY);

    public static ServiceName threadFactoryName(String name) {
        return FACTORY.append(name);
    }

    public static ServiceName executorName(final String name) {
        return EXECUTOR.append(name);
    }
}
