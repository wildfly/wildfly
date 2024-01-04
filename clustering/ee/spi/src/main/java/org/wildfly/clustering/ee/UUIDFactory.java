/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * UUID factory implementations.
 * @author Paul Ferraro
 */
public enum UUIDFactory implements Supplier<UUID> {
    /**
     * UUID factory that uses a {@link ThreadLocalRandom}.
     * UUIDs generated by this factory are <em>not</em> cryptographically secure.
     */
    INSECURE() {
        @Override
        public java.util.UUID get() {
            byte[] data = new byte[16];
            ThreadLocalRandom.current().nextBytes(data);
            data[6] &= 0x0f; /* clear version */
            data[6] |= 0x40; /* set to version 4 */
            data[8] &= 0x3f; /* clear variant */
            data[8] |= 0x80; /* set to IETF variant */
            long msb = 0;
            long lsb = 0;
            for (int i = 0; i < 8; i++) {
               msb = (msb << 8) | (data[i] & 0xff);
            }
            for (int i = 8; i < 16; i++) {
               lsb = (lsb << 8) | (data[i] & 0xff);
            }
            return new UUID(msb, lsb);
        }
    },
    /**
     * UUID factory that uses a {@link java.security.SecureRandom}.
     * UUIDs generated by this factory are cryptographically secure.
     */
    SECURE() {
        @Override
        public java.util.UUID get() {
            return UUID.randomUUID();
        }
    },
    ;
}
