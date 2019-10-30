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

package org.jboss.as.clustering.jgroups;

import java.security.PrivilegedAction;

import org.jgroups.util.ThreadFactory;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * {@link ThreadFactory} decorator that associates a specific class loader to created threads.
 * @author Paul Ferraro
 */
public class ClassLoaderThreadFactory implements org.jgroups.util.ThreadFactory {
    private final ThreadFactory factory;
    private final ClassLoader loader;

    public ClassLoaderThreadFactory(ThreadFactory factory, ClassLoader loader) {
        this.factory = factory;
        this.loader = loader;
    }

    @Override
    public Thread newThread(Runnable r) {
        return this.newThread(r, null);
    }

    @Override
    public Thread newThread(final Runnable r, String name) {
        Runnable task = () -> {
            try {
                r.run();
            } finally {
                // Defensively reset the TCCL
                this.setContextClassLoader(Thread.currentThread());
            }
        };
        return this.setContextClassLoader(this.factory.newThread(task, name));
    }

    private Thread setContextClassLoader(Thread thread) {
        PrivilegedAction<Thread> action = () -> {
            thread.setContextClassLoader(this.loader);
            return thread;
        };
        return WildFlySecurityManager.doUnchecked(action);
    }

    @Override
    public void setPattern(String pattern) {
        this.factory.setPattern(pattern);
    }

    @Override
    public void setIncludeClusterName(boolean includeClusterName) {
        this.factory.setIncludeClusterName(includeClusterName);
    }

    @Override
    public void setClusterName(String channelName) {
        this.factory.setClusterName(channelName);
    }

    @Override
    public void setAddress(String address) {
        this.factory.setAddress(address);
    }

    @Override
    public void renameThread(String base_name, Thread thread) {
        this.factory.renameThread(base_name, thread);
    }
}