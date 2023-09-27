/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
