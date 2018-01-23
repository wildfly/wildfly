/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
            IndexSerializer.UNSIGNED_BYTE.writeInt(output, (address != null) ? address.getAddress().length : 0);
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
}
