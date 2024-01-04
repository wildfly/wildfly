/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;

/**
 * Writes value as a set of fields.
 * @author Paul Ferraro
 */
public interface FieldSetWriter<T> {

    /**
     * Writes the specified value as a set of fields.
     * @param value the value to be written.
     * @throws IOException if the value could not be written
     */
    void writeFields(T value) throws IOException;
}
