/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;

/**
 * Readable view of a marshaller.
 * @author Paul Ferraro
 */
public interface Readable<T> {

    /**
     * Reads an object from the specified reader.
     * @param reader a ProtoStream reader
     * @return the read object
     * @throws IOException if the object could not be read
     */
    T readFrom(ProtoStreamReader reader) throws IOException;
}
