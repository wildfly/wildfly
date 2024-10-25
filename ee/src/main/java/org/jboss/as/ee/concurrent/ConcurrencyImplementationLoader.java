/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent;

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;

import java.util.ServiceLoader;

/**
 * ConcurrencyImplementation service loader.
 * @author emartins
 */
class ConcurrencyImplementationLoader {

    private ConcurrencyImplementationLoader() {
    }

    /**
     *
     * @return the implementation loaded using JBoss Module classloader, if found, otherwise uses current thread's class loader (needed for unit testing)
     */
    static ConcurrencyImplementation load() {
        try {
            final ModuleLoader moduleLoader = Module.getCallerModuleLoader();
            if (moduleLoader != null) {
                final Module module = moduleLoader.loadModule("org.wildfly.concurrency");
                if (module != null) {
                    return load(module.getClassLoader());
                }
            }
            return load(Thread.currentThread().getContextClassLoader());
        } catch (Throwable e) {
            throw EeLogger.ROOT_LOGGER.failedToLoadConcurrencyImplementation(e);
        }
    }

    /**
     *
     * @param classLoader
     * @return the implementation loaded using the specified classloader
     */
    static ConcurrencyImplementation load(ClassLoader classLoader) {
        final ConcurrencyImplementation concurrencyImplementation = ServiceLoader.load(ConcurrencyImplementation.class, classLoader).findFirst().get();
        EeLogger.ROOT_LOGGER.debugf("Jakarta Concurrency Implementation '%s' loaded.", concurrencyImplementation);
        return concurrencyImplementation;
    }
}
