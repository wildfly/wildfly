/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.cmp.jdbc.metadata;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.as.cmp.CmpMessages;
import static org.jboss.as.cmp.CmpMessages.MESSAGES;
import org.jboss.as.cmp.jdbc.metadata.parser.ParsedCmpField;
import org.jboss.as.cmp.jdbc.metadata.parser.ParsedEntity;
import org.jboss.as.cmp.jdbc.metadata.parser.ParsedQuery;
import org.jboss.metadata.ejb.spec.CMPFieldMetaData;
import org.jboss.metadata.ejb.spec.EntityBeanMetaData;
import org.jboss.metadata.ejb.spec.QueryMetaData;

/**
 * This immutable class contains information about an entity
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="sebastien.alborini@m4x.org">Sebastien Alborini</a>
 * @author <a href="mailto:dirk@jboss.de">Dirk Zimmermann</a>
 * @author <a href="mailto:loubyansky@hotmail.com">Alex Loubyansky</a>
 * @author <a href="mailto:heiko.rupp@cellent.de">Heiko W. Rupp</a>
 * @version $Revision: 81030 $
 */
public final class JDBCEntityMetaData {
    /**
     * application metadata in which this entity is defined
     */
    private final JDBCApplicationMetaData jdbcApplication;

    /**
     * data source name in jndi
     */
    private final String dataSourceName;

    /**
     * type mapping used for this entity
     */
    private final String dataSourceMappingName;

    /**
     * the name of this entity
     */
    private final String entityName;

    /**
     * the abstract schema name of this entity
     */
    private final String abstractSchemaName;

    /**
     * the implementation class of this entity
     */
    private final Class<?> entityClass;

    /**
     * the home class of this entity
     */
    private final Class<?> homeClass;

    /**
     * the remote class of this entity
     */
    private final Class<?> remoteClass;

    /**
     * the local home class of this entity
     */
    private final Class<?> localHomeClass;

    /**
     * the local class of this entity
     */
    private final Class<?> localClass;

    /**
     * Does this entity use cmp 1.x?
     */
    private final boolean isCMP1x;

    /**
     * the name of the table to which this entity is persisted
     */
    private final String tableName;

    /**
     * Should we try and create the table when deployed?
     */
    private final boolean createTable;

    /**
     * Should we drop the table when undeployed?
     */
    private final boolean removeTable;

    /**
     * Should we alter the table when deployed?
     */
    private final boolean alterTable;

    /**
     * What command should be issued directly after creation
     * of a table?
     */
    private final List<String> tablePostCreateCmd = new ArrayList<String>();

    /**
     * Should we use 'SELECT ... FOR UPDATE' syntax when loading?
     */
    private final boolean rowLocking;

    /**
     * Is this entity read-only?
     */
    private final boolean readOnly;

    /**
     * how long is a read valid
     */
    private final Integer readTimeOut;

    /**
     * Should the table have a primary key constraint?
     */
    private final boolean primaryKeyConstraint;

    /**
     * the java class of the primary key
     */
    private final Class<?> primaryKeyClass;

    /**
     * the name of the primary key field or null if the primary key field
     * is multivalued
     */
    private final String primaryKeyFieldName;

    /**
     * Map of the cmp fields of this entity by field name.
     */
    private final Map<String, JDBCCMPFieldMetaData> cmpFieldsByName = new HashMap<String, JDBCCMPFieldMetaData>();
    private final List<JDBCCMPFieldMetaData> cmpFields = new ArrayList<JDBCCMPFieldMetaData>();

    /**
     * A map of all the load groups by name.
     */
    private final Map<String, List<String>> loadGroups = new HashMap<String, List<String>>();

    /**
     * The fields which should always be loaded when an entity of this type
     * is loaded.
     */
    private final String eagerLoadGroup;

    /**
     * A list of groups (also lists) of the fields that should be lazy
     * loaded together.
     */
    private final List<String> lazyLoadGroups = new ArrayList<String>();

    /**
     * Map of the queries on this entity by the Method that invokes the query.
     */
    private final Map<Method, JDBCQueryMetaData> queries = new HashMap<Method, JDBCQueryMetaData>();

    /**
     * The factory used to used to create query meta data
     */
    private final JDBCQueryMetaDataFactory queryFactory = new JDBCQueryMetaDataFactory(this);

