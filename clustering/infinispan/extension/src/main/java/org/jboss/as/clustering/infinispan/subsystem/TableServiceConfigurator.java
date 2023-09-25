/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.TableResourceDefinition.Attribute.CREATE_ON_START;
import static org.jboss.as.clustering.infinispan.subsystem.TableResourceDefinition.Attribute.DROP_ON_STOP;
import static org.jboss.as.clustering.infinispan.subsystem.TableResourceDefinition.Attribute.FETCH_SIZE;
import static org.jboss.as.clustering.infinispan.subsystem.TableResourceDefinition.ColumnAttribute.DATA;
import static org.jboss.as.clustering.infinispan.subsystem.TableResourceDefinition.ColumnAttribute.ID;
import static org.jboss.as.clustering.infinispan.subsystem.TableResourceDefinition.ColumnAttribute.SEGMENT;
import static org.jboss.as.clustering.infinispan.subsystem.TableResourceDefinition.ColumnAttribute.TIMESTAMP;

import java.util.AbstractMap;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.TableManipulationConfiguration;
import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.clustering.infinispan.subsystem.TableResourceDefinition.ColumnAttribute;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * @author Paul Ferraro
 */
public class TableServiceConfigurator extends ComponentServiceConfigurator<TableManipulationConfiguration> {

    private final Attribute prefixAttribute;
    private final Map<ColumnAttribute, Map.Entry<String, String>> columns = new EnumMap<>(ColumnAttribute.class);

    private volatile int fetchSize;
    private volatile String prefix;
    private volatile boolean createOnStart;
    private volatile boolean dropOnStop;

    public TableServiceConfigurator(Attribute prefixAttribute, PathAddress address) {
        super(CacheComponent.STRING_TABLE, address.getParent());
        this.prefixAttribute = prefixAttribute;
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        for (ColumnAttribute column : EnumSet.allOf(ColumnAttribute.class)) {
            ModelNode columnModel = column.resolveModelAttribute(context, model);
            String name = column.getColumnName().resolveModelAttribute(context, columnModel).asString();
            String type = column.getColumnType().resolveModelAttribute(context, columnModel).asString();
            this.columns.put(column, new AbstractMap.SimpleImmutableEntry<>(name, type));
        }

        this.fetchSize = FETCH_SIZE.resolveModelAttribute(context, model).asInt();
        this.prefix = this.prefixAttribute.resolveModelAttribute(context, model).asString();
        this.createOnStart = CREATE_ON_START.resolveModelAttribute(context, model).asBoolean();
        this.dropOnStop = DROP_ON_STOP.resolveModelAttribute(context, model).asBoolean();
        return this;
    }

    @Override
    public TableManipulationConfiguration get() {
        return new ConfigurationBuilder().persistence().addStore(JdbcStringBasedStoreConfigurationBuilder.class).table()
                .createOnStart(this.createOnStart)
                .dropOnExit(this.dropOnStop)
                .idColumnName(this.columns.get(ID).getKey())
                .idColumnType(this.columns.get(ID).getValue())
                .dataColumnName(this.columns.get(DATA).getKey())
                .dataColumnType(this.columns.get(DATA).getValue())
                .segmentColumnName(this.columns.get(SEGMENT).getKey())
                .segmentColumnType(this.columns.get(SEGMENT).getValue())
                .timestampColumnName(this.columns.get(TIMESTAMP).getKey())
                .timestampColumnType(this.columns.get(TIMESTAMP).getValue())
                .fetchSize(this.fetchSize)
                .tableNamePrefix(this.prefix)
                .create();
    }
}
