/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.infinispan.spi;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.infinispan.commons.io.ByteBufferImpl;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.IndexSerializer;

/**
 * Externalizer for a {@link ByteBufferImpl}.
 * @author Paul Ferraro
 */
@MetaInfServices(Externalizer.class)
public class ByteBufferExternalizer implements Externalizer<ByteBufferImpl> {

    @Override
    public void writeObject(ObjectOutput output, ByteBufferImpl buffer) throws IOException {
        IndexSerializer.VARIABLE.writeInt(output, buffer.getLength());
        output.write(buffer.getBuf(), buffer.getOffset(), buffer.getLength());
    }

    @Override
    public ByteBufferImpl readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        byte[] bytes = new byte[IndexSerializer.VARIABLE.readInt(input)];
        input.readFully(bytes);
        return new ByteBufferImpl(bytes);
    }

    @Override
    public Class<ByteBufferImpl> getTargetClass() {
        return ByteBufferImpl.class;
    }
}
