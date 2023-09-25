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
final class ValueFactorySupplier extends DelegatingSupplier {

    private final String methodName;
    private final Class<?>[] paramTypes;
    private final Object[] args;

    ValueFactorySupplier(final String methodName, final Class<?>[] paramTypes, final Object[] args) {
        this.methodName = methodName;
        this.paramTypes = paramTypes;
        this.args = args;
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
        try {
            return ReflectionUtils.getMethod(o.getClass(), methodName, paramTypes).invoke(o, args);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Method is not accessible", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Failed to invoke method", e);
        }
    }

}
