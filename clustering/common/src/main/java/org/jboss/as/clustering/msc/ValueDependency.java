/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.msc;

import org.jboss.msc.value.Value;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * Service dependency that provides a value.
 * @author Paul Ferraro
 * @param <T> the dependency type
 * @deprecated Replaced by {@link SupplierDependency}.
 */
@Deprecated(forRemoval = true)
public interface ValueDependency<T> extends Value<T>, SupplierDependency<T> {

    @Override
    default T get() {
        return this.getValue();
    }
}
