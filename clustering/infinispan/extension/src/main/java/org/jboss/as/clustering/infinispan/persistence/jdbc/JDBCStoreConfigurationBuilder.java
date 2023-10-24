/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.persistence.jdbc;

import static org.infinispan.persistence.jdbc.common.logging.Log.CONFIG;
import static org.infinispan.persistence.jdbc.configuration.TableManipulationConfiguration.BATCH_SIZE;
import static org.infinispan.persistence.jdbc.configuration.TableManipulationConfiguration.CREATE_ON_START;
import static org.infinispan.persistence.jdbc.configuration.TableManipulationConfiguration.DROP_ON_EXIT;
import static org.infinispan.persistence.jdbc.configuration.TableManipulationConfiguration.FETCH_SIZE;
import static org.infinispan.persistence.jdbc.configuration.TableManipulationConfiguration.TABLE_NAME_PREFIX;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.PrivilegedAction;
import java.util.List;

import org.infinispan.commons.configuration.Builder;
import org.infinispan.commons.configuration.Combine;
import org.infinispan.commons.configuration.Self;
import org.infinispan.commons.configuration.attributes.AttributeDefinition;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.persistence.jdbc.common.configuration.AbstractJdbcStoreConfigurationBuilder;
import org.infinispan.persistence.jdbc.common.configuration.AbstractJdbcStoreConfigurationChildBuilder;
import org.infinispan.persistence.jdbc.common.configuration.Element;
import org.infinispan.persistence.jdbc.configuration.DataColumnConfiguration;
import org.infinispan.persistence.jdbc.configuration.DataColumnConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.IdColumnConfiguration;
import org.infinispan.persistence.jdbc.configuration.IdColumnConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.SegmentColumnConfiguration;
import org.infinispan.persistence.jdbc.configuration.SegmentColumnConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.TableManipulationConfiguration;
import org.infinispan.persistence.jdbc.configuration.TimestampColumnConfiguration;
import org.infinispan.persistence.jdbc.configuration.TimestampColumnConfigurationBuilder;
import org.infinispan.persistence.keymappers.Key2StringMapper;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Builder of a {@link JDBCStoreConfiguration}.
 * @author Paul Ferraro
 */
public class JDBCStoreConfigurationBuilder extends AbstractJdbcStoreConfigurationBuilder<JDBCStoreConfiguration, JDBCStoreConfigurationBuilder> {
    private TableConfigurationBuilder table;

    public JDBCStoreConfigurationBuilder(PersistenceConfigurationBuilder builder) {
        super(builder, JDBCStoreConfiguration.attributeDefinitionSet());
        this.table = new TableConfigurationBuilder(this);
    }

    public TableConfigurationBuilder table() {
        return this.table;
    }

    public JDBCStoreConfigurationBuilder key2StringMapper(Class<? extends Key2StringMapper> mapperClass) {
        this.attributes.attribute(JDBCStoreConfiguration.KEY2STRING_MAPPER).set(mapperClass.getName());
        return this;
    }

    @Override
    public JDBCStoreConfigurationBuilder self() {
        return this;
    }

    @Override
    public JDBCStoreConfiguration create() {
        return new JDBCStoreConfiguration(this.attributes.protect(), this.async.create(), this.connectionFactory.create(), this.table.create());
    }

    @Override
    public JDBCStoreConfigurationBuilder read(JDBCStoreConfiguration template, Combine combine) {
        super.read(template, combine);
        this.table.read(template.table(), combine);
        return this;
    }

    static void validateIfSet(AttributeSet attributes, AttributeDefinition<?>... definitions) {
        for (AttributeDefinition<?> definition : definitions) {
            String value = (String) attributes.attribute(definition).get();
            if (value == null || value.isEmpty()) {
                throw CONFIG.tableManipulationAttributeNotSet(attributes.getName(), definition.name());
            }
        }
    }

    /*
     * Duplicates JdbcStringBasedStoreConfigurationBuilder.StringTableManipulationConfigurationBuilder, which is unfortunately inaccessible.
     */
    public static class TableConfigurationBuilder extends AbstractJdbcStoreConfigurationChildBuilder<JDBCStoreConfigurationBuilder> implements Builder<TableManipulationConfiguration>, Self<TableConfigurationBuilder> {
        private final AttributeSet attributes = new AttributeSet(TableManipulationConfiguration.class, Element.TABLE_JDBC_STORE, TABLE_NAME_PREFIX, BATCH_SIZE, FETCH_SIZE, CREATE_ON_START, DROP_ON_EXIT);
        // All of the following constructors are inaccessible
        private final DataColumnConfigurationBuilder dataColumn = newInstance(DataColumnConfigurationBuilder.class);
        private final IdColumnConfigurationBuilder idColumn = newInstance(IdColumnConfigurationBuilder.class);
        private final TimestampColumnConfigurationBuilder timeStampColumn = newInstance(TimestampColumnConfigurationBuilder.class);
        private final SegmentColumnConfigurationBuilder segmentColumn;

