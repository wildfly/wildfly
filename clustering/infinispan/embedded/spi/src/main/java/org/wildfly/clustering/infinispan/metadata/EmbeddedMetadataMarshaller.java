/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.metadata;

import java.io.IOException;

import org.infinispan.container.versioning.NumericVersion;
import org.infinispan.container.versioning.SimpleClusteredVersion;
import org.infinispan.metadata.EmbeddedMetadata;
import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshaller for EmbeddedMetaData types.
 * @author Paul Ferraro
 */
public class EmbeddedMetadataMarshaller<MD extends EmbeddedMetadata> implements ProtoStreamMarshaller<EmbeddedMetadata> {

    private static final int VERSION_INDEX = 1;
    private static final int TOPOLOGY_INDEX = 2;
    private static final int LIFESPAN_INDEX = 3;
    private static final int MAX_IDLE_INDEX = 4;

    private Class<MD> targetClass;

    EmbeddedMetadataMarshaller(Class<MD> targetClass) {
        this.targetClass = targetClass;
    }

    @Override
    public EmbeddedMetadata readFrom(ProtoStreamReader reader) throws IOException {
        EmbeddedMetadata.Builder builder = new EmbeddedMetadata.Builder();
        Long version = null;
        Integer topologyId = null;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case VERSION_INDEX: {
                    version = reader.readSInt64();
                    break;
                }
                case TOPOLOGY_INDEX: {
                    topologyId = reader.readSInt32();
                    break;
                }
                case LIFESPAN_INDEX: {
                    builder.lifespan(reader.readUInt64());
                    break;
                }
                case MAX_IDLE_INDEX: {
                    builder.maxIdle(reader.readUInt64());
                    break;
                }
                default: {
                    reader.skipField(tag);
                }
            }
        }
        if (version != null) {
            builder.version((topologyId != null) ? new SimpleClusteredVersion(topologyId, version) : new NumericVersion(version));
        }
        return (EmbeddedMetadata) builder.build();
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, EmbeddedMetadata metadata) throws IOException {
        if (metadata.getClusteredVersion() != null) {
            writer.writeSInt64(VERSION_INDEX, metadata.getClusteredVersion().getVersion());
            writer.writeSInt32(TOPOLOGY_INDEX, metadata.getClusteredVersion().getTopologyId());
        } else if (metadata.getNumericVersion() != null) {
            writer.writeSInt64(VERSION_INDEX, metadata.getNumericVersion().getVersion());
        }
        if (metadata.lifespan() != -1) {
            writer.writeUInt64(LIFESPAN_INDEX, metadata.lifespan());
        }
        if (metadata.maxIdle() != -1) {
            writer.writeUInt64(MAX_IDLE_INDEX, metadata.maxIdle());
        }
    }

    @Override
    public Class<? extends EmbeddedMetadata> getJavaClass() {
        return this.targetClass;
    }
}