    /**
     * The read ahead meta data
     */
    private final JDBCReadAheadMetaData readAhead;

    /**
     * clean-read-ahead-on-load
     * Since 3.2.5RC1, previously read ahead cache was cleaned after loading.
     */
    private final boolean cleanReadAheadOnLoad;

    /**
     * The maximum number of read ahead lists that can be tracked for this
     * entity.
     */
    private final int listCacheMax;

    /**
     * The number of entities to read in one round-trip to the
     * underlying data store.
     */
    private final int fetchSize;

    /**
     * entity command meta data
     */
    private final JDBCEntityCommandMetaData entityCommand;


    /**
     * optimistic locking metadata
     */
    private final JDBCOptimisticLockingMetaData optimisticLocking;


    /**
     * audit metadata
     */
    private final JDBCAuditMetaData audit;

    private final Class<?> qlCompiler;

    /**
     * throw runtime exception metadata
     */
    private final boolean throwRuntimeExceptions;


    public JDBCEntityMetaData(final JDBCApplicationMetaData jdbcApplication) {
        this.jdbcApplication = jdbcApplication;
        listCacheMax = 1000;
        fetchSize = 0;
        entityName = null;
        entityClass = null;
        primaryKeyClass = null;
        isCMP1x = false;
        primaryKeyFieldName = null;
        homeClass = null;
        remoteClass = null;
        localHomeClass = null;
        localClass = null;
        abstractSchemaName = null;
        dataSourceName = null;
        dataSourceMappingName = null;
        tableName = null;
        createTable = false;
        removeTable = false;
        alterTable = false;
        readOnly = false;
        readTimeOut = -1;
        rowLocking = false;
        primaryKeyConstraint = false;
        entityCommand = null;
        qlCompiler = null;
        throwRuntimeExceptions = false;
        eagerLoadGroup = "*";
        readAhead = JDBCReadAheadMetaData.DEFAULT;
        cleanReadAheadOnLoad = false;
        optimisticLocking = null;
        audit = null;

    }

