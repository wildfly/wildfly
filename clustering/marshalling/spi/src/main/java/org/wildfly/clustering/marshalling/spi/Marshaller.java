/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.marshalling.spi;

import java.io.IOException;

/**
 * Marshals an object to and from its serialized form.
 * @author Paul Ferraro
 * @param V the value type
 * @param S the serialized form type
 */
public interface Marshaller<V, S> extends Marshallability {

    /**
     * Reads a value from its marshalled form.
     * @param value the marshalled form
     * @return an unmarshalled value/
     */
    V read(S value) throws IOException;

    /**
     * Writes a value to its serialized form
     * @param a value to marshal.
     * @return the serialized form of the value
     */
    S write(V value) throws IOException;
}
