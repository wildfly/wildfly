/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
