/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
