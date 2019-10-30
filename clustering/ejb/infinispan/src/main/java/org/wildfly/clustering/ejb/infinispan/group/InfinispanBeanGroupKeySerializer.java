/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.ejb.infinispan.group;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.jboss.ejb.client.SessionID;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.ejb.infinispan.SessionIDSerializer;
import org.wildfly.clustering.infinispan.spi.persistence.BinaryKeyFormat;
import org.wildfly.clustering.infinispan.spi.persistence.KeyFormat;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.Serializer;
import org.wildfly.clustering.marshalling.spi.SerializerExternalizer;

/**
 * Serializer for an {@link InfinispanBeanGroupKey}.
 * @author Paul Ferraro
 */
public enum InfinispanBeanGroupKeySerializer implements Serializer<InfinispanBeanGroupKey<SessionID>> {
    INSTANCE;

    @Override
    public void write(DataOutput output, InfinispanBeanGroupKey<SessionID> key) throws IOException {
        SessionIDSerializer.INSTANCE.write(output, key.getId());
    }

    @Override
    public InfinispanBeanGroupKey<SessionID> read(DataInput input) throws IOException {
        return new InfinispanBeanGroupKey<>(SessionIDSerializer.INSTANCE.read(input));
    }

    @MetaInfServices(Externalizer.class)
    public static class InfinispanBeanGroupKeyExternalizer extends SerializerExternalizer<InfinispanBeanGroupKey<SessionID>> {
        @SuppressWarnings("unchecked")
        public InfinispanBeanGroupKeyExternalizer() {
            super((Class<InfinispanBeanGroupKey<SessionID>>) (Class<?>) InfinispanBeanGroupKey.class, INSTANCE);
        }
    }

    @MetaInfServices(KeyFormat.class)
    public static class InfinispanBeanGroupKeyFormat extends BinaryKeyFormat<InfinispanBeanGroupKey<SessionID>> {
        @SuppressWarnings("unchecked")
        public InfinispanBeanGroupKeyFormat() {
            super((Class<InfinispanBeanGroupKey<SessionID>>) (Class<?>) InfinispanBeanGroupKey.class, INSTANCE);
        }
    }
}
