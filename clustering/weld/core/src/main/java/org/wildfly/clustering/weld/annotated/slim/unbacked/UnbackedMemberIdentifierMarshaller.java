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
package org.wildfly.clustering.weld.annotated.slim.unbacked;

import java.io.IOException;

import org.infinispan.protostream.descriptors.WireType;
import org.jboss.weld.annotated.slim.unbacked.UnbackedAnnotatedType;
import org.jboss.weld.annotated.slim.unbacked.UnbackedMemberIdentifier;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshaller for a {@link UnbackedMemberIdentifier}.
 * @author Paul Ferraro
 */
public class UnbackedMemberIdentifierMarshaller<X> implements ProtoStreamMarshaller<UnbackedMemberIdentifier<X>> {

    private static final int TYPE_INDEX = 1;
    private static final int MEMBER_ID_INDEX = 2;

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends UnbackedMemberIdentifier<X>> getJavaClass() {
        return (Class<UnbackedMemberIdentifier<X>>) (Class<?>) UnbackedMemberIdentifier.class;
    }

    @Override
    public UnbackedMemberIdentifier<X> readFrom(ProtoStreamReader reader) throws IOException {
        String memberId = null;
        UnbackedAnnotatedType<X> type = null;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case TYPE_INDEX:
                    type = reader.readObject(UnbackedAnnotatedType.class);
                    break;
                case MEMBER_ID_INDEX:
                    memberId = reader.readString();
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return new UnbackedMemberIdentifier<>(type, memberId);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, UnbackedMemberIdentifier<X> identifier) throws IOException {
        UnbackedAnnotatedType<X> type = identifier.getType();
        if (type != null) {
            writer.writeObject(TYPE_INDEX, type);
        }
        String memberId = identifier.getMemberId();
        if (memberId != null) {
            writer.writeString(MEMBER_ID_INDEX, memberId);
        }
    }
}
