/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.util;

import java.util.Collection;
import java.util.function.Function;

import org.wildfly.clustering.marshalling.protostream.FunctionalScalarMarshaller;
import org.wildfly.clustering.marshalling.protostream.Scalar;
import org.wildfly.common.function.Functions;

/**
 * Marshaller for singleton collections.
 * @author Paul Ferraro
 * @param <T> the collection type of this marshaller
 */
public class SingletonCollectionMarshaller<T extends Collection<Object>> extends FunctionalScalarMarshaller<T, Object> {

    public SingletonCollectionMarshaller(Function<Object, T> factory) {
        super(Scalar.ANY, Functions.constantSupplier(factory.apply(null)), collection -> collection.iterator().next(), factory::apply);
    }
}
