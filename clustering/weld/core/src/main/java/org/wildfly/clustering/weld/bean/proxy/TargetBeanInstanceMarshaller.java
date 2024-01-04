/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
