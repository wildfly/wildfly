/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.persistence.jipijapa.hibernate7.cache;

import java.io.IOException;
import java.util.Objects;
import java.util.OptionalInt;

import org.hibernate.cache.internal.NaturalIdCacheKey;
import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * ProtoStream marshaller for {@link NaturalIdCacheKey}.
 * @author Paul Ferraro
 */
public class NaturalIdCacheKeyMarshaller implements ProtoStreamMarshaller<NaturalIdCacheKey> {

    private static final int VALUES_INDEX = 1;
    private static final int ENTITY_INDEX = 2;
    private static final int TENANT_INDEX = 3;
    private static final int HASH_CODE_INDEX = 4;

    @Override
    public Class<? extends NaturalIdCacheKey> getJavaClass() {
        return NaturalIdCacheKey.class;
    }

    @Override
    public NaturalIdCacheKey readFrom(ProtoStreamReader reader) throws IOException {
        Object values = null;
        String entity = null;
        String tenant = null;
        OptionalInt hashCode = OptionalInt.empty();
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case VALUES_INDEX:
                    values = reader.readAny();
                    break;
                case ENTITY_INDEX:
                    entity = reader.readString();
                    break;
                case TENANT_INDEX:
                    tenant = reader.readString();
                    break;
                case HASH_CODE_INDEX:
                    hashCode = OptionalInt.of(reader.readSFixed32());
                    break;
            }
        }
        return new NaturalIdCacheKey(values, entity, tenant, hashCode.orElse(Objects.hashCode(values)));
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, NaturalIdCacheKey key) throws IOException {
        Object values = key.getNaturalIdValues();
        if (values != null) {
            writer.writeAny(VALUES_INDEX, values);
        }
        String entity = key.getEntityName();
        if (entity != null) {
            writer.writeString(ENTITY_INDEX, entity);
        }
        String tenant = key.getTenantId();
        if (tenant != null) {
            writer.writeString(TENANT_INDEX, tenant);
        }
        int hashCode = key.hashCode();
        if (hashCode != Objects.hashCode(values)) {
            writer.writeSFixed32(HASH_CODE_INDEX, hashCode);
        }
    }
}
