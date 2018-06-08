/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.clustering.infinispan.subsystem.TableResourceDefinition.Attribute.FETCH_SIZE;
import static org.jboss.as.clustering.infinispan.subsystem.TableResourceDefinition.ColumnAttribute.DATA;
import static org.jboss.as.clustering.infinispan.subsystem.TableResourceDefinition.ColumnAttribute.ID;
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
        return this;
    }

    @Override
    public TableManipulationConfiguration get() {
        return new ConfigurationBuilder().persistence().addStore(JdbcStringBasedStoreConfigurationBuilder.class).table()
                .idColumnName(this.columns.get(ID).getKey())
                .idColumnType(this.columns.get(ID).getValue())
                .dataColumnName(this.columns.get(DATA).getKey())
                .dataColumnType(this.columns.get(DATA).getValue())
                .timestampColumnName(this.columns.get(TIMESTAMP).getKey())
                .timestampColumnType(this.columns.get(TIMESTAMP).getValue())
                .fetchSize(this.fetchSize)
                .tableNamePrefix(this.prefix)
                .create();
    }
}
