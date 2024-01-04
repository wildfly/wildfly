/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import org.infinispan.protostream.descriptors.WireType;

/**
 * Marshaller for a single scalar value.
 * This marshaller does not write any tags, nor does it read beyond a single value.
 * @author Paul Ferraro
 * @param <T> the type of this marshaller
 */
public interface ScalarMarshaller<T> extends Marshallable<T> {

    /**
     * Returns the wire type of the scalar value written by this marshaller.
     * @return the wire type of the scalar value written by this marshaller.
     */
    WireType getWireType();
}
