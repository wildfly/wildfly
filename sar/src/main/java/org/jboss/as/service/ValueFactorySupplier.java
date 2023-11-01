/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.service;

import org.jboss.as.service.logging.SarLogger;

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
            throw SarLogger.ROOT_LOGGER.objectSupplierNotAvailable();
        }
        final Object o = objectSupplier.get();
        if (o == null) {
            throw SarLogger.ROOT_LOGGER.objectNotAvailable();
        }
        try {
            return ReflectionUtils.getMethod(o.getClass(), methodName, paramTypes).invoke(o, args);
        } catch (IllegalAccessException e) {
            throw SarLogger.ROOT_LOGGER.methodIsNotAccessible(e);
        } catch (InvocationTargetException e) {
            throw SarLogger.ROOT_LOGGER.failedToInvokeMethod(e);
        }
    }

}
