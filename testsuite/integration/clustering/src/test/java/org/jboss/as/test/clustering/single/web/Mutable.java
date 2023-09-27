/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.single.web;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

public class Mutable implements Serializable {
    private static final long serialVersionUID = -5129400250276547619L;
    private transient boolean serialized = false;
    private final AtomicInteger value;

    public Mutable(int value) {
        this.value = new AtomicInteger(value);
    }

    public int increment() {
        return this.value.incrementAndGet();
    }

    @Override
    public String toString() {
        return String.valueOf(this.value.get());
    }

    public boolean wasSerialized() {
        return this.serialized;
    }

    @Override
    public int hashCode() {
        return this.value.get();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Mutable)) return false;
        return this.value.get() == ((Mutable) object).value.get();
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        this.serialized = true;
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.serialized = true;
    }
}