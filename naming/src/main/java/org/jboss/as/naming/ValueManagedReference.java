/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.naming;

import java.util.function.Supplier;

import org.jboss.msc.value.Value;

/**
 * A ManagedReference that simply holds a value.
 *
 * @author Stuart Douglas
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class ValueManagedReference implements ManagedReference {
    private final Supplier<?> value;

    /**
     * Construct a new instance.
     *
     * @param value the value to wrap
     * @deprecated use {@link ValueManagedReference#ValueManagedReference(Object)} instead. This constructor will be removed in the future.
     */
    @Deprecated
    public ValueManagedReference(final Value<?> value) {
        this.value = () -> value.getValue();
    }

    /**
     * Construct a new instance.
     *
     * @param value the value to wrap
     */
    public ValueManagedReference(final Object value) {
        this.value = () -> value;
    }

    @Override
    public void release() {

    }

    @Override
    public Object getInstance() {
        return value.get();
    }
}
