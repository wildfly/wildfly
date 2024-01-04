/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.function;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * @author Paul Ferraro
 */
public class CollectionFunctionMarshaller<V, C extends Collection<V>, F extends CollectionFunction<V, C>> implements ProtoStreamMarshaller<F> {
    private static final int ELEMENT_INDEX = 1;

    private final Class<? extends F> targetClass;
    private final Function<Collection<V>, F> factory;

    public CollectionFunctionMarshaller(Class<? extends F> targetClass, Function<Collection<V>, F> factory) {
        this.targetClass = targetClass;
        this.factory = factory;
    }

    @Override
    public Class<? extends F> getJavaClass() {
        return this.targetClass;
    }

    @SuppressWarnings("unchecked")
    @Override
    public F readFrom(ProtoStreamReader reader) throws IOException {
        List<V> operand = new LinkedList<>();
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case ELEMENT_INDEX:
                    operand.add((V) reader.readAny());
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return this.factory.apply(operand);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, F function) throws IOException {
        for (V value : function.getOperand()) {
            writer.writeAny(ELEMENT_INDEX, value);
        }
    }
}
