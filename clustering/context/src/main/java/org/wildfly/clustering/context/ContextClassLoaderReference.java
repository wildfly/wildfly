/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.context;

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

    private static final ParametricPrivilegedAction<ClassLoader, Thread> GET_CONTEXT_CLASS_LOADER_ACTION = new ParametricPrivilegedAction<>() {
        @Override
        public ClassLoader run(Thread thread) {
            return thread.getContextClassLoader();
        }
    };

    private static final ParametricPrivilegedAction<Void, Map.Entry<Thread, ClassLoader>> SET_CONTEXT_CLASS_LOADER_ACTION = new ParametricPrivilegedAction<>() {
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
