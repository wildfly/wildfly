/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.weld.bean.proxy;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.PrivilegedAction;

import jakarta.enterprise.inject.spi.Decorator;

import org.infinispan.protostream.descriptors.WireType;
import org.jboss.weld.bean.proxy.DecoratorProxyMethodHandler;
import org.jboss.weld.serialization.spi.helpers.SerializableContextualInstance;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Paul Ferraro
 */
public class DecoratorProxyMethodHandlerMarshaller implements ProtoStreamMarshaller<DecoratorProxyMethodHandler> {

    static final Field DECORATOR_FIELD = WildFlySecurityManager.doUnchecked(new PrivilegedAction<>() {
        @Override
        public Field run() {
            for (Field field : DecoratorProxyMethodHandler.class.getDeclaredFields()) {
                if (field.getType() == SerializableContextualInstance.class) {
                    field.setAccessible(true);
                    return field;
                }
            }
            throw new IllegalArgumentException(SerializableContextualInstance.class.getName());
        }
    });
    private static final int DECORATOR_INDEX = 1;
    private static final int DELEGATE_INDEX = 2;

    @Override
    public Class<? extends DecoratorProxyMethodHandler> getJavaClass() {
        return DecoratorProxyMethodHandler.class;
    }

    @Override
    public DecoratorProxyMethodHandler readFrom(ProtoStreamReader reader) throws IOException {
        SerializableContextualInstance<Decorator<Object>, Object> decorator = null;
        Object delegate = null;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case DECORATOR_INDEX:
                    decorator = reader.readAny(SerializableContextualInstance.class);
                    break;
                case DELEGATE_INDEX:
                    delegate = reader.readAny();
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return new DecoratorProxyMethodHandler(decorator, delegate);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, DecoratorProxyMethodHandler handler) throws IOException {
        SerializableContextualInstance<Decorator<Object>, Object> decorator = WildFlySecurityManager.doUnchecked(new PrivilegedAction<>() {
            @SuppressWarnings("unchecked")
            @Override
            public SerializableContextualInstance<Decorator<Object>, Object> run() {
                try {
                    return (SerializableContextualInstance<Decorator<Object>, Object>) DECORATOR_FIELD.get(handler);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
        if (decorator != null) {
            writer.writeAny(DECORATOR_INDEX, decorator);
        }
        Object delegate = handler.getTargetInstance();
        if (delegate != null) {
            writer.writeAny(DELEGATE_INDEX, delegate);
        }
    }
}
