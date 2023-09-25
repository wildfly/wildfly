/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.naming;

import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;

/**
 * A n adaptor between value injectors and ManagedReferenceFactory
 *
 * @author Stuart Douglas
 */
public class ManagedReferenceInjector<T> implements Injector<T> {

    private final Injector<ManagedReferenceFactory> injectable;

    public ManagedReferenceInjector(Injector<ManagedReferenceFactory> injectable) {
        this.injectable = injectable;
    }

    @Override
    public void inject(T value) throws InjectionException {
        injectable.inject(new ValueManagedReferenceFactory(value));
    }

    @Override
    public void uninject() {
        injectable.uninject();
    }
}
