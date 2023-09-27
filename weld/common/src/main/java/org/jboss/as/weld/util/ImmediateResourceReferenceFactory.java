/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.weld.util;

import org.jboss.weld.injection.spi.ResourceReference;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;
import org.jboss.weld.injection.spi.helpers.SimpleResourceReference;

/**
 * @author Stuart Douglas
 */
public class ImmediateResourceReferenceFactory<T> implements ResourceReferenceFactory<T> {
    private final T instance;

    public ImmediateResourceReferenceFactory(final T instance) {
        this.instance = instance;
    }

    @Override
    public ResourceReference<T> createResource() {
        return new SimpleResourceReference<T>(instance);
    }
}
