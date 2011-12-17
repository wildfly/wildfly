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
package org.jboss.as.cmp.jdbc.bridge;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJBException;
import javax.ejb.EJBLocalObject;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.RemoveException;
import javax.ejb.TransactionRolledbackLocalException;
import javax.sql.DataSource;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.jboss.as.cmp.bridge.EntityBridge;
import org.jboss.as.cmp.bridge.FieldBridge;
import org.jboss.as.cmp.component.CmpEntityBeanComponent;
import org.jboss.as.cmp.context.CmpEntityBeanContext;
import org.jboss.as.cmp.ejbql.Catalog;
import org.jboss.as.cmp.jdbc.CascadeDeleteStrategy;
import org.jboss.as.cmp.jdbc.JDBCContext;
import org.jboss.as.cmp.jdbc.JDBCEntityPersistenceStore;
import org.jboss.as.cmp.jdbc.JDBCParameterSetter;
import org.jboss.as.cmp.jdbc.JDBCResultSetReader;
import org.jboss.as.cmp.jdbc.JDBCStoreManager;
import org.jboss.as.cmp.jdbc.JDBCType;
import org.jboss.as.cmp.jdbc.RelationData;
import org.jboss.as.cmp.jdbc.SQLUtil;
import org.jboss.as.cmp.jdbc.metadata.JDBCCMPFieldMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCReadAheadMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCRelationMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCRelationshipRoleMetaData;
import org.jboss.logging.Logger;
import org.jboss.tm.TransactionLocal;

/**
 * JDBCCMRFieldBridge a bean relationship. This class only supports
 * relationships between entities managed by a JDBCStoreManager in the same
 * application.
 * <p/>
 * Life-cycle:
 * Tied to the EntityBridge.
 * <p/>
 * Multiplicity:
 * One for each role that entity has.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="mailto:alex@jboss.org">Alex Loubyansky</a>
 * @version $Revision: 81030 $
 */
public final class JDBCCMRFieldBridge extends JDBCAbstractCMRFieldBridge {
    /**
     * The entity bridge to which this cmr field belongs.
     */
    private final JDBCEntityBridge entity;
    /**
     * The manager of this entity.
     */
    private final JDBCStoreManager manager;
    /**
     * Metadata of the relationship role that this field represents.
     */
    private final JDBCRelationshipRoleMetaData metadata;
    /**
     * The data source used to acess the relation table if relevant.
     */
    private DataSource dataSource;
    /**
     * The relation table name if relevent.
     */
    private String qualifiedTableName;
    private String tableName;
    /**
     * The key fields that this entity maintains in the relation table.
     */
    private JDBCCMP2xFieldBridge[] tableKeyFields;
    /**
     * JDBCType for the foreign key fields. Basically, this is an ordered
     * merge of the JDBCType of the foreign key field.
     */
    private JDBCType jdbcType;
    /**
     * The related entity's container.
     */
    private WeakReference<CmpEntityBeanComponent> relatedContainerRef;
    /**
     * The related entity's jdbc store manager
     */
    private JDBCStoreManager relatedManager;
    /**
     * The related entity.
     */
    private JDBCEntityBridge relatedEntity;
    /**
     * The related entity's cmr field for this relationship.
     */
    private JDBCCMRFieldBridge relatedCMRField;
    /**
     * da log.
     */
    private final Logger log;

    /**
     * Foreign key fields of this entity (i.e., related entities pk fields)
     */
    private JDBCCMP2xFieldBridge[] foreignKeyFields;
    /**
     * Indicates whether all FK fields are mapped to PK fields
     */
    private boolean allFKFieldsMappedToPKFields;
    /**
     * This map contains related PK fields that are mapped through FK fields to this entity's PK fields
     */
    private final Map relatedPKFieldsByMyPKFields = new HashMap();
    /**
     * This map contains related PK fields keyed by FK fields
     */
    private final Map relatedPKFieldsByMyFKFields = new HashMap();
    /**
     * Indicates whether there are foreign key fields mapped to CMP fields
     */
    private boolean hasFKFieldsMappedToCMPFields;

    // Map for lists of related PK values keyed by this side's PK values.
    // The values are put/removed by related entities when its fields representing
    // foreign key are changed. When entity with this CMR is created, this map is checked
    // for waiting for it entities. Relationship with waiting entities is established,
    // removing waiting entities' primary keys from the map.
    // NOTE: this map is used only for foreign key fields mapped to CMP fields.
    private TransactionLocal relatedPKValuesWaitingForMyPK;

    /**
     * FindByPrimaryKey method used to find related instances in case when FK fields mapped to PK fields
     */
    private Method relatedFindByPrimaryKey;

    /**
     * index of the field in the JDBCContext
     */
    private final int jdbcContextIndex;

    /**
     * cascade-delete strategy
     */
    private CascadeDeleteStrategy cascadeDeleteStrategy;

    /**
     * This CMR field and its related CMR field share the same RelationDataManager
     */
    private RelationDataManager relationManager;

    /**
     * Creates a cmr field for the entity based on the metadata.
     */
    public JDBCCMRFieldBridge(JDBCEntityBridge entity,
                              JDBCStoreManager manager,
                              JDBCRelationshipRoleMetaData metadata) {
        this.entity = entity;
        this.manager = manager;
        this.metadata = metadata;
        this.jdbcContextIndex = ((JDBCEntityBridge) manager.getEntityBridge()).getNextJDBCContextIndex();

        //  Creat the log
        String categoryName = this.getClass().getName() +
                "." + manager.getMetaData().getName() + ".";
        if (metadata.getCMRFieldName() != null) {
            categoryName += metadata.getCMRFieldName();
        } else {
            categoryName += metadata.getRelatedRole().getEntity().getName() +
                    "-" + metadata.getRelatedRole().getCMRFieldName();
        }
        this.log = Logger.getLogger(categoryName);
    }

    public RelationDataManager getRelationDataManager() {
        return relationManager;
    }

