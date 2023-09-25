/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.el.expressly;

import org.infinispan.protostream.SerializationContextInitializer;
import org.wildfly.clustering.el.expressly.lang.ELLangMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.ProviderSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializerProvider;

/**
 * @author Paul Ferraro
 */
public enum ELSerializationContextInitializerProvider implements SerializationContextInitializerProvider {

    EL(new ProviderSerializationContextInitializer<>("org.glassfish.expressly.proto", ELMarshallerProvider.class)),
    EL_LANG(new ProviderSerializationContextInitializer<>("org.glassfish.expressly.lang.proto", ELLangMarshallerProvider.class)),
    ;

    private final SerializationContextInitializer initializer;

    ELSerializationContextInitializerProvider(SerializationContextInitializer initializer) {
        this.initializer = initializer;
    }

    @Override
    public SerializationContextInitializer getInitializer() {
        return this.initializer;
    }
}
