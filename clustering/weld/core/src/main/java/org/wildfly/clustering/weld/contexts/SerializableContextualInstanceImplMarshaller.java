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

package org.wildfly.clustering.weld.contexts;

import java.io.IOException;
import java.security.PrivilegedAction;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.PassivationCapable;

import org.infinispan.protostream.descriptors.WireType;
import org.jboss.weld.Container;
import org.jboss.weld.bean.proxy.MethodHandler;
import org.jboss.weld.bean.proxy.ProxyInstantiator;
import org.jboss.weld.bean.proxy.ProxyObject;
import org.jboss.weld.contexts.SerializableContextualInstanceImpl;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Paul Ferraro
 */
public class SerializableContextualInstanceImplMarshaller<C extends Contextual<I> & PassivationCapable, I> implements ProtoStreamMarshaller<SerializableContextualInstanceImpl<C, I>> {

    private static final int CONTEXTUAL_INDEX = 1;
    private static final int INSTANCE_INDEX = 2;
    private static final int PROXY_CLASS_INDEX = 3;
    private static final int METHOD_HANDLER_INDEX = 4;
    private static final int CREATIONAL_CONTEXT_INDEX = 5;

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends SerializableContextualInstanceImpl<C, I>> getJavaClass() {
        return (Class<SerializableContextualInstanceImpl<C, I>>) (Class<?>) SerializableContextualInstanceImpl.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public SerializableContextualInstanceImpl<C, I> readFrom(ProtoStreamReader reader) throws IOException {
        PassivationCapableContextual<C, I> contextual = null;
        I instance = null;
        CreationalContext<I> creationalContext = null;
        Class<?> proxyClass = null;
        MethodHandler handler = null;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case CONTEXTUAL_INDEX:
                    contextual = reader.readAny(PassivationCapableContextual.class);
                    break;
                case INSTANCE_INDEX:
                    instance = (I) reader.readAny();
                    break;
                case PROXY_CLASS_INDEX:
                    proxyClass = reader.readObject(Class.class);
                    break;
                case METHOD_HANDLER_INDEX:
                    handler = reader.readAny(MethodHandler.class);
                    break;
                case CREATIONAL_CONTEXT_INDEX:
                    creationalContext = reader.readAny(CreationalContext.class);
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        if (proxyClass != null) {
            Class<?> targetClass = proxyClass;
            ProxyInstantiator instantiator = Container.instance(contextual.getContextId()).services().get(ProxyInstantiator.class);
            PrivilegedAction<I> action = new PrivilegedAction<>() {
                @Override
                public I run() {
                    try {
                        return (I) instantiator.newInstance(targetClass);
                    } catch (InstantiationException | IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                }
            };
            instance = WildFlySecurityManager.doUnchecked(action);
            if (handler != null) {
                ((ProxyObject) instance).weld_setHandler(handler);
            }
        }
        return new SerializableContextualInstanceImpl<>(contextual, instance, creationalContext);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, SerializableContextualInstanceImpl<C, I> contextualInstance) throws IOException {
        writer.writeAny(CONTEXTUAL_INDEX, contextualInstance.getContextual());
        Object instance = contextualInstance.getInstance();
        // If this is a weld proxy, marshal class and method handler
        if (instance instanceof ProxyObject) {
            writer.writeObject(PROXY_CLASS_INDEX, instance.getClass());
            writer.writeAny(METHOD_HANDLER_INDEX, ((ProxyObject) instance).weld_getHandler());
        } else {
            writer.writeAny(INSTANCE_INDEX, instance);
        }
        writer.writeAny(CREATIONAL_CONTEXT_INDEX, contextualInstance.getCreationalContext());
    }
}
