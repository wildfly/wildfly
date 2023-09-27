/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.elytron;

import java.io.IOException;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.elytron.web.undertow.server.servlet.ServletSecurityContextImpl.IdentityContainer;
import org.wildfly.security.cache.CachedIdentity;

/**
 * Marshaller for an {@link IdentityContainer}.
 * @author Paul Ferraro
 */
public class IdentityContainerMarshaller implements ProtoStreamMarshaller<IdentityContainer> {

    private static final int IDENTITY_INDEX = 1;
    private static final int TYPE_INDEX = 2;

    @Override
    public Class<? extends IdentityContainer> getJavaClass() {
        return IdentityContainer.class;
    }

    @Override
    public IdentityContainer readFrom(ProtoStreamReader reader) throws IOException {
        CachedIdentity identity = null;
        String type = null;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case IDENTITY_INDEX:
                    identity = reader.readObject(CachedIdentity.class);
                    break;
                case TYPE_INDEX:
                    type = reader.readString();
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return new IdentityContainer(identity, type);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, IdentityContainer container) throws IOException {
        CachedIdentity identity = container.getSecurityIdentity();
        if (identity != null) {
            writer.writeObject(IDENTITY_INDEX, identity);
        }
        String type = container.getAuthType();
        if (type != null) {
            writer.writeString(TYPE_INDEX, type);
        }
    }
}
