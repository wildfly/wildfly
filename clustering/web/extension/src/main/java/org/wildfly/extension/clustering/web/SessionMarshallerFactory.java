/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.infinispan.protostream.FileDescriptorSource;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.EEModuleConfiguration;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.serialization.WriteReplaceInterface;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.modules.Module;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.marshalling.jboss.JBossByteBufferMarshaller;
import org.wildfly.clustering.marshalling.jboss.MarshallingConfigurationRepository;
import org.wildfly.clustering.marshalling.protostream.DefaultSerializationContext;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamByteBufferMarshaller;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextBuilder;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.modules.ModuleClassLoaderMarshaller;
import org.wildfly.clustering.marshalling.protostream.reflect.ProxyMarshaller;

/**
 * Enumerates factories for creating a session attribute marshaller.
 * @author Paul Ferraro
 */
public enum SessionMarshallerFactory implements Function<DeploymentUnit, ByteBufferMarshaller> {

    JBOSS() {
        @Override
        public ByteBufferMarshaller apply(DeploymentUnit unit) {
            Module module = unit.getAttachment(Attachments.MODULE);
            return new JBossByteBufferMarshaller(MarshallingConfigurationRepository.from(JBossMarshallingVersion.CURRENT, module), module.getClassLoader());
        }
    },
    PROTOSTREAM() {
        @Override
        public ByteBufferMarshaller apply(DeploymentUnit unit) {
            Module module = unit.getAttachment(Attachments.MODULE);
            SerializationContextBuilder<SerializationContextInitializer> builder = SerializationContextBuilder.newInstance(new ModuleClassLoaderMarshaller(module.getModuleLoader()), DefaultSerializationContext::new).load(module.getClassLoader());

            EEModuleConfiguration configuration = unit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_CONFIGURATION);
            // Sort component views by view class
            Map<Class<?>, List<ViewConfiguration>> components = new IdentityHashMap<>();
            for (ComponentConfiguration component : configuration.getComponentConfigurations()) {
                for (ViewConfiguration view : component.getViews()) {
                    Class<?> proxyClass = view.getProxyFactory().defineClass();
                    // Find components with views implementing WriteReplaceInterface
                    if (WriteReplaceInterface.class.isAssignableFrom(proxyClass)) {
                        List<ViewConfiguration> views = components.get(view.getViewClass());
                        if (views == null) {
                            views = new LinkedList<>();
                            components.put(view.getViewClass(), views);
                        }
                        views.add(view);
                    }
                }
            }

            // Create schemas/marshallers for component view proxies
            if (!components.isEmpty()) {
                for (Map.Entry<Class<?>, List<ViewConfiguration>> entry : components.entrySet()) {
                    String viewClassName = entry.getKey().getName();
                    StringBuilder schemaBuilder = new StringBuilder();
                    schemaBuilder.append("package ").append(viewClassName).append(';').append(System.lineSeparator());
                    for (ViewConfiguration view : entry.getValue()) {
                        schemaBuilder.append("message ").append(view.getComponentConfiguration().getComponentName()).append(" { optional bytes proxy = 1; }").append(System.lineSeparator());
                    }
                    String schema = schemaBuilder.toString();
                    builder.register(new SerializationContextInitializer() {

                        @Override
                        public void registerSchema(SerializationContext context) {
                            context.registerProtoFiles(FileDescriptorSource.fromString(viewClassName + ".proto", schema));
                        }

                        @Override
                        public void registerMarshallers(SerializationContext context) {
                            for (ViewConfiguration view : entry.getValue()) {
                                @SuppressWarnings("unchecked")
                                Class<Object> proxyClass = (Class<Object>) view.getProxyFactory().defineClass();
                                context.registerMarshaller(new ProxyMarshaller<>(proxyClass) {
                                    @Override
                                    public String getTypeName() {
                                        return viewClassName + "." + view.getComponentConfiguration().getComponentName();
                                    }
                                });
                            }
                        }
                    });
                }
            }

            return new ProtoStreamByteBufferMarshaller(builder.build(), module.getClassLoader());
        }
    },
    ;
}
