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
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.reflect.BinaryFieldMarshaller;
import org.wildfly.clustering.marshalling.protostream.reflect.TernaryFieldMarshaller;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class ProxyBeanSerializationContextInitializer extends AbstractSerializationContextInitializer {

    public ProxyBeanSerializationContextInitializer() {
        super(ProxyMethodHandler.class.getPackage());
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new CombinedInterceptorAndDecoratorStackMethodHandlerMarshaller());
        context.registerMarshaller(new BinaryFieldMarshaller<>(ContextBeanInstance.class, String.class, BeanIdentifier.class, (contextId, id) -> new ContextBeanInstance<>(Container.instance(contextId).services().get(ContextualStore.class).<Bean<Object>, Object>getContextual(id), id, contextId)));
        context.registerMarshaller(new DecoratorProxyMethodHandlerMarshaller());
        context.registerMarshaller(new BinaryFieldMarshaller<>(EnterpriseTargetBeanInstance.class, Class.class, MethodHandler.class, EnterpriseTargetBeanInstance::new));
        context.registerMarshaller(new TernaryFieldMarshaller<>(ProxyMethodHandler.class, String.class, BeanInstance.class, BeanIdentifier.class, (contextId, instance, beanId) -> new ProxyMethodHandler(contextId, instance, Container.instance(contextId).services().get(ContextualStore.class).<Bean<Object>, Object>getContextual(beanId))));
        context.registerMarshaller(new TargetBeanInstanceMarshaller());
    }
}
