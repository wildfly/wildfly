/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.as.service;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class PropertySupplier extends DelegatingSupplier {

    private final String propertyName;

    PropertySupplier(final String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    public Object get() {
        final Supplier<Object> objectSupplier = this.objectSupplier;
        if (objectSupplier == null) {
            throw new IllegalStateException("Object supplier not available");
        }
        final Object o = objectSupplier.get();
        if (o == null) {
            throw new IllegalStateException("Object not available");
        }
        if (propertyName != null) {
            try {
                return ReflectionUtils.getGetter(o.getClass(), propertyName).invoke(o, (Object[]) null);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Method is not accessible", e);
            } catch (InvocationTargetException e) {
                throw new IllegalStateException("Failed to invoke method", e);
            }

        } else {
            return o;
        }
    }

}
