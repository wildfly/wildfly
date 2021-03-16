/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.ejb.BeanManagerFactoryServiceConfiguratorConfiguration;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.spi.InfinispanDefaultCacheRequirement;

/**
 * @author Paul Ferraro
 */
@Deprecated
public class ClusterPassivationStoreResourceDefinition extends LegacyPassivationStoreResourceDefinition {

    protected static final String INFINISPAN_CACHE_CONTAINER_CAPABILITY_NAME = "org.wildfly.clustering.infinispan.cache-container";

    public static final String CLUSTER_PASSIVATION_STORE_CAPABILITY_NAME = "org.wildfly.ejb.cluster-passivation-store";
    public static final RuntimeCapability<Void> CLUSTER_PASSIVATION_STORE_CAPABILITY = RuntimeCapability.Builder.of(CLUSTER_PASSIVATION_STORE_CAPABILITY_NAME)
            .setServiceType(Void.class)
            .build();

    @Deprecated
    static final SimpleAttributeDefinition MAX_SIZE = new SimpleAttributeDefinitionBuilder(MAX_SIZE_BUILDER.build())
            .setDefaultValue(new ModelNode(10000))
            .build()
    ;
    @Deprecated
    static final SimpleAttributeDefinition CACHE_CONTAINER = new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.CACHE_CONTAINER, ModelType.STRING, true)
            .setXmlName(EJB3SubsystemXMLAttribute.CACHE_CONTAINER.getLocalName())
            .setDefaultValue(new ModelNode(BeanManagerFactoryServiceConfiguratorConfiguration.DEFAULT_CONTAINER_NAME))
            // Capability references should not allow expressions
            .setAllowExpression(false)
            .setFlags(AttributeAccess.Flag.RESTART_NONE)
            // a CapabilityReference to a UnaryRequirement
            .setCapabilityReference(new CapabilityReference(()->CLUSTER_PASSIVATION_STORE_CAPABILITY, InfinispanDefaultCacheRequirement.CONFIGURATION))
            .build()
    ;
    @Deprecated
    static final SimpleAttributeDefinition BEAN_CACHE = new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.BEAN_CACHE, ModelType.STRING, true)
            .setXmlName(EJB3SubsystemXMLAttribute.BEAN_CACHE.getLocalName())
            // Capability references should not allow expressions
            .setAllowExpression(false)
            .setFlags(AttributeAccess.Flag.RESTART_NONE)
            // a CapabilityReference to a BinaryRequirement (including a parent attribute)
            .setCapabilityReference(new CapabilityReference(()->CLUSTER_PASSIVATION_STORE_CAPABILITY, InfinispanCacheRequirement.CONFIGURATION, ()->CACHE_CONTAINER))
            .build()
    ;
    @Deprecated
    static final SimpleAttributeDefinition CLIENT_MAPPINGS_CACHE = new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.CLIENT_MAPPINGS_CACHE, ModelType.STRING, true)
            .setXmlName(EJB3SubsystemXMLAttribute.CLIENT_MAPPINGS_CACHE.getLocalName())
            .setDefaultValue(new ModelNode("remote-connector-client-mappings"))
            // Capability references should not allow expressions
            .setAllowExpression(false)
            .setFlags(AttributeAccess.Flag.RESTART_NONE)
            .setDeprecated(DEPRECATED_VERSION)
            // TODO: replace this with a Requirement reference when the ejb-spi module for clustering is available
            .setCapabilityReference(INFINISPAN_CACHE_CONTAINER_CAPABILITY_NAME, CLUSTER_PASSIVATION_STORE_CAPABILITY)
            .build()
    ;
    @Deprecated
    static final SimpleAttributeDefinition PASSIVATE_EVENTS_ON_REPLICATE = new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.PASSIVATE_EVENTS_ON_REPLICATE, ModelType.BOOLEAN, true)
            .setXmlName(EJB3SubsystemXMLAttribute.PASSIVATE_EVENTS_ON_REPLICATE.getLocalName())
            .setDefaultValue(ModelNode.TRUE)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_NONE)
            .setDeprecated(DEPRECATED_VERSION)
            .build()
    ;

    private static final AttributeDefinition[] ATTRIBUTES = { MAX_SIZE, IDLE_TIMEOUT, IDLE_TIMEOUT_UNIT, CACHE_CONTAINER, BEAN_CACHE, CLIENT_MAPPINGS_CACHE, PASSIVATE_EVENTS_ON_REPLICATE };

    private static final ClusterPassivationStoreAdd ADD_HANDLER = new ClusterPassivationStoreAdd(ATTRIBUTES);
    private static final PassivationStoreRemove REMOVE_HANDLER = new PassivationStoreRemove(ADD_HANDLER);

    @Deprecated
    static final ClusterPassivationStoreResourceDefinition INSTANCE = new ClusterPassivationStoreResourceDefinition();

    private ClusterPassivationStoreResourceDefinition() {
        super(EJB3SubsystemModel.CLUSTER_PASSIVATION_STORE, ADD_HANDLER, REMOVE_HANDLER, OperationEntry.Flag.RESTART_NONE, OperationEntry.Flag.RESTART_RESOURCE_SERVICES, CLUSTER_PASSIVATION_STORE_CAPABILITY, ATTRIBUTES);
    }
}
