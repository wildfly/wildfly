/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.ejb.component.stateful;

import java.io.IOException;

import org.infinispan.protostream.descriptors.WireType;
import org.jboss.as.ejb3.component.stateful.StatefulSerializedProxy;
import org.jboss.ejb.client.SessionID;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * @author Paul Ferraro
 */
public class StatefulSerializedProxyMarshaller implements ProtoStreamMarshaller<StatefulSerializedProxy> {

    private static final int VIEW_NAME_INDEX = 1;
    private static final int SESSION_ID_INDEX = 2;

    @Override
    public Class<? extends StatefulSerializedProxy> getJavaClass() {
        return StatefulSerializedProxy.class;
    }

    @Override
    public StatefulSerializedProxy readFrom(ProtoStreamReader reader) throws IOException {
        String viewName = null;
        SessionID id = null;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case VIEW_NAME_INDEX:
                    viewName = reader.readString();
                    break;
                case SESSION_ID_INDEX:
                    id = reader.readObject(SessionID.class);
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return new StatefulSerializedProxy(viewName, id);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, StatefulSerializedProxy proxy) throws IOException {
        String viewName = proxy.getViewName();
        if (viewName != null) {
            writer.writeString(VIEW_NAME_INDEX, viewName);
        }
        SessionID id = proxy.getSessionID();
        if (id != null) {
            writer.writeObject(SESSION_ID_INDEX, id);
        }
    }
}
