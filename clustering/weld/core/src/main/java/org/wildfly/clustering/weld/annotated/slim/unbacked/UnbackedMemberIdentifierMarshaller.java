/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