    public void resolveRelationship() {
        //
        // Set handles to the related entity's container, cache,
        // manager, and invoker
        //

        // Related Entity Name
        String relatedEntityName = metadata.getRelatedRole().getEntity().getName();

        // Related Entity
        Catalog catalog = manager.getCatalog();
        relatedEntity = (JDBCEntityBridge) catalog.getEntityByEJBName(relatedEntityName);
        if (relatedEntity == null) {
            throw new RuntimeException("Related entity not found: " +
                    "entity=" +
                    entity.getEntityName() +
                    ", " +
                    "cmrField=" +
                    getFieldName() +
                    ", " +
                    "relatedEntity=" + relatedEntityName);
        }

        // Related CMR Field
        JDBCCMRFieldBridge[] cmrFields = (JDBCCMRFieldBridge[]) relatedEntity.getCMRFields();
        for (int i = 0; i < cmrFields.length; ++i) {
            JDBCCMRFieldBridge cmrField = cmrFields[i];
            if (metadata.getRelatedRole() == cmrField.getMetaData()) {
                relatedCMRField = cmrField;
                break;
            }
        }

        // if we didn't find the related CMR field throw an exception
        // with a detailed message
        if (relatedCMRField == null) {
            String message = "Related CMR field not found in " +
                    relatedEntity.getEntityName() + " for relationship from";

            message += entity.getEntityName() + ".";
            if (getFieldName() != null) {
                message += getFieldName();
            } else {
                message += "<no-field>";
            }

            message += " to ";
            message += relatedEntityName + ".";
            if (metadata.getRelatedRole().getCMRFieldName() != null) {
                message += metadata.getRelatedRole().getCMRFieldName();
            } else {
                message += "<no-field>";
            }

            throw new RuntimeException(message);
        }

        // Related Manager
        relatedManager = (JDBCStoreManager) relatedEntity.getManager();

        // Related Container
        CmpEntityBeanComponent relatedComponent = relatedManager.getComponent();
        this.relatedContainerRef = new WeakReference<CmpEntityBeanComponent>(relatedComponent);

        // related findByPrimaryKey
        Class homeClass = (relatedComponent.getLocalHomeClass() != null ?
                relatedComponent.getLocalHomeClass() : relatedComponent.getHomeClass());
        try {
            relatedFindByPrimaryKey =
                    homeClass.getMethod("findByPrimaryKey", new Class[]{relatedEntity.getPrimaryKeyClass()});
        } catch (Exception e) {
            throw new RuntimeException("findByPrimaryKey(" +
                    relatedEntity.getPrimaryKeyClass().getName()
                    + " pk) was not found in " + homeClass.getName());
        }

        //
        // Initialize the key fields
        //
        if (metadata.getRelationMetaData().isTableMappingStyle()) {
            // initialize relation table key fields
            Collection tableKeys = metadata.getKeyFields();
            List keyFieldsList = new ArrayList(tableKeys.size());

            // first phase is to create fk fields
            Map pkFieldsToFKFields = new HashMap(tableKeys.size());
            for (Iterator i = tableKeys.iterator(); i.hasNext(); ) {
                JDBCCMPFieldMetaData cmpFieldMetaData = (JDBCCMPFieldMetaData) i.next();
                FieldBridge pkField = entity.getFieldByName(cmpFieldMetaData.getFieldName());
                if (pkField == null) {
                    throw new RuntimeException("Primary key not found for key-field " + cmpFieldMetaData.getFieldName());
                }
                pkFieldsToFKFields.put(pkField, new JDBCCMP2xFieldBridge(manager, cmpFieldMetaData));
            }
            // second step is to order fk fields to match the order of pk fields
            JDBCFieldBridge[] pkFields = entity.getPrimaryKeyFields();
            for (int i = 0; i < pkFields.length; ++i) {
                Object fkField = pkFieldsToFKFields.get(pkFields[i]);
                if (fkField == null) {
                    throw new RuntimeException("Primary key " + pkFields[i].getFieldName() + " is not mapped.");
                }
                keyFieldsList.add(fkField);
            }
            tableKeyFields = (JDBCCMP2xFieldBridge[]) keyFieldsList.toArray(
                    new JDBCCMP2xFieldBridge[keyFieldsList.size()]);

            dataSource = manager.getDataSource(metadata.getRelationMetaData().getDataSourceName());
        } else {
            initializeForeignKeyFields();
            dataSource = hasForeignKey() ? entity.getDataSource() : relatedEntity.getDataSource();
        }

        // Fix table name
        //
        // This code doesn't work here...  The problem each side will generate
        // the table name and this will only work for simple generation.
        qualifiedTableName = SQLUtil.fixTableName(metadata.getRelationMetaData().getDefaultTableName(), dataSource);
        tableName = SQLUtil.getTableNameWithoutSchema(qualifiedTableName);

        relationManager = relatedCMRField.initRelationManager(this);
    }

    /**
     * The third phase of deployment. The method is called when relationships are already resolved.
     *
     * @
     */
    public void start() {
        cascadeDeleteStrategy = CascadeDeleteStrategy.getCascadeDeleteStrategy(this);
        relatedPKValuesWaitingForMyPK = new TransactionLocal(this.manager.getComponent().getTransactionManager()) {
            protected Object initialValue() {
                return new HashMap();
            }

            public Transaction getTransaction() {
                try {
                    return transactionManager.getTransaction();
                } catch (SystemException e) {
                    throw new IllegalStateException("An error occured while getting the " +
                            "transaction associated with the current thread: " + e);
                }
            }
        };
    }

    public boolean removeFromRelations(CmpEntityBeanContext ctx, Object[] oldRelationsRef) {
        load(ctx);

        FieldState fieldState = getFieldState(ctx);
        List value = fieldState.getValue();

        boolean removed = false;
        if (!value.isEmpty()) {
            if (hasFKFieldsMappedToCMPFields) {
                if (isForeignKeyValid(value.get(0))) {
                    cascadeDeleteStrategy.removedIds(ctx, oldRelationsRef, value);
                    removed = true;
                }
            } else {
                cascadeDeleteStrategy.removedIds(ctx, oldRelationsRef, value);
                removed = true;
            }
        }
        return removed;
    }

    public void cascadeDelete(CmpEntityBeanContext ctx, List oldValues) throws RemoveException, RemoteException {
        cascadeDeleteStrategy.cascadeDelete(ctx, oldValues);
    }

    public boolean isBatchCascadeDelete() {
        return (cascadeDeleteStrategy instanceof CascadeDeleteStrategy.BatchCascadeDeleteStrategy);
    }

    /**
     * Gets the manager of this entity.
     */
    public JDBCStoreManager getJDBCStoreManager() {
        return manager;
    }

    /**
     * Gets bridge for this entity.
     */
    public JDBCAbstractEntityBridge getEntity() {
        return entity;
    }

    /**
     * Gets the metadata of the relationship role that this field represents.
     */
    public JDBCRelationshipRoleMetaData getMetaData() {
        return metadata;
    }

    /**
     * Gets the relation metadata.
     */
    public JDBCRelationMetaData getRelationMetaData() {
        return metadata.getRelationMetaData();
    }

    /**
     * Gets the name of this field.
     */
    public String getFieldName() {
        return metadata.getCMRFieldName();
    }

    /**
     * Gets the name of the relation table if relevent.
     */
    public String getQualifiedTableName() {
        return qualifiedTableName;
    }

    public String getTableName() {
        return tableName;
    }

    /**
     * Gets the datasource of the relation table if relevent.
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Gets the read ahead meta data.
     */
    public JDBCReadAheadMetaData getReadAhead() {
        return metadata.getReadAhead();
    }

    public JDBCType getJDBCType() {
        return jdbcType;
    }

    public boolean isPrimaryKeyMember() {
        return false;
    }

    /**
     * Does this cmr field have foreign keys.
     */
    public boolean hasForeignKey() {
        return foreignKeyFields != null;
    }

    /**
     * Returns true if all FK fields are mapped to PK fields
     */
    public boolean allFkFieldsMappedToPkFields() {
        return allFKFieldsMappedToPKFields;
    }

    /**
     * Is this a collection valued field.
     */
    public boolean isCollectionValued() {
        return metadata.getRelatedRole().isMultiplicityMany();
    }

    /**
     * Is this a single valued field.
     */
    public boolean isSingleValued() {
        return metadata.getRelatedRole().isMultiplicityOne();
    }

    /**
     * Gets the key fields that this entity maintains in the relation table.
     */
    public JDBCFieldBridge[] getTableKeyFields() {
        return tableKeyFields;
    }

    /**
     * Gets the foreign key fields of this entity (i.e., related entities pk fields)
     */
    public JDBCFieldBridge[] getForeignKeyFields() {
        return foreignKeyFields;
    }

    /**
     * The related entity's cmr field for this relationship.
     */
    public JDBCAbstractCMRFieldBridge getRelatedCMRField() {
        return relatedCMRField;
    }

    /**
     * The related manger.
     */
    public JDBCStoreManager getRelatedManager() {
        return relatedManager;
    }

    /**
     * The related entity.
     */
    public EntityBridge getRelatedEntity() {
        return relatedEntity;
    }

    /**
     * The related entity.
     */
    public JDBCEntityBridge getRelatedJDBCEntity() {
        return relatedEntity;
    }

    /**
     * The related container
     */
    public CmpEntityBeanComponent getRelatedComponent() {
        return relatedContainerRef.get();
    }

    /**
     * The related entity's local home interface.
     */
    public Class getRelatedLocalInterface() {
        return getRelatedComponent().getStoreManager().getEntityBridge().getLocalInterface();
    }

