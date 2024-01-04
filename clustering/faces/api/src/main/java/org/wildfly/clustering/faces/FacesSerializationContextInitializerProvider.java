/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.faces;

import org.infinispan.protostream.SerializationContextInitializer;
import org.wildfly.clustering.faces.component.ComponentMarshallerProvider;
import org.wildfly.clustering.faces.view.ViewMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.ProviderSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializerProvider;

/**
 * @author Paul Ferraro
 */
public enum FacesSerializationContextInitializerProvider implements SerializationContextInitializerProvider {

    COMPONENT(new ProviderSerializationContextInitializer<>("jakarta.faces.component.proto", ComponentMarshallerProvider.class)),
    VIEW(new ProviderSerializationContextInitializer<>("jakarta.faces.view.proto", ViewMarshallerProvider.class)),
    ;
    private final SerializationContextInitializer initializer;

    FacesSerializationContextInitializerProvider(SerializationContextInitializer initializer) {
        this.initializer = initializer;
    }

    @Override
    public SerializationContextInitializer getInitializer() {
        return this.initializer;
    }
}