    /**
     * Constructs jdbc entity meta data defined in the jdbcApplication and
     * with the data from the entity meta data which is loaded from the
     * ejb-jar.xml file.
     *
     * @param jdbcApplication the application in which this entity is defined
     * @param entity          the entity meta data for this entity that is loaded
     *                        from the ejb-jar.xml file
     */
    public JDBCEntityMetaData(JDBCApplicationMetaData jdbcApplication, EntityBeanMetaData entity) {
        this.jdbcApplication = jdbcApplication;
        entityName = entity.getEjbName();
        listCacheMax = 1000;
        fetchSize = 0;

        final ClassLoader classLoader = jdbcApplication.getClassLoader();
        try {
            entityClass = classLoader.loadClass(entity.getEjbClass());
        } catch (ClassNotFoundException e) {
            throw MESSAGES.failedToLoadEntityClass(e);
        }
        try {
            primaryKeyClass = classLoader.loadClass(entity.getPrimKeyClass());
        } catch (ClassNotFoundException e) {
            throw MESSAGES.failedToLoadPkClass(e);
        }

        isCMP1x = entity.isCMP1x();
        if (isCMP1x) {
            abstractSchemaName = (entity.getAbstractSchemaName() == null ? entityName : entity.getAbstractSchemaName());
        } else {
            abstractSchemaName = entity.getAbstractSchemaName();
        }

        primaryKeyFieldName = entity.getPrimKeyField();

        String home = entity.getHome();
        if (home != null) {
            try {
                homeClass = classLoader.loadClass(home);
            } catch (ClassNotFoundException e) {
                throw MESSAGES.failedToLoadHomeClass(e);
            }
            try {
                remoteClass = classLoader.loadClass(entity.getRemote());
            } catch (ClassNotFoundException e) {
                throw MESSAGES.failedToLoadRemoteClass(e);
            }
        } else {
            homeClass = null;
            remoteClass = null;
        }

        String localHome = entity.getLocalHome();
        if (localHome != null) {
            try {
                localHomeClass = classLoader.loadClass(localHome);
            } catch (ClassNotFoundException e) {
                throw MESSAGES.failedToLoadLocalHomeClass(e);
            }
            try {
                localClass = classLoader.loadClass(entity.getLocal());
            } catch (ClassNotFoundException e) {
                throw MESSAGES.failedToLoadLocalClass(e);
            }
        } else {
            // we must have a home or local home
            if (home == null) {
                throw MESSAGES.entityMustHaveHome(entityName);
            }

            localHomeClass = null;
            localClass = null;
        }

        // we replace the . by _ because some dbs die on it...
        // the table name may be overridden in importXml(jbosscmp-jdbc.xml)
        tableName = entityName.replace('.', '_');

        // Warn: readTimeOut should be setup before cmp fields are created
        // otherwise readTimeOut in cmp fields will be set to 0 by default
        dataSourceName = null;
        dataSourceMappingName = null;
        createTable = false;
        removeTable = false;
        alterTable = false;
        rowLocking = false;
        primaryKeyConstraint = false;
        readOnly = false;
        readTimeOut = -1;
        qlCompiler = null;
        throwRuntimeExceptions = false;

        // build the metadata for the cmp fields now in case there is
        // no jbosscmp-jdbc.xml

        if (entity.getCmpFields() != null) for (CMPFieldMetaData cmpFieldMetaData : entity.getCmpFields()) {
            JDBCCMPFieldMetaData cmpField = new JDBCCMPFieldMetaData(this, cmpFieldMetaData.getFieldName());
            cmpFields.add(cmpField);
            cmpFieldsByName.put(cmpFieldMetaData.getFieldName(), cmpField);
        }

        // AL: add unknown primary key if primaryKeyClass is Object
        // AL: this is set up only in this constructor
        // AL: because, AFAIK, others are called with default value
        // AL: produced by this one
        if (primaryKeyClass == java.lang.Object.class) {
            JDBCCMPFieldMetaData upkField = new JDBCCMPFieldMetaData(this);
            cmpFields.add(upkField);
            cmpFieldsByName.put(upkField.getFieldName(), upkField);
        }

        eagerLoadGroup = "*";

        if (entity.getQueries() != null) for (QueryMetaData queryData : entity.getQueries()) {
            queries.putAll(queryFactory.createJDBCQueryMetaData(queryData));
        }

        readAhead = JDBCReadAheadMetaData.DEFAULT;
        cleanReadAheadOnLoad = false;
        entityCommand = null;
        optimisticLocking = null;
        audit = null;
    }

    public JDBCEntityMetaData(JDBCApplicationMetaData jdbcApplication, JDBCEntityMetaData defaultValues) {
        this.jdbcApplication = jdbcApplication;
        entityName = defaultValues.entityName;
        entityClass = defaultValues.entityClass;
        primaryKeyClass = defaultValues.primaryKeyClass;
        isCMP1x = defaultValues.isCMP1x;
        primaryKeyFieldName = defaultValues.primaryKeyFieldName;
        homeClass = defaultValues.homeClass;
        remoteClass = defaultValues.remoteClass;
        localHomeClass = defaultValues.localHomeClass;
        localClass = defaultValues.localClass;
        abstractSchemaName = defaultValues.abstractSchemaName;
        dataSourceName = defaultValues.dataSourceName;
        dataSourceMappingName = defaultValues.dataSourceMappingName;
        tableName = defaultValues.tableName;
        createTable = defaultValues.createTable;
        removeTable = defaultValues.removeTable;
        alterTable = defaultValues.alterTable;
        tablePostCreateCmd.addAll(defaultValues.tablePostCreateCmd);
        readOnly = defaultValues.readOnly;
        readTimeOut = defaultValues.readTimeOut;
        rowLocking = defaultValues.rowLocking;
        primaryKeyConstraint = defaultValues.primaryKeyConstraint;
        listCacheMax = defaultValues.listCacheMax;
        fetchSize = defaultValues.fetchSize;
        entityCommand = defaultValues.entityCommand;
        qlCompiler = defaultValues.qlCompiler;
        throwRuntimeExceptions = defaultValues.throwRuntimeExceptions;

        for (JDBCCMPFieldMetaData cmpField : defaultValues.cmpFields) {
            JDBCCMPFieldMetaData newCmpField = new JDBCCMPFieldMetaData(this, cmpField);
            cmpFields.add(cmpField);
            cmpFieldsByName.put(cmpField.getFieldName(), newCmpField);
        }

        loadGroups.putAll(defaultValues.loadGroups);
        eagerLoadGroup = defaultValues.eagerLoadGroup;
        readAhead = defaultValues.readAhead;
        cleanReadAheadOnLoad = defaultValues.cleanReadAheadOnLoad;
        optimisticLocking = defaultValues.optimisticLocking;
        audit = defaultValues.audit;


        for (JDBCQueryMetaData query : defaultValues.queries.values()) {
            queries.put(query.getMethod(), queryFactory.createJDBCQueryMetaData(query, readAhead, qlCompiler));
        }
    }

