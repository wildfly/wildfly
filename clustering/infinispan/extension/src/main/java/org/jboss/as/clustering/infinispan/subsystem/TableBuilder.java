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

import static org.jboss.as.clustering.infinispan.subsystem.TableResourceDefinition.ColumnAttribute.*;
import static org.jboss.as.clustering.infinispan.subsystem.TableResourceDefinition.Attribute.*;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.TableManipulationConfiguration;
import org.infinispan.persistence.jdbc.configuration.TableManipulationConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfigurationBuilder.StringTableManipulationConfigurationBuilder;
import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.Builder;

/**
 * @author Paul Ferraro
 */
public class TableBuilder extends CacheComponentBuilder<TableManipulationConfiguration> implements ResourceServiceBuilder<TableManipulationConfiguration> {

    private final Attribute prefixAttribute;
    private final TableManipulationConfigurationBuilder<JdbcStringBasedStoreConfigurationBuilder, StringTableManipulationConfigurationBuilder> builder = new ConfigurationBuilder().persistence().addStore(JdbcStringBasedStoreConfigurationBuilder.class).table();

    public TableBuilder(Attribute prefixAttribute, CacheComponent component, String containerName, String cacheName) {
        super(component, containerName, cacheName);
        this.prefixAttribute = prefixAttribute;
    }

    @Override
    public Builder<TableManipulationConfiguration> configure(ExpressionResolver resolver, ModelNode model) throws OperationFailedException {
        this.builder.idColumnName(ID.getColumnName().getDefinition().resolveModelAttribute(resolver, model).asString())
                .idColumnType(ID.getColumnType().getDefinition().resolveModelAttribute(resolver, model).asString())
                .dataColumnName(DATA.getColumnName().getDefinition().resolveModelAttribute(resolver, model).asString())
                .dataColumnType(DATA.getColumnType().getDefinition().resolveModelAttribute(resolver, model).asString())
                .timestampColumnName(TIMESTAMP.getColumnName().getDefinition().resolveModelAttribute(resolver, model).asString())
                .timestampColumnType(TIMESTAMP.getColumnType().getDefinition().resolveModelAttribute(resolver, model).asString())
                .batchSize(BATCH_SIZE.getDefinition().resolveModelAttribute(resolver, model).asInt())
                .fetchSize(FETCH_SIZE.getDefinition().resolveModelAttribute(resolver, model).asInt())
                .tableNamePrefix(this.prefixAttribute.getDefinition().resolveModelAttribute(resolver, model).asString())
        ;
        return this;
    }

    @Override
    public TableManipulationConfiguration getValue() {
        return this.builder.create();
    }
}
