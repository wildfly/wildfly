/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi.net;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetAddress;
import java.util.OptionalInt;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.IndexSerializer;

/**
 * @author Paul Ferraro
 */
public class InetAddressExternalizer<A extends InetAddress> implements Externalizer<A> {

    private final Class<A> targetClass;
    private final OptionalInt size;

    public InetAddressExternalizer(Class<A> targetClass, OptionalInt size) {
        this.targetClass = targetClass;
        this.size = size;
    }

    @Override
    public void writeObject(ObjectOutput output, A address) throws IOException {
        if (!this.size.isPresent()) {
            int length = (address != null) ? address.getAddress().length : 0;
            IndexSerializer.UNSIGNED_BYTE.writeInt(output, length);
        }
        if (address != null) {
            output.write(address.getAddress());
        }
    }

    @Override
    public A readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        int size = this.size.isPresent() ? this.size.getAsInt() : IndexSerializer.UNSIGNED_BYTE.readInt(input);
        if (size == 0) return null;
        byte[] bytes = new byte[size];
        input.readFully(bytes);
        return this.targetClass.cast(InetAddress.getByAddress(bytes));
    }

    @Override
    public Class<A> getTargetClass() {
        return this.targetClass;
    }

    @Override
    public OptionalInt size(A address) {
        if (this.size.isPresent()) return this.size;
        int length = (address != null) ? address.getAddress().length : 0;
        return OptionalInt.of(IndexSerializer.UNSIGNED_BYTE.size(length) + length);
    }
}
