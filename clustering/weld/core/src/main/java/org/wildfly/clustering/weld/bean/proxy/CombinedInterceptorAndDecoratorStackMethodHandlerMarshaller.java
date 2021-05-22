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
