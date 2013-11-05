/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.clustering.controller.AttributeMarshallerFactory;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleMapAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.services.path.ResolvePathHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Base class for cache resources which require common cache attributes only.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class CacheResourceDefinition extends SimpleResourceDefinition {

    // attributes
    static final SimpleAttributeDefinition BATCHING = new SimpleAttributeDefinitionBuilder(ModelKeys.BATCHING, ModelType.BOOLEAN, true)
            .setXmlName(Attribute.BATCHING.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set(false))
            .build()
    ;
    static final SimpleAttributeDefinition MODULE = new SimpleAttributeDefinitionBuilder(ModelKeys.MODULE, ModelType.STRING, true)
            .setXmlName(Attribute.MODULE.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new ModuleIdentifierValidator(true))
            .build()
    ;
    static final SimpleAttributeDefinition INDEXING = new SimpleAttributeDefinitionBuilder(ModelKeys.INDEXING, ModelType.STRING, true)
            .setXmlName(Attribute.INDEX.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new EnumValidator<>(Indexing.class, true, false))
            .setDefaultValue(new ModelNode().set(Indexing.NONE.name()))
            .build()
    ;
    static final SimpleMapAttributeDefinition INDEXING_PROPERTIES = new SimpleMapAttributeDefinition.Builder(ModelKeys.INDEXING_PROPERTIES, true)
            .setAllowExpression(true)
            .setAttributeMarshaller(AttributeMarshallerFactory.createPropertyListAttributeMarshaller())
            .build()
    ;
    static final SimpleAttributeDefinition JNDI_NAME = new SimpleAttributeDefinitionBuilder(ModelKeys.JNDI_NAME, ModelType.STRING, true)
            .setXmlName(Attribute.JNDI_NAME.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build()
    ;
    static final SimpleAttributeDefinition START = new SimpleAttributeDefinitionBuilder(ModelKeys.START, ModelType.STRING, true)
            .setXmlName(Attribute.START.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new EnumValidator<>(StartMode.class, true, false))
            .setDefaultValue(new ModelNode().set(StartMode.LAZY.name()))
            .build()
    ;
    static final SimpleAttributeDefinition STATISTICS = new SimpleAttributeDefinitionBuilder(ModelKeys.STATISTICS, ModelType.BOOLEAN, true)
            .setXmlName(Attribute.STATISTICS.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setDefaultValue(new ModelNode().set(false))
            .build()
    ;

    static final AttributeDefinition[] CACHE_ATTRIBUTES = { BATCHING, MODULE, INDEXING, INDEXING_PROPERTIES, JNDI_NAME, START, STATISTICS };

    // here for legacy purposes only
    static final SimpleAttributeDefinition NAME = new SimpleAttributeDefinitionBuilder(ModelKeys.NAME, ModelType.STRING, true)
            .setXmlName(Attribute.NAME.getLocalName())
            .setAllowExpression(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build()
    ;

    // metrics
    static final AttributeDefinition ACTIVATIONS = new SimpleAttributeDefinitionBuilder(MetricKeys.ACTIVATIONS, ModelType.STRING, true).setStorageRuntime().build();
    static final AttributeDefinition AVERAGE_READ_TIME = new SimpleAttributeDefinitionBuilder(MetricKeys.AVERAGE_READ_TIME, ModelType.LONG, true).setStorageRuntime().build();
    static final AttributeDefinition AVERAGE_WRITE_TIME = new SimpleAttributeDefinitionBuilder(MetricKeys.AVERAGE_WRITE_TIME, ModelType.LONG, true).setStorageRuntime().build();
    static final AttributeDefinition CACHE_STATUS = new SimpleAttributeDefinitionBuilder(MetricKeys.CACHE_STATUS, ModelType.STRING, true).setStorageRuntime().build();
    static final AttributeDefinition ELAPSED_TIME = new SimpleAttributeDefinitionBuilder(MetricKeys.ELAPSED_TIME, ModelType.LONG, true).setStorageRuntime().build();
    static final AttributeDefinition HIT_RATIO = new SimpleAttributeDefinitionBuilder(MetricKeys.HIT_RATIO, ModelType.DOUBLE, true).setStorageRuntime().build();
    static final AttributeDefinition HITS = new SimpleAttributeDefinitionBuilder(MetricKeys.HITS, ModelType.LONG, true).setStorageRuntime().build();
    static final AttributeDefinition INVALIDATIONS = new SimpleAttributeDefinitionBuilder(MetricKeys.INVALIDATIONS, ModelType.LONG, true).setStorageRuntime().build();
    static final AttributeDefinition MISSES = new SimpleAttributeDefinitionBuilder(MetricKeys.MISSES, ModelType.LONG, true).setStorageRuntime().build();
    static final AttributeDefinition NUMBER_OF_ENTRIES = new SimpleAttributeDefinitionBuilder(MetricKeys.NUMBER_OF_ENTRIES, ModelType.INT, true).setStorageRuntime().build();
    static final AttributeDefinition PASSIVATIONS = new SimpleAttributeDefinitionBuilder(MetricKeys.PASSIVATIONS, ModelType.STRING, true).setStorageRuntime().build();
    static final AttributeDefinition READ_WRITE_RATIO = new SimpleAttributeDefinitionBuilder(MetricKeys.READ_WRITE_RATIO, ModelType.DOUBLE, true).setStorageRuntime().build();
    static final AttributeDefinition REMOVE_HITS = new SimpleAttributeDefinitionBuilder(MetricKeys.REMOVE_HITS, ModelType.LONG, true).setStorageRuntime().build();
    static final AttributeDefinition REMOVE_MISSES = new SimpleAttributeDefinitionBuilder(MetricKeys.REMOVE_MISSES, ModelType.LONG, true).setStorageRuntime().build();
    static final AttributeDefinition STORES = new SimpleAttributeDefinitionBuilder(MetricKeys.STORES, ModelType.LONG, true).setStorageRuntime().build();
    static final AttributeDefinition TIME_SINCE_RESET = new SimpleAttributeDefinitionBuilder(MetricKeys.TIME_SINCE_RESET, ModelType.LONG, true).setStorageRuntime().build();

    static final AttributeDefinition[] CACHE_METRICS = { /*ACTIVATIONS,*/ AVERAGE_READ_TIME, AVERAGE_WRITE_TIME, CACHE_STATUS, ELAPSED_TIME, HIT_RATIO, HITS,
            INVALIDATIONS, MISSES, NUMBER_OF_ENTRIES, PASSIVATIONS, READ_WRITE_RATIO, REMOVE_HITS, REMOVE_MISSES, STORES, TIME_SINCE_RESET };

    private final ResolvePathHandler resolvePathHandler;
    final boolean runtimeRegistration;

    public CacheResourceDefinition(PathElement pathElement, ResourceDescriptionResolver descriptionResolver, AbstractAddStepHandler addHandler, OperationStepHandler removeHandler, ResolvePathHandler resolvePathHandler, boolean runtimeRegistration) {
        super(pathElement, descriptionResolver, addHandler, removeHandler);
        this.resolvePathHandler = resolvePathHandler;
        this.runtimeRegistration = runtimeRegistration;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        // do we really need a special handler here?
        final OperationStepHandler writeHandler = new CacheWriteAttributeHandler(CACHE_ATTRIBUTES);
        for (AttributeDefinition attr : CACHE_ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, CacheReadAttributeHandler.INSTANCE, writeHandler);
        }

        // register any metrics
        for (AttributeDefinition attr : CACHE_METRICS) {
            resourceRegistration.registerMetric(attr, CacheMetricsHandler.INSTANCE);
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new LockingResourceDefinition());
        resourceRegistration.registerSubModel(new TransactionResourceDefinition());
        resourceRegistration.registerSubModel(new EvictionResourceDefinition());
        resourceRegistration.registerSubModel(new ExpirationResourceDefinition());
        resourceRegistration.registerSubModel(new CustomStoreResourceDefinition());
        resourceRegistration.registerSubModel(new FileStoreResourceDefinition(this.resolvePathHandler));
        resourceRegistration.registerSubModel(new StringKeyedJDBCStoreResourceDefinition());
        resourceRegistration.registerSubModel(new BinaryKeyedJDBCStoreResourceDefinition());
        resourceRegistration.registerSubModel(new MixedKeyedJDBCStoreResourceDefinition());
        resourceRegistration.registerSubModel(new RemoteStoreResourceDefinition());
    }
}
