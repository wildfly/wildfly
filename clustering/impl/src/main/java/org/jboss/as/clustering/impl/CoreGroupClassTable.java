/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.impl;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.IdentityHashMap;
import java.util.Map;

import org.jboss.as.clustering.ClusterNode;
import org.jboss.as.clustering.lock.RemoteLockResponse;
import org.jboss.marshalling.ClassTable;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Unmarshaller;
import org.jgroups.blocks.MethodCall;
import org.jgroups.util.UUID;

/**
 * @author Stuart Douglas
 */
class CoreGroupClassTable implements ClassTable {

    public static final CoreGroupClassTable INSTANCE = new CoreGroupClassTable();

    private static final Class<?>[] classes = new Class<?>[] {
            ClusterNodeImpl.class,
            MethodCall.class,
            InetAddress.class,
            InetSocketAddress.class,
            SocketAddress.class,
            UUID.class,
            Serializable.class,
            ClusterNode.class,
            RemoteLockResponse.class,
            RemoteLockResponse.Flag.class,
    };

    private static final Map<Class<?>, Writer> writers = createWriters();
    private static Map<Class<?>, Writer> createWriters() {
        Map<Class<?>, Writer> writers = new IdentityHashMap<Class<?>, Writer>();
        for (int i = 0; i < classes.length; i++) {
            writers.put(classes[i], new ByteWriter((byte) i));
        }
        return writers;
    }

    @Override
    public Writer getClassWriter(final Class<?> clazz) throws IOException {
        return writers.get(clazz);
    }

    @Override
    public Class<?> readClass(final Unmarshaller unmarshaller) throws IOException, ClassNotFoundException {
        int index = unmarshaller.readUnsignedByte();
        if (index >= classes.length) {
            throw new ClassNotFoundException(String.format("ClassTable %s cannot find a class for class index %d", this.getClass().getName(), index));
        }
        return classes[index];
    }

    private static final class ByteWriter implements Writer {
        final byte[] bytes;

        ByteWriter(final byte... bytes) {
            this.bytes = bytes;
        }

        @Override
        public void writeClass(final Marshaller marshaller, final Class<?> clazz) throws IOException {
            marshaller.write(bytes);
        }
    }
}
