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
final class PropertySupplier extends DelegatingSupplier {

    private final String propertyName;

    PropertySupplier(final String propertyName) {
        this.propertyName = propertyName;
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
        if (propertyName != null) {
            try {
                return ReflectionUtils.getGetter(o.getClass(), propertyName).invoke(o, (Object[]) null);
            } catch (IllegalAccessException e) {
                throw SarLogger.ROOT_LOGGER.methodIsNotAccessible(e);
            } catch (InvocationTargetException e) {
                throw SarLogger.ROOT_LOGGER.failedToInvokeMethod(e);
            }

        } else {
            return o;
        }
    }

}
