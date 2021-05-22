/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.clustering.web;

import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.EEModuleConfiguration;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.serialization.WriteReplaceInterface;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.modules.Module;
import org.wildfly.clustering.marshalling.jboss.JBossByteBufferMarshaller;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingConfigurationRepository;
import org.wildfly.clustering.marshalling.protostream.ModuleClassLoaderMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamByteBufferMarshaller;
import org.wildfly.clustering.marshalling.protostream.SerializationContextBuilder;
import org.wildfly.clustering.marshalling.protostream.reflect.ProxyMarshaller;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;

/**
 * @author Paul Ferraro
 */
public enum SessionMarshallerFactory implements Function<DeploymentUnit, ByteBufferMarshaller> {

    JBOSS() {
        @Override
        public ByteBufferMarshaller apply(DeploymentUnit unit) {
            Module module = unit.getAttachment(Attachments.MODULE);
            return new JBossByteBufferMarshaller(new SimpleMarshallingConfigurationRepository(JBossMarshallingVersion.class, JBossMarshallingVersion.CURRENT, module), module.getClassLoader());
        }
    },
    PROTOSTREAM() {
        @Override
        public ByteBufferMarshaller apply(DeploymentUnit unit) {
            Module module = unit.getAttachment(Attachments.MODULE);
            SerializationContextBuilder builder = new SerializationContextBuilder(new ModuleClassLoaderMarshaller(module.getModuleLoader())).load(module.getClassLoader());

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
                        @Deprecated
                        @Override
                        public String getProtoFileName() {
                            return null;
                        }

                        @Deprecated
                        @Override
                        public String getProtoFile() {
                            return null;
                        }

                        @Override
                        public void registerSchema(SerializationContext context) {
                            context.registerProtoFiles(FileDescriptorSource.fromString(viewClassName + ".proto", schema));
                        }

                        @Override
                        public void registerMarshallers(SerializationContext context) {
                            for (ViewConfiguration view : entry.getValue()) {
                                context.registerMarshaller(new ProxyMarshaller<Object>(view.getProxyFactory().defineClass()) {
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

            return new ProtoStreamByteBufferMarshaller(builder.build());
        }
    },
    ;
}
