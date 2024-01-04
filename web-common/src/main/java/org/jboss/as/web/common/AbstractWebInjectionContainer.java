/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.web.common;

import java.lang.reflect.InvocationTargetException;

import org.jboss.as.ee.component.ComponentRegistry;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractWebInjectionContainer implements WebInjectionContainer {

    private final ClassLoader loader;
    private final ComponentRegistry registry;

    public AbstractWebInjectionContainer(ClassLoader loader, ComponentRegistry registry) {
        this.loader = loader;
        this.registry = registry;
    }

    @Override
    public Object newInstance(String className) throws IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException {
        return this.newInstance(className, this.loader);
    }

    @Override
    public ComponentRegistry getComponentRegistry() {
        return this.registry;
    }
}
