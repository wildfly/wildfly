/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.services.bootstrap;

import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.weld.injection.spi.ResourceReference;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;

/**
 * {@link ResourceReferenceFactory} backed by {@link ManagedReferenceFactory}.
 *
 * @author Jozef Hartinger
 *
 * @param <T>
 */
public class ManagedReferenceFactoryToResourceReferenceFactoryAdapter<T> implements ResourceReferenceFactory<T> {

    private final ManagedReferenceFactory factory;

    public ManagedReferenceFactoryToResourceReferenceFactoryAdapter(ManagedReferenceFactory factory) {
        this.factory = factory;
    }

    @Override
    public ResourceReference<T> createResource() {
        final ManagedReference instance = factory.getReference();
        return new ManagedReferenceToResourceReferenceAdapter<T>(instance);
    }
}
