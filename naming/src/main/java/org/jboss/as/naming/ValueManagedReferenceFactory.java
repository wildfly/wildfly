/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.naming;

import java.io.Serializable;

import org.jboss.msc.value.Value;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A JNDI injectable which simply uses an MSC {@link Value}
 * to fetch the injected value, and takes no action when the value is returned.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Eduardo Martins
 */
public final class ValueManagedReferenceFactory implements ContextListAndJndiViewManagedReferenceFactory {
    private final Value<?> value;

    /**
     * Construct a new instance.
     *
     * @param value the value to wrap
     */
    public ValueManagedReferenceFactory(final Value<?> value) {
        this.value = value;
    }

    @Override
    public ManagedReference getReference() {
        return new ValueManagedReference(value.getValue());
    }

    @Override
    public String getInstanceClassName() {
        final Object instance = value != null ? value.getValue() : null;
        if(instance == null) {
            return ContextListManagedReferenceFactory.DEFAULT_INSTANCE_CLASS_NAME;
        }
        return instance.getClass().getName();
    }

    @Override
    public String getJndiViewInstanceValue() {
        final Object instance = value != null ? value.getValue() : null;
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
