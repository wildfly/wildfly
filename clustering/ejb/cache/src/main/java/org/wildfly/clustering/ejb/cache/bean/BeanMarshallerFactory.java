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
import org.wildfly.clustering.marshalling.jboss.IdentitySerializabilityChecker;
import org.wildfly.clustering.marshalling.jboss.JBossByteBufferMarshaller;
import org.wildfly.clustering.marshalling.jboss.MarshallingConfigurationBuilder;
import org.wildfly.clustering.marshalling.jboss.MarshallingConfigurationRepository;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;

/**
 * Enumerates factory implementations for all supported bean marshallers.
 * @author Paul Ferraro
 */
public enum BeanMarshallerFactory implements Function<BeanDeploymentMarshallingContext, ByteBufferMarshaller> {

    JBOSS() {
        @Override
        public ByteBufferMarshaller apply(BeanDeploymentMarshallingContext context) {
            MarshallingConfigurationRepository repository = MarshallingConfigurationRepository.from(MarshallingVersion.CURRENT, context);
            return new JBossByteBufferMarshaller(repository, context.getModule().getClassLoader());
        }
    };

    enum MarshallingVersion implements Function<BeanDeploymentMarshallingContext, MarshallingConfiguration> {
        VERSION_1() {
            @SuppressWarnings("deprecation")
            @Override
            public MarshallingConfiguration apply(BeanDeploymentMarshallingContext context) {
                Module module = context.getModule();
                MarshallingConfiguration config = MarshallingConfigurationBuilder.newInstance(ModularClassResolver.getInstance(module.getModuleLoader())).load(module.getClassLoader()).build();
                config.setSerializabilityChecker(new IdentitySerializabilityChecker(context.getBeanClasses()));
                config.setObjectTable(new org.wildfly.clustering.ejb.client.EJBProxyObjectTable());
                return config;
            }
        },
        VERSION_2() {
            @Override
            public MarshallingConfiguration apply(BeanDeploymentMarshallingContext context) {
                Module module = context.getModule();
                MarshallingConfiguration config = MarshallingConfigurationBuilder.newInstance(ModularClassResolver.getInstance(module.getModuleLoader())).load(module.getClassLoader()).build();
                config.setSerializabilityChecker(new IdentitySerializabilityChecker(context.getBeanClasses()));
                config.setObjectResolver(new EJBProxyResolver());
                return config;
            }
        },
        ;
        static final MarshallingVersion CURRENT = VERSION_2;
    }
}
