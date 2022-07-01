/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.weld.manager;

import java.io.IOException;

import org.infinispan.protostream.descriptors.WireType;
import org.jboss.weld.Container;
import org.jboss.weld.manager.BeanManagerImpl;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * @author Paul Ferraro
 */
public class BeanManagerImplMarshaller implements ProtoStreamMarshaller<BeanManagerImpl> {

    private static final int CONTEXT_INDEX = 1;
    private static final int IDENTIFIER_INDEX = 2;

    @Override
    public Class<? extends BeanManagerImpl> getJavaClass() {
        return BeanManagerImpl.class;
    }

    @Override
    public BeanManagerImpl readFrom(ProtoStreamReader reader) throws IOException {
        String contextId = null;
        String id = null;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case CONTEXT_INDEX:
                    contextId = reader.readString();
                    break;
                case IDENTIFIER_INDEX:
                    id = reader.readString();
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return Container.instance(contextId).getBeanManager(id);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, BeanManagerImpl manager) throws IOException {
        String contextId = manager.getContextId();
        if (contextId != null) {
            writer.writeString(CONTEXT_INDEX, manager.getContextId());
        }
        String id = manager.getId();
        if (id != null) {
            writer.writeString(IDENTIFIER_INDEX, manager.getId());
        }
    }
}
