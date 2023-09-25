/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.interceptor.proxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.PrivilegedAction;
import java.util.Map;

import org.jboss.weld.annotated.slim.SlimAnnotatedType;
import org.jboss.weld.interceptor.proxy.InterceptionContext;
import org.jboss.weld.interceptor.proxy.InterceptorMethodHandler;
import org.jboss.weld.interceptor.spi.model.InterceptionModel;
import org.jboss.weld.manager.BeanManagerImpl;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.reflect.TernaryFieldMarshaller;
import org.wildfly.clustering.marshalling.protostream.reflect.UnaryFieldMarshaller;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Paul Ferraro
 */
public enum ProxyInterceptorMarshallerProvider implements ProtoStreamMarshallerProvider {

    INTERCEPTOR_CONTEXT(new TernaryFieldMarshaller<>(InterceptionContext.class, Map.class, BeanManagerImpl.class, SlimAnnotatedType.class, (interceptorInstances, manager, annotatedType) -> {
        InterceptionModel interceptionModel = manager.getInterceptorModelRegistry().get(annotatedType);
        // How inconvenient. The constructor we need is private...
        PrivilegedAction<InterceptionContext> action = () -> {
            try {
                Constructor<InterceptionContext> constructor = InterceptionContext.class.getDeclaredConstructor(Map.class, BeanManagerImpl.class, InterceptionModel.class, SlimAnnotatedType.class);
                return constructor.newInstance(interceptorInstances, manager, interceptionModel, annotatedType);
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException(e);
            }
        };
        return WildFlySecurityManager.doUnchecked(action);
    })),
    INTERCEPTOR_METHOD_HANDLER(new UnaryFieldMarshaller<>(InterceptorMethodHandler.class, InterceptionContext.class, InterceptorMethodHandler::new))
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    ProxyInterceptorMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