    public JDBCEntityMetaData(JDBCApplicationMetaData jdbcApplication, ParsedEntity parsed, JDBCEntityMetaData defaultValues) {
        // store passed in application... application in defaultValues may
        // be different because jdbcApplication is immutable
        this.jdbcApplication = jdbcApplication;

        // set default values
        entityName = defaultValues.getName();
        entityClass = defaultValues.getEntityClass();
        primaryKeyClass = defaultValues.getPrimaryKeyClass();
        isCMP1x = defaultValues.isCMP1x;
        primaryKeyFieldName = defaultValues.getPrimaryKeyFieldName();
        homeClass = defaultValues.getHomeClass();
        remoteClass = defaultValues.getRemoteClass();
        localHomeClass = defaultValues.getLocalHomeClass();
        localClass = defaultValues.getLocalClass();

        if (isCMP1x) {
            abstractSchemaName = (defaultValues.getAbstractSchemaName() == null ?
                    entityName : defaultValues.getAbstractSchemaName());
        } else {
            abstractSchemaName = defaultValues.getAbstractSchemaName();
        }

        // datasource name
        if (parsed.getDataSourceName() != null) {
            dataSourceName = parsed.getDataSourceName();
        } else {
            dataSourceName = defaultValues.getDataSourceName();
        }

        // get the datasource mapping for this datasource (optional, but always
        // set in standardjbosscmp-jdbc.xml)
        if (parsed.getDataSourceMappingName() != null) {
            dataSourceMappingName = parsed.getDataSourceMappingName();
        } else {
            dataSourceMappingName = defaultValues.dataSourceMappingName;
        }

        // get table name
        if (parsed.getTableName() != null) {
            tableName = parsed.getTableName();
        } else {
            tableName = defaultValues.getDefaultTableName();
        }

        // create table?  If not provided, keep default.
        if (parsed.getCreateTable() != null) {
            createTable = parsed.getCreateTable();
        } else {
            createTable = defaultValues.getCreateTable();
        }

        // remove table?  If not provided, keep default.
        if (parsed.getRemoveTable() != null) {
            removeTable = parsed.getRemoveTable();
        } else {
            removeTable = defaultValues.getRemoveTable();
        }

        // alter table?  If not provided, keep default.
        if (parsed.getAlterTable() != null) {
            alterTable = parsed.getAlterTable();
        } else {
            alterTable = defaultValues.getAlterTable();
        }

        // get the SQL command to execute after table creation
        if (!parsed.getTablePostCreateCmd().isEmpty()) {
            tablePostCreateCmd.addAll(parsed.getTablePostCreateCmd());
        } else {
            tablePostCreateCmd.addAll(defaultValues.getDefaultTablePostCreateCmd());
        }

        // read-only
        if (parsed.getReadOnly() != null) {
            readOnly = parsed.getReadOnly();
        } else {
            readOnly = defaultValues.isReadOnly();
        }

        // read-time-out
        if (parsed.getReadTimeOut() != null) {
            readTimeOut = parsed.getReadTimeOut();
        } else {
            readTimeOut = defaultValues.getReadTimeOut();
        }

        if (parsed.getRowLocking() != null) {
            rowLocking = !isReadOnly() && parsed.getRowLocking();
        } else {
            rowLocking = defaultValues.hasRowLocking();
        }

        // primary key constraint?  If not provided, keep default.
        if (parsed.getPrimaryKeyConstraint() != null) {
            primaryKeyConstraint = parsed.getPrimaryKeyConstraint();
        } else {
            primaryKeyConstraint = defaultValues.hasPrimaryKeyConstraint();
        }

        // list-cache-max
        Integer listCacheMaxInt = parsed.getListCacheMax();
        if (listCacheMaxInt != null) {
            listCacheMax = listCacheMaxInt;
            if (listCacheMax < 0) {
                throw MESSAGES.negativeListCacheMax(listCacheMax);
            }
        } else {
            listCacheMax = defaultValues.getListCacheMax();
        }

        // fetch-size
        Integer fetchSizeInt = parsed.getFetchSize();
        if (fetchSizeInt != null) {
            fetchSize = fetchSizeInt;
            if (fetchSize < 0) {
                throw MESSAGES.negativeFetchSize(fetchSize);
            }
        } else {
            fetchSize = defaultValues.getFetchSize();
        }

        if (parsed.getQlCompiler() == null) {
            qlCompiler = defaultValues.qlCompiler;
        } else {
            qlCompiler = parsed.getQlCompiler();
        }

        // throw runtime exceptions ?  If not provided, keep default.
        if (parsed.getThrowRuntimeExceptions() != null) {
            throwRuntimeExceptions = parsed.getThrowRuntimeExceptions();
        } else {
            throwRuntimeExceptions = defaultValues.getThrowRuntimeExceptions();
        }


        //
        // cmp fields
        //

        // update all existing queries with the new read ahead value
        for (JDBCCMPFieldMetaData cmpField : defaultValues.cmpFields) {
            cmpFields.add(cmpField);
            cmpFieldsByName.put(cmpField.getFieldName(), cmpField);
        }

        // apply new configurations to the cmpfields
        for (ParsedCmpField parsedField : parsed.getCmpFields()) {
            String fieldName = parsedField.getFieldName();
            JDBCCMPFieldMetaData oldCMPField = cmpFieldsByName.get(fieldName);
            if (oldCMPField == null) {
                throw MESSAGES.cmpFieldNotFound(fieldName, entityName);
            }
            JDBCCMPFieldMetaData cmpFieldMetaData = new JDBCCMPFieldMetaData(this, parsedField, oldCMPField);

            // replace the old cmp meta data with the new
            cmpFieldsByName.put(fieldName, cmpFieldMetaData);
            int index = cmpFields.indexOf(oldCMPField);
            cmpFields.remove(oldCMPField);
            cmpFields.add(index, cmpFieldMetaData);
        }

        // unknown primary key field
        if (primaryKeyClass == java.lang.Object.class) {
            if (parsed.getUpkField() != null) {
                // assume now there is only one upk field
                JDBCCMPFieldMetaData oldUpkField = null;
                for (JDBCCMPFieldMetaData cmpField : cmpFields) {
                    if (cmpField.isUnknownPkField()) {
                        oldUpkField = cmpField;
                        break;
                    }
                }

                // IMO, this is a redundant check
                if (oldUpkField == null) {
                    oldUpkField = new JDBCCMPFieldMetaData(this);
                }

                JDBCCMPFieldMetaData upkField = new JDBCCMPFieldMetaData(this, parsed.getUpkField(), oldUpkField);

                // remove old upk field
                cmpFieldsByName.remove(oldUpkField.getFieldName());
                cmpFieldsByName.put(upkField.getFieldName(), upkField);

                int oldUpkFieldInd = cmpFields.indexOf(oldUpkField);
                cmpFields.remove(oldUpkField);
                cmpFields.add(oldUpkFieldInd, upkField);
            }
        }

        // load-loads
        loadGroups.putAll(defaultValues.loadGroups);
        loadGroups.putAll(parsed.getLoadGroups());

        // eager-load
        if (parsed.getEagerLoadGroup() != null) {
            if (!parsed.getEagerLoadGroup().equals("*") && !loadGroups.containsKey(parsed.getEagerLoadGroup())) {
                throw CmpMessages.MESSAGES.eagerLoadGroupNotFound(parsed.getEagerLoadGroup());
            }
            eagerLoadGroup = parsed.getEagerLoadGroup();
        } else {
            eagerLoadGroup = defaultValues.getEagerLoadGroup();
        }

        // lazy-loads
        lazyLoadGroups.addAll(defaultValues.lazyLoadGroups);
        lazyLoadGroups.addAll(parsed.getLazyLoadGroups());

        // read-ahead
        if (parsed.getReadAhead() != null) {
            readAhead = new JDBCReadAheadMetaData(parsed.getReadAhead(), defaultValues.getReadAhead());
        } else {
            readAhead = defaultValues.readAhead;
        }

        if (parsed.getCleanReadAheadOnLoad() != null) {
            cleanReadAheadOnLoad = parsed.getCleanReadAheadOnLoad();
        } else {
            cleanReadAheadOnLoad = defaultValues.cleanReadAheadOnLoad;
        }

        // optimistic locking group
        if (parsed.getOptimisticLocking() != null) {
            optimisticLocking = new JDBCOptimisticLockingMetaData(this, parsed.getOptimisticLocking());
        } else {
            optimisticLocking = defaultValues.getOptimisticLocking();
        }

        // audit
        if (parsed.getAudit() != null) {
            audit = new JDBCAuditMetaData(this, parsed.getAudit());
        } else {
            audit = defaultValues.getAudit();
        }

        // queries

        // update all existing queries with the new read ahead value
        for (JDBCQueryMetaData query : defaultValues.queries.values()) {
            query = queryFactory.createJDBCQueryMetaData(query, readAhead, qlCompiler);
            queries.put(query.getMethod(), query);
        }

        // apply new configurations to the queries
        for (ParsedQuery parsedQuery : parsed.getQueries()) {
            // overrides defaults added above
            for (JDBCQueryMetaData queryMetaData : queryFactory.createJDBCQueryMetaData(parsedQuery)) {
                queries.put(queryMetaData.getMethod(), queryMetaData);
            }
        }

        // get the entity command for this entity
        if (parsed.getEntityCommand() != null) {
            String entityCommandName = parsed.getEntityCommand().getCommandName();
            JDBCEntityCommandMetaData defaultEntityCommand = defaultValues.getEntityCommand();

            if ((defaultEntityCommand == null) || (!entityCommandName.equals(defaultEntityCommand.getCommandName()))) {
                defaultEntityCommand = jdbcApplication.getEntityCommandByName(entityCommandName);
            }

            if (defaultEntityCommand != null) {
                entityCommand = new JDBCEntityCommandMetaData(parsed.getEntityCommand(), defaultEntityCommand);
            } else {
                entityCommand = parsed.getEntityCommand();
            }
        } else {
            entityCommand = defaultValues.getEntityCommand();
        }
    }

