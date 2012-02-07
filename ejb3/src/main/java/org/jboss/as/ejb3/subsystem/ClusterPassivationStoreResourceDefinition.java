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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.ejb3.cache.impl.backing.clustering.ClusteredBackingCacheEntryStoreConfig;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Paul Ferraro
 */
public class ClusterPassivationStoreResourceDefinition extends PassivationStoreResourceDefinition {

    public static final SimpleAttributeDefinition MAX_SIZE = MAX_SIZE_BUILDER.setDefaultValue(new ModelNode().set(ClusteredBackingCacheEntryStoreConfig.DEFAULT_MAX_SIZE)).build();
    public static final SimpleAttributeDefinition CACHE_CONTAINER =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.CACHE_CONTAINER, ModelType.STRING, true)
                    .setXmlName(EJB3SubsystemXMLAttribute.CACHE_CONTAINER.getLocalName())
                    .setDefaultValue(new ModelNode().set(ClusteredBackingCacheEntryStoreConfig.DEFAULT_CACHE_CONTAINER))
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();
    public static final SimpleAttributeDefinition BEAN_CACHE =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.BEAN_CACHE, ModelType.STRING, true)
                    .setXmlName(EJB3SubsystemXMLAttribute.BEAN_CACHE.getLocalName())
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();
    public static final SimpleAttributeDefinition CLIENT_MAPPINGS_CACHE =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.CLIENT_MAPPINGS_CACHE, ModelType.STRING, true)
                    .setXmlName(EJB3SubsystemXMLAttribute.CLIENT_MAPPINGS_CACHE.getLocalName())
                    .setDefaultValue(new ModelNode().set(ClusteredBackingCacheEntryStoreConfig.DEFAULT_CLIENT_MAPPING_CACHE))
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();
    public static final SimpleAttributeDefinition PASSIVATE_EVENTS_ON_REPLICATE =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.PASSIVATE_EVENTS_ON_REPLICATE, ModelType.BOOLEAN, true)
                    .setXmlName(EJB3SubsystemXMLAttribute.PASSIVATE_EVENTS_ON_REPLICATE.getLocalName())
                    .setDefaultValue(new ModelNode().set(ClusteredBackingCacheEntryStoreConfig.DEFAULT_PASSIVATE_EVENTS_ON_REPLICATE))
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();

    private static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { IDLE_TIMEOUT, IDLE_TIMEOUT_UNIT, MAX_SIZE, CACHE_CONTAINER, BEAN_CACHE, CLIENT_MAPPINGS_CACHE, PASSIVATE_EVENTS_ON_REPLICATE };

    private static final ClusterPassivationStoreAdd ADD_HANDLER = new ClusterPassivationStoreAdd(ATTRIBUTES);
    private static final ClusterPassivationStoreRemove REMOVE_HANDLER = new ClusterPassivationStoreRemove(ADD_HANDLER);
    private static final ClusterPassivationStoreWriteHandler WRITE_HANDLER = new ClusterPassivationStoreWriteHandler(ATTRIBUTES);

    public static final ClusterPassivationStoreResourceDefinition INSTANCE = new ClusterPassivationStoreResourceDefinition();

    private ClusterPassivationStoreResourceDefinition() {
        super(EJB3SubsystemModel.CLUSTER_PASSIVATION_STORE, ADD_HANDLER, REMOVE_HANDLER, OperationEntry.Flag.RESTART_NONE, OperationEntry.Flag.RESTART_RESOURCE_SERVICES, WRITE_HANDLER, ATTRIBUTES);
    }
}
