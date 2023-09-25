/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.context;

/**
 * {@link ContextualizerFactory} that creates a thread context class loader {@link Contextualizer}.
 * @author Paul Ferraro
 */
public class ClassLoaderContextualizerFactory implements ContextualizerFactory {

    @Override
    public Contextualizer createContextualizer(ClassLoader loader) {
        return new ContextReferenceExecutor<>(loader, ContextClassLoaderReference.INSTANCE);
    }
}
