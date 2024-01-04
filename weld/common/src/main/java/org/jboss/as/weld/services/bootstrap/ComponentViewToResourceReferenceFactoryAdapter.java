/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.services.bootstrap;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.naming.ManagedReference;
import org.jboss.weld.injection.spi.ResourceReference;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;

/**
 * {@link ResourceReferenceFactory} backed by a {@link ComponentView}.
 *
 * @author Jozef Hartinger
 *
 * @param <T>
 */
public class ComponentViewToResourceReferenceFactoryAdapter<T> implements ResourceReferenceFactory<T> {

    private final ComponentView view;

    public ComponentViewToResourceReferenceFactoryAdapter(ComponentView view) {
        this.view = view;
    }

    @Override
    public ResourceReference<T> createResource() {
        try {
            final ManagedReference instance = view.createInstance();
            return new ManagedReferenceToResourceReferenceAdapter<T>(instance);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
