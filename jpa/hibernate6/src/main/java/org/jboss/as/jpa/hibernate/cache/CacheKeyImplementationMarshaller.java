/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jpa.hibernate.cache;

import java.io.IOException;
import java.util.Objects;
import java.util.OptionalInt;

import org.hibernate.cache.internal.CacheKeyImplementation;
import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * ProtoStream marshaller for {@link CacheKeyImplementation}.
 * @author Paul Ferraro
 */
public class CacheKeyImplementationMarshaller implements ProtoStreamMarshaller<CacheKeyImplementation> {

    private static final int ID_INDEX = 1;
    private static final int ENTITY_INDEX = 2;
    private static final int TENANT_INDEX = 3;
    private static final int HASH_CODE_INDEX = 4;

    @Override
    public Class<? extends CacheKeyImplementation> getJavaClass() {
        return CacheKeyImplementation.class;
    }

    @Override
    public CacheKeyImplementation readFrom(ProtoStreamReader reader) throws IOException {
        Object id = null;
        String entity = null;
        String tenant = null;
        OptionalInt hashCode = OptionalInt.empty();
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case ID_INDEX:
                    id = reader.readAny();
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
                default:
                    reader.skipField(tag);
            }
        }
        return new CacheKeyImplementation(id, entity, tenant, hashCode.orElse(Objects.hashCode(id)));
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, CacheKeyImplementation key) throws IOException {
        Object id = key.getId();
        if (id != null) {
            writer.writeAny(ID_INDEX, id);
        }
        String entity = key.getEntityOrRoleName();
        if (entity != null) {
            writer.writeString(ENTITY_INDEX, entity);
        }
        String tenant = key.getTenantId();
        if (tenant != null) {
            writer.writeString(TENANT_INDEX, tenant);
        }
        int hashCode = key.hashCode();
        if (hashCode != Objects.hashCode(id)) {
            writer.writeSFixed32(HASH_CODE_INDEX, hashCode);
        }
    }
}
