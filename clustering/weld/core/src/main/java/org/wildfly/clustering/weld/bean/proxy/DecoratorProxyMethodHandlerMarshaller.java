/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.weld.bean.proxy;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

import jakarta.enterprise.inject.spi.Decorator;

import org.infinispan.protostream.descriptors.WireType;
import org.jboss.weld.bean.proxy.DecoratorProxyMethodHandler;
import org.jboss.weld.serialization.spi.helpers.SerializableContextualInstance;
import org.wildfly.clustering.function.Function;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * @author Paul Ferraro
 */
public class DecoratorProxyMethodHandlerMarshaller implements ProtoStreamMarshaller<DecoratorProxyMethodHandler> {

    static final Function<DecoratorProxyMethodHandler, SerializableContextualInstance<Decorator<Object>, Object>> DECORATOR_HANDLE = Function.invoke(findHandle(DecoratorProxyMethodHandler.class, SerializableContextualInstance.class));

    private static MethodHandle findHandle(Class<?> sourceClass, Class<?> fieldClass) {
        for (Field field : sourceClass.getDeclaredFields()) {
            if (field.getType() == fieldClass) {
                try {
                    return MethodHandles.privateLookupIn(sourceClass, MethodHandles.lookup()).findGetter(sourceClass, field.getName(), fieldClass);
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        throw new IllegalArgumentException(fieldClass.getName());
    }

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
        SerializableContextualInstance<Decorator<Object>, Object> decorator = DECORATOR_HANDLE.apply(handler);
        if (decorator != null) {
            writer.writeAny(DECORATOR_INDEX, decorator);
        }
        Object delegate = handler.getTargetInstance();
        if (delegate != null) {
            writer.writeAny(DELEGATE_INDEX, delegate);
        }
    }
}
