/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.group;

import java.io.IOException;

import org.jgroups.util.UUID;
import org.wildfly.clustering.marshalling.protostream.FunctionalMarshaller;
import org.wildfly.common.function.ExceptionFunction;

/**
 * Marshaller for a {@link UUID} address.
 * @author Paul Ferraro
 */
public class UUIDMarshaller extends FunctionalMarshaller<UUID, java.util.UUID> {

    private static final ExceptionFunction<UUID, java.util.UUID, IOException> FUNCTION = new ExceptionFunction<>() {
        @Override
        public java.util.UUID apply(UUID uuid) throws IOException {
            return new java.util.UUID(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
        }
    };
    private static final ExceptionFunction<java.util.UUID, UUID, IOException> FACTORY = new ExceptionFunction<>() {
        @Override
        public UUID apply(java.util.UUID uuid) throws IOException {
            return new UUID(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
        }
    };

    public UUIDMarshaller() {
        super(UUID.class, java.util.UUID.class, FUNCTION, FACTORY);
    }
}
