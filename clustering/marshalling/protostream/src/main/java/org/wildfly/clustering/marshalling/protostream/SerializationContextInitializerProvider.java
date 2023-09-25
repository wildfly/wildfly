/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;

/**
 * @author Paul Ferraro
 */
public interface SerializationContextInitializerProvider extends SerializationContextInitializer {

    SerializationContextInitializer getInitializer();

    @Deprecated
    @Override
    default String getProtoFileName() {
        return this.getInitializer().getProtoFileName();
    }

    @Deprecated
    @Override
    default String getProtoFile() {
        return this.getInitializer().getProtoFile();
    }

    @Override
    default void registerSchema(SerializationContext context) {
        this.getInitializer().registerSchema(context);
    }

    @Override
    default void registerMarshallers(SerializationContext context) {
        this.getInitializer().registerMarshallers(context);
    }
}
