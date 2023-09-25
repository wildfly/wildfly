/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.web.common;

import java.lang.reflect.InvocationTargetException;
import java.util.EnumSet;
import java.util.Map;

import org.jboss.as.ee.component.ComponentRegistry;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;

/**
 * A {@link WebInjectionContainer} that caches {@link ManagedReference} instances between {@link #newInstance(Object)} and {@link #destroyInstance(Object)}.
 * @author Emanuel Muckenhuber
 * @author Paul Ferraro
 */
public class CachingWebInjectionContainer extends AbstractWebInjectionContainer {

    private final Map<Object, ManagedReference> references;

    public CachingWebInjectionContainer(ClassLoader loader, ComponentRegistry componentRegistry) {
        super(loader, componentRegistry);
        this.references = new ConcurrentReferenceHashMap<>(256, ConcurrentReferenceHashMap.DEFAULT_LOAD_FACTOR,
                Runtime.getRuntime().availableProcessors(), ConcurrentReferenceHashMap.ReferenceType.STRONG,
                ConcurrentReferenceHashMap.ReferenceType.STRONG, EnumSet.of(ConcurrentReferenceHashMap.Option.IDENTITY_COMPARISONS));
    }

    @Override
    public void destroyInstance(Object instance) {
        final ManagedReference reference = this.references.remove(instance);
        if (reference != null) {
            reference.release();
        }
    }

    @Override
    public Object newInstance(Class<?> clazz) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        final ManagedReferenceFactory factory = this.getComponentRegistry().createInstanceFactory(clazz);
        ManagedReference reference = factory.getReference();
        if (reference != null) {
            this.references.put(reference.getInstance(), reference);
            return reference.getInstance();
        }
        return clazz.newInstance();
    }

    @Override
    public void newInstance(Object instance) {
        final ManagedReference reference = this.getComponentRegistry().createInstance(instance);
        if (reference != null) {
            this.references.put(instance, reference);
        }
    }
}
