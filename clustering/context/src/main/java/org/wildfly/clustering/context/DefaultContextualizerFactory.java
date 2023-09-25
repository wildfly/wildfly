/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.context;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @author Paul Ferraro
 *
 */
public enum DefaultContextualizerFactory implements ContextualizerFactory {
    INSTANCE;

    private final List<ContextualizerFactory> factories = new LinkedList<>();

    DefaultContextualizerFactory() {
        this.factories.add(new ClassLoaderContextualizerFactory());
        for (ContextualizerFactory factory : ServiceLoader.load(ContextualizerFactory.class, ContextualizerFactory.class.getClassLoader())) {
            this.factories.add(factory);
        }
    }

    @Override
    public Contextualizer createContextualizer(ClassLoader loader) {
        List<Contextualizer> contextualizers = new ArrayList<>(this.factories.size());
        for (ContextualizerFactory factory : this.factories) {
            contextualizers.add(factory.createContextualizer(loader));
        }
        return new CompositeContextualizer(contextualizers);
    }
}
