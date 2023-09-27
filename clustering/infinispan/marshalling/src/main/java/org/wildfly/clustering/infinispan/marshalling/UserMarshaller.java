/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.marshalling;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.OptionalInt;

import org.infinispan.commons.dataconversion.MediaType;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;

/**
 * A user marshaller that delegates marshalling to a {@link ByteBufferMarshaller}.
 * @author Paul Ferraro
 */
public class UserMarshaller extends AbstractMarshaller {

    private final MediaType type;
    private final ByteBufferMarshaller marshaller;

    public UserMarshaller(MediaType type, ByteBufferMarshaller marshaller) {
        this.type = type;
        this.marshaller = marshaller;
    }

    @Override
    public int sizeEstimate(Object object) {
        OptionalInt size = this.marshaller.size(object);
        return size.isPresent() ? size.getAsInt() : super.sizeEstimate(object);
    }

    @Override
    public boolean isMarshallable(Object object) {
        return this.marshaller.isMarshallable(object);
    }

    @Override
    public Object readObject(InputStream input) throws ClassNotFoundException, IOException {
        return this.marshaller.readFrom(input);
    }

    @Override
    public void writeObject(Object object, OutputStream output) throws IOException {
        this.marshaller.writeTo(output, object);
    }

    @Override
    public MediaType mediaType() {
        return this.type;
    }
}
