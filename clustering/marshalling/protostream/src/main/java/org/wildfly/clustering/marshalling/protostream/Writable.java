/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;

/**
 * Writable view of a marshaller.
 * @author Paul Ferraro
 */
public interface Writable<T> {

    /**
     * Writes the specified object to the specified writer.
     * @param writer a ProtoStream writer
     * @param value the object to be written
     * @throws IOException if the object could not be written
     */
    void writeTo(ProtoStreamWriter writer, T value) throws IOException;
}
