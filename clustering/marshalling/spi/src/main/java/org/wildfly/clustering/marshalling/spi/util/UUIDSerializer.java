/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.UUID;

import org.wildfly.clustering.marshalling.spi.Serializer;

/**
 * {@link Serializer} for a {@link UUID}.
 * @author Paul Ferraro
 */
public enum UUIDSerializer implements Serializer<UUID> {
    INSTANCE;

    @Override
    public void write(DataOutput output, UUID value) throws IOException {
        output.writeLong(value.getMostSignificantBits());
        output.writeLong(value.getLeastSignificantBits());
    }

    @Override
    public UUID read(DataInput input) throws IOException {
        return new UUID(input.readLong(), input.readLong());
    }
}
