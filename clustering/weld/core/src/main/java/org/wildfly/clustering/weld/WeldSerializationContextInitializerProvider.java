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

package org.wildfly.clustering.weld;

import org.infinispan.protostream.SerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.ProviderSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializerProvider;
import org.wildfly.clustering.weld.annotated.slim.SlimAnnotatedMarshallerProvider;
import org.wildfly.clustering.weld.annotated.slim.backed.BackedSlimAnnotatedMarshallerProvider;
import org.wildfly.clustering.weld.annotated.slim.unbacked.UnbackedSlimAnnotatedMarshallerProvider;
import org.wildfly.clustering.weld.bean.BeanMarshallerProvider;
import org.wildfly.clustering.weld.bean.builtin.BuiltinBeanMarshallerProvider;
import org.wildfly.clustering.weld.bean.proxy.ProxyBeanMarshallerProvider;
import org.wildfly.clustering.weld.contexts.ContextsMarshallerProvider;
import org.wildfly.clustering.weld.contexts.DistributedContextsMarshallerProvider;
import org.wildfly.clustering.weld.contexts.beanstore.BeanStoreMarshallerProvider;
import org.wildfly.clustering.weld.injection.InjectionMarshallerProvider;
import org.wildfly.clustering.weld.interceptor.proxy.ProxyInterceptorMarshallerProvider;
import org.wildfly.clustering.weld.manager.ManagerMarshallerProvider;

/**
 * @author Paul Ferraro
 */
public enum WeldSerializationContextInitializerProvider implements SerializationContextInitializerProvider {

    MANAGER(new ProviderSerializationContextInitializer<>("org.jboss.weld.manager.proto", ManagerMarshallerProvider.class)),
    SLIM_ANNOTATED(new ProviderSerializationContextInitializer<>("org.jboss.weld.annotated.slim.proto", SlimAnnotatedMarshallerProvider.class)),
    SLIM_BACKED_ANNOTATED(new ProviderSerializationContextInitializer<>("org.jboss.weld.annotated.slim.backed.proto", BackedSlimAnnotatedMarshallerProvider.class)),
    SLIM_UNBACKED_ANNOTATED(new ProviderSerializationContextInitializer<>("org.jboss.weld.annotated.slim.unbacked.proto", UnbackedSlimAnnotatedMarshallerProvider.class)),
    BEAN(new ProviderSerializationContextInitializer<>("org.jboss.weld.bean.proto", BeanMarshallerProvider.class)),
    BUILTIN_BEAN(new ProviderSerializationContextInitializer<>("org.jboss.weld.bean.builtin.proto", BuiltinBeanMarshallerProvider.class)),
    PROXY_INTERCEPTOR(new ProviderSerializationContextInitializer<>("org.jboss.weld.interceptor.proxy.proto", ProxyInterceptorMarshallerProvider.class)),
    PROXY_BEAN(new ProviderSerializationContextInitializer<>("org.jboss.weld.bean.proxy.proto", ProxyBeanMarshallerProvider.class)),
    CONTEXTS(new ProviderSerializationContextInitializer<>("org.jboss.weld.contexts.proto", ContextsMarshallerProvider.class)),
    DISTRIBUTED_CONTEXTS(new ProviderSerializationContextInitializer<>("org.wildfly.clustering.weld.contexts.proto", DistributedContextsMarshallerProvider.class)),
    BEAN_STORE(new ProviderSerializationContextInitializer<>("org.jboss.weld.contexts.beanstore.proto", BeanStoreMarshallerProvider.class)),
    INJECTION(new ProviderSerializationContextInitializer<>("org.jboss.weld.injection.proto", InjectionMarshallerProvider.class)),
    ;

    private final SerializationContextInitializer initializer;

    WeldSerializationContextInitializerProvider(SerializationContextInitializer initializer) {
        this.initializer = initializer;
    }

    @Override
    public SerializationContextInitializer getInitializer() {
        return this.initializer;
    }
}
