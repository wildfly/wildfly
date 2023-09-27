/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.services.bootstrap;

import static org.jboss.weld.util.reflection.Reflections.cast;

import org.jboss.as.naming.ManagedReference;
import org.jboss.weld.injection.spi.ResourceReference;

/**
 * {@link ResourceReference} backed by {@link ManagedReference}.
 *
 * @author Jozef Hartinger
 *
 * @param <T>
 */
public class ManagedReferenceToResourceReferenceAdapter<T> implements ResourceReference<T> {

    private final ManagedReference reference;

    public ManagedReferenceToResourceReferenceAdapter(ManagedReference reference) {
        this.reference = reference;
    }

    @Override
    public T getInstance() {
        return cast(reference.getInstance());
    }

    @Override
    public void release() {
        reference.release();
    }
}
