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

import jakarta.enterprise.inject.spi.Bean;

import org.jboss.weld.Container;
import org.jboss.weld.bean.proxy.BeanInstance;
import org.jboss.weld.bean.proxy.ContextBeanInstance;
import org.jboss.weld.bean.proxy.EnterpriseTargetBeanInstance;
import org.jboss.weld.bean.proxy.MethodHandler;
import org.jboss.weld.bean.proxy.ProxyMethodHandler;
import org.jboss.weld.serialization.spi.BeanIdentifier;
import org.jboss.weld.serialization.spi.ContextualStore;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.reflect.BinaryFieldMarshaller;
import org.wildfly.clustering.marshalling.protostream.reflect.TernaryFieldMarshaller;

/**
 * @author Paul Ferraro
 */
public enum ProxyBeanMarshallerProvider implements ProtoStreamMarshallerProvider {

    COMBINED_INTERCEPTOR_DECORATOR_STACK_METHOD_HANDLER(new CombinedInterceptorAndDecoratorStackMethodHandlerMarshaller()),
    CONTEXT_BEAN_INSTANCE(new BinaryFieldMarshaller<>(ContextBeanInstance.class, String.class, BeanIdentifier.class, (contextId, id) -> new ContextBeanInstance<>(Container.instance(contextId).services().get(ContextualStore.class).<Bean<Object>, Object>getContextual(id), id, contextId))),
    DECORATOR_PROXY_METHOD_HANDLER(new DecoratorProxyMethodHandlerMarshaller()),
    ENTERPRISE_TARGET_BEAN_INSTANCE(new BinaryFieldMarshaller<>(EnterpriseTargetBeanInstance.class, Class.class, MethodHandler.class, EnterpriseTargetBeanInstance::new)),
    PROXY_METHOD_HANDLER(new TernaryFieldMarshaller<>(ProxyMethodHandler.class, String.class, BeanInstance.class, BeanIdentifier.class, (contextId, instance, beanId) -> new ProxyMethodHandler(contextId, instance, Container.instance(contextId).services().get(ContextualStore.class).<Bean<Object>, Object>getContextual(beanId)))),
    TARGET_BEAN_INSTANCE(new TargetBeanInstanceMarshaller()),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    ProxyBeanMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
