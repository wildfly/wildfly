/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.naming;

import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A JNDI injectable which returns a static object and takes no action when the value is returned.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Eduardo Martins
 */
public final class StaticManagedObject implements ContextListManagedReferenceFactory,JndiViewManagedReferenceFactory {
    private final Object value;

    /**
     * Construct a new instance.
     *
     * @param value the value to wrap
     */
    public StaticManagedObject(final Object value) {
        this.value = value;
    }

    @Override
    public ManagedReference getReference() {
        return new ManagedReference() {
            @Override
            public void release() {

            }

            @Override
            public Object getInstance() {
                return value;
            }
        };
    }

    @Override
    public String getInstanceClassName() {
        return value != null ? value.getClass().getName() : ContextListManagedReferenceFactory.DEFAULT_INSTANCE_CLASS_NAME;
    }

    @Override
    public String getJndiViewInstanceValue() {
        if (value == null) {
            return "null";
        }
        final ClassLoader cl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(value.getClass().getClassLoader());
            return value.toString();
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(cl);
        }
    }

}
