/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.marshalling.spi.util;

import java.util.OptionalInt;
import java.util.UUID;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.SerializerExternalizer;

/**
 * {@link Externalizer} for {@link UUID} instances.
 * @author Paul Ferraro
 */
public class UUIDExternalizer extends SerializerExternalizer<UUID> {

    public UUIDExternalizer() {
        super(UUID.class, UUIDSerializer.INSTANCE);
    }

    @Override
    public OptionalInt size(UUID object) {
        return OptionalInt.of(Long.BYTES + Long.BYTES);
    }
}
