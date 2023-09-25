/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
