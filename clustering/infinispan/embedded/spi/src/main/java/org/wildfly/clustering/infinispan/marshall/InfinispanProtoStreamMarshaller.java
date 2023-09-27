/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.marshall;

import java.util.function.UnaryOperator;

import org.wildfly.clustering.infinispan.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.infinispan.metadata.MetadataSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.ClassLoaderMarshaller;
import org.wildfly.clustering.marshalling.protostream.SerializationContextBuilder;

/**
 * @author Paul Ferraro
 */
public class InfinispanProtoStreamMarshaller extends ProtoStreamMarshaller {

    public InfinispanProtoStreamMarshaller(ClassLoaderMarshaller loaderMarshaller, UnaryOperator<SerializationContextBuilder> operator) {
        super(loaderMarshaller, new UnaryOperator<SerializationContextBuilder>() {
            @Override
            public SerializationContextBuilder apply(SerializationContextBuilder builder) {
                return operator.apply(builder.register(new MetadataSerializationContextInitializer()));
            }
        });
    }
}
