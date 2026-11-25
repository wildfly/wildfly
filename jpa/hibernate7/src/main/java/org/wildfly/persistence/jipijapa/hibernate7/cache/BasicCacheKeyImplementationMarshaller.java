/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.persistence.jipijapa.hibernate7.cache;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;
import java.util.OptionalInt;

import org.hibernate.cache.internal.BasicCacheKeyImplementation;
import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * ProtoStream marshaller for {@link BasicCacheKeyImplementation}.
 * @author Paul Ferraro
 */
public class BasicCacheKeyImplementationMarshaller implements ProtoStreamMarshaller<BasicCacheKeyImplementation> {

    private static final int ID_INDEX = 1;
    private static final int ENTITY_INDEX = 2;
    private static final int HASH_CODE_INDEX = 3;

    @Override
    public Class<? extends BasicCacheKeyImplementation> getJavaClass() {
        return BasicCacheKeyImplementation.class;
    }

    @Override
    public BasicCacheKeyImplementation readFrom(ProtoStreamReader reader) throws IOException {
        Serializable id = null;
        String entity = null;
        OptionalInt hashCode = OptionalInt.empty();
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case ID_INDEX:
                    id = reader.readAny(Serializable.class);
                    break;
                case ENTITY_INDEX:
                    entity = reader.readString();
                    break;
                case HASH_CODE_INDEX:
                    hashCode = OptionalInt.of(reader.readSFixed32());
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return new BasicCacheKeyImplementation(id, entity, hashCode.orElse(Objects.hashCode(id)));
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, BasicCacheKeyImplementation key) throws IOException {
        Object id = key.getId();
        if (id != null) {
            writer.writeAny(ID_INDEX, id);
        }
        String entity = key.getEntityOrRoleName();
        if (entity != null) {
            writer.writeString(ENTITY_INDEX, entity);
        }
        int hashCode = key.hashCode();
        if (hashCode != Objects.hashCode(id)) {
            writer.writeSFixed32(HASH_CODE_INDEX, hashCode);
        }
    }
}
