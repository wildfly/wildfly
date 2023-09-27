/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.naming;

import java.io.Serializable;
import java.util.function.Supplier;

import org.jboss.msc.value.Value;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A JNDI injectable which simply uses a value and takes no action when the value is returned.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Eduardo Martins
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class ValueManagedReferenceFactory implements ContextListAndJndiViewManagedReferenceFactory {
    private final Supplier<?> value;

    /**
     * Construct a new instance.
     *
     * @param value the value to wrap
     * @deprecated use {@link ValueManagedReferenceFactory#ValueManagedReferenceFactory(Object)} instead. This constructor will be removed in the future.
     */
    @Deprecated
    public ValueManagedReferenceFactory(final Value<?> value) {
        this.value = () -> value.getValue();
    }

    /**
     * Construct a new instance.
     *
     * @param value the value to wrap
     */
    public ValueManagedReferenceFactory(final Object value) {
        this.value = () -> value;
    }

    @Override
    public ManagedReference getReference() {
        return new ValueManagedReference(value.get());
    }

    @Override
    public String getInstanceClassName() {
        final Object instance = value != null ? value.get() : null;
        if(instance == null) {
            return ContextListManagedReferenceFactory.DEFAULT_INSTANCE_CLASS_NAME;
        }
        return instance.getClass().getName();
    }

    @Override
    public String getJndiViewInstanceValue() {
        final Object instance = value != null ? value.get() : null;
        if (instance == null) {
            return "null";
        }
        final ClassLoader cl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(instance.getClass().getClassLoader());
            return instance.toString();
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(cl);
        }
    }

    public static class ValueManagedReference implements ManagedReference, Serializable {

        private static final long serialVersionUID = 1L;

        private ValueManagedReference(final Object instance) {
            this.instance = instance;
        }

        private volatile Object instance;

        @Override
        public void release() {
            this.instance = null;
        }

        @Override
        public Object getInstance() {
            return this.instance;
        }
    }
}
