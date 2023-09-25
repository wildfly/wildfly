/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.web.common;

import java.lang.reflect.InvocationTargetException;

import org.jboss.as.ee.component.ComponentRegistry;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;

/**
 * A {@link WebInjectionContainer} implementation for use with distributable web applications that does not cache {@link ManagedReference} instances.
 * @author Paul Ferraro
 */
public class SimpleWebInjectionContainer extends AbstractWebInjectionContainer {

    public SimpleWebInjectionContainer(ClassLoader loader, ComponentRegistry componentRegistry) {
        super(loader, componentRegistry);
    }

    @Override
    public void destroyInstance(Object instance) {
        ManagedReference reference = this.getComponentRegistry().getInstance(instance);
        if (reference != null) {
            reference.release();
        }
    }

    @Override
    public Object newInstance(Class<?> clazz) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        final ManagedReferenceFactory factory = this.getComponentRegistry().createInstanceFactory(clazz);
        ManagedReference reference = factory.getReference();
        if (reference != null) {
            return reference.getInstance();
        }
        return clazz.newInstance();
    }

    @Override
    public void newInstance(Object instance) {
        this.getComponentRegistry().createInstance(instance);
    }
}