    /**
     * @param ctx - entity's context
     * @return true if entity is loaded, false - otherwise.
     */
    public boolean isLoaded(CmpEntityBeanContext ctx) {
        return getFieldState(ctx).isLoaded;
    }

    /**
     * Establishes relationships with related entities waited for passed in context
     * to be created.
     *
     * @param ctx - entity's context.
     */
    public void addRelatedPKsWaitedForMe(CmpEntityBeanContext ctx) {
        final Map relatedPKsMap = getRelatedPKsWaitingForMyPK();
        synchronized (relatedPKsMap) {
            List relatedPKsWaitingForMe = (List) relatedPKsMap.get(ctx.getPrimaryKey());
            if (relatedPKsWaitingForMe != null) {
                for (Iterator waitingPKsIter = relatedPKsWaitingForMe.iterator(); waitingPKsIter.hasNext(); ) {
                    Object waitingPK = waitingPKsIter.next();
                    waitingPKsIter.remove();
                    if (isForeignKeyValid(waitingPK)) {
                        createRelationLinks(ctx, waitingPK);
                    }
                }
            }
        }
    }

    /**
     * Is this field readonly?
     */
    public boolean isReadOnly() {
        return getRelationMetaData().isReadOnly();
    }

    /**
     * Had the read time expired?
     */
    public boolean isReadTimedOut(CmpEntityBeanContext ctx) {
        // if we are read/write then we are always timed out
        if (!isReadOnly()) {
            return true;
        }

        // if read-time-out is -1 then we never time out.
        if (getRelationMetaData().getReadTimeOut() == -1) {
            return false;
        }

        long readInterval = System.currentTimeMillis() - getFieldState(ctx).getLastRead();
        return readInterval > getRelationMetaData().getReadTimeOut();
    }

    /**
     * @param ctx - entity's context.
     * @return the value of this field.
     */
    public Object getValue(CmpEntityBeanContext ctx) {
        // no user checks yet, but this is where they would go
        return getInstanceValue(ctx);
    }

    /**
     * Sets new value.
     *
     * @param ctx   - entity's context;
     * @param value - new value.
     */
    public void setValue(CmpEntityBeanContext ctx, Object value) {
        if (isReadOnly()) {
            throw new EJBException("Field is read-only: fieldName=" + getFieldName());
        }

        if (!JDBCEntityBridge.isEjbCreateDone(ctx)) {
            throw new IllegalStateException("A CMR field cannot be set " +
                    "in ejbCreate; this should be done in the ejbPostCreate " +
                    "method instead [EJB 2.0 Spec. 10.5.2].");
        }

        if (isCollectionValued() && value == null) {
            throw new IllegalArgumentException("null cannot be assigned to a " +
                    "collection-valued cmr-field [EJB 2.0 Spec. 10.3.8].");
        }
        /*
        if(allFKFieldsMappedToPKFields)
        {
           throw new IllegalStateException(
              "Can't modify relationship: CMR field "
              + entity.getEntityName() + "." + getFieldName()
              + " has foreign key fields mapped to the primary key columns."
              + " Primary key may only be set once in ejbCreate [EJB 2.0 Spec. 10.3.5].");
        }
        */
        setInstanceValue(ctx, value);
    }

    /**
     * Gets the value of the cmr field for the instance associated with
     * the context.
     */
    public Object getInstanceValue(CmpEntityBeanContext myCtx) {
        load(myCtx);
        FieldState fieldState = getFieldState(myCtx);
        if (isCollectionValued()) {
            return fieldState.getRelationSet();
        }

        // only return one
        try {
            List value = fieldState.getValue();
            if (!value.isEmpty()) {
                Object fk = value.get(0);
                return getRelatedEntityByFK(fk);
            } else if (foreignKeyFields != null) {
                // for those completely mapped to CMP fields and created in this current tx !!!
                Object relatedId = getRelatedIdFromContext(myCtx);
                if (relatedId != null) {
                    return getRelatedEntityByFK(relatedId);
                }
            }
            return null;
        } catch (EJBException e) {
            throw e;
        } catch (Exception e) {
            throw new EJBException(e);
        }
    }

    /**
     * Returns related entity's local interface.
     * If there are foreign key fields mapped to CMP fields, existence of related entity is checked
     * with findByPrimaryKey and if, in this case, related instance is not found, null is returned.
     * If foreign key fields mapped to its own columns then existence of related entity is not checked
     * and just its local object is returned.
     *
     * @param fk - foreign key value.
     * @return related local object instance.
     */
    public EJBLocalObject getRelatedEntityByFK(Object fk) {
        EJBLocalObject relatedLocalObject = null;
        final CmpEntityBeanComponent relatedContainer = getRelatedComponent();

        if (hasFKFieldsMappedToCMPFields && relatedManager.getReadAheadCache().getPreloadDataMap(fk, false) == null) {  // not in preload cache
            try {
                relatedLocalObject = relatedContainer.getEJBLocalObject(fk);
            } catch (Exception ignore) {
                // no such entity. it is ok to ignore
            }
        } else {
            relatedLocalObject = relatedContainer.getEJBLocalObject(fk);
        }

        return relatedLocalObject;
    }

    /**
     * This method is called only for CMR fields with foreign key fields mapped to CMP fields
     * to check the validity of the foreign key value.
     *
     * @param fk the foreign key to check
     * @return true if there is related entity with the equal primary key
     */
    public boolean isForeignKeyValid(Object fk) {
        boolean valid;
        if (relatedManager.getReadAheadCache().getPreloadDataMap(fk, false) != null) {
            valid = true;
        } else {
            try {
                Object result = getRelatedComponent().getEJBLocalObject(fk);
                valid = result != null;
            } catch (Exception ignore) {
                // no such entity. it is ok to ignore
                valid = false;
            }
        }
        return valid;
    }