        private static <T> T newInstance(Class<T> targetClass) {
            return newInstance(targetClass, List.of(), List.of());
        }

        private static <T, C, V extends C> T newInstance(Class<T> targetClass, Class<C> argumentClass, V argument) {
            return newInstance(targetClass, List.of(argumentClass), List.of(argument));
        }

        private static <T> T newInstance(Class<T> targetClass, List<Class<?>> argumentClasses, List<Object> arguments) {
            return WildFlySecurityManager.doUnchecked(new PrivilegedAction<T>() {
                @Override
                public T run() {
                    try {
                        Constructor<T> constructor = targetClass.getDeclaredConstructor(argumentClasses.toArray(Class<?>[]::new));
                        constructor.setAccessible(true);
                        return constructor.newInstance(arguments.toArray());
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                        throw new IllegalStateException(e);
                    }
                }
            });
        }

        TableConfigurationBuilder(JDBCStoreConfigurationBuilder builder) {
            super(builder);
            this.segmentColumn = newInstance(SegmentColumnConfigurationBuilder.class, AbstractJdbcStoreConfigurationBuilder.class, builder);
        }

        @Override
        public TableConfigurationBuilder self() {
            return this;
        }

        @Override
        public AttributeSet attributes() {
           return this.attributes;
        }

        public TableConfigurationBuilder fetchSize(int fetchSize) {
            this.attributes.attribute(FETCH_SIZE).set(fetchSize);
            return this;
        }

        public TableConfigurationBuilder tableNamePrefix(String tableNamePrefix) {
            this.attributes.attribute(TABLE_NAME_PREFIX).set(tableNamePrefix);
            return this;
        }

        public TableConfigurationBuilder createOnStart(boolean createOnStart) {
            this.attributes.attribute(CREATE_ON_START).set(createOnStart);
            return self();
        }

        public TableConfigurationBuilder dropOnExit(boolean dropOnExit) {
            this.attributes.attribute(DROP_ON_EXIT).set(dropOnExit);
            return self();
        }

        public TableConfigurationBuilder idColumnName(String idColumnName) {
            this.idColumn.idColumnName(idColumnName);
            return self();
        }

        public TableConfigurationBuilder idColumnType(String idColumnType) {
            this.idColumn.idColumnType(idColumnType);
            return self();
        }

        public TableConfigurationBuilder dataColumnName(String dataColumnName) {
            this.dataColumn.dataColumnName(dataColumnName);
            return self();
        }

        public TableConfigurationBuilder dataColumnType(String dataColumnType) {
            this.dataColumn.dataColumnType(dataColumnType);
            return self();
        }

        public TableConfigurationBuilder timestampColumnName(String timestampColumnName) {
            this.timeStampColumn.dataColumnName(timestampColumnName);
            return self();
        }

        public TableConfigurationBuilder timestampColumnType(String timestampColumnType) {
            this.timeStampColumn.dataColumnType(timestampColumnType);
            return self();
        }

        public TableConfigurationBuilder segmentColumnName(String segmentColumnName) {
            this.segmentColumn.columnName(segmentColumnName);
            return self();
        }

        public TableConfigurationBuilder segmentColumnType(String segmentColumnType) {
            this.segmentColumn.columnType(segmentColumnType);
            return self();
        }

        @Override
        public void validate() {
            JDBCStoreConfigurationBuilder.validateIfSet(this.attributes, TABLE_NAME_PREFIX);
            this.idColumn.validate();
            this.dataColumn.validate();
            this.timeStampColumn.validate();
            this.segmentColumn.validate();
        }

        @Override
        public void validate(GlobalConfiguration globalConfig) {
        }

        @Override
        public TableManipulationConfiguration create() {
            return newInstance(TableManipulationConfiguration.class, List.of(AttributeSet.class, IdColumnConfiguration.class, DataColumnConfiguration.class, TimestampColumnConfiguration.class, SegmentColumnConfiguration.class), List.of(this.attributes.protect(), this.idColumn.create(), this.dataColumn.create(), this.timeStampColumn.create(), this.segmentColumn.create()));
        }

        @Override
        public TableConfigurationBuilder read(TableManipulationConfiguration template, Combine combine) {
            this.attributes.read(template.attributes(), combine);
            this.idColumn.read(template.idColumnConfiguration(), combine);
            this.dataColumn.read(template.dataColumnConfiguration(), combine);
            this.timeStampColumn.read(template.timeStampColumnConfiguration(), combine);
            this.segmentColumn.read(template.segmentColumnConfiguration(), combine);
            return this;
        }
    }
}