    /**
     * Gets the meta data for the application of which this entity is a member.
     *
     * @return the meta data for the application that this entity is a member
     */
    public JDBCApplicationMetaData getJDBCApplication() {
        return jdbcApplication;
    }

    /**
     * Gets the name of the datasource in jndi for this entity
     *
     * @return the name of datasource in jndi
     */
    public String getDataSourceName() {
        return dataSourceName;
    }

    /**
     * Gets the jdbc type mapping for this entity
     *
     * @return the jdbc type mapping for this entity
     */
    public JDBCTypeMappingMetaData getTypeMapping() {
        if (dataSourceMappingName == null) {
            return null;
        }
        final JDBCTypeMappingMetaData typeMapping = jdbcApplication.getTypeMappingByName(dataSourceMappingName);
        if (typeMapping == null) {
            throw MESSAGES.typeMappingNotInitialized(dataSourceName);
        }

        return typeMapping;
    }

    /**
     * Gets the name of this entity. The name come from the ejb-jar.xml file.
     *
     * @return the name of this entity
     */
    public String getName() {
        return entityName;
    }

    /**
     * Gets the abstract schema name of this entity. The name come from
     * the ejb-jar.xml file.
     *
     * @return the abstract schema name of this entity
     */
    public String getAbstractSchemaName() {
        return abstractSchemaName;
    }