    /**
     * Sets the value of the cmr field for the instance associated with
     * the context.
     */
    public void setInstanceValue(CmpEntityBeanContext myCtx, Object newValue) {
        // validate new value first
        List newPks;
        if (newValue instanceof Collection) {
            Collection col = (Collection) newValue;
            if (!col.isEmpty()) {
                newPks = new ArrayList(col.size());
                for (Iterator iter = col.iterator(); iter.hasNext(); ) {
                    Object localObject = iter.next();
                    if (localObject != null) {
                        Object relatedId;
                        try {
                            relatedId = getRelatedPrimaryKey(localObject);
                        } catch (TransactionRolledbackLocalException e) {
                            //if the instance has been removed we need to throw IllegalArgumentException
                            throw new IllegalArgumentException(e);
                        }
                        // check whether new value modifies the primary key if there are FK fields mapped to PK fields
                        if (relatedPKFieldsByMyPKFields.size() > 0) {
                            checkSetForeignKey(myCtx, relatedId);
                        }

                        newPks.add(relatedId);

                    }
                }
            } else {
                newPks = Collections.EMPTY_LIST;
            }
        } else {
            if (newValue != null) {
                try {
                    //if the instance has been removed we need to throw IllegalArgumentException
                    newPks = Collections.singletonList(getRelatedPrimaryKey(newValue));
                } catch (TransactionRolledbackLocalException e) {
                    throw new IllegalArgumentException(e);
                }
            } else {
                newPks = Collections.EMPTY_LIST;
            }
        }

        // load the current value
        load(myCtx);
        FieldState fieldState = getFieldState(myCtx);

        // is this just setting our own relation set back
        if (newValue == fieldState.getRelationSet()) {
            return;
        }

        try {
            // Remove old value(s)
            List value = fieldState.getValue();
            if (!value.isEmpty()) {
                Object[] curPks = value.toArray(new Object[value.size()]);
                for (int i = 0; i < curPks.length; ++i) {
                    destroyRelationLinks(myCtx, curPks[i]);
                }
            }

            // Add new value(s)
            for (int i = 0; i < newPks.size(); ++i) {
                createRelationLinks(myCtx, newPks.get(i));
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new EJBException(e);
        }
    }

    /**
     * Checks whether new foreign key value conflicts with primary key value
     * in case of foreign key to primary key mapping.
     *
     * @param myCtx    - entity's context;
     * @param newValue - new foreign key value.
     * @throws IllegalStateException - if new foreign key value changes
     *                               primary key value, otherwise returns silently.
     */
    private void checkSetForeignKey(CmpEntityBeanContext myCtx, Object newValue)
            throws IllegalStateException {
        JDBCFieldBridge[] pkFields = entity.getPrimaryKeyFields();
        for (int i = 0; i < pkFields.length; ++i) {
            JDBCCMP2xFieldBridge pkField = (JDBCCMP2xFieldBridge) pkFields[i];
            JDBCCMP2xFieldBridge relatedPkField = (JDBCCMP2xFieldBridge) relatedPKFieldsByMyPKFields.get(pkField);
            if (relatedPkField != null) {
                Object comingValue = relatedPkField.getPrimaryKeyValue(newValue);
                Object currentValue = pkField.getInstanceValue(myCtx);

                // they shouldn't be null
                if (!comingValue.equals(currentValue)) {
                    throw new IllegalStateException("Can't create relationship: CMR field "
                            +
                            entity.getEntityName() +
                            "." +
                            getFieldName()
                            +
                            " has foreign key fields mapped to the primary key columns."
                            +
                            " Primary key may only be set once in ejbCreate [EJB 2.0 Spec. 10.3.5]."
                            +
                            " primary key value is " +
                            currentValue
                            + " overriding value is " + comingValue);
                }
            }
        }
    }

    /**
     * Creates the relation links between the instance associated with the
     * context and the related instance (just the id is passed in).
     * <p/>
     * This method calls a.addRelation(b) and b.addRelation(a)
     */
    public void createRelationLinks(CmpEntityBeanContext myCtx, Object relatedId) {
        createRelationLinks(myCtx, relatedId, true);
    }

    public void createRelationLinks(CmpEntityBeanContext myCtx, Object relatedId, boolean updateForeignKey) {
        if (isReadOnly()) {
            throw new EJBException("Field is read-only: " + getFieldName());
        }

        // If my multiplicity is one, then we need to free the new related context
        // from its old relationship.
        if (metadata.isMultiplicityOne()) {
            Object oldRelatedId = relatedCMRField.invokeGetRelatedId(relatedId);
            if (oldRelatedId != null) {
                invokeRemoveRelation(oldRelatedId, relatedId);
                relatedCMRField.invokeRemoveRelation(relatedId, oldRelatedId);
            }
        }

        addRelation(myCtx, relatedId, updateForeignKey);
        relatedCMRField.invokeAddRelation(relatedId, myCtx.getPrimaryKey());
    }

    /**
     * Destroys the relation links between the instance associated with the
     * context and the related instance (just the id is passed in).
     * <p/>
     * This method calls a.removeRelation(b) and b.removeRelation(a)
     */
    public void destroyRelationLinks(CmpEntityBeanContext myCtx, Object relatedId) {
        destroyRelationLinks(myCtx, relatedId, true);
    }

    /**
     * Destroys the relation links between the instance associated with the
     * context and the related instance (just the id is passed in).
     * <p/>
     * This method calls a.removeRelation(b) and b.removeRelation(a)
     * <p/>
     * If updateValueCollection is false, the related id collection is not
     * updated. This form is only used by the RelationSet iterator.
     */
    public void destroyRelationLinks(CmpEntityBeanContext myCtx,
                                     Object relatedId,
                                     boolean updateValueCollection) {
        destroyRelationLinks(myCtx, relatedId, updateValueCollection, true);
    }

    public void destroyRelationLinks(CmpEntityBeanContext myCtx,
                                     Object relatedId,
                                     boolean updateValueCollection,
                                     boolean updateForeignKey) {
        if (isReadOnly()) {
            throw new EJBException("Field is read-only: " + getFieldName());
        }

        removeRelation(myCtx, relatedId, updateValueCollection, updateForeignKey);
        relatedCMRField.invokeRemoveRelation(relatedId, myCtx.getPrimaryKey());
    }

    /**
     * Schedules children for cascade delete.
     */
    public void scheduleChildrenForCascadeDelete(CmpEntityBeanContext ctx) {
        load(ctx);
        FieldState fieldState = getFieldState(ctx);
        List value = fieldState.getValue();
        if (!value.isEmpty()) {
            Transaction tx = getTransaction();
            for (int i = 0; i < value.size(); ++i) {
                relatedCMRField.invokeScheduleForCascadeDelete(value.get(i));
            }
        }
    }

    /**
     * Schedules children for batch cascade delete.
     */
    public void scheduleChildrenForBatchCascadeDelete(CmpEntityBeanContext ctx) {
        load(ctx);
        FieldState fieldState = getFieldState(ctx);
        List value = fieldState.getValue();
        if (!value.isEmpty()) {
            Transaction tx = getTransaction();
            for (int i = 0; i < value.size(); ++i) {
                relatedCMRField.invokeScheduleForBatchCascadeDelete(value.get(i));
            }
        }
    }

    /**
     * Schedules the instance with myId for cascade delete.
     */
    private void invokeScheduleForCascadeDelete(Object myId) {
        try {
            getManager().getComponent().invoke(CMRMessage.SCHEDULE_FOR_CASCADE_DELETE, myId, this);
        } catch (Exception e) {
            throw new EJBException("Error in scheduleForCascadeDelete", e);
        }
    }

    /**
     * Schedules the instance with myId for batch cascade delete.
     */
    private void invokeScheduleForBatchCascadeDelete(Object myId) {
        try {
            getManager().getComponent().invoke(CMRMessage.SCHEDULE_FOR_BATCH_CASCADE_DELETE, myId, this);
        } catch (Exception e) {
            throw new EJBException("Error in scheduleForBatchCascadeDelete", e);
        }
    }

    /**
     * Invokes the getRelatedId on the related CMR field via the container
     * invocation interceptor chain.
     */
    private Object invokeGetRelatedId(Object myId) {
        try {
            return getManager().getComponent().invoke(CMRMessage.GET_RELATED_ID, myId, this);
        } catch (Exception e) {
            throw new EJBException("Error in getRelatedId", e);
        }
    }

    private void invokeAddRelation(Object myId, Object relatedId) {
        try {
            getManager().getComponent().invoke(CMRMessage.ADD_RELATION, myId, this, relatedId);
        } catch (Exception e) {
            throw new EJBException("Error in addRelation", e);
        }
    }

    private void invokeRemoveRelation(Object myId, Object relatedId) {
        try {
            getManager().getComponent().invoke(CMRMessage.REMOVE_RELATION, myId, this, relatedId);
        } catch (Exception e) {
            throw new EJBException("Error in removeRelation", e);
        }
    }

    /**
     * Get the related entity's id.  This only works on single valued cmr fields.
     */
    public Object getRelatedId(CmpEntityBeanContext myCtx) {
        if (isCollectionValued()) {
            throw new EJBException("getRelatedId may only be called on a cmr-field with a multiplicity of one.");
        }

        load(myCtx);
        List value = getFieldState(myCtx).getValue();
        return value.isEmpty() ? null : value.get(0);
    }

    /**
     * Creates a new instance of related id based on foreign key value in the context.
     *
     * @param ctx - entity's context.
     * @return related entity's id.
     */
    public Object getRelatedIdFromContext(CmpEntityBeanContext ctx) {
        Object relatedId = null;
        Object fkFieldValue;
        for (JDBCCMP2xFieldBridge fkField : foreignKeyFields) {
            fkFieldValue = fkField.getInstanceValue(ctx);
            if (fkFieldValue == null) {
                return null;
            }
            JDBCCMP2xFieldBridge relatedPKField = (JDBCCMP2xFieldBridge) relatedPKFieldsByMyFKFields.get(fkField);
            relatedId = relatedPKField.setPrimaryKeyValue(relatedId, fkFieldValue);
        }
        return relatedId;
    }

    /**
     * Adds the foreign key to the set of related ids, and updates any foreign key fields.
     */
    public void addRelation(CmpEntityBeanContext myCtx, Object fk) {
        addRelation(myCtx, fk, true);
        relationManager.addRelation(this, myCtx.getPrimaryKey(), relatedCMRField, fk);
    }

    private void addRelation(CmpEntityBeanContext myCtx, Object fk, boolean updateForeignKey) {
        checkSetForeignKey(myCtx, fk);

        if (isReadOnly()) {
            throw new EJBException("Field is read-only: " + getFieldName());
        }

        if (!JDBCEntityBridge.isEjbCreateDone(myCtx)) {
            throw new IllegalStateException("A CMR field cannot be set or added " +
                    "to a relationship in ejbCreate; this should be done in the " +
                    "ejbPostCreate method instead [EJB 2.0 Spec. 10.5.2].");
        }
        // add to current related set
        FieldState myState = getFieldState(myCtx);
        myState.addRelation(fk);

        // set the foreign key, if we have one.
        if (hasForeignKey() && updateForeignKey) {
            setForeignKey(myCtx, fk);
        }
    }

    /**
     * Removes the foreign key to the set of related ids, and updates any foreign key fields.
     */
    public void removeRelation(CmpEntityBeanContext myCtx, Object fk) {
        removeRelation(myCtx, fk, true, true);
        relationManager.removeRelation(this, myCtx.getPrimaryKey(), relatedCMRField, fk);
    }

    private void removeRelation(CmpEntityBeanContext myCtx,
                                Object fk,
                                boolean updateValueCollection,
                                boolean updateForeignKey) {
        if (isReadOnly()) {
            throw new EJBException("Field is read-only: " + getFieldName());
        }

        // remove from current related set
        if (updateValueCollection) {
            FieldState myState = getFieldState(myCtx);
            myState.removeRelation(fk);
        }

        // set the foreign key to null, if we have one.
        if (hasForeignKey() && updateForeignKey) {
            setForeignKey(myCtx, null);
        }
    }

    /**
     * loads the collection of related ids
     * NOTE: after loading, the field might not be in a clean state as we support adding and removing
     * relations while the field is not loaded. The actual value of the field will be the value loaded
     * plus added relations and minus removed relations while the field was not loaded.
     */
    private void load(CmpEntityBeanContext myCtx) {
        // if we are already loaded we're done
        FieldState fieldState = getFieldState(myCtx);
        if (fieldState.isLoaded()) {
            return;
        }

        // check the preload cache
        if (log.isTraceEnabled()) {
            log.trace("Read ahead cahce load: cmrField=" + getFieldName() + " pk=" + myCtx.getPrimaryKey());
        }

        manager.getReadAheadCache().load(myCtx);
        if (fieldState.isLoaded()) {
            return;
        }

        // load the value from the database
        Collection values;
        if (hasForeignKey()) {
            // WARN: this method will load foreign keys if they are not yet loaded and
            // changes relationship lazy loading in advanced training labs.
            // i.e. it will load lazy cmp fields first of this entity and then will lazy load the related entity
            // instead of loading this entity JOIN related entity in one query.
            //Object fk = getRelatedIdFromContext(myCtx);

            boolean loadWithManager = false;
            Object fk = null;
            for (int i = 0; i < foreignKeyFields.length; ++i) {
                JDBCCMP2xFieldBridge fkField = foreignKeyFields[i];
                // if the field is not loaded then load relationship with manager
                if (!fkField.isLoaded(myCtx)) {
                    loadWithManager = true;
                    break;
                }

                Object fkFieldValue = fkField.getInstanceValue(myCtx);
                // if one of the fk is null, the whole fk is considered to be null
                if (fkFieldValue == null) {
                    fk = null;
                    break;
                }
                JDBCCMP2xFieldBridge relatedPKField = (JDBCCMP2xFieldBridge) relatedPKFieldsByMyFKFields.get(fkField);
                fk = relatedPKField.setPrimaryKeyValue(fk, fkFieldValue);
            }

            if (loadWithManager) {
                values = manager.loadRelation(this, myCtx.getPrimaryKey());
            } else {
                values = (fk == null ? Collections.EMPTY_LIST : Collections.singletonList(fk));
            }
        } else {
            values = manager.loadRelation(this, myCtx.getPrimaryKey());
        }
        load(myCtx, values);
    }

    public void load(CmpEntityBeanContext myCtx, Collection values) {
        // did we get more then one value for a single valued field
        if (isSingleValued() && values.size() > 1) {
            throw new EJBException("Data contains multiple values, but this cmr field is single valued: " + values);
        }

        // add the new values
        FieldState fieldState = getFieldState(myCtx);
        fieldState.loadRelations(values);

        // set the foreign key, if we have one.
        if (hasForeignKey()) {
            // update the states and locked values of FK fields
            if (!values.isEmpty()) {
                Object loadedValue = values.iterator().next();
                for (int i = 0; i < foreignKeyFields.length; ++i) {
                    JDBCCMP2xFieldBridge fkField = foreignKeyFields[i];
                    Object fieldValue = fkField.getPrimaryKeyValue(loadedValue);
                    fkField.updateState(myCtx, fieldValue);
                }
            }

            // set the real FK value
            List realValue = fieldState.getValue();
            Object fk = realValue.isEmpty() ? null : realValue.get(0);
            setForeignKey(myCtx, fk);
        }

        JDBCEntityBridge.setCreated(myCtx);
    }

    /**
     * Sets the foreign key field value.
     */
    public void setForeignKey(CmpEntityBeanContext myCtx, Object fk) {
        if (!hasForeignKey()) {
            throw new EJBException(getFieldName() + " CMR field does not have a foreign key to set.");
        }

        for (int i = 0; i < foreignKeyFields.length; ++i) {
            JDBCCMP2xFieldBridge fkField = foreignKeyFields[i];
            Object fieldValue = fkField.getPrimaryKeyValue(fk);
            fkField.setInstanceValue(myCtx, fieldValue);
        }
    }

    /**
     * Initialized the foreign key fields.
     */
    public void initInstance(CmpEntityBeanContext ctx) {
        // mark this field as loaded
        getFieldState(ctx).loadRelations(Collections.EMPTY_SET);

        if (foreignKeyFields == null) {
            return;
        }

        for (int i = 0; i < foreignKeyFields.length; ++i) {
            JDBCCMP2xFieldBridge foreignKeyField = foreignKeyFields[i];
            if (!foreignKeyField.isFKFieldMappedToCMPField()) {
                foreignKeyField.setInstanceValue(ctx, null);
            }
        }
    }

    /**
     * resets the persistence context of the foreign key fields
     */
    public void resetPersistenceContext(CmpEntityBeanContext ctx) {
        // only resetStats if the read has timed out
        if (!isReadTimedOut(ctx)) {
            return;
        }

        // clear the field state
        JDBCContext jdbcCtx = (JDBCContext) ctx.getPersistenceContext();
        // invalidate current field state
        /*
     FieldState currentFieldState = (FieldState) jdbcCtx.getFieldState(jdbcContextIndex);
     if(currentFieldState != null)
        currentFieldState.invalidate();
        */
        jdbcCtx.setFieldState(jdbcContextIndex, null);

        if (foreignKeyFields == null) {
            return;
        }

        for (int i = 0; i < foreignKeyFields.length; ++i) {
            JDBCCMP2xFieldBridge foreignKeyField = foreignKeyFields[i];
            if (!foreignKeyField.isFKFieldMappedToCMPField()) {
                foreignKeyField.resetPersistenceContext(ctx);
            }
        }
    }

    public int setInstanceParameters(PreparedStatement ps,
                                     int parameterIndex,
                                     CmpEntityBeanContext ctx) {
        if (foreignKeyFields == null) {
            return parameterIndex;
        }

        List value = getFieldState(ctx).getValue();
        Object fk = (value.isEmpty() ? null : value.get(0));

        for (int i = 0; i < foreignKeyFields.length; ++i) {
            parameterIndex = foreignKeyFields[i].setPrimaryKeyParameters(ps, parameterIndex, fk);
        }

        return parameterIndex;
    }

    public int loadInstanceResults(ResultSet rs,
                                   int parameterIndex,
                                   CmpEntityBeanContext ctx) {
        if (!hasForeignKey()) {
            return parameterIndex;
        }

        // load the value from the database
        Object[] ref = new Object[1];
        parameterIndex = loadArgumentResults(rs, parameterIndex, ref);

        // only actually set the value if the state is not already loaded
        FieldState fieldState = getFieldState(ctx);
        if (!fieldState.isLoaded()) {
            if (ref[0] != null) {
                load(ctx, Collections.singleton(ref[0]));
            } else {
                load(ctx, Collections.EMPTY_SET);
            }
        }
        return parameterIndex;
    }

    public int loadArgumentResults(ResultSet rs, int parameterIndex, Object[] fkRef) {
        if (foreignKeyFields == null) {
            return parameterIndex;
        }

        boolean fkIsNull = false;

        // value of this field,  will be filled in below
        Object[] argumentRef = new Object[1];
        for (int i = 0; i < foreignKeyFields.length; ++i) {
            JDBCCMPFieldBridge field = foreignKeyFields[i];
            parameterIndex = field.loadArgumentResults(rs, parameterIndex, argumentRef);

            if (fkIsNull) {
                continue;
            }
            if (field.getPrimaryKeyField() != null) {
                // if there is a null field among FK fields, the whole FK field is considered null.
                // NOTE: don't throw exception in this case, it's ok if FK is partly mapped to a PK
                // NOTE2: we still need to iterate through foreign key fields and 'load' them to
                // return correct parameterIndex.
                if (argumentRef[0] == null) {
                    fkRef[0] = null;
                    fkIsNull = true;
                } else {
                    // if we don't have a pk object yet create one
                    if (fkRef[0] == null) {
                        fkRef[0] = relatedEntity.createPrimaryKeyInstance();
                    }
                    try {
                        // Set this field's value into the primary key object.
                        field.getPrimaryKeyField().set(fkRef[0], argumentRef[0]);
                    } catch (Exception e) {
                        // Non recoverable internal exception
                        throw new EJBException("Internal error setting foreign-key field " + getFieldName(), e);
                    }
                }
            } else {
                // This field is the primary key, so no extraction is necessary.
                fkRef[0] = argumentRef[0];
            }
        }
        return parameterIndex;
    }

    /**
     * This method is never called.
     * In case of a CMR with foreign key fields, only the foreign key fields are asked for the dirty state.
     */
    public boolean isDirty(CmpEntityBeanContext ctx) {
        return foreignKeyFields == null ? relationManager.isDirty() : false;
    }

    public boolean invalidateCache(CmpEntityBeanContext ctx) {
        JDBCContext jdbcCtx = (JDBCContext) ctx.getPersistenceContext();
        FieldState fieldState = (FieldState) jdbcCtx.getFieldState(jdbcContextIndex);
        return fieldState == null ? false : fieldState.isChanged();
    }

    /**
     * This method is never called.
     * In case of a CMR
     * - with foreign key fields, the foreign key fields are cleaned when necessary according to CMP fields'
     * behaviour.
     * - from m:m relationship, added/removed key pairs are cleared in application tx data map on sync.
     */
    public void setClean(CmpEntityBeanContext ctx) {
        throw new UnsupportedOperationException();
    }

    public boolean isCMPField() {
        return false;
    }

    public JDBCEntityPersistenceStore getManager() {
        return manager;
    }

    public boolean hasFKFieldsMappedToCMPFields() {
        return hasFKFieldsMappedToCMPFields;
    }

    public void addRelatedPKWaitingForMyPK(Object myPK, Object relatedPK) {
        Map relatedPKsWaitingForMyPK = getRelatedPKsWaitingForMyPK();
        synchronized (relatedPKsWaitingForMyPK) {
            List relatedPKs = (List) relatedPKsWaitingForMyPK.get(myPK);
            if (relatedPKs == null) {
                relatedPKs = new ArrayList(1);
                relatedPKsWaitingForMyPK.put(myPK, relatedPKs);
            }
            relatedPKs.add(relatedPK);
        }
    }

    public void removeRelatedPKWaitingForMyPK(Object myPK, Object relatedPK) {
        final Map relatedPKMap = getRelatedPKsWaitingForMyPK();
        synchronized (relatedPKMap) {
            List relatedPKs = (List) relatedPKMap.get(myPK);
            if (relatedPKs != null) {
                relatedPKs.remove(relatedPK);
            }
        }
    }

    /**
     * Gets the field state object from the persistence context.
     */
    private FieldState getFieldState(CmpEntityBeanContext ctx) {
        JDBCContext jdbcCtx = (JDBCContext) ctx.getPersistenceContext();
        FieldState fieldState = (FieldState) jdbcCtx.getFieldState(jdbcContextIndex);
        if (fieldState == null) {
            fieldState = new FieldState(ctx);
            jdbcCtx.setFieldState(jdbcContextIndex, fieldState);
        }
        return fieldState;
    }

    /**
     * Initializes foreign key fields
     *
     * @
     */
    private void initializeForeignKeyFields() {
        Collection foreignKeys = metadata.getRelatedRole().getKeyFields();

        // temporary map used later to write fk fields in special order
        Map fkFieldsByRelatedPKFields = new HashMap();
        for (Iterator i = foreignKeys.iterator(); i.hasNext(); ) {
            JDBCCMPFieldMetaData fkFieldMetaData = (JDBCCMPFieldMetaData) i.next();
            JDBCCMP2xFieldBridge relatedPKField =
                    (JDBCCMP2xFieldBridge) relatedEntity.getFieldByName(fkFieldMetaData.getFieldName());

            // now determine whether the fk is mapped to a pk column
            String fkColumnName = fkFieldMetaData.getColumnName();
            JDBCCMP2xFieldBridge fkField = null;

            // look among the CMP fields for the field with the same column name
            JDBCFieldBridge[] tableFields = entity.getTableFields();
            for (int tableInd = 0; tableInd < tableFields.length && fkField == null; ++tableInd) {
                JDBCCMP2xFieldBridge cmpField = (JDBCCMP2xFieldBridge) tableFields[tableInd];
                if (fkColumnName.equals(cmpField.getColumnName())) {
                    hasFKFieldsMappedToCMPFields = true;

                    // construct the foreign key field
                    fkField = new JDBCCMP2xFieldBridge((JDBCStoreManager) cmpField.getManager(), // this cmpField's manager
                            relatedPKField.getFieldName(),
                            relatedPKField.getFieldType(),
                            cmpField.getJDBCType(), // this cmpField's jdbc type
                            relatedPKField.isReadOnly(),
                            relatedPKField.getReadTimeOut(),
                            relatedPKField.getPrimaryKeyClass(),
                            relatedPKField.getPrimaryKeyField(),
                            cmpField, // CMP field I am mapped to
                            this,
                            fkColumnName);

                    if (cmpField.isPrimaryKeyMember()) {
                        relatedPKFieldsByMyPKFields.put(cmpField, relatedPKField);
                    }
                }
            }

            // if the fk is not a part of pk then create a new field
            if (fkField == null) {
                fkField = new JDBCCMP2xFieldBridge(manager,
                        fkFieldMetaData,
                        manager.getJDBCTypeFactory().getJDBCType(fkFieldMetaData));
            }

            fkFieldsByRelatedPKFields.put(relatedPKField, fkField); // temporary map
            relatedPKFieldsByMyFKFields.put(fkField, relatedPKField);
        }

        // Note: this important to order the foreign key fields so that their order matches
        // the order of related entity's pk fields in case of complex primary keys.
        // The order is important in fk-constraint generation and in SELECT when loading
        if (fkFieldsByRelatedPKFields.size() > 0) {
            JDBCFieldBridge[] relatedPKFields = relatedEntity.getPrimaryKeyFields();
            List fkList = new ArrayList(relatedPKFields.length);
            for (int i = 0; i < relatedPKFields.length; ++i) {
                JDBCCMPFieldBridge fkField = (JDBCCMPFieldBridge) fkFieldsByRelatedPKFields.remove(relatedPKFields[i]);
                fkList.add(fkField);
            }
            foreignKeyFields = (JDBCCMP2xFieldBridge[]) fkList.toArray(new JDBCCMP2xFieldBridge[fkList.size()]);
        } else {
            foreignKeyFields = null;
        }

        // are all FK fields mapped to PK fields?
        allFKFieldsMappedToPKFields = relatedPKFieldsByMyPKFields.size() > 0
                && relatedPKFieldsByMyPKFields.size() == foreignKeyFields.length;

        if (foreignKeyFields != null) {
            jdbcType = new CMRJDBCType(Arrays.asList(foreignKeyFields));
        }
    }

    private Transaction getTransaction() {
        try {
            CmpEntityBeanComponent component = getJDBCStoreManager().getComponent();
            TransactionManager tm = component.getTransactionManager();
            return tm.getTransaction();
        } catch (SystemException e) {
            throw new EJBException("Error getting transaction from the transaction manager", e);
        }
    }

    /**
     * @return Map of lists of waiting related PK values keyed by not yet created this side's PK value.
     */
    private Map getRelatedPKsWaitingForMyPK() {
        return (Map) relatedPKValuesWaitingForMyPK.get();
    }

    private RelationDataManager initRelationManager(JDBCCMRFieldBridge relatedField) {
        if (relationManager == null) {
            if (metadata.getRelationMetaData().isTableMappingStyle()) {
                relationManager = new M2MRelationManager(this, relatedField);
            } else {
                relationManager = EMPTY_RELATION_MANAGER;
            }
        }
        return relationManager;
    }

    private Object getRelatedPrimaryKey(Object localObject) {
        Object relatedId;
        if (relatedEntity.getLocalInterface().isAssignableFrom(localObject.getClass())) {
            EJBLocalObject local = (EJBLocalObject) localObject;
            try {
                relatedId = local.getPrimaryKey();
            } catch (NoSuchObjectLocalException e) {
                throw new IllegalArgumentException(e.getMessage());
            }

            /*
            if(relatedManager.wasCascadeDeleted(relatedId))
            {
               throw new IllegalArgumentException("The instance was cascade-deleted: pk=" + relatedId);
            }
            */
        } else {
            throw new IllegalArgumentException("The values of this field must be of type " +
                    relatedEntity.getLocalInterface().getName());
        }
        return relatedId;
    }

    public String toString() {
        return entity.getEntityName() + '.' + getFieldName();
    }

    private final class FieldState {
        private final CmpEntityBeanContext ctx;
        private List[] setHandle = new List[1];
        private Set addedRelations;
        private Set removedRelations;
        private Set relationSet;
        private boolean isLoaded = false;
        private final long lastRead = -1;

        private boolean changed;

        public FieldState(CmpEntityBeanContext ctx) {
            this.ctx = ctx;
            setHandle[0] = new ArrayList();
        }

        /**
         * Get the current value (list of primary keys).
         */
        public List getValue() {
            if (!isLoaded) {
                throw new EJBException("CMR field value not loaded yet");
            }
            return Collections.unmodifiableList(setHandle[0]);
        }

        /**
         * Has this relation been loaded.
         */
        public boolean isLoaded() {
            return isLoaded;
        }

        /**
         * When was this value last read from the datastore.
         */
        public long getLastRead() {
            return lastRead;
        }

        /**
         * Add this foreign to the relationship.
         */
        public void addRelation(Object fk) {
            if (isLoaded) {
                setHandle[0].add(fk);
            } else {
                if (removedRelations == null) {
                    removedRelations = new HashSet();
                    addedRelations = new HashSet();
                }
                removedRelations.remove(fk);
                addedRelations.add(fk);
            }
            changed = true;
        }

        /**
         * Remove this foreign to the relationship.
         */
        public void removeRelation(Object fk) {
            if (isLoaded) {
                setHandle[0].remove(fk);
            } else {
                if (removedRelations == null) {
                    removedRelations = new HashSet();
                    addedRelations = new HashSet();
                }
                addedRelations.remove(fk);
                removedRelations.add(fk);
            }

            changed = true;
        }

        /**
         * loads the collection of related ids
         */
        public void loadRelations(Collection values) {
            // check if we are aleready loaded
            if (isLoaded) {
                throw new EJBException("CMR field value is already loaded");
            }

            // just in the case where there are lingering values
            setHandle[0].clear();

            // add the new values
            setHandle[0].addAll(values);

            if (removedRelations != null) {
                // remove the already removed values
                setHandle[0].removeAll(removedRelations);
                removedRelations = null;
            }

            if (addedRelations != null) {
                // add the already added values
                // but remove FKs we are going to add to avoid duplication
                setHandle[0].removeAll(addedRelations);
                setHandle[0].addAll(addedRelations);
                addedRelations = null;
            }

            // mark the field loaded
            isLoaded = true;
        }

        /**
         * Get the current relation set or create a new one.
         */
        public Set getRelationSet() {
            if (!isLoaded) {
                throw new EJBException("CMR field value not loaded yet");
            }

            if (ctx.isReadOnly()) {
                // we are in a read-only invocation, so return a snapshot set
                return new RelationSet(JDBCCMRFieldBridge.this,
                        ctx,
                        new List[]{new ArrayList(setHandle[0])},
                        true);
            }

            // if we already have a relationset use it
            if (relationSet != null) {
                return relationSet;
            }

            // construct a new relationshet
            try {
                // get the curent transaction
                CmpEntityBeanComponent component = getJDBCStoreManager().getComponent();
                TransactionManager tm = component.getTransactionManager();
                Transaction tx = tm.getTransaction();

                // if whe have a valid transaction...
                if (tx != null && (tx.getStatus() == Status.STATUS_ACTIVE || tx.getStatus() == Status.STATUS_PREPARING)) {
                    // crete the relation set and register for a tx callback
                    relationSet = new RelationSet(JDBCCMRFieldBridge.this, ctx, setHandle, false);
                    TxSynchronization sync = new TxSynchronization(FieldState.this);
                    tx.registerSynchronization(sync);
                } else {
                    // if there is no transaction create a pre-failed list
                    relationSet = new RelationSet(JDBCCMRFieldBridge.this, ctx, new List[1], false);
                }

                return relationSet;
            } catch (SystemException e) {
                throw new EJBException("Error while creating RelationSet", e);
            } catch (RollbackException e) {
                throw new EJBException("Error while creating RelationSet", e);
            }
        }

        /**
         * Invalidate the current relationship set.
         */
        public void invalidate() {
            // make a new set handle and copy the currentList to the new handle
            // this will cause old references to the relationSet to throw an
            // IllegalStateException if accesses, but will not cause a reload
            // in Commit Option A
            List currentList = null;
            if (setHandle != null && setHandle.length > 0) {
                currentList = setHandle[0];
                setHandle[0] = null;
            }
            setHandle = new List[1];
            setHandle[0] = currentList;

            relationSet = null;
            changed = false;
        }

        public boolean isChanged() {
            return changed;
        }
    }

    private static final class CMRJDBCType implements JDBCType {
        private final String[] columnNames;
        private final Class[] javaTypes;
        private final int[] jdbcTypes;
        private final String[] sqlTypes;
        private final boolean[] notNull;

        private CMRJDBCType(List fields) {
            List columnNamesList = new ArrayList();
            List javaTypesList = new ArrayList();
            List jdbcTypesList = new ArrayList();
            List sqlTypesList = new ArrayList();
            List notNullList = new ArrayList();

            for (Iterator iter = fields.iterator(); iter.hasNext(); ) {
                JDBCCMPFieldBridge field = (JDBCCMPFieldBridge) iter.next();
                JDBCType type = field.getJDBCType();
                for (int i = 0; i < type.getColumnNames().length; i++) {
                    columnNamesList.add(type.getColumnNames()[i]);
                    javaTypesList.add(type.getJavaTypes()[i]);
                    jdbcTypesList.add(new Integer(type.getJDBCTypes()[i]));
                    sqlTypesList.add(type.getSQLTypes()[i]);
                    notNullList.add(new Boolean(type.getNotNull()[i]));
                }
            }
            columnNames = (String[]) columnNamesList.toArray(new String[columnNamesList.size()]);
            javaTypes = (Class[]) javaTypesList.toArray(new Class[javaTypesList.size()]);
            sqlTypes = (String[]) sqlTypesList.toArray(new String[sqlTypesList.size()]);

            jdbcTypes = new int[jdbcTypesList.size()];
            for (int i = 0; i < jdbcTypes.length; i++) {
                jdbcTypes[i] = ((Integer) jdbcTypesList.get(i)).intValue();
            }

            notNull = new boolean[notNullList.size()];
            for (int i = 0; i < notNull.length; i++) {
                notNull[i] = ((Boolean) notNullList.get(i)).booleanValue();
            }
        }

        public String[] getColumnNames() {
            return columnNames;
        }

        public Class[] getJavaTypes() {
            return javaTypes;
        }

        public int[] getJDBCTypes() {
            return jdbcTypes;
        }

        public String[] getSQLTypes() {
            return sqlTypes;
        }

        public boolean[] getNotNull() {
            return notNull;
        }

        public boolean[] getAutoIncrement() {
            return new boolean[]{false};
        }

        public Object getColumnValue(int index, Object value) {
            throw new UnsupportedOperationException();
        }

        public Object setColumnValue(int index, Object value, Object columnValue) {
            throw new UnsupportedOperationException();
        }

        public boolean hasMapper() {
            throw new UnsupportedOperationException("hasMapper is not implemented.");
        }

        public boolean isSearchable() {
            throw new UnsupportedOperationException("isSearchable is not implemented.");
        }

        public JDBCResultSetReader[] getResultSetReaders() {
            // foreign key fields has their result set readers
            throw new UnsupportedOperationException();
        }

        public JDBCParameterSetter[] getParameterSetter() {
            throw new UnsupportedOperationException();
        }
    }

    private static final class TxSynchronization implements Synchronization {
        private final WeakReference fieldStateRef;

        private TxSynchronization(FieldState fieldState) {
            if (fieldState == null) {
                throw new IllegalArgumentException("fieldState is null");
            }
            this.fieldStateRef = new WeakReference(fieldState);
        }

        public void beforeCompletion() {
            // REVIEW: THIS WILL NOT BE INVOKED ON A ROLLBACK
            // Be Careful where you put this invalidate
            // If you put it in afterCompletion, the beanlock will probably
            // be released before the invalidate and you will have a race
            FieldState fieldState = (FieldState) fieldStateRef.get();
            if (fieldState != null) {
                fieldState.invalidate();
            }
        }

        public void afterCompletion(int status) {
        }
    }

    public static interface RelationDataManager {
        void addRelation(JDBCCMRFieldBridge field, Object id, JDBCCMRFieldBridge relatedField, Object relatedId);

        void removeRelation(JDBCCMRFieldBridge field, Object id, JDBCCMRFieldBridge relatedField, Object relatedId);

        boolean isDirty();

        RelationData getRelationData();
    }

    private static final RelationDataManager EMPTY_RELATION_MANAGER = new RelationDataManager() {
        public void addRelation(JDBCCMRFieldBridge field, Object id, JDBCCMRFieldBridge relatedField, Object relatedId) {
        }

        public void removeRelation(JDBCCMRFieldBridge field,
                                   Object id,
                                   JDBCCMRFieldBridge relatedField,
                                   Object relatedId) {
        }

        public boolean isDirty() {
            return false;
        }

        public RelationData getRelationData() {
            throw new UnsupportedOperationException();
        }
    };

    public static class M2MRelationManager implements RelationDataManager {
        private final JDBCCMRFieldBridge leftField;
        private final JDBCCMRFieldBridge rightField;

        private final TransactionLocal relationData;

        public M2MRelationManager(final JDBCCMRFieldBridge leftField, final JDBCCMRFieldBridge rightField) {
            this.leftField = leftField;
            this.rightField = rightField;

            relationData = new TransactionLocal(leftField.manager.getComponent().getTransactionManager()) {
                protected Object initialValue() {
                    return new RelationData(leftField, rightField);
                }

                public Transaction getTransaction() {
                    try {
                        return transactionManager.getTransaction();
                    } catch (SystemException e) {
                        throw new IllegalStateException("An error occured while getting the " +
                                "transaction associated with the current thread: " + e);
                    }
                }
            };
        }

        public void addRelation(JDBCCMRFieldBridge field,
                                Object id,
                                JDBCCMRFieldBridge relatedField,
                                Object relatedId) {
            final RelationData local = getRelationData();
            local.addRelation(field, id, relatedField, relatedId);
        }

        public void removeRelation(JDBCCMRFieldBridge field,
                                   Object id,
                                   JDBCCMRFieldBridge relatedField,
                                   Object relatedId) {
            RelationData local = getRelationData();
            local.removeRelation(field, id, relatedField, relatedId);
        }

        public boolean isDirty() {
            RelationData local = getRelationData();
            return local.isDirty();
        }

        public RelationData getRelationData() {
            final RelationData local = (RelationData) relationData.get();
            return local;
        }
    }

    /*interface SecurityActions
    {
       class UTIL
       {
          static SecurityActions getSecurityActions()
          {
             return System.getSecurityManager() == null ? NON_PRIVILEGED : PRIVILEGED;
          }
       }

       SecurityActions NON_PRIVILEGED = new SecurityActions()
       {
          public Principal getPrincipal()
          {
             return SecurityAssociation.getPrincipal();
          }

          public Object getCredential()
          {
             return SecurityAssociation.getCredential();
          }
       };

       SecurityActions PRIVILEGED = new SecurityActions()
       {
          private final PrivilegedAction getPrincipalAction = new PrivilegedAction()
          {
             public Object run()
             {
                return SecurityAssociation.getPrincipal();
             }
          };

          private final PrivilegedAction getCredentialAction = new PrivilegedAction()
          {
             public Object run()
             {
                return SecurityAssociation.getCredential();
             }
          };

          public Principal getPrincipal()
          {
             return (Principal) AccessController.doPrivileged(getPrincipalAction);
          }

          public Object getCredential()
          {
             return AccessController.doPrivileged(getCredentialAction);
          }
       };

       Principal getPrincipal();

       Object getCredential();
    }*/
}
