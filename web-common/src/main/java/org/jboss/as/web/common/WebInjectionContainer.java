/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.web.common;

import java.lang.reflect.InvocationTargetException;

import org.jboss.as.ee.component.ComponentRegistry;

/**
 * The web injection container.
 *
 * @author Emanuel Muckenhuber
 * @author Paul Ferraro
 */
public interface WebInjectionContainer {

    void destroyInstance(Object instance);

    Object newInstance(String className) throws IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException;

    Object newInstance(Class<?> clazz) throws IllegalAccessException, InvocationTargetException, InstantiationException;

    void newInstance(Object arg0);

    default Object newInstance(String className, ClassLoader loader) throws IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException {
        return this.newInstance(loader.loadClass(className));
    }

    ComponentRegistry getComponentRegistry();
}
