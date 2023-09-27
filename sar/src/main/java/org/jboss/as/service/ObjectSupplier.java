/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.service;

import java.util.function.Supplier;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class ObjectSupplier implements Supplier<Object> {

    private final Object object;

    ObjectSupplier(final Object object) {
        this.object = object;
    }

    @Override
    public Object get() {
        return object;
    }

}
