/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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