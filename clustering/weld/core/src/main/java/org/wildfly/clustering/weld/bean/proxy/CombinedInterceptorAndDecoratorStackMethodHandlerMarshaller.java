/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.bean.proxy;

import java.io.IOException;

import org.infinispan.protostream.descriptors.WireType;
import org.jboss.weld.bean.proxy.CombinedInterceptorAndDecoratorStackMethodHandler;
import org.jboss.weld.interceptor.proxy.InterceptorMethodHandler;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * @author Paul Ferraro
 */
public class CombinedInterceptorAndDecoratorStackMethodHandlerMarshaller implements ProtoStreamMarshaller<CombinedInterceptorAndDecoratorStackMethodHandler> {

    private static final int INTERCEPTOR_METHOD_HANDLER_INDEX = 1;
    private static final int OUTER_DECORATOR_INDEX = 2;

    @Override
    public Class<? extends CombinedInterceptorAndDecoratorStackMethodHandler> getJavaClass() {
        return CombinedInterceptorAndDecoratorStackMethodHandler.class;
    }

    @Override
    public CombinedInterceptorAndDecoratorStackMethodHandler readFrom(ProtoStreamReader reader) throws IOException {
        CombinedInterceptorAndDecoratorStackMethodHandler handler = new CombinedInterceptorAndDecoratorStackMethodHandler();
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case INTERCEPTOR_METHOD_HANDLER_INDEX:
                    handler.setInterceptorMethodHandler(reader.readObject(InterceptorMethodHandler.class));
                    break;
                case OUTER_DECORATOR_INDEX:
                    handler.setOuterDecorator(reader.readAny());
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return handler;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, CombinedInterceptorAndDecoratorStackMethodHandler handler) throws IOException {
        InterceptorMethodHandler interceptorHandler = handler.getInterceptorMethodHandler();
        if (interceptorHandler != null) {
            writer.writeObject(INTERCEPTOR_METHOD_HANDLER_INDEX, interceptorHandler);
        }
        Object decorator = handler.getOuterDecorator();
        if (decorator != null) {
            writer.writeAny(OUTER_DECORATOR_INDEX, decorator);
        }
    }
}
