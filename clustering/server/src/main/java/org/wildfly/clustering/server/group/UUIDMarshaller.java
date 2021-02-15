/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.server.group;

import java.io.IOException;

import org.jgroups.util.UUID;
import org.wildfly.clustering.marshalling.protostream.FunctionalMarshaller;
import org.wildfly.common.function.ExceptionFunction;

/**
 * Marshaller for a {@link UUID} address.
 * @author Paul Ferraro
 */
public class UUIDMarshaller extends FunctionalMarshaller<UUID, java.util.UUID> {

    private static final ExceptionFunction<UUID, java.util.UUID, IOException> FUNCTION = new ExceptionFunction<UUID, java.util.UUID, IOException>() {
        @Override
        public java.util.UUID apply(UUID uuid) throws IOException {
            return new java.util.UUID(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
        }
    };
    private static final ExceptionFunction<java.util.UUID, UUID, IOException> FACTORY = new ExceptionFunction<java.util.UUID, UUID, IOException>() {
        @Override
        public UUID apply(java.util.UUID uuid) throws IOException {
            return new UUID(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
        }
    };

    public UUIDMarshaller() {
        super(UUID.class, java.util.UUID.class, FUNCTION, FACTORY);
    }
}
