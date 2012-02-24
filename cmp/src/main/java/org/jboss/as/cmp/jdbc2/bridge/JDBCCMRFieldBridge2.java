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
package org.jboss.as.cmp.jdbc2.bridge;

import java.lang.reflect.Array;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
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
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import org.jboss.as.cmp.CmpMessages;
import org.jboss.as.cmp.bridge.EntityBridge;
import org.jboss.as.cmp.bridge.FieldBridge;
import org.jboss.as.cmp.component.CmpEntityBeanComponent;
import org.jboss.as.cmp.context.CmpEntityBeanContext;
import org.jboss.as.cmp.ejbql.Catalog;
import org.jboss.as.cmp.jdbc.JDBCEntityPersistenceStore;
import org.jboss.as.cmp.jdbc.JDBCType;
import org.jboss.as.cmp.jdbc.JDBCUtil;
import org.jboss.as.cmp.jdbc.bridge.JDBCAbstractCMRFieldBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCAbstractEntityBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCFieldBridge;
import org.jboss.as.cmp.jdbc.metadata.JDBCCMPFieldMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCRelationshipRoleMetaData;
import org.jboss.as.cmp.jdbc2.JDBCStoreManager2;
import org.jboss.as.cmp.jdbc2.PersistentContext;
import org.jboss.as.cmp.jdbc2.schema.Cache;
import org.jboss.as.cmp.jdbc2.schema.EntityTable;
import org.jboss.as.cmp.jdbc2.schema.RelationTable;
import org.jboss.logging.Logger;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;


/**
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version <tt>$Revision: 81030 $</tt>
 */
public class JDBCCMRFieldBridge2 extends JDBCAbstractCMRFieldBridge {
    private final JDBCRelationshipRoleMetaData metadata;
    private final JDBCStoreManager2 manager;
    private final JDBCEntityBridge2 entity;
    private final int cmrIndex;
    private final Logger log;

    private JDBCEntityBridge2 relatedEntity;
    private JDBCCMRFieldBridge2 relatedCMRField;
    private CmpEntityBeanComponent relatedComponent;

    private JDBCCMPFieldBridge2[] tableKeyFields;
    private JDBCCMPFieldBridge2[] foreignKeyFields;
    private JDBCCMPFieldBridge2[] relatedPKFields;

    private CMRFieldLoader loader;
    private RelationTable relationTable;

    private EntityTable.ForeignKeyConstraint fkConstraint;

    private final TransactionManager tm;

    public JDBCCMRFieldBridge2(JDBCEntityBridge2 entityBridge,
                               JDBCStoreManager2 manager,
                               JDBCRelationshipRoleMetaData metadata) {
        this.manager = manager;
        this.entity = entityBridge;
        this.metadata = metadata;
        cmrIndex = entity.getNextCMRIndex();
        tm = manager.getComponent().getTransactionManager();

        log = Logger.getLogger(getClass().getName() + "." + entity.getEntityName() + "#" + getFieldName());
    }

    // Public

