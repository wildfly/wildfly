/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;

/**
 * Reads a value from a set of fields.
 * @author Paul Ferraro
 */
public interface FieldSetReader<T> {

    /**
     * Reads a value from a field of a field set.
     * @param current the current field value
     * @return the updated value.
     * @throws IOException if the value could not be read.
     */
    T readField(T current) throws IOException;

    /**
     * Indicates whether or not the specified index in contained in this field set.
     * @param index a field index
     * @return true, if the specified index in contained in this field set, false otherwise.
     */
    boolean contains(int index);
}
