/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi.util;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

import org.wildfly.clustering.marshalling.spi.ValueFunction;
import org.wildfly.clustering.marshalling.spi.SupplierFunction;
import org.wildfly.clustering.marshalling.spi.ValueExternalizer;

/**
 * Externalizer for unbounded implementations of {@link Collection}.
 * @author Paul Ferraro
 */
public class UnboundedCollectionExternalizer<T extends Collection<Object>> extends CollectionExternalizer<T, Void, Void> {

    public UnboundedCollectionExternalizer(Class<T> targetClass, Supplier<T> factory) {
        super(targetClass, new SupplierFunction<>(factory), Map.Entry::getKey, ValueFunction.voidFunction(), ValueExternalizer.VOID);
    }
}