    /**
     * Gets the implementation class of this entity
     *
     * @return the implementation class of this entity
     */
    public Class<?> getEntityClass() {
        return entityClass;
    }

    /**
     * Gets the home class of this entity
     *
     * @return the home class of this entity
     */
    public Class<?> getHomeClass() {
        return homeClass;
    }

    /**
     * Gets the remote class of this entity
     *
     * @return the remote class of this entity
     */
    public Class<?> getRemoteClass() {
        return remoteClass;
    }

    /**
     * Gets the local home class of this entity
     *
     * @return the local home class of this entity
     */
    public Class<?> getLocalHomeClass() {
        return localHomeClass;
    }

    /**
     * Gets the local class of this entity
     *
     * @return the local class of this entity
     */
    public Class<?> getLocalClass() {
        return localClass;
    }

    /**
     * Does this entity use CMP version 1.x
     *
     * @return true if this entity used CMP version 1.x; otherwise false
     */
    public boolean isCMP1x() {
        return isCMP1x;
    }

    /**
     * Gets the cmp fields of this entity
     *
     * @return an unmodifiable collection of JDBCCMPFieldMetaData objects
     */
    public List<JDBCCMPFieldMetaData> getCMPFields() {
        return Collections.unmodifiableList(cmpFields);
    }

