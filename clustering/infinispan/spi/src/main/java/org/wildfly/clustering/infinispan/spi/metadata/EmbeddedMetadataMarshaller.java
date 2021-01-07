/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.infinispan.spi.metadata;

import java.io.IOException;

import org.infinispan.container.versioning.NumericVersion;
import org.infinispan.container.versioning.SimpleClusteredVersion;
import org.infinispan.metadata.EmbeddedMetadata;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.clustering.marshalling.protostream.AutoSizedProtoStreamMarshaller;

/**
 * Marshaller for EmbeddedMetaData types.
 * @author Paul Ferraro
 */
public class EmbeddedMetadataMarshaller<MD extends EmbeddedMetadata> implements AutoSizedProtoStreamMarshaller<EmbeddedMetadata> {
    private Class<MD> targetClass;

    EmbeddedMetadataMarshaller(Class<MD> targetClass) {
        this.targetClass = targetClass;
    }

    @Override
    public EmbeddedMetadata readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        EmbeddedMetadata.Builder builder = new EmbeddedMetadata.Builder();
        int tag = reader.readTag();
        Long version = null;
        Integer topologyId = null;
        while (tag != 0) {
            int field = WireFormat.getTagFieldNumber(tag);
            switch (field) {
                case 1: {
                    version = reader.readSInt64();
                    break;
                }
                case 2: {
                    topologyId = reader.readSInt32();
                    break;
                }
                case 3: {
                    builder.lifespan(reader.readUInt64());
                    break;
                }
                case 4: {
                    builder.maxIdle(reader.readUInt64());
                    break;
                }
                default: {
                    reader.skipField(tag);
                }
            }
            tag = reader.readTag();
        }
        if (version != null) {
            builder.version((topologyId != null) ? new SimpleClusteredVersion(topologyId, version) : new NumericVersion(version));
        }
        return (EmbeddedMetadata) builder.build();
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, EmbeddedMetadata metadata) throws IOException {
        if (metadata.getClusteredVersion() != null) {
            writer.writeSInt64(1, metadata.getClusteredVersion().getVersion());
            writer.writeSInt32(2, metadata.getClusteredVersion().getTopologyId());
        } else if (metadata.getNumericVersion() != null) {
            writer.writeSInt64(1, metadata.getNumericVersion().getVersion());
        }
        if (metadata.lifespan() != -1) {
            writer.writeUInt64(3, metadata.lifespan());
        }
        if (metadata.maxIdle() != -1) {
            writer.writeUInt64(4, metadata.maxIdle());
        }
    }

    @Override
    public Class<? extends EmbeddedMetadata> getJavaClass() {
        return this.targetClass;
    }
}
