/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.infinispan.marshalling;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.OptionalInt;

import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;

/**
 * An abstract user marshaller that delegates marshalling to a {@link ByteBufferMarshaller}.
 * @author Paul Ferraro
 */
public abstract class AbstractUserMarshaller extends AbstractMarshaller {

    private final ByteBufferMarshaller marshaller;

    public AbstractUserMarshaller(ByteBufferMarshaller marshaller) {
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
}
