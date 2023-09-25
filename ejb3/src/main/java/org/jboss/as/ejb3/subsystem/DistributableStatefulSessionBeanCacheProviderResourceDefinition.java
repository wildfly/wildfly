/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.subsystem;

import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.SimpleResourceDescriptorConfigurator;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.ejb3.component.stateful.cache.distributable.DistributableStatefulSessionBeanCacheProviderServiceConfigurator;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.ejb.bean.BeanProviderRequirement;

/**
 * Defines a CacheFactoryBuilder instance which, during deployment, is used to configure, build and install a CacheFactory for the SFSB being deployed.
 * The CacheFactory resource instances defined here produce bean caches which are non distributed and do not have passivation-enabled.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class DistributableStatefulSessionBeanCacheProviderResourceDefinition extends StatefulSessionBeanCacheProviderResourceDefinition {

    public enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        BEAN_MANAGEMENT(EJB3SubsystemModel.BEAN_MANAGEMENT, ModelType.STRING, new CapabilityReference(Capability.CACHE_FACTORY, BeanProviderRequirement.BEAN_MANAGEMENT_PROVIDER))
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, CapabilityReference referenece) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(false)
                    .setRequired(false)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .setCapabilityReference(referenece)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    public DistributableStatefulSessionBeanCacheProviderResourceDefinition() {
        super(EJB3SubsystemModel.DISTRIBUTABLE_CACHE_PATH, new SimpleResourceDescriptorConfigurator<>(Attribute.class), DistributableStatefulSessionBeanCacheProviderServiceConfigurator::new);
    }
}
