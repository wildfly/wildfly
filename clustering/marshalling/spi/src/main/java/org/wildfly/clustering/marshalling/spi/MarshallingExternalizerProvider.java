/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * @author Paul Ferraro
 */
public enum MarshallingExternalizerProvider implements ExternalizerProvider {
    MARSHALLED_KEY(new ByteBufferMarshalledKeyExternalizer()),
    MARSHALLED_VALUE(new ByteBufferMarshalledValueExternalizer()),
    ;
    private final Externalizer<?> externalizer;

    MarshallingExternalizerProvider(Externalizer<?> externalizer) {
        this.externalizer = externalizer;
    }

    @Override
    public Externalizer<?> getExternalizer() {
        return this.externalizer;
    }
}
