/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow;

import org.infinispan.protostream.SerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializerProvider;
import org.wildfly.clustering.web.undertow.elytron.ElytronUndertowSerializationContextInitializer;
import org.wildfly.clustering.web.undertow.elytron.SecurityCacheSerializationContextInitializer;

/**
 * @author Paul Ferraro
 */
public enum UndertowSerializationContextInitializerProvider implements SerializationContextInitializerProvider {

    SECURITY_CACHE(new SecurityCacheSerializationContextInitializer()),
    ELYTRON_UNDERTOW(new ElytronUndertowSerializationContextInitializer()),
    ;
    private final SerializationContextInitializer initializer;

    UndertowSerializationContextInitializerProvider(SerializationContextInitializer initializer) {
        this.initializer = initializer;
    }

    @Override
    public SerializationContextInitializer getInitializer() {
        return this.initializer;
    }
}
