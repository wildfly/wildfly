/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.context;

import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Factory for creating a {@link Contextualizer} for a {@link ClassLoader}.
 * @author Paul Ferraro
 */
public interface ContextualizerFactory {
    /**
     * Creates a {@link Contextualizer} for the specified {@link ClassLoader}.
     * @param loader a class loader
     * @return a contextualizer
     */
    Contextualizer createContextualizer(ClassLoader loader);

    /**
     * Creates a {@link Contextualizer} for the {@link ClassLoader} of the specified {@link Class}.
     * @param targetClass a class from which to obtain a class loader
     * @return a contextualizer
     */
    default Contextualizer createContextualizer(Class<?> targetClass) {
        return this.createContextualizer(WildFlySecurityManager.getClassLoaderPrivileged(targetClass));
    }
}
