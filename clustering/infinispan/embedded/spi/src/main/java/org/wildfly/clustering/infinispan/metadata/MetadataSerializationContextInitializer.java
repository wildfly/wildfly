/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.metadata;

import org.infinispan.metadata.EmbeddedMetadata;
import org.infinispan.metadata.EmbeddedMetadata.EmbeddedExpirableMetadata;
import org.infinispan.metadata.EmbeddedMetadata.EmbeddedLifespanExpirableMetadata;
import org.infinispan.metadata.EmbeddedMetadata.EmbeddedMaxIdleExpirableMetadata;
import org.infinispan.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;

/**
 * @author Paul Ferraro
 */
public class MetadataSerializationContextInitializer extends AbstractSerializationContextInitializer {

    public MetadataSerializationContextInitializer() {
        super("org.infinispan.metadata.proto");
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new EmbeddedMetadataMarshaller<>(EmbeddedMetadata.class));
        context.registerMarshaller(new EmbeddedMetadataMarshaller<>(EmbeddedExpirableMetadata.class));
        context.registerMarshaller(new EmbeddedMetadataMarshaller<>(EmbeddedLifespanExpirableMetadata.class));
        context.registerMarshaller(new EmbeddedMetadataMarshaller<>(EmbeddedMaxIdleExpirableMetadata.class));
    }
}
