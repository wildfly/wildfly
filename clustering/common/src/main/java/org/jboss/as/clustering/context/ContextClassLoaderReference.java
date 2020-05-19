/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.context;

import java.util.AbstractMap;
import java.util.Map;

import org.wildfly.security.ParametricPrivilegedAction;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Thread-aware reference for a context {@link ClassLoader}.
 * @author Paul Ferraro
 */
public enum ContextClassLoaderReference implements ThreadContextReference<ClassLoader> {
    INSTANCE;

    private static final ParametricPrivilegedAction<ClassLoader, Thread> GET_CONTEXT_CLASS_LOADER_ACTION = new ParametricPrivilegedAction<ClassLoader, Thread>() {
        @Override
        public ClassLoader run(Thread thread) {
            return thread.getContextClassLoader();
        }
    };

    private static final ParametricPrivilegedAction<Void, Map.Entry<Thread, ClassLoader>> SET_CONTEXT_CLASS_LOADER_ACTION = new ParametricPrivilegedAction<Void, Map.Entry<Thread, ClassLoader>>() {
        @Override
        public Void run(Map.Entry<Thread, ClassLoader> entry) {
            entry.getKey().setContextClassLoader(entry.getValue());
            return null;
        }
    };

    @Override
    public ClassLoader apply(Thread thread) {
        return WildFlySecurityManager.doUnchecked(thread, GET_CONTEXT_CLASS_LOADER_ACTION);
    }

    @Override
    public void accept(Thread thread, ClassLoader loader) {
        WildFlySecurityManager.doUnchecked(new AbstractMap.SimpleImmutableEntry<>(thread, loader), SET_CONTEXT_CLASS_LOADER_ACTION);
    }
}
