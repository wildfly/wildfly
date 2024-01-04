/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.service;

import java.util.function.Supplier;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
abstract class DelegatingSupplier implements Supplier<Object> {

    protected volatile Supplier<Object> objectSupplier;

    void setObjectSupplier(final Supplier<Object> objectSupplier) {
        this.objectSupplier = objectSupplier;
    }

}