    /**
     * Gets the name of the eager load group. This name can be used to
     * look up the load group.
     *
     * @return the name of the eager load group
     */
    public String getEagerLoadGroup() {
        return eagerLoadGroup;
    }

    /**
     * Gets the collection of lazy load group names.
     *
     * @return an unmodifiable collection of load group names
     */
    public List<String> getLazyLoadGroups() {
        return Collections.unmodifiableList(lazyLoadGroups);
    }

    /**
     * Gets the map from load group name to a List of field names, which
     * forms a logical load group.
     *
     * @return an unmodifiable map of load groups (Lists) by group name.
     */
    public Map<String, List<String>> getLoadGroups() {
        return Collections.unmodifiableMap(loadGroups);
    }

    /**
     * Gets the load group with the specified name.
     *
     * @return the load group with the specified name
     */
    public List<String> getLoadGroup(String name) {
        List<String> group = loadGroups.get(name);
        if (group == null) {
            throw MESSAGES.unknownLoadGroup(name);
        }
        return group;
    }

    /**
     * Returns optimistic locking metadata
     */
    public JDBCOptimisticLockingMetaData getOptimisticLocking() {
        return optimisticLocking;
    }

    /**
     * Returns audit metadata
     */
    public JDBCAuditMetaData getAudit() {
        return audit;
    }

    /**
     * Gets the cmp field with the specified name
     *
     * @param name the name of the desired field
     * @return the cmp field with the specified name or null if not found
     */
    public JDBCCMPFieldMetaData getCMPFieldByName(String name) {
        return cmpFieldsByName.get(name);
    }

    /**
     * Gets the name of the table to which this entity is persisted
     *
     * @return the name of the table to which this entity is persisted
     */
    public String getDefaultTableName() {
        return tableName;
    }

    /**
     * Gets the flag used to determine if the store manager should attempt to
     * create database table when the entity is deployed.
     *
     * @return true if the store manager should attempt to create the table
     */
    public boolean getCreateTable() {
        return createTable;
    }

    /**
     * Gets the flag used to determine if the store manager should attempt to
     * remove database table when the entity is undeployed.
     *
     * @return true if the store manager should attempt to remove the table
     */
    public boolean getRemoveTable() {
        return removeTable;
    }

    /**
     * Gets the flag used to determine if the store manager should attempt to
     * alter table when the entity is deployed.
     */
    public boolean getAlterTable() {
        return alterTable;
    }

    /**
     * Get the (user-defined) SQL commands that should be issued after table
     * creation
     *
     * @return the SQL command to issue to the DB-server
     */
    public List<String> getDefaultTablePostCreateCmd() {
        return tablePostCreateCmd;
    }

    /**
     * Gets the flag used to determine if the store manager should add a
     * primary key constraint when creating the table
     *
     * @return true if the store manager should add a primary key constraint to
     *         the create table sql statement
     */
    public boolean hasPrimaryKeyConstraint() {
        return primaryKeyConstraint;
    }

    /**
     * Gets the flag used to determine if the store manager should do row locking
     * when loading entity beans
     *
     * @return true if the store manager should add a row locking
     *         clause when selecting data from the table
     */
    public boolean hasRowLocking() {
        return rowLocking;
    }

    /**
     * The maximum number of query result lists that will be tracked.
     */
    public int getListCacheMax() {
        return listCacheMax;
    }

    /**
     * The number of rows that the database driver should get in a single
     * trip to the database.
     */
    public int getFetchSize() {
        return fetchSize;
    }

    /**
     * Gets the queries defined on this entity
     *
     * @return an unmodifiable collection of JDBCQueryMetaData objects
     */
    public Collection<JDBCQueryMetaData> getQueries() {
        return Collections.unmodifiableCollection(queries.values());
    }

