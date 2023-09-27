/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.util.function.IntSupplier;

/**
 * Encapsulates an object reference.
 * @author Paul Ferraro
 */
public class Reference implements IntSupplier {
    private final int reference;

    public Reference(int reference) {
        this.reference = reference;
    }

    @Override
    public int getAsInt() {
        return this.reference;
    }

    @Override
    public int hashCode() {
        return this.reference;
    }

    @Override
    public boolean equals(Object object) {
        return (object instanceof Reference) ? this.reference == ((Reference) object).reference : false;
    }

    @Override
    public String toString() {
        return Integer.toString(this.reference);
    }
}
