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

package org.wildfly.clustering.weld.bean.proxy;

import java.io.IOException;

import org.infinispan.protostream.descriptors.WireType;
import org.jboss.weld.bean.proxy.MethodHandler;
import org.jboss.weld.bean.proxy.TargetBeanInstance;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * @author Paul Ferraro
 */
public class TargetBeanInstanceMarshaller implements ProtoStreamMarshaller<TargetBeanInstance> {

    private static final int INSTANCE_INDEX = 1;
    private static final int TYPE_INDEX = 2;
    private static final int HANDLER_INDEX = 3;

    @Override
    public Class<? extends TargetBeanInstance> getJavaClass() {
        return TargetBeanInstance.class;
    }

    @Override
    public TargetBeanInstance readFrom(ProtoStreamReader reader) throws IOException {
        Object instance = null;
        Class<?> type = null;
        MethodHandler handler = null;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case INSTANCE_INDEX:
                    instance = reader.readAny();
                    break;
                case TYPE_INDEX:
                    type = reader.readObject(Class.class);
                    break;
                case HANDLER_INDEX:
                    handler = reader.readAny(MethodHandler.class);
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        TargetBeanInstance result = (type != null) ? new TargetBeanInstance(new MockBean<>(type), instance) :  new TargetBeanInstance(instance);
        result.setInterceptorsHandler(handler);
        return result;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, TargetBeanInstance source) throws IOException {
        Object instance = source.getInstance();
        if (instance != null) {
            writer.writeAny(INSTANCE_INDEX, instance);
        }
        Class<?> type = source.getInstanceType();
        if ((type != null) && (type != instance.getClass())) {
            writer.writeObject(TYPE_INDEX, type);
        }
        MethodHandler handler = source.getInterceptorsHandler();
        if (handler != null) {
            writer.writeAny(HANDLER_INDEX, handler);
        }
    }
}
