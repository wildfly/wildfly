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
