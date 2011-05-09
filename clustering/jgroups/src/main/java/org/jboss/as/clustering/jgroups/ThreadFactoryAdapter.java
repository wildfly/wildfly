/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.jgroups;

import java.util.concurrent.ThreadFactory;


/**
 * Adapts a {@link ThreadFactory} to a {@link org.jgroups.util.ThreadFactory}.
 * @author Paul Ferraro
 */
public class ThreadFactoryAdapter implements org.jgroups.util.ThreadFactory {
    private final ThreadFactory factory;
    public ThreadFactoryAdapter(ThreadFactory factory) {
        this.factory = factory;
    }

    /**
     * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
     */
    @Override
    public Thread newThread(Runnable r) {
        return this.factory.newThread(r);
    }

    /**
     * @see org.jgroups.util.ThreadFactory#newThread(java.lang.Runnable, java.lang.String)
     */
    @Override
    public Thread newThread(Runnable r, String name) {
        return this.factory.newThread(r);
    }

    /**
     * @see org.jgroups.util.ThreadFactory#newThread(java.lang.ThreadGroup, java.lang.Runnable, java.lang.String)
     */
    @Override
    public Thread newThread(ThreadGroup group, Runnable r, String name) {
        return this.factory.newThread(r);
    }

    /**
     * @see org.jgroups.util.ThreadFactory#setPattern(java.lang.String)
     */
    @Override
    public void setPattern(String pattern) {
        // no-op
    }

    /**
     * @see org.jgroups.util.ThreadFactory#setIncludeClusterName(boolean)
     */
    @Override
    public void setIncludeClusterName(boolean includeClusterName) {
        // no-op
    }

    /**
     * @see org.jgroups.util.ThreadFactory#setClusterName(java.lang.String)
     */
    @Override
    public void setClusterName(String channelName) {
        // no-op
    }

    /**
     * @see org.jgroups.util.ThreadFactory#setAddress(java.lang.String)
     */
    @Override
    public void setAddress(String address) {
        // no-op
    }

    /**
     * @see org.jgroups.util.ThreadFactory#renameThread(java.lang.String, java.lang.Thread)
     */
    @Override
    public void renameThread(String base_name, Thread thread) {
        // no-op
    }
}