    public void resolveRelationship() {
        //
        // Set handles to the related entity's container, cache, manager, and invoker
        //

        // Related Entity Name
        String relatedEntityName = metadata.getRelatedRole().getEntity().getName();

        // Related Entity
        Catalog catalog = manager.getCatalog();
        relatedEntity = (JDBCEntityBridge2) catalog.getEntityByEJBName(relatedEntityName);
        if (relatedEntity == null) {
            throw CmpMessages.MESSAGES.relatedEntityNotFound(entity.getEntityName(), getFieldName(), relatedEntityName);
        }

        // Related CMR Field
        JDBCCMRFieldBridge2[] cmrFields = (JDBCCMRFieldBridge2[]) relatedEntity.getCMRFields();
        for (int i = 0; i < cmrFields.length; ++i) {
            JDBCCMRFieldBridge2 cmrField = cmrFields[i];
            if (metadata.getRelatedRole() == cmrField.getMetaData()) {
                relatedCMRField = cmrField;
                break;
            }
        }

        // if we didn't find the related CMR field throw an exception with a detailed message
        if (relatedCMRField == null) {
            throw CmpMessages.MESSAGES.relatedCmrFieldNotFound(relatedEntityName, entity.getEntityName(), getFieldName() != null ? getFieldName() : "<no-field>",
                    relatedEntityName, metadata.getRelatedRole().getCMRFieldName() != null ? metadata.getRelatedRole().getCMRFieldName() : "<no-field>");
        }

        // Related Container
        relatedComponent = relatedEntity.getComponent();

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
                    throw CmpMessages.MESSAGES.pkNotFoundForKeyField(cmpFieldMetaData.getFieldName());
                }
                pkFieldsToFKFields.put(pkField, new JDBCCMPFieldBridge2(manager, entity, cmpFieldMetaData, -1));
            }

            // second step is to order fk fields to match the order of pk fields
            JDBCFieldBridge[] pkFields = entity.getPrimaryKeyFields();
            for (int i = 0; i < pkFields.length; ++i) {
                Object fkField = pkFieldsToFKFields.get(pkFields[i]);
                if (fkField == null) {
                    throw CmpMessages.MESSAGES.primaryKeyNotMapped(pkFields[i].getFieldName());
                }
                keyFieldsList.add(fkField);
            }
            tableKeyFields = (JDBCCMPFieldBridge2[]) keyFieldsList.toArray(new JDBCCMPFieldBridge2[keyFieldsList.size()]);
        } else {
            initializeForeignKeyFields();
        }
    }

    public void initLoader() {
        if (metadata.getRelationMetaData().isTableMappingStyle()) {
            relationTable = relatedCMRField.getRelationTable();
            loader = new RelationTableLoader();
        } else {
            if (foreignKeyFields != null) {
                loader = new ContextForeignKeyLoader();
            } else {
                loader = new ForeignKeyLoader();
            }
        }
    }

    public JDBCRelationshipRoleMetaData getMetaData() {
        return metadata;
    }

    public boolean removeRelatedId(CmpEntityBeanContext ctx, Object relatedId) {
        FieldState state = getFieldState(ctx);
        return state.removeRelatedId(ctx, relatedId);
    }

    public boolean addRelatedId(CmpEntityBeanContext ctx, Object relatedId) {
        FieldState state = getFieldState(ctx);
        return state.addRelatedId(ctx, relatedId);
    }

    public void remove(CmpEntityBeanContext ctx) throws RemoveException {
        if (metadata.getRelatedRole().isCascadeDelete()) {
            FieldState state = getFieldState(ctx);
            state.cascadeDelete(ctx);
        } else {
            destroyExistingRelationships(ctx);
        }
    }

    public void destroyExistingRelationships(CmpEntityBeanContext ctx) {
        FieldState state = getFieldState(ctx);
        state.destroyExistingRelationships(ctx);
    }

    public JDBCFieldBridge[] getTableKeyFields() {
        return tableKeyFields;
    }

    public JDBCEntityPersistenceStore getManager() {
        return manager;
    }

    public boolean hasForeignKey() {
        return foreignKeyFields != null;
    }

    public JDBCAbstractCMRFieldBridge getRelatedCMRField() {
        return this.relatedCMRField;
    }

    public JDBCFieldBridge[] getForeignKeyFields() {
        return foreignKeyFields;
    }

    public JDBCCMRFieldBridge2 getRelatedField() {
        return relatedCMRField;
    }

    public JDBCAbstractEntityBridge getEntity() {
        return entity;
    }

    public String getQualifiedTableName() {
        return relationTable.getTableName();
    }

    public String getTableName() {
        throw CmpMessages.MESSAGES.methodNotSupported();
    }

    // JDBCFieldBridge implementation

    public JDBCType getJDBCType() {
        throw CmpMessages.MESSAGES.methodNotSupported();
    }

    public boolean isPrimaryKeyMember() {
        throw CmpMessages.MESSAGES.methodNotSupported();
    }

    public boolean isReadOnly() {
        throw CmpMessages.MESSAGES.methodNotSupported();
    }

    public boolean isReadTimedOut(CmpEntityBeanContext ctx) {
        throw CmpMessages.MESSAGES.methodNotSupported();
    }

    public boolean isLoaded(CmpEntityBeanContext ctx) {
        throw CmpMessages.MESSAGES.methodNotSupported();
    }

    public void initInstance(CmpEntityBeanContext ctx) {
        getFieldState(ctx).init();
    }

    public void resetPersistenceContext(CmpEntityBeanContext ctx) {
        throw CmpMessages.MESSAGES.methodNotSupported();
    }

    public int setInstanceParameters(PreparedStatement ps, int parameterIndex, CmpEntityBeanContext ctx) {
        throw CmpMessages.MESSAGES.methodNotSupported();
    }

    public Object getInstanceValue(CmpEntityBeanContext ctx) {
        throw CmpMessages.MESSAGES.methodNotSupported();
    }

    public void setInstanceValue(CmpEntityBeanContext ctx, Object value) {
        throw CmpMessages.MESSAGES.methodNotSupported();
    }

    public int loadInstanceResults(ResultSet rs, int parameterIndex, CmpEntityBeanContext ctx) {
        throw CmpMessages.MESSAGES.methodNotSupported();
    }

    public int loadArgumentResults(ResultSet rs, int parameterIndex, Object[] argumentRef) {
        throw CmpMessages.MESSAGES.methodNotSupported();
    }

    public boolean isDirty(CmpEntityBeanContext ctx) {
        return getFieldState(ctx).isModified();
    }

    public void setClean(CmpEntityBeanContext ctx) {
        throw CmpMessages.MESSAGES.methodNotSupported();
    }

    public boolean isCMPField() {
        return false;
    }

    // CMRFieldBridge implementation

    public String getFieldName() {
        return metadata.getCMRFieldName();
    }

    public Object getValue(CmpEntityBeanContext ctx) {
        FieldState state = getFieldState(ctx);
        return state.getValue(ctx);
    }

    public void setValue(CmpEntityBeanContext ctx, Object value) {
        FieldState state = getFieldState(ctx);
        state.setValue(ctx, value);
        state.cacheValue(ctx);
    }

    public boolean isSingleValued() {
        return metadata.getRelatedRole().isMultiplicityOne();
    }

    public EntityBridge getRelatedEntity() {
        return relatedEntity;
    }

    // Private

    private void initializeForeignKeyFields() {
        Collection foreignKeys = metadata.getRelatedRole().getKeyFields();

        // temporary map used later to write fk fields in special order
        Map fkFieldsByRelatedPKFields = new HashMap();
        for (Iterator i = foreignKeys.iterator(); i.hasNext(); ) {
            JDBCCMPFieldMetaData fkFieldMetaData = (JDBCCMPFieldMetaData) i.next();
            JDBCCMPFieldBridge2 relatedPKField =
                    (JDBCCMPFieldBridge2) relatedEntity.getFieldByName(fkFieldMetaData.getFieldName());

            // now determine whether the fk is mapped to a pk column
            String fkColumnName = fkFieldMetaData.getColumnName();
            JDBCCMPFieldBridge2 fkField = null;

            // look among the CMP fields for the field with the same column name
            JDBCCMPFieldBridge2[] tableFields = (JDBCCMPFieldBridge2[]) entity.getTableFields();
            for (int tableInd = 0; tableInd < tableFields.length && fkField == null; ++tableInd) {
                JDBCCMPFieldBridge2 cmpField = tableFields[tableInd];
                if (fkColumnName.equals(cmpField.getColumnName())) {
                    // construct the foreign key field
                    fkField = new JDBCCMPFieldBridge2(cmpField, relatedPKField);
                    /*
                       cmpField.getManager(), // this cmpField's manager
                       relatedPKField.getFieldName(),
                       relatedPKField.getFieldType(),
                       cmpField.getJDBCType(), // this cmpField's jdbc type
                       relatedPKField.isReadOnly(),
                       relatedPKField.getReadTimeOut(),
                       relatedPKField.getPrimaryKeyClass(),
                       relatedPKField.getPrimaryKeyField(),
                       cmpField, // CMP field I am mapped to
                       this,
                       fkColumnName
                    );
                    */
                }
            }

            // if the fk is not a part of pk then create a new field
            if (fkField == null) {
                fkField = entity.addTableField(fkFieldMetaData);
            }
            fkFieldsByRelatedPKFields.put(relatedPKField, fkField); // temporary map
        }

        // Note: this important to order the foreign key fields so that their order matches
        // the order of related entity's pk fields in case of complex primary keys.
        // The order is important in fk-constraint generation and in SELECT when loading
        if (fkFieldsByRelatedPKFields.size() > 0) {
            JDBCFieldBridge[] pkFields = relatedEntity.getPrimaryKeyFields();
            List fkList = new ArrayList(pkFields.length);
            List relatedPKList = new ArrayList(pkFields.length);
            for (int i = 0; i < pkFields.length; ++i) {
                JDBCFieldBridge relatedPKField = pkFields[i];
                JDBCFieldBridge fkField = (JDBCCMPFieldBridge2) fkFieldsByRelatedPKFields.remove(relatedPKField);
                fkList.add(fkField);
                relatedPKList.add(relatedPKField);
            }
            foreignKeyFields = (JDBCCMPFieldBridge2[]) fkList.toArray(new JDBCCMPFieldBridge2[fkList.size()]);
            relatedPKFields =
                    (JDBCCMPFieldBridge2[]) relatedPKList.toArray(new JDBCCMPFieldBridge2[relatedPKList.size()]);

            if (metadata.hasForeignKeyConstraint()) {
                fkConstraint = entity.getTable().addFkConstraint(foreignKeyFields, relatedEntity.getTable());
            }
        } else {
            foreignKeyFields = null;
            relatedPKFields = null;
        }
    }

    private FieldState getFieldState(CmpEntityBeanContext ctx) {
        PersistentContext pctx = (PersistentContext) ctx.getPersistenceContext();
        FieldState state = pctx.getCMRState(cmrIndex);
        if (state == null) {
            if (isSingleValued()) {
                state = new SingleValuedFieldState();
            } else {
                state = new CollectionValuedFieldState();
            }
            pctx.setCMRState(cmrIndex, state);
        }
        return state;
    }

    private void invokeRemoveRelatedId(CmpEntityBeanContext ctx, Object myId, Object relatedId) {
        if (log.isTraceEnabled()) {
            log.trace("Remove relation: field=" + getFieldName() +
                    " id=" + myId +
                    " relatedId=" + relatedId);
        }
        removeRelatedId(ctx, relatedId);
    }

    private void invokeAddRelatedId(CmpEntityBeanContext ctx, Object myId, Object relatedId) {
        if (log.isTraceEnabled()) {
            log.trace("Add relation: field=" + getFieldName() +
                    " id=" + myId +
                    " relatedId=" + relatedId);
        }
        addRelatedId(ctx, relatedId);
    }

    private Transaction getTransaction() throws SystemException {
        return tm.getTransaction();
    }

    private RelationTable getRelationTable() {
        if (relationTable == null) {
            relationTable = manager.getSchema().createRelationTable(this, relatedCMRField);
        }
        return relationTable;
    }

    private Object getPrimaryKey(Object o) {
        if (o == null) {
            throw CmpMessages.MESSAGES.nullNumbersNotSupported();
        }

        if (!relatedEntity.getLocalInterface().isInstance(o)) {
            throw CmpMessages.MESSAGES.invalidArgumentType(entity.getLocalInterface().getName());
        }

        EJBLocalObject local = (EJBLocalObject) o;
        try {
            return local.getPrimaryKey();
        } catch (NoSuchObjectLocalException e) {
            throw new IllegalStateException(e);
        }
    }

    // Inner

    public class SingleValuedFieldState
            implements FieldState {
        private boolean loaded;
        private Object value;
        private EJBLocalObject localObject;
        private boolean modified;

        public void init() {
            loaded = true;
        }

        public Object getValue(CmpEntityBeanContext ctx) {
            Object value = getLoadedValue(ctx);
            if (value == null) {
                localObject = null;
            } else if (localObject == null) {
                localObject = relatedComponent.getEJBLocalObject(value);
            }
            return localObject;
        }

        public void setValue(CmpEntityBeanContext ctx, Object value) {
            if (value != null) {
                Object relatedId = getPrimaryKey(value);
                addRelatedId(ctx, relatedId);
                relatedCMRField.invokeAddRelatedId(ctx, relatedId, ctx.getPrimaryKey());
                localObject = (EJBLocalObject) value;
            } else {
                destroyExistingRelationships(ctx);
            }
        }

        public void cascadeDelete(CmpEntityBeanContext ctx) throws RemoveException {
            if (manager.registerCascadeDelete(ctx.getPrimaryKey(), ctx.getPrimaryKey())) {
                EJBLocalObject value = (EJBLocalObject) getValue(ctx);
                if (value != null) {
                    changeValue(null);

                    final Object relatedId = value.getPrimaryKey();
                    final JDBCStoreManager2 relatedManager = (JDBCStoreManager2) relatedEntity.getManager();

                    if (!relatedManager.isCascadeDeleted(relatedId)) {
                        value.remove();
                    }
                }

                manager.unregisterCascadeDelete(ctx.getPrimaryKey());
            }
        }

        public void destroyExistingRelationships(CmpEntityBeanContext ctx) {
            Object value = getLoadedValue(ctx);
            if (value != null) {
                removeRelatedId(ctx, value);
                relatedCMRField.invokeRemoveRelatedId(ctx, value, ctx.getPrimaryKey());
            }
        }

        public boolean removeRelatedId(CmpEntityBeanContext ctx, Object relatedId) {
            if (hasForeignKey()) {
                getLoadedValue(ctx);
            }

            changeValue(null);
            loader.removeRelatedId(ctx, relatedId);

            cacheValue(ctx);

            modified = true;

            return true;
        }

        public boolean addRelatedId(CmpEntityBeanContext ctx, Object relatedId) {
            Object value = getLoadedValue(ctx);
            if (value != null) {
                relatedCMRField.invokeRemoveRelatedId(ctx, value, ctx.getPrimaryKey());
            }

            changeValue(relatedId);
            loader.addRelatedId(ctx, relatedId);

            cacheValue(ctx);

            modified = true;

            return true;
        }

        public void addLoadedPk(Object pk) {
            if (loaded) {
                throw CmpMessages.MESSAGES.singleValuedCmrFieldAlreadyLoaded(entity.getEntityName(), getFieldName(), value, pk);
            }

            changeValue(pk);
        }

        public Object loadFromCache(Object value) {
            if (value != null) {
                changeValue(NULL_VALUE == value ? null : value);
            }
            return value;
        }

        public Object getCachedValue() {
            return value == null ? NULL_VALUE : value;
        }

        public void cacheValue(CmpEntityBeanContext ctx) {
            PersistentContext pctx = (PersistentContext) ctx.getPersistenceContext();
            pctx.cacheRelations(cmrIndex, this);
        }

        public boolean isModified() {
            return modified;
        }

        // Private

        private void changeValue(Object newValue) {
            this.value = newValue;
            this.localObject = null;
            loaded = true;
        }

        private Object getLoadedValue(CmpEntityBeanContext ctx) {
            if (!loaded) {
                PersistentContext pctx = (PersistentContext) ctx.getPersistenceContext();
                pctx.loadCachedRelations(cmrIndex, this);
                if (!loaded) {
                    loader.load(ctx, this);
                    loaded = true;
                    cacheValue(ctx);
                }
            }
            return value;
        }
    }

    public class CollectionValuedFieldState
            implements FieldState {
        private boolean loaded;
        private Set value;
        private CMRSet cmrSet;

        private Set removedWhileNotLoaded;
        private Set addedWhileNotLoaded;

        private boolean modified;

        public void init() {
            loaded = true;
            value = new HashSet();
        }

        public Object getValue(CmpEntityBeanContext ctx) {
            if (cmrSet == null) {
                cmrSet = new CMRSet(ctx, this);
            }
            return cmrSet;
        }

        public void setValue(CmpEntityBeanContext ctx, Object value) {
            if (value == null) {
                throw CmpMessages.MESSAGES.cannotSetCmrCollectionToNull(entity.getEntityName(), getFieldName());
            }

            destroyExistingRelationships(ctx);

            Collection newValue = (Collection) value;
            if (!newValue.isEmpty()) {
                Set copy = new HashSet(newValue);
                for (Iterator iter = copy.iterator(); iter.hasNext(); ) {
                    Object relatedId = getPrimaryKey(iter.next());
                    addRelatedId(ctx, relatedId);
                    relatedCMRField.invokeAddRelatedId(ctx, relatedId, ctx.getPrimaryKey());
                    loader.addRelatedId(ctx, relatedId);
                }
            }
        }

        public void cascadeDelete(CmpEntityBeanContext ctx) throws RemoveException {
            Collection value = (Collection) getValue(ctx);
            if (!value.isEmpty()) {
                EJBLocalObject[] locals = (EJBLocalObject[]) value.toArray();
                for (int i = 0; i < locals.length; ++i) {
                    locals[i].remove();
                }
            }
        }

        public void destroyExistingRelationships(CmpEntityBeanContext ctx) {
            Set value = getLoadedValue(ctx);
            if (!value.isEmpty()) {
                Object[] copy = value.toArray();
                for (int i = 0; i < copy.length; ++i) {
                    Object relatedId = copy[i];
                    removeRelatedId(ctx, relatedId);
                    relatedCMRField.invokeRemoveRelatedId(ctx, relatedId, ctx.getPrimaryKey());
                    loader.removeRelatedId(ctx, relatedId);
                }
            }
        }

        public boolean removeRelatedId(CmpEntityBeanContext ctx, Object relatedId) {
            boolean removed = false;
            if (loaded) {
                Set value = getLoadedValue(ctx);
                if (!value.isEmpty()) {
                    removed = value.remove(relatedId);
                }
            } else {
                loadOnlyFromCache(ctx);
                if (loaded) {
                    Set value = getLoadedValue(ctx);
                    if (!value.isEmpty()) {
                        removed = value.remove(relatedId);
                    }
                } else {
                    removed = removeWhileNotLoaded(relatedId);
                }
            }

            modified = true;

            if (removed) {
                ((PersistentContext) ctx.getPersistenceContext()).setDirtyRelations();
            }

            return removed;
        }

        public boolean addRelatedId(CmpEntityBeanContext ctx, Object relatedId) {
            boolean added;
            if (loaded) {
                Set value = getLoadedValue(ctx);
                added = value.add(relatedId);
            } else {
                loadOnlyFromCache(ctx);
                if (loaded) {
                    Set value = getLoadedValue(ctx);
                    added = value.add(relatedId);
                } else {
                    added = addWhileNotLoaded(relatedId);
                }
            }

            modified = true;

            if (added) {
                ((PersistentContext) ctx.getPersistenceContext()).setDirtyRelations();
            }

            return added;
        }

        public void addLoadedPk(Object pk) {
            if (loaded) {
                throw CmpMessages.MESSAGES.collectionValuedCmrFieldAlreadyLoaded(entity.getEntityName(), getFieldName(), value, pk);
            }

            if (pk != null) {
                value.add(pk);
            }
        }

        public Object loadFromCache(Object value) {
            if (value != null) {
                value = this.value = new HashSet((Set) value);
                loaded = true;
            }
            return value;
        }

        public Object getCachedValue() {
            return value;
        }

        public void cacheValue(CmpEntityBeanContext ctx) {
            PersistentContext pctx = (PersistentContext) ctx.getPersistenceContext();
            pctx.cacheRelations(cmrIndex, this);
        }

        public boolean isModified() {
            return modified;
        }

        // Private

        private Set getLoadedValue(CmpEntityBeanContext ctx) {
            if (!loaded) {
                loadOnlyFromCache(ctx);

                if (!loaded) {
                    if (value == null || value == Collections.EMPTY_SET) {
                        value = new HashSet();
                    }

                    loader.load(ctx, this);
                    cacheValue(ctx);

                    loaded = true;
                }

                if (addedWhileNotLoaded != null) {
                    value.addAll(addedWhileNotLoaded);
                    addedWhileNotLoaded = null;
                }

                if (removedWhileNotLoaded != null) {
                    value.removeAll(removedWhileNotLoaded);
                    removedWhileNotLoaded = null;
                }
            }
            return value;
        }

        private void loadOnlyFromCache(CmpEntityBeanContext ctx) {
            PersistentContext pctx = (PersistentContext) ctx.getPersistenceContext();
            if (pctx == null) {
                throw CmpMessages.MESSAGES.persistenceContextNotAvailable();
            }
            pctx.loadCachedRelations(cmrIndex, this);
        }

        private boolean removeWhileNotLoaded(Object relatedId) {
            boolean removed = false;
            if (addedWhileNotLoaded != null) {
                removed = addedWhileNotLoaded.remove(relatedId);
            }

            if (!removed) {
                if (removedWhileNotLoaded == null) {
                    removedWhileNotLoaded = new HashSet();
                }
                removed = removedWhileNotLoaded.add(relatedId);
            }

            if (log.isTraceEnabled() && removed) {
                log.trace("removed while not loaded: relatedId=" + relatedId);
            }

            return removed;
        }

        private boolean addWhileNotLoaded(Object relatedId) {
            boolean added = false;
            if (removedWhileNotLoaded != null) {
                added = removedWhileNotLoaded.remove(relatedId);
            }

            if (!added) {
                if (addedWhileNotLoaded == null) {
                    addedWhileNotLoaded = new HashSet();
                }
                added = addedWhileNotLoaded.add(relatedId);
            }

            if (log.isTraceEnabled() && added) {
                log.trace("added while not loaded: relatedId=" + relatedId);
            }

            return added;
        }
    }

    public interface FieldState
            extends Cache.CacheLoader {
        Object NULL_VALUE = new Object();

        void init();

        Object getValue(CmpEntityBeanContext ctx);

        void cascadeDelete(CmpEntityBeanContext ctx) throws RemoveException;

        void destroyExistingRelationships(CmpEntityBeanContext ctx);

        void setValue(CmpEntityBeanContext ctx, Object value);

        boolean removeRelatedId(CmpEntityBeanContext ctx, Object relatedId);

        boolean addRelatedId(CmpEntityBeanContext ctx, Object value);

        void addLoadedPk(Object pk);

        void cacheValue(CmpEntityBeanContext ctx);

        boolean isModified();
    }

    private class RelationTableLoader
            implements CMRFieldLoader {
        private final String loadSql;

        public RelationTableLoader() {
            StringBuffer sql = new StringBuffer();
            sql.append("select ");

            String relatedTable = relatedEntity.getQualifiedTableName();
            String relationTable = metadata.getRelationMetaData().getDefaultTableName();

            relatedEntity.getTable().appendColumnNames((JDBCCMPFieldBridge2[]) relatedEntity.getTableFields(),
                    relatedTable,
                    sql
            );
            sql.append(" from ")
                    .append(relatedTable)
                    .append(" inner join ")
                    .append(relationTable)
                    .append(" on ");

            JDBCCMPFieldBridge2[] pkFields = (JDBCCMPFieldBridge2[]) relatedEntity.getPrimaryKeyFields();
            for (int i = 0; i < pkFields.length; ++i) {
                if (i > 0) {
                    sql.append(" and ");
                }

                sql.append(relatedTable).append('.').append(pkFields[i].getColumnName())
                        .append('=')
                        .append(relationTable).append('.').append(relatedCMRField.tableKeyFields[i].getColumnName());
            }

            /*
            sql.append(" inner join ")
               .append(myTable)
               .append(" on ");

            String myTable = entity.getQualifiedTableName();
            pkFields = entity.getPrimaryKeyFields();
            for(int i = 0; i < pkFields.length; ++i)
            {
               if(i > 0)
               {
                  sql.append(", ");
               }

               sql.append(myTable).append('.').append(pkFields[i].getColumnName())
                  .append('=')
                  .append(relationTable).append('.').append(tableKeyFields[i].getColumnName());
            }
            */

            sql.append(" where ");
            for (int i = 0; i < tableKeyFields.length; ++i) {
                if (i > 0) {
                    sql.append(" and ");
                }

                sql.append(relationTable).append('.').append(tableKeyFields[i].getColumnName()).append("=?");
            }

            loadSql = sql.toString();

            if (log.isTraceEnabled()) {
                log.trace("load sql: " + loadSql);
            }
        }

        public void load(CmpEntityBeanContext ctx, FieldState state) {
            Object value;
            EntityTable relatedTable = relatedEntity.getTable();

            Connection con = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                if (log.isDebugEnabled()) {
                    log.debug("executing: " + loadSql);
                }

                con = relatedTable.getDataSource().getConnection();
                ps = con.prepareStatement(loadSql);

                JDBCCMPFieldBridge2[] pkFields = (JDBCCMPFieldBridge2[]) entity.getPrimaryKeyFields();

                Object myPk = ctx.getPrimaryKey();
                int paramInd = 1;
                for (int i = 0; i < pkFields.length; ++i) {
                    JDBCCMPFieldBridge2 pkField = pkFields[i];
                    Object fieldValue = pkField.getPrimaryKeyValue(myPk);

                    JDBCCMPFieldBridge2 relatedFkField = tableKeyFields[i];
                    relatedFkField.setArgumentParameters(ps, paramInd++, fieldValue);
                }

                rs = ps.executeQuery();

                while (rs.next()) {
                    value = relatedTable.loadRow(rs, false);
                    state.addLoadedPk(value);
                }
            } catch (SQLException e) {
                throw CmpMessages.MESSAGES.failedToLoadRelatedRole(entity.getEntityName(), getFieldName(), e);
            } finally {
                JDBCUtil.safeClose(rs);
                JDBCUtil.safeClose(ps);
                JDBCUtil.safeClose(con);
            }
        }

        public void removeRelatedId(CmpEntityBeanContext ctx, Object relatedId) {
            relationTable.removeRelation(JDBCCMRFieldBridge2.this, ctx.getPrimaryKey(), relatedCMRField, relatedId);
        }

        public void addRelatedId(CmpEntityBeanContext ctx, Object relatedId) {
            relationTable.addRelation(JDBCCMRFieldBridge2.this, ctx.getPrimaryKey(), relatedCMRField, relatedId);
        }
    }

    private class ForeignKeyLoader
            implements CMRFieldLoader {
        private final String loadSql;

        public ForeignKeyLoader() {
            StringBuffer sql = new StringBuffer();
            sql.append("select ");
            relatedEntity.getTable().appendColumnNames((JDBCCMPFieldBridge2[]) relatedEntity.getTableFields(), null, sql);
            sql.append(" from ").append(relatedEntity.getQualifiedTableName()).append(" where ");

            JDBCCMPFieldBridge2[] relatedFkFields = relatedCMRField.foreignKeyFields;
            sql.append(relatedFkFields[0].getColumnName()).append("=?");
            for (int i = 1; i < relatedFkFields.length; ++i) {
                JDBCCMPFieldBridge2 relatedFkField = relatedFkFields[i];
                sql.append(" and ").append(relatedFkField.getColumnName()).append("=?");
            }

            loadSql = sql.toString();

            if (log.isTraceEnabled()) {
                log.trace("load sql: " + loadSql);
            }
        }

        public void load(CmpEntityBeanContext ctx, FieldState state) {
            Object value;
            EntityTable relatedTable = relatedEntity.getTable();

            Connection con = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                if (log.isDebugEnabled()) {
                    log.debug("executing: " + loadSql);
                }

                con = relatedTable.getDataSource().getConnection();
                ps = con.prepareStatement(loadSql);

                JDBCCMPFieldBridge2[] relatedFkFields = relatedCMRField.foreignKeyFields;
                JDBCCMPFieldBridge2[] myPkFields = relatedCMRField.relatedPKFields;

                Object myPk = ctx.getPrimaryKey();
                int paramInd = 1;
                for (int i = 0; i < relatedFkFields.length; ++i) {
                    JDBCCMPFieldBridge2 myPkField = myPkFields[i];
                    Object fieldValue = myPkField.getPrimaryKeyValue(myPk);

                    JDBCCMPFieldBridge2 relatedFkField = relatedFkFields[i];
                    relatedFkField.setArgumentParameters(ps, paramInd++, fieldValue);
                }

                rs = ps.executeQuery();

                while (rs.next()) {
                    value = relatedTable.loadRow(rs, false);
                    state.addLoadedPk(value);
                }
            } catch (SQLException e) {
                throw CmpMessages.MESSAGES.failedToLoadRelatedRole(entity.getEntityName(), getFieldName(), e);
            } finally {
                JDBCUtil.safeClose(rs);
                JDBCUtil.safeClose(ps);
                JDBCUtil.safeClose(con);
            }
        }

        public void removeRelatedId(CmpEntityBeanContext ctx, Object relatedId) {
        }

        public void addRelatedId(CmpEntityBeanContext ctx, Object relatedId) {
        }
    }

    private class ContextForeignKeyLoader
            implements CMRFieldLoader {
        public void load(CmpEntityBeanContext ctx, FieldState state) {
            Object relatedId = null;
            for (int i = 0; i < foreignKeyFields.length; ++i) {
                JDBCCMPFieldBridge2 fkField = foreignKeyFields[i];
                Object fkFieldValue = fkField.getValue(ctx);
                if (fkFieldValue == null) {
                    break;
                }

                JDBCCMPFieldBridge2 relatedPKField = relatedPKFields[i];
                relatedId = relatedPKField.setPrimaryKeyValue(relatedId, fkFieldValue);
            }

            state.addLoadedPk(relatedId);
        }

        public void removeRelatedId(CmpEntityBeanContext ctx, Object relatedId) {
            for (int i = 0; i < foreignKeyFields.length; ++i) {
                foreignKeyFields[i].setValueInternal(ctx, null, fkConstraint == null);
            }

            if (fkConstraint != null) {
                PersistentContext pctx = (PersistentContext) ctx.getPersistenceContext();
                pctx.nullForeignKey(fkConstraint);
            }
        }

        public void addRelatedId(CmpEntityBeanContext ctx, Object relatedId) {
            final boolean markDirty = relatedId != null || fkConstraint == null;
            for (int i = 0; i < foreignKeyFields.length; ++i) {
                JDBCCMPFieldBridge2 relatedPKField = relatedPKFields[i];
                Object fieldValue = relatedPKField.getPrimaryKeyValue(relatedId);
                foreignKeyFields[i].setValueInternal(ctx, fieldValue, markDirty);
            }

            if (fkConstraint != null) {
                PersistentContext pctx = (PersistentContext) ctx.getPersistenceContext();
                if (relatedId == null) {
                    pctx.nullForeignKey(fkConstraint);
                } else {
                    pctx.nonNullForeignKey(fkConstraint);
                }
            }
        }
    }

    private interface CMRFieldLoader {
        void load(CmpEntityBeanContext ctx, FieldState state);

        void removeRelatedId(CmpEntityBeanContext ctx, Object relatedId);

        void addRelatedId(CmpEntityBeanContext ctx, Object relatedId);
    }

    private class CMRSet
            implements Set {
        private final CmpEntityBeanContext ctx;
        private final CollectionValuedFieldState state;

        public CMRSet(CmpEntityBeanContext ctx, CollectionValuedFieldState state) {
            this.ctx = ctx;
            this.state = state;
        }

        public int size() {
            return state.getLoadedValue(ctx).size();
        }

        public void clear() {
            destroyExistingRelationships(ctx);
        }

        public boolean isEmpty() {
            return size() == 0;
        }

        public boolean add(Object o) {
            Object relatedId = getPrimaryKey(o);
            boolean modified = addRelatedId(ctx, relatedId);

            if (modified) {
                relatedCMRField.invokeAddRelatedId(ctx, relatedId, ctx.getPrimaryKey());
                loader.addRelatedId(ctx, relatedId);
            }

            return modified;
        }

        public boolean contains(Object o) {
            Object pk = getPrimaryKey(o);
            return state.getLoadedValue(ctx).contains(pk);
        }

        public boolean remove(Object o) {
            Object relatedId = getPrimaryKey(o);
            return removeById(relatedId);
        }

        public boolean addAll(Collection c) {
            if (c == null || c.isEmpty()) {
                return false;
            }

            boolean modified = false;
            Object[] copy = c.toArray();
            for (int i = 0; i < copy.length; ++i) {
                // not modified || add()
                modified = add(copy[i]) || modified;
            }

            return modified;
        }

        public boolean containsAll(Collection c) {
            if (c == null || c.isEmpty()) {
                return true;
            }

            Set ids = argumentToIdSet(c);
            return state.getLoadedValue(ctx).containsAll(ids);
        }

        public boolean removeAll(Collection c) {
            if (c == null || c.isEmpty()) {
                return false;
            }

            boolean modified = false;
            Object[] copy = c.toArray();
            for (int i = 0; i < copy.length; ++i) {
                modified = remove(copy[i]) || modified;
            }

            return modified;
        }

        public boolean retainAll(Collection c) {
            Set value = state.getLoadedValue(ctx);
            if (c == null || c.isEmpty()) {
                if (value.isEmpty()) {
                    return false;
                } else {
                    clear();
                }
            }

            boolean modified = false;
            Set idSet = argumentToIdSet(c);
            Object[] valueCopy = value.toArray();
            for (int i = 0; i < valueCopy.length; ++i) {
                Object id = valueCopy[i];
                if (!idSet.contains(id)) {
                    removeById(id);
                    modified = true;
                }
            }

            return modified;
        }

        public Iterator iterator() {
            return new Iterator() {
                // todo get rid of copying
                private final Iterator idIter = new HashSet(state.getLoadedValue(ctx)).iterator();
                private Object curId;

                public void remove() {
                    try {
                        idIter.remove();
                    } catch (ConcurrentModificationException e) {
                        throw new IllegalStateException(e);
                    }

                    removeById(curId);
                }

                public boolean hasNext() {
                    try {
                        return idIter.hasNext();
                    } catch (ConcurrentModificationException e) {
                        throw new IllegalStateException(e);
                    }
                }

                public Object next() {
                    try {
                        curId = idIter.next();
                    } catch (ConcurrentModificationException e) {
                        throw new IllegalStateException(e);
                    }

                    return relatedComponent.getEJBLocalObject(curId);
                }
            };
        }

        public Object[] toArray() {
            Set value = state.getLoadedValue(ctx);

            Object[] result = (Object[]) Array.newInstance(relatedEntity.getLocalInterface(), value.size());

            int i = 0;
            for (Iterator iter = value.iterator(); iter.hasNext(); ) {
                Object id = iter.next();
                result[i++] = relatedComponent.getEJBLocalObject(id);
            }

            return result;
        }

        public Object[] toArray(Object[] a) {
            Set value = state.getLoadedValue(ctx);
            if (a == null || a.length < value.size()) {
                a = (Object[]) Array.newInstance(entity.getLocalInterface(), value.size());
            }

            int i = 0;
            for (Iterator iter = value.iterator(); iter.hasNext(); ) {
                Object id = iter.next();
                a[i++] = relatedComponent.getEJBLocalObject(id);
            }

            return a;
        }

        public String toString() {
            return state.getLoadedValue(ctx).toString();
        }

        // Private

        private boolean removeById(Object relatedId) {
            boolean modified = removeRelatedId(ctx, relatedId);
            if (modified) {
                relatedCMRField.invokeRemoveRelatedId(ctx, relatedId, ctx.getPrimaryKey());
                loader.removeRelatedId(ctx, relatedId);
            }
            return modified;
        }

        private Set argumentToIdSet(Collection c) {
            Set ids = new HashSet();
            for (Iterator iter = c.iterator(); iter.hasNext(); ) {
                Object pk = getPrimaryKey(iter.next());
                ids.add(pk);
            }
            return ids;
        }
    }

    interface SecurityActions {
        class UTIL {
            static SecurityActions getSecurityActions() {
                return System.getSecurityManager() == null ? NON_PRIVILEGED : PRIVILEGED;
            }
        }

        SecurityActions NON_PRIVILEGED = new SecurityActions() {
            public Principal getPrincipal() {
                //return SecurityAssociation.getPrincipal();
                Principal p = null;
                SecurityContext sc = getSecurityContext();
                if (sc != null) {
                    p = sc.getUtil().getUserPrincipal();
                }
                return p;
            }

            public Object getCredential() {
                //return SecurityAssociation.getCredential();
                Object credential = null;
                SecurityContext sc = getSecurityContext();
                if (sc != null) {
                    credential = sc.getUtil().getCredential();
                }
                return credential;
            }

            public SecurityContext getSecurityContext() {
                return SecurityContextAssociation.getSecurityContext();
            }
        };

        SecurityActions PRIVILEGED = new SecurityActions() {
            private final PrivilegedAction getPrincipalAction = new PrivilegedAction() {
                public Object run() {
                    //return SecurityAssociation.getPrincipal();
                    Principal p = null;
                    SecurityContext sc = getSecurityContext();
                    if (sc != null) {
                        p = sc.getUtil().getUserPrincipal();
                    }
                    return p;
                }
            };

            private final PrivilegedAction getCredentialAction = new PrivilegedAction() {
                public Object run() {
                    //return SecurityAssociation.getCredential();
                    Object credential = null;
                    SecurityContext sc = getSecurityContext();
                    if (sc != null) {
                        credential = sc.getUtil().getCredential();
                    }
                    return credential;
                }
            };

            public Principal getPrincipal() {
                return (Principal) AccessController.doPrivileged(getPrincipalAction);
            }

            public Object getCredential() {
                return AccessController.doPrivileged(getCredentialAction);
            }

            public SecurityContext getSecurityContext() {
                return (SecurityContext) AccessController.doPrivileged(new PrivilegedAction() {

                    public Object run() {
                        return SecurityContextAssociation.getSecurityContext();
                    }
                });
            }
        };

        Principal getPrincipal();

        Object getCredential();

        SecurityContext getSecurityContext();
    }
}
