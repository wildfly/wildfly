/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import org.infinispan.protostream.descriptors.WireType;

/**
 * Marshaller for a field.
 * {@link #writeTo(ProtoStreamWriter, Object)} does not write a field tag, but may write additional tagged fields.
 * Likewise, {@link #readFrom(ProtoStreamReader)} will continue to read fields until a zero tag is reached.
 * @author Paul Ferraro
 * @param <T> the type of this marshaller
 */
public interface FieldMarshaller<T> extends Marshallable<T> {

    /**
     * Returns the wire type of the scalar value written by this marshaller.
     * @return the wire type of the scalar value written by this marshaller.
     */
    WireType getWireType();
}
