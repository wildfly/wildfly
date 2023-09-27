/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.faces.mojarra;

import org.infinispan.protostream.SerializationContextInitializer;
import org.wildfly.clustering.faces.mojarra.context.flash.ContextFlashMarshallerProvider;
import org.wildfly.clustering.faces.mojarra.facelets.el.FaceletsELMarshallerProvider;
import org.wildfly.clustering.faces.mojarra.util.UtilMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.ProviderSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializerProvider;

/**
 * @author Paul Ferraro
 */
public enum MojarraSerializationContextInitializerProvider implements SerializationContextInitializerProvider {

    CONTEXT_FLASH(new ProviderSerializationContextInitializer<>("com.sun.faces.context.flash.proto", ContextFlashMarshallerProvider.class)),
    FACELETS_EL(new ProviderSerializationContextInitializer<>("com.sun.faces.facelets.el.proto", FaceletsELMarshallerProvider.class)),
    UTIL(new ProviderSerializationContextInitializer<>("com.sun.faces.util.proto", UtilMarshallerProvider.class)),
    ;
    private final SerializationContextInitializer initializer;

    MojarraSerializationContextInitializerProvider(SerializationContextInitializer initializer) {
        this.initializer = initializer;
    }

    @Override
    public SerializationContextInitializer getInitializer() {
        return this.initializer;
    }
}
