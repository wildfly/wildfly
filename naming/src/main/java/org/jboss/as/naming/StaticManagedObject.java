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
