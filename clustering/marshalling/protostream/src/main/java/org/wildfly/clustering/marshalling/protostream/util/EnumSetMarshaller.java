/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.util;

import java.util.EnumSet;

import org.wildfly.clustering.marshalling.protostream.ProtoStreamBuilderFieldSetMarshaller;

/**
 * Marshaller for an {@link EnumSet}.
 * @author Paul Ferraro
 * @param <E> the enum type of this marshaller
 */
public class EnumSetMarshaller<E extends Enum<E>> extends ProtoStreamBuilderFieldSetMarshaller<EnumSet<E>, EnumSetBuilder<E>> {

    @SuppressWarnings("unchecked")
    public EnumSetMarshaller() {
        super((Class<EnumSet<E>>) (Class<?>) EnumSet.class, new EnumSetFieldSetMarshaller<>());
    }
}
