/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
