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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
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
import org.jboss.dmr.Property;

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
    static final SimpleAttributeDefinition CACHE_MODULE = new SimpleAttributeDefinitionBuilder(ModelKeys.MODULE, ModelType.STRING, true)
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
            .setValidator(new EnumValidator<Indexing>(Indexing.class, true, false))
            .setDefaultValue(new ModelNode().set(Indexing.NONE.name()))
            .build()
    ;
    static final SimpleMapAttributeDefinition INDEXING_PROPERTIES = new SimpleMapAttributeDefinition.Builder(ModelKeys.INDEXING_PROPERTIES, true)
            .setAllowExpression(true)
            .setAttributeMarshaller(new AttributeMarshaller() {
                @Override
                public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
                    resourceModel = resourceModel.get(attribute.getName());
                    if (!resourceModel.isDefined()) {
                        return;
                    }
                    for (Property property : resourceModel.asPropertyList()) {
                        writer.writeStartElement(org.jboss.as.controller.parsing.Element.PROPERTY.getLocalName());
                        writer.writeAttribute(org.jboss.as.controller.parsing.Element.NAME.getLocalName(), property.getName());
                        writer.writeCharacters(property.getValue().asString());
                        writer.writeEndElement();
                    }
                }
            })
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
            .setValidator(new EnumValidator<StartMode>(StartMode.class, true, false))
            .setDefaultValue(new ModelNode().set(StartMode.LAZY.name()))
            .build()
    ;
    static final SimpleAttributeDefinition STATISTICS_ENABLED = new SimpleAttributeDefinitionBuilder(ModelKeys.STATISTICS_ENABLED, ModelType.BOOLEAN, true)
            .setXmlName(Attribute.STATISTICS_ENABLED.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setDefaultValue(new ModelNode().set(false))
            .build()
    ;

    static final AttributeDefinition[] CACHE_ATTRIBUTES = { BATCHING, CACHE_MODULE, INDEXING, INDEXING_PROPERTIES, JNDI_NAME, START, STATISTICS_ENABLED };

    // here for legacy purposes only
    static final SimpleAttributeDefinition NAME = new SimpleAttributeDefinitionBuilder(ModelKeys.NAME, ModelType.STRING, true)
            .setXmlName(Attribute.NAME.getLocalName())
            .setAllowExpression(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build()
    ;

    // metrics
    static final SimpleAttributeDefinition ACTIVATIONS = buildMetric(MetricKeys.ACTIVATIONS, ModelType.STRING);
    static final SimpleAttributeDefinition AVERAGE_READ_TIME = buildMetric(MetricKeys.AVERAGE_READ_TIME, ModelType.LONG);
    static final SimpleAttributeDefinition AVERAGE_WRITE_TIME = buildMetric(MetricKeys.AVERAGE_WRITE_TIME, ModelType.LONG);
    static final SimpleAttributeDefinition CACHE_STATUS = buildMetric(MetricKeys.CACHE_STATUS, ModelType.STRING);
    static final SimpleAttributeDefinition ELAPSED_TIME = buildMetric(MetricKeys.ELAPSED_TIME, ModelType.LONG);
    static final SimpleAttributeDefinition HIT_RATIO = buildMetric(MetricKeys.HIT_RATIO, ModelType.DOUBLE);
    static final SimpleAttributeDefinition HITS = buildMetric(MetricKeys.HITS, ModelType.LONG);
    static final SimpleAttributeDefinition INVALIDATIONS = buildMetric(MetricKeys.INVALIDATIONS, ModelType.LONG);
    static final SimpleAttributeDefinition MISSES = buildMetric(MetricKeys.MISSES, ModelType.LONG);
    static final SimpleAttributeDefinition NUMBER_OF_ENTRIES = buildMetric(MetricKeys.NUMBER_OF_ENTRIES, ModelType.INT);
    static final SimpleAttributeDefinition PASSIVATIONS = buildMetric(MetricKeys.PASSIVATIONS, ModelType.STRING);
    static final SimpleAttributeDefinition READ_WRITE_RATIO = buildMetric(MetricKeys.READ_WRITE_RATIO, ModelType.DOUBLE);
    static final SimpleAttributeDefinition REMOVE_HITS = buildMetric(MetricKeys.REMOVE_HITS, ModelType.LONG);
    static final SimpleAttributeDefinition REMOVE_MISSES = buildMetric(MetricKeys.REMOVE_MISSES, ModelType.LONG);
    static final SimpleAttributeDefinition STORES = buildMetric(MetricKeys.STORES, ModelType.LONG);
    static final SimpleAttributeDefinition TIME_SINCE_RESET = buildMetric(MetricKeys.TIME_SINCE_RESET, ModelType.LONG);

    private static SimpleAttributeDefinition buildMetric(String key, ModelType type) {
        return new SimpleAttributeDefinitionBuilder(key, type, true).setStorageRuntime().build();
    }

    static final AttributeDefinition[] CACHE_METRICS = { /*ACTIVATIONS,*/ AVERAGE_READ_TIME, AVERAGE_WRITE_TIME, CACHE_STATUS,
            ELAPSED_TIME, HIT_RATIO, HITS, INVALIDATIONS, MISSES, NUMBER_OF_ENTRIES, PASSIVATIONS, READ_WRITE_RATIO, REMOVE_HITS,
            REMOVE_MISSES, STORES, TIME_SINCE_RESET };

    private final ResolvePathHandler resolvePathHandler;
    public CacheResourceDefinition(PathElement pathElement, ResourceDescriptionResolver descriptionResolver, AbstractAddStepHandler addHandler, OperationStepHandler removeHandler, ResolvePathHandler resolvePathHandler) {
        super(pathElement, descriptionResolver, addHandler, removeHandler);
        this.resolvePathHandler = resolvePathHandler;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

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
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);

        resourceRegistration.registerSubModel(new LockingResource());
        resourceRegistration.registerSubModel(new TransactionResourceDefinition());
        resourceRegistration.registerSubModel(new EvictionResourceDefinition());
        resourceRegistration.registerSubModel(new ExpirationResourceDefinition());
        resourceRegistration.registerSubModel(new StoreResourceDefinition());
        resourceRegistration.registerSubModel(new FileStoreResourceDefinition(resolvePathHandler));
        resourceRegistration.registerSubModel(new StringKeyedJDBCStoreResourceDefinition());
        resourceRegistration.registerSubModel(new BinaryKeyedJDBCStoreResourceDefinition());
        resourceRegistration.registerSubModel(new MixedKeyedJDBCStoreResourceDefinition());
        resourceRegistration.registerSubModel(new RemoteStoreResourceDefinition());
    }
}