    /**
     * @param method finder method name.
     * @return corresponding query metadata or null.
     */
    public JDBCQueryMetaData getQueryMetaDataForMethod(Method method) {
        return (JDBCQueryMetaData) queries.get(method);
    }

    /**
     * Get the relationship roles of this entity.
     * Items are instance of JDBCRelationshipRoleMetaData.
     *
     * @return an unmodifiable collection of the relationship roles defined
     *         for this entity
     */
    public Collection<JDBCRelationshipRoleMetaData> getRelationshipRoles() {
        return jdbcApplication.getRolesForEntity(entityName);
    }

    /**
     * Gets the primary key class for this entity
     *
     * @return the primary key class for this entity
     */
    public Class<?> getPrimaryKeyClass() {
        return primaryKeyClass;
    }

    /**
     * Gets the entity command metadata
     *
     * @return the entity command metadata
     */
    public JDBCEntityCommandMetaData getEntityCommand() {
        return entityCommand;
    }

    /**
     * Is this entity read only? A readonly entity will never be stored into
     * the database.
     *
     * @return true if this entity is read only
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * How long is a read of this entity valid. This property should only be
     * used on read only entities, and determines how long the data read from
     * the database is valid. When the read times out it should be reread from
     * the database. If the value is -1 and the entity is not using commit
     * option a, the read is only valid for the length of the transaction in
     * which it was loaded.
     *
     * @return the length of time that a read is valid or -1 if the read is only
     *         valid for the length of the transaction
     */
    public int getReadTimeOut() {
        return readTimeOut != null ? readTimeOut : -1;
    }

    /**
     * Gets the name of the primary key field of this entity or null if
     * the primary key is multivalued
     *
     * @return the name of the primary key field of this entity or null
     *         if the primary key is multivalued
     */
    public String getPrimaryKeyFieldName() {
        return primaryKeyFieldName;
    }


    /**
     * Gets the read ahead meta data for this entity.
     *
     * @return the read ahead meta data for this entity.
     */
    public JDBCReadAheadMetaData getReadAhead() {
        return readAhead;
    }

    public Class<?> getQlCompiler() {
        return qlCompiler;
    }

    /**
     * Is the throw-runtime-exceptions meta data for this entity is true.
     *
     * @return the throw-runtime-exceptions meta data for this entity.
     */
    public boolean isThrowRuntimeExceptions() {
        return throwRuntimeExceptions;
    }

    /**
     * Gets the throw-runtime-exceptions meta data for this entity.
     *
     * @return the throw-runtime-exceptions meta data for this entity.
     */
    public boolean getThrowRuntimeExceptions() {
        return throwRuntimeExceptions;
    }


    public boolean isCleanReadAheadOnLoad() {
        return cleanReadAheadOnLoad;
    }

    /**
     * Compares this JDBCEntityMetaData against the specified object. Returns
     * true if the objects are the same. Two JDBCEntityMetaData are the same
     * if they both have the same name and are defined in the same application.
     *
     * @param o the reference object with which to compare
     * @return true if this object is the same as the object argument;
     *         false otherwise
     */
    public boolean equals(Object o) {
        if (o instanceof JDBCEntityMetaData) {
            JDBCEntityMetaData entity = (JDBCEntityMetaData) o;
            return entityName.equals(entity.entityName) &&
                    jdbcApplication.equals(entity.jdbcApplication);
        }
        return false;
    }

    /**
     * Returns a hashcode for this JDBCEntityMetaData. The hashcode is computed
     * based on the hashCode of the declaring application and the hashCode of
     * the entityName
     *
     * @return a hash code value for this object
     */
    public int hashCode() {
        int result = 17;
        result = 37 * result + jdbcApplication.hashCode();
        result = 37 * result + entityName.hashCode();
        return result;
    }

    /**
     * Returns a string describing this JDBCEntityMetaData. The exact details
     * of the representation are unspecified and subject to change, but the
     * following may be regarded as typical:
     * <p/>
     * "[JDBCEntityMetaData: entityName=UserEJB]"
     *
     * @return a string representation of the object
     */
    public String toString() {
        return "[JDBCEntityMetaData : entityName=" + entityName + "]";
    }
}
