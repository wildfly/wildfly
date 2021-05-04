/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.ejb.infinispan.bean;

import java.io.IOException;
import java.time.Instant;

import org.infinispan.protostream.descriptors.WireType;
import org.jboss.ejb.client.SessionID;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * @author Paul Ferraro
 */
public class InfinispanBeanEntryMarshaller implements ProtoStreamMarshaller<InfinispanBeanEntry<SessionID>> {

    private static final int BEAN_NAME_INDEX = 1;
    private static final int GROUP_INDEX = 2;
    private static final int LAST_ACCESSED_INDEX = 3;

    @Override
    public InfinispanBeanEntry<SessionID> readFrom(ProtoStreamReader reader) throws IOException {
        String beanName = null;
        SessionID groupId = null;
        Instant lastAccessed = null;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case BEAN_NAME_INDEX:
                    beanName = reader.readString();
                    break;
                case GROUP_INDEX:
                    groupId = reader.readObject(SessionID.class);
                    break;
                case LAST_ACCESSED_INDEX:
                    lastAccessed = reader.readObject(Instant.class);
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        InfinispanBeanEntry<SessionID> entry = new InfinispanBeanEntry<>(beanName, groupId);
        entry.setLastAccessedTime(lastAccessed);
        return entry;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, InfinispanBeanEntry<SessionID> entry) throws IOException {
        writer.writeString(BEAN_NAME_INDEX, entry.getBeanName());
        writer.writeObject(GROUP_INDEX, entry.getGroupId());
        Instant lastAccessed = entry.getLastAccessedTime();
        if (lastAccessed != null) {
            writer.writeObject(LAST_ACCESSED_INDEX, lastAccessed);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends InfinispanBeanEntry<SessionID>> getJavaClass() {
        return (Class<InfinispanBeanEntry<SessionID>>) (Class<?>) InfinispanBeanEntry.class;
    }
}
