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

package org.wildfly.clustering.ejb.cache.bean;

import java.util.function.Function;

import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.modules.Module;
import org.wildfly.clustering.ejb.bean.BeanDeploymentMarshallingContext;
import org.wildfly.clustering.ejb.client.EJBProxyResolver;
import org.wildfly.clustering.marshalling.jboss.DynamicClassTable;
import org.wildfly.clustering.marshalling.jboss.DynamicExternalizerObjectTable;
import org.wildfly.clustering.marshalling.jboss.JBossByteBufferMarshaller;
import org.wildfly.clustering.marshalling.jboss.MarshallingConfigurationRepository;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingConfigurationRepository;
import org.wildfly.clustering.marshalling.jboss.SimpleSerializabilityChecker;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;

/**
 * Enumerates factory implementations for all supported bean marshallers.
 * @author Paul Ferraro
 */
public enum BeanMarshallerFactory implements Function<BeanDeploymentMarshallingContext, ByteBufferMarshaller> {

    JBOSS() {
        @Override
        public ByteBufferMarshaller apply(BeanDeploymentMarshallingContext context) {
            MarshallingConfigurationRepository repository = new SimpleMarshallingConfigurationRepository(MarshallingVersion.class, MarshallingVersion.CURRENT, context);
            return new JBossByteBufferMarshaller(repository, context.getModule().getClassLoader());
        }
    };

    enum MarshallingVersion implements Function<BeanDeploymentMarshallingContext, MarshallingConfiguration> {
        VERSION_1() {
            @SuppressWarnings("deprecation")
            @Override
            public MarshallingConfiguration apply(BeanDeploymentMarshallingContext context) {
                Module module = context.getModule();
                MarshallingConfiguration config = new MarshallingConfiguration();
                config.setClassResolver(ModularClassResolver.getInstance(module.getModuleLoader()));
                config.setSerializabilityChecker(new SimpleSerializabilityChecker(context.getBeanClasses()));
                config.setClassTable(new DynamicClassTable(module.getClassLoader()));
                config.setObjectTable(new org.wildfly.clustering.ejb.client.EJBProxyObjectTable());
                return config;
            }
        },
        VERSION_2() {
            @Override
            public MarshallingConfiguration apply(BeanDeploymentMarshallingContext context) {
                Module module = context.getModule();
                MarshallingConfiguration config = new MarshallingConfiguration();
                config.setClassResolver(ModularClassResolver.getInstance(module.getModuleLoader()));
                config.setSerializabilityChecker(new SimpleSerializabilityChecker(context.getBeanClasses()));
                config.setClassTable(new DynamicClassTable(module.getClassLoader()));
                config.setObjectResolver(new EJBProxyResolver());
                config.setObjectTable(new DynamicExternalizerObjectTable(module.getClassLoader()));
                return config;
            }
        },
        ;
        static final MarshallingVersion CURRENT = VERSION_2;
    }
}
