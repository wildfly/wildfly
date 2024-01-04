/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.web;

import org.infinispan.protostream.SerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.ProviderSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializerProvider;
import org.wildfly.clustering.weld.web.el.WebELMarshallerProvider;

/**
 * @author Paul Ferraro
 */
public enum WeldWebSerializationContextInitializerProvider implements SerializationContextInitializerProvider {
    WEB_EL(new ProviderSerializationContextInitializer<>("org.jboss.weld.module.web.el.proto", WebELMarshallerProvider.class))
    ;

    private final SerializationContextInitializer initializer;

    WeldWebSerializationContextInitializerProvider(SerializationContextInitializer initializer) {
        this.initializer = initializer;
    }

    @Override
    public SerializationContextInitializer getInitializer() {
        return this.initializer;
    }
}
