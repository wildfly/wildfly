/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

/**
 * Builder used during {@link ProtoStreamMarshaller#readFrom(org.infinispan.protostream.ImmutableSerializationContext, org.infinispan.protostream.RawProtoStreamReader)}.
 * @author Paul Ferraro
 * @param <T> the target type of this builder
 */
public interface ProtoStreamBuilder<T> {
    /**
     * Builds an object read from a ProtoStream reader.
     * @return the built object
     */
    T build();
}
