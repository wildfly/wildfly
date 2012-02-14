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

import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.ejb.EJBException;
import javax.ejb.RemoveException;
import javax.sql.DataSource;
import org.jboss.as.cmp.bridge.FieldBridge;
import org.jboss.as.cmp.context.CmpEntityBeanContext;
import org.jboss.as.cmp.jdbc.JDBCContext;
import org.jboss.as.cmp.jdbc.JDBCEntityPersistenceStore;
import org.jboss.as.cmp.jdbc.JDBCStoreManager;
import org.jboss.as.cmp.jdbc.JDBCTypeFactory;
import org.jboss.as.cmp.jdbc.LockingStrategy;
import org.jboss.as.cmp.jdbc.SQLUtil;
import org.jboss.as.cmp.jdbc.metadata.JDBCAuditMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCCMPFieldMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCEntityMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCOptimisticLockingMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCQueryMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCReadAheadMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCRelationshipRoleMetaData;
import org.jboss.logging.Logger;


/**
 * JDBCEntityBridge follows the Bridge pattern [Gamma et. al, 1995].
 * The main job of this class is to construct the bridge from entity meta data.
 * <p/>
 * Life-cycle:
 * Undefined. Should be tied to CMPStoreManager.
 * <p/>
 * Multiplicity:
 * One per cmp entity bean type.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="mailto:loubyansky@ua.fm">Alex Loubyansky</a>
 * @author <a href="mailto:heiko.rupp@cellent.de">Heiko W. Rupp</a>
 * @version $Revision: 81030 $
 */
public class JDBCEntityBridge implements JDBCAbstractEntityBridge {
    public static final byte LOADED = 1;
    public static final byte LOAD_REQUIRED = 2;
    public static final byte DIRTY = 4;
    public static final byte CHECK_DIRTY = 8;
    public static final byte LOCKED = 16;
    public static final byte ADD_TO_SET_ON_UPDATE = 32;
    public static final byte ADD_TO_WHERE_ON_UPDATE = 64;

    private static final String DEFAULT_LOADGROUP_NAME = "*";

    private JDBCEntityMetaData metadata;
    private JDBCStoreManager manager;
    private DataSource dataSource;
    private String qualifiedTableName;
    private String tableName;

    /**
     * primary key fields (not added to cmpFields)
     */
    private final String primaryKeyFieldName;
    private final Class<?> primaryKeyClass;
    private JDBCCMPFieldBridge[] primaryKeyFields;
    /**
     * CMP fields
     */
    private JDBCCMPFieldBridge[] cmpFields;
    /**
     * CMR fields
     */
    private JDBCCMRFieldBridge[] cmrFields;
    /**
     * table fields
     */
    private JDBCCMPFieldBridge[] tableFields;

    /**
     * used for optimistic locking. (added to cmpFields)
     */
    private JDBCCMPFieldBridge versionField;

    // Audit fields (added to cmpFields)
    private JDBCCMPFieldBridge createdPrincipalField;
    private JDBCCMPFieldBridge createdTimeField;
    private JDBCCMPFieldBridge updatedPrincipalField;
    private JDBCCMPFieldBridge updatedTimeField;

    private Map<Method, JDBCSelectorBridge> selectorsByMethod;

    /**
     * Load group is a boolean array with tableFields.length elements. True means the element is in the group.
     */
    private Map loadGroupMasks;
    private List lazyLoadGroupMasks;
    private boolean[] eagerLoadGroupMask;
    private boolean[] defaultLockGroupMask;

    private int jdbcContextSize;

    private final Logger log;

    public JDBCEntityBridge(JDBCEntityMetaData metadata, JDBCStoreManager manager) throws Exception {
        this.metadata = metadata;
        this.manager = manager;
        primaryKeyFieldName = metadata.getPrimaryKeyFieldName();
        primaryKeyClass = metadata.getPrimaryKeyClass();
        log = Logger.getLogger(this.getClass().getName() + "." + metadata.getName());
    }

    public void init() throws Exception {
        dataSource = manager.getDataSource(metadata.getDataSourceName());
        qualifiedTableName = SQLUtil.fixTableName(metadata.getDefaultTableName(), dataSource);
        int dotIndex = qualifiedTableName.indexOf('.');
        tableName = dotIndex == -1 ? qualifiedTableName : qualifiedTableName.substring(dotIndex + 1);

        // CMP fields
        loadCMPFields(metadata);

        // CMR fields
        loadCMRFields(metadata);

        // create locking field
        JDBCOptimisticLockingMetaData lockMetaData = metadata.getOptimisticLocking();
        if (lockMetaData != null && lockMetaData.getLockingField() != null) {
            JDBCOptimisticLockingMetaData.LockingStrategy strategy = lockMetaData.getLockingStrategy();
            JDBCCMPFieldMetaData versionMD = lockMetaData.getLockingField();

            versionField = getCMPFieldByName(versionMD.getFieldName());
            boolean hidden = versionField == null;
            switch (strategy) {
                case VERSION_COLUMN_STRATEGY: {
                    if (hidden)
                        versionField = new JDBCLongVersionFieldBridge(manager, versionMD);
                    else
                        versionField = new JDBCLongVersionFieldBridge((JDBCCMP2xFieldBridge) versionField);
                    break;
                }
                case TIMESTAMP_COLUMN_STRATEGY: {
                    if (hidden)
                        versionField = new JDBCTimestampVersionFieldBridge(manager, versionMD);
                    else
                        versionField = new JDBCTimestampVersionFieldBridge((JDBCCMP2xFieldBridge) versionField);
                    break;
                }
                case KEYGENERATOR_COLUMN_STRATEGY: {
                    if (hidden)
                        versionField = new JDBCKeyGenVersionFieldBridge(manager, versionMD, lockMetaData.getKeyGeneratorFactory());
                    else
                        versionField = new JDBCKeyGenVersionFieldBridge((JDBCCMP2xFieldBridge) versionField, lockMetaData.getKeyGeneratorFactory());
                    break;
                }
            }
            if (hidden)
                addCMPField(versionField);
            else
                tableFields[versionField.getTableIndex()] = versionField;
        }

        // audit fields
        JDBCAuditMetaData auditMetaData = metadata.getAudit();
        if (auditMetaData != null) {
            JDBCCMPFieldMetaData auditField = auditMetaData.getCreatedPrincipalField();
            if (auditField != null) {
                createdPrincipalField = getCMPFieldByName(auditField.getFieldName());
                if (createdPrincipalField == null) {
                    createdPrincipalField = new JDBCCMP2xFieldBridge(manager, auditField);
                    addCMPField(createdPrincipalField);
                }
            } else {
                createdPrincipalField = null;
            }

            auditField = auditMetaData.getCreatedTimeField();
            if (auditField != null) {
                createdTimeField = getCMPFieldByName(auditField.getFieldName());
                if (createdTimeField == null) {
                    createdTimeField = new JDBCCMP2xFieldBridge(manager, auditField, JDBCTypeFactory.EQUALS, false);
                    addCMPField(createdTimeField);
                } else {
                    // just to override state factory and check-dirty-after-get
                    createdTimeField = new JDBCCMP2xFieldBridge(
                            (JDBCCMP2xFieldBridge) createdTimeField, JDBCTypeFactory.EQUALS, false);
                    tableFields[createdTimeField.getTableIndex()] = createdTimeField;
                }
            } else {
                createdTimeField = null;
            }

            auditField = auditMetaData.getUpdatedPrincipalField();
            if (auditField != null) {
                updatedPrincipalField = getCMPFieldByName(auditField.getFieldName());
                if (updatedPrincipalField == null) {
                    updatedPrincipalField = new JDBCCMP2xUpdatedPrincipalFieldBridge(manager, auditField);
                    addCMPField(updatedPrincipalField);
                } else {
                    updatedPrincipalField = new JDBCCMP2xUpdatedPrincipalFieldBridge(
                            (JDBCCMP2xFieldBridge) updatedPrincipalField);
                    tableFields[updatedPrincipalField.getTableIndex()] = updatedPrincipalField;
                }
            } else {
                updatedPrincipalField = null;
            }

            auditField = auditMetaData.getUpdatedTimeField();
            if (auditField != null) {
                updatedTimeField = getCMPFieldByName(auditField.getFieldName());
                if (updatedTimeField == null) {
                    updatedTimeField = new JDBCCMP2xUpdatedTimeFieldBridge(manager, auditField);
                    addCMPField(updatedTimeField);
                } else {
                    updatedTimeField = new JDBCCMP2xUpdatedTimeFieldBridge((JDBCCMP2xFieldBridge) updatedTimeField);
                    tableFields[updatedTimeField.getTableIndex()] = updatedTimeField;
                }
            } else {
                updatedTimeField = null;
            }
        }

        // ejbSelect methods
        loadSelectors(metadata);
    }

    public void resolveRelationships() {
        for (int i = 0; i < cmrFields.length; ++i)
            cmrFields[i].resolveRelationship();

        // load groups:  cannot be created until relationships have
        // been resolved because loadgroups must check for foreign keys
        loadLoadGroups(metadata);
        loadEagerLoadGroup(metadata);
        loadLazyLoadGroups(metadata);
    }

    /**
     * The third phase of deployment. The method is called when relationships are already resolved.
     */
    public void start() {
        for (int i = 0; i < cmrFields.length; ++i) {
            cmrFields[i].start();
        }
    }

    public boolean removeFromRelations(CmpEntityBeanContext ctx, Object[] oldRelations) {
        boolean removed = false;
        for (int i = 0; i < cmrFields.length; ++i) {
            if (cmrFields[i].removeFromRelations(ctx, oldRelations))
                removed = true;
        }
        return removed;
    }

    public void cascadeDelete(CmpEntityBeanContext ctx, Map oldRelations) throws RemoveException, RemoteException {
        for (int i = 0; i < cmrFields.length; ++i) {
            JDBCCMRFieldBridge cmrField = cmrFields[i];
            Object value = oldRelations.get(cmrField);
            if (value != null)
                cmrField.cascadeDelete(ctx, (List) value);
        }
    }

    public String getEntityName() {
        return metadata.getName();
    }

    public String getAbstractSchemaName() {
        return metadata.getAbstractSchemaName();
    }

    public Class getRemoteInterface() {
        return metadata.getRemoteClass();
    }

    public Class getLocalInterface() {
        return metadata.getLocalClass();
    }

    public JDBCEntityMetaData getMetaData() {
        return metadata;
    }

    public JDBCEntityPersistenceStore getManager() {
        return manager;
    }

    /**
     * Returns the datasource for this entity.
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    public String getTableName() {
        return tableName;
    }

    public String getQualifiedTableName() {
        return qualifiedTableName;
    }

    public Class getPrimaryKeyClass() {
        return primaryKeyClass;
    }

    public int getListCacheMax() {
        return metadata.getListCacheMax();
    }

    public int getFetchSize() {
        return metadata.getFetchSize();
    }

    public Object createPrimaryKeyInstance() {
        if (primaryKeyFieldName == null) {
            try {
                return primaryKeyClass.newInstance();
            } catch (Exception e) {
                throw new EJBException("Error creating primary key instance: ", e);
            }
        }
        return null;
    }

    public JDBCFieldBridge[] getPrimaryKeyFields() {
        return primaryKeyFields;
    }

    /**
     * This method is called only at deployment time, not called at runtime.
     *
     * @return the list of all the fields.
     */
    public List<FieldBridge> getFields() {
        int fieldsTotal = primaryKeyFields.length + cmpFields.length + cmrFields.length;
        FieldBridge[] fields = new FieldBridge[fieldsTotal];
        int position = 0;
        // primary key fields
        System.arraycopy(primaryKeyFields, 0, fields, position, primaryKeyFields.length);
        position += primaryKeyFields.length;
        // cmp fields
        System.arraycopy(cmpFields, 0, fields, position, cmpFields.length);
        position += cmpFields.length;
        // cmr fields
        System.arraycopy(cmrFields, 0, fields, position, cmrFields.length);
        return Arrays.asList(fields);
    }

    public FieldBridge getFieldByName(String name) {
        FieldBridge field = null;
        for (int i = 0; i < primaryKeyFields.length; ++i) {
            JDBCCMPFieldBridge primaryKeyField = primaryKeyFields[i];
            if (primaryKeyField.getFieldName().equals(name)) {
                field = primaryKeyField;
                break;
            }
        }
        if (field == null) {
            field = getCMPFieldByName(name);
        }
        if (field == null) {
            field = getCMRFieldByName(name);
        }
        return field;
    }

    public boolean[] getEagerLoadMask() {
        return eagerLoadGroupMask;
    }

    public Iterator getLazyLoadGroupMasks() {
        return lazyLoadGroupMasks.iterator();
    }

    public boolean[] getLoadGroupMask(String name) {
        boolean[] mask = (boolean[]) loadGroupMasks.get(name);
        if (mask == null) {
            throw new IllegalStateException("Load group '" + name + "' is not defined. Defined load groups: " + loadGroupMasks.keySet());
        }
        return mask;
    }

    public FieldIterator getLoadIterator(JDBCCMPFieldBridge requiredField,
                                         JDBCReadAheadMetaData readahead,
                                         CmpEntityBeanContext ctx) {
        boolean[] loadGroup;
        if (requiredField == null) {
            if (readahead != null && !readahead.isNone()) {
                if (log.isTraceEnabled()) {
                    log.trace("Eager-load for entity: readahead=" + readahead);
                }
                loadGroup = getLoadGroupMask(readahead.getEagerLoadGroup());
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("Default eager-load for entity: readahead=" + readahead);
                }
                loadGroup = eagerLoadGroupMask;
            }
        } else {
            loadGroup = new boolean[tableFields.length];
            int requiredInd = requiredField.getTableIndex();
            loadGroup[requiredInd] = true;
            for (Iterator groups = lazyLoadGroupMasks.iterator(); groups.hasNext(); ) {
                boolean[] lazyGroup = (boolean[]) groups.next();
                if (lazyGroup[requiredInd]) {
                    for (int i = 0; i < loadGroup.length; ++i)
                        loadGroup[i] = loadGroup[i] || lazyGroup[i];
                }
            }
        }

        FieldIterator loadIter;
        if (loadGroup != null) {
            // filter
            int fieldsToLoad = 0;
            EntityState entityState = getEntityState(ctx);
            for (int i = 0; i < tableFields.length; ++i) {
                JDBCCMPFieldBridge field = tableFields[i];
                if (loadGroup[i] && !field.isPrimaryKeyMember() && !field.isLoaded(ctx)) {
                    entityState.setLoadRequired(i);
                    ++fieldsToLoad;
                }
            }
            loadIter = (fieldsToLoad > 0 ? entityState.getLoadIterator(ctx) : EMPTY_FIELD_ITERATOR);
        } else {
            loadIter = EMPTY_FIELD_ITERATOR;
        }
        return loadIter;
    }

    /**
     * @param name CMP field name
     * @return JDBCCMPFieldBridge instance or null if no field found.
     */
    public JDBCCMPFieldBridge getCMPFieldByName(String name) {
        for (int i = 0; i < primaryKeyFields.length; ++i) {
            JDBCCMPFieldBridge cmpField = primaryKeyFields[i];
            if (cmpField.getFieldName().equals(name))
                return cmpField;
        }
        for (int i = 0; i < cmpFields.length; ++i) {
            JDBCCMPFieldBridge cmpField = cmpFields[i];
            if (cmpField.getFieldName().equals(name))
                return cmpField;
        }
        return null;
    }

    public JDBCAbstractCMRFieldBridge[] getCMRFields() {
        return cmrFields;
    }

    public JDBCCMRFieldBridge getCMRFieldByName(String name) {
        for (int i = 0; i < cmrFields.length; ++i) {
            JDBCCMRFieldBridge cmrField = cmrFields[i];
            if (cmrField.getFieldName().equals(name))
                return cmrField;
        }
        return null;
    }

    public JDBCCMPFieldBridge getVersionField() {
        return versionField;
    }

    public JDBCCMPFieldBridge getCreatedPrincipalField() {
        return createdPrincipalField;
    }

    public JDBCCMPFieldBridge getCreatedTimeField() {
        return createdTimeField;
    }

    public JDBCCMPFieldBridge getUpdatedPrincipalField() {
        return updatedPrincipalField;
    }

    public JDBCCMPFieldBridge getUpdatedTimeField() {
        return updatedTimeField;
    }

    public Collection<JDBCSelectorBridge> getSelectors() {
        return selectorsByMethod.values();
    }

    public void initInstance(CmpEntityBeanContext ctx) {
        for (int i = 0; i < tableFields.length; ++i)
            tableFields[i].initInstance(ctx);
        //for(int i = 0; i < primaryKeyFields.length; ++i)
        //   primaryKeyFields[i].initInstance(ctx);
        //for(int i = 0; i < cmpFields.length; ++i)
        //   cmpFields[i].initInstance(ctx);
        for (int i = 0; i < cmrFields.length; ++i) {
            JDBCCMRFieldBridge cmrField = cmrFields[i];
            cmrField.initInstance(ctx);
        }
    }

    public static boolean isEjbCreateDone(CmpEntityBeanContext ctx) {
        return getEntityState(ctx).ejbCreateDone;
    }

    public static void setCreated(CmpEntityBeanContext ctx) {
        getEntityState(ctx).setCreated();
    }

    public static void setEjbCreateDone(CmpEntityBeanContext ctx) {
        getEntityState(ctx).ejbCreateDone = true;
    }

    /**
     * This method is used to determined whether the instance was modified.
     * NOTE, even if the method returns true the isStoreRequired for this same instance
     * might return false, e.g. a CMR field that doesn't have a foreign key was modified.
     *
     * @param ctx
     * @return
     */
    public boolean isModified(CmpEntityBeanContext ctx) {
        boolean invalidateCache = false;
        final EntityState entityState = getEntityState(ctx);
        if (entityState.isCreated()) {
            invalidateCache = areCmpFieldsDirty(ctx, entityState);
            if (!invalidateCache) {
                for (int i = 0; i < cmrFields.length; ++i) {
                    if (cmrFields[i].invalidateCache(ctx)) {
                        invalidateCache = true;
                        break;
                    }
                }
            }
        }
        return invalidateCache;
    }

    public boolean isStoreRequired(CmpEntityBeanContext ctx) {
        boolean modified = false;
        final EntityState entityState = getEntityState(ctx);
        if (entityState.isCreated()) {
            modified = areCmpFieldsDirty(ctx, entityState);
            if (!modified) {
                for (int i = 0; i < cmrFields.length; ++i) {
                    if (cmrFields[i].isDirty(ctx)) {
                        modified = true;
                        break;
                    }
                }
            }
        }
        return modified;
    }

    private boolean areCmpFieldsDirty(final CmpEntityBeanContext ctx,
                                      final EntityState entityState) {
        for (int i = 0; i < tableFields.length; ++i) {
            final JDBCCMPFieldBridge field = tableFields[i];
            if (entityState.isCheckDirty(i) && field.isDirty(ctx)) {
                return true;
            }
        }
        return false;
    }

    public FieldIterator getDirtyIterator(CmpEntityBeanContext ctx) {
        int dirtyFields = 0;
        final EntityState entityState = getEntityState(ctx);
        for (int i = 0; i < tableFields.length; ++i) {
            JDBCCMPFieldBridge field = tableFields[i];
            if (entityState.isCheckDirty(i) && field.isDirty(ctx)) {
                entityState.setUpdateRequired(i);
                ++dirtyFields;
            }
        }

        return dirtyFields > 0 ? getEntityState(ctx).getDirtyIterator(ctx) : EMPTY_FIELD_ITERATOR;
    }

    public boolean hasLockedFields(CmpEntityBeanContext ctx) {
        return getEntityState(ctx).hasLockedFields();
    }

    public FieldIterator getLockedIterator(CmpEntityBeanContext ctx) {
        return getEntityState(ctx).getLockedIterator(ctx);
    }

    public void initPersistenceContext(CmpEntityBeanContext ctx) {
        ctx.setPersistenceContext(new JDBCContext(jdbcContextSize, new EntityState()));
    }

    /**
     * This is only called in commit option B
     */
    public void resetPersistenceContext(CmpEntityBeanContext ctx) {
        for (int i = 0; i < primaryKeyFields.length; ++i)
            primaryKeyFields[i].resetPersistenceContext(ctx);
        for (int i = 0; i < cmpFields.length; ++i)
            cmpFields[i].resetPersistenceContext(ctx);
        for (int i = 0; i < cmrFields.length; ++i)
            cmrFields[i].resetPersistenceContext(ctx);
    }


    public static void destroyPersistenceContext(CmpEntityBeanContext ctx) {
        ctx.setPersistenceContext(null);
    }

    //
    // Commands to handle primary keys
    //

    public int setPrimaryKeyParameters(PreparedStatement ps, int parameterIndex, Object primaryKey) {
        for (int i = 0; i < primaryKeyFields.length; ++i)
            parameterIndex = primaryKeyFields[i].setPrimaryKeyParameters(ps, parameterIndex, primaryKey);
        return parameterIndex;
    }

    public int loadPrimaryKeyResults(ResultSet rs, int parameterIndex, Object[] pkRef) {
        pkRef[0] = createPrimaryKeyInstance();
        for (int i = 0; i < primaryKeyFields.length; ++i)
            parameterIndex = primaryKeyFields[i].loadPrimaryKeyResults(rs, parameterIndex, pkRef);
        return parameterIndex;
    }

    public Object extractPrimaryKeyFromInstance(CmpEntityBeanContext ctx) {
        try {
            Object pk = null;
            for (int i = 0; i < primaryKeyFields.length; ++i) {
                JDBCCMPFieldBridge pkField = primaryKeyFields[i];
                Object fieldValue = pkField.getInstanceValue(ctx);

                // updated pk object with return form set primary key value to
                // handle single valued non-composite pks and more complicated behivors.
                pk = pkField.setPrimaryKeyValue(pk, fieldValue);
            }
            return pk;
        } catch (EJBException e) {
            // to avoid double wrap of EJBExceptions
            throw e;
        } catch (Exception e) {
            // Non recoverable internal exception
            throw new EJBException("Internal error extracting primary key from " +
                    "instance", e);
        }
    }

    public void injectPrimaryKeyIntoInstance(CmpEntityBeanContext ctx, Object pk) {
        for (int i = 0; i < primaryKeyFields.length; ++i) {
            JDBCCMPFieldBridge pkField = primaryKeyFields[i];
            Object fieldValue = pkField.getPrimaryKeyValue(pk);
            pkField.setInstanceValue(ctx, fieldValue);
        }
    }

    int getNextJDBCContextIndex() {
        return jdbcContextSize++;
    }

    int addTableField(JDBCCMPFieldBridge field) {
        JDBCCMPFieldBridge[] tmpFields = tableFields;
        if (tableFields == null) {
            tableFields = new JDBCCMPFieldBridge[1];
        } else {
            tableFields = new JDBCCMPFieldBridge[tableFields.length + 1];
            System.arraycopy(tmpFields, 0, tableFields, 0, tmpFields.length);
        }
        int index = tableFields.length - 1;
        tableFields[index] = field;

        return index;
    }

    public JDBCFieldBridge[] getTableFields() {
        return tableFields;
    }

    /**
     * Marks the context as removed.
     *
     * @param ctx instance's context
     */
    public void setRemoved(CmpEntityBeanContext ctx) {
        getEntityState(ctx).setRemoved();
    }

    /**
     * @param ctx instance's context.
     * @return true if instance was removed.
     */
    public boolean isRemoved(CmpEntityBeanContext ctx) {
        return getEntityState(ctx).isRemoved();
    }

    /**
     * Marks an instance as being removed
     */
    public void setIsBeingRemoved(CmpEntityBeanContext ctx) {
        getEntityState(ctx).setIsBeingRemoved();
    }

    /**
     * @param ctx instance's context.
     * @return true if instance is being removed.
     */
    public boolean isBeingRemoved(CmpEntityBeanContext ctx) {
        return getEntityState(ctx).isBeingRemoved();
    }

    /**
     * Marks the instance as scheduled for cascade delete (not for batch cascade delete)
     *
     * @param ctx instance's context.
     */
    public void scheduleForCascadeDelete(CmpEntityBeanContext ctx) {
        getEntityState(ctx).scheduleForCascadeDelete();
        if (log.isTraceEnabled())
            log.trace("Scheduled for cascade-delete: " + ctx.getPrimaryKey());
    }

    /**
     * @param ctx instance's context.
     * @return true if instance was scheduled for cascade delete (not for batch cascade delete)
     */
    public boolean isScheduledForCascadeDelete(CmpEntityBeanContext ctx) {
        return getEntityState(ctx).isScheduledForCascadeDelete();
    }

    /**
     * Marks the instance as scheduled for batch cascade delete (not for cascade delete)
     *
     * @param ctx instance's context.
     */
    public void scheduleForBatchCascadeDelete(CmpEntityBeanContext ctx) {
        getEntityState(ctx).scheduleForBatchCascadeDelete();
        if (log.isTraceEnabled())
            log.trace("Scheduled for batch-cascade-delete: " + ctx.getPrimaryKey());
    }

    /**
     * @param ctx instance's context.
     * @return true if instance was scheduled for batch cascade delete (not for cascade delete)
     */
    public boolean isScheduledForBatchCascadeDelete(CmpEntityBeanContext ctx) {
        return getEntityState(ctx).isScheduledForBatchCascadeDelete();
    }

    private static EntityState getEntityState(CmpEntityBeanContext ctx) {
        JDBCContext jdbcCtx = (JDBCContext) ctx.getPersistenceContext();
        EntityState entityState = jdbcCtx.getEntityState();
        if (entityState == null)
            throw new IllegalStateException("Entity state is null.");
        return entityState;
    }

    private void loadCMPFields(JDBCEntityMetaData metadata) {
        // only non pk fields are stored here at first and then later
        // the pk fields are added to the front (makes sql easier to read)
        List<JDBCCMPFieldMetaData> cmpFieldsMD = metadata.getCMPFields();
        List<JDBCCMPFieldBridge> cmpFieldsList = new ArrayList<JDBCCMPFieldBridge>();
        // primary key cmp fields
        List<JDBCCMPFieldBridge> pkFieldsList = new ArrayList<JDBCCMPFieldBridge>();

        // create pk fields
        for (JDBCCMPFieldMetaData fieldMD : cmpFieldsMD) {
            if (fieldMD.isPrimaryKeyMember()) {
                pkFieldsList.add(createCMPField(metadata, fieldMD));

            }
        }
        for (JDBCCMPFieldMetaData fieldMD : cmpFieldsMD) {
            if (!fieldMD.isPrimaryKeyMember()) {
                cmpFieldsList.add(createCMPField(metadata, fieldMD));

            }
        }

        // save the pk fields in the pk field array
        primaryKeyFields = pkFieldsList.toArray(new JDBCCMPFieldBridge[pkFieldsList.size()]);
        // add the pk fields to the front of the cmp list, per guarantee above
        cmpFields = cmpFieldsList.toArray(new JDBCCMPFieldBridge[cmpFieldsList.size()]);
    }

    private void loadCMRFields(JDBCEntityMetaData metadata) {
        cmrFields = new JDBCCMRFieldBridge[metadata.getRelationshipRoles().size()];
        // create each field
        int cmrFieldIndex = 0;
        for (Iterator iter = metadata.getRelationshipRoles().iterator(); iter.hasNext(); ) {
            JDBCRelationshipRoleMetaData relationshipRole = (JDBCRelationshipRoleMetaData) iter.next();
            JDBCCMRFieldBridge cmrField = new JDBCCMRFieldBridge(this, manager, relationshipRole);
            cmrFields[cmrFieldIndex++] = cmrField;
        }
    }

    private void loadLoadGroups(JDBCEntityMetaData metadata) {
        loadGroupMasks = new HashMap();

        // load optimistic locking mask and add it to all the load group masks
        JDBCOptimisticLockingMetaData olMD = metadata.getOptimisticLocking();
        if (olMD != null) {
            if (versionField != null) {
                defaultLockGroupMask = new boolean[tableFields.length];
                defaultLockGroupMask[versionField.getTableIndex()] = true;
                versionField.setLockingStrategy(LockingStrategy.VERSION);
            } else if (olMD.getGroupName() != null) {
                defaultLockGroupMask = loadGroupMask(olMD.getGroupName(), null);
                for (int i = 0; i < tableFields.length; ++i) {
                    if (defaultLockGroupMask[i]) {
                        JDBCCMPFieldBridge tableField = tableFields[i];
                        tableField.setLockingStrategy(LockingStrategy.GROUP);
                        tableField.addDefaultFlag(ADD_TO_WHERE_ON_UPDATE);
                    }
                }
            } else {// read or modified strategy
                LockingStrategy strategy =
                        (olMD.getLockingStrategy() == JDBCOptimisticLockingMetaData.LockingStrategy.READ_STRATEGY ?
                                LockingStrategy.READ : LockingStrategy.MODIFIED
                        );
                for (int i = 0; i < tableFields.length; ++i) {
                    JDBCCMPFieldBridge field = tableFields[i];
                    if (!field.isPrimaryKeyMember())
                        field.setLockingStrategy(strategy);
                }
            }
        }

        // add the * load group
        boolean[] defaultLoadGroup = new boolean[tableFields.length];
        Arrays.fill(defaultLoadGroup, true);
        for (int i = 0; i < primaryKeyFields.length; ++i) {
            int tableIndex = primaryKeyFields[i].getTableIndex();
            defaultLoadGroup[tableIndex] = false;
        }
        loadGroupMasks.put(DEFAULT_LOADGROUP_NAME, defaultLoadGroup);

        // put each group in the load groups map by group name
        Iterator groupNames = metadata.getLoadGroups().keySet().iterator();
        while (groupNames.hasNext()) {
            // get the group name
            String groupName = (String) groupNames.next();
            boolean[] loadGroup = loadGroupMask(groupName, defaultLockGroupMask);
            loadGroupMasks.put(groupName, loadGroup);
        }
        loadGroupMasks = Collections.unmodifiableMap(loadGroupMasks);
    }

    private boolean[] loadGroupMask(String groupName, boolean[] defaultGroup) {
        List<String> fieldNames = metadata.getLoadGroup(groupName);
        boolean[] group = new boolean[tableFields.length];
        if (defaultGroup != null)
            System.arraycopy(defaultGroup, 0, group, 0, group.length);
        for (String fieldName : fieldNames) {
            JDBCFieldBridge field = (JDBCFieldBridge) getFieldByName(fieldName);
            if (field == null)
                throw new RuntimeException(
                        "Field " + fieldName + " not found for entity " + getEntityName());

            if (field instanceof JDBCCMRFieldBridge) {
                JDBCCMRFieldBridge cmrField = (JDBCCMRFieldBridge) field;
                if (cmrField.hasForeignKey()) {
                    JDBCCMPFieldBridge[] fkFields = (JDBCCMPFieldBridge[]) cmrField.getForeignKeyFields();
                    for (int i = 0; i < fkFields.length; ++i) {
                        group[fkFields[i].getTableIndex()] = true;
                    }
                } else {
                    throw new RuntimeException("Only CMR fields that have " +
                            "a foreign-key may be a member of a load group: " +
                            "fieldName=" + fieldName);
                }
            } else {
                group[((JDBCCMPFieldBridge) field).getTableIndex()] = true;
            }
        }
        return group;
    }

    private void loadEagerLoadGroup(JDBCEntityMetaData metadata) {
        String eagerLoadGroupName = metadata.getEagerLoadGroup();
        if (eagerLoadGroupName == null) {
            // can be null in case of <eager-load-group/>, meaning empty load group
            eagerLoadGroupMask = defaultLockGroupMask;
        } else
            eagerLoadGroupMask = (boolean[]) loadGroupMasks.get(eagerLoadGroupName);
    }

    private void loadLazyLoadGroups(JDBCEntityMetaData metadata) {
        List lazyGroupNames = metadata.getLazyLoadGroups();
        lazyLoadGroupMasks = new ArrayList(lazyGroupNames.size());
        for (Iterator lazyLoadGroupNames = lazyGroupNames.iterator(); lazyLoadGroupNames.hasNext(); ) {
            String lazyLoadGroupName = (String) lazyLoadGroupNames.next();
            lazyLoadGroupMasks.add(loadGroupMasks.get(lazyLoadGroupName));
        }
        lazyLoadGroupMasks = Collections.unmodifiableList(lazyLoadGroupMasks);
    }

    private JDBCCMPFieldBridge createCMPField(JDBCEntityMetaData metadata,
                                              JDBCCMPFieldMetaData cmpFieldMetaData) {
        JDBCCMPFieldBridge cmpField;
        if (metadata.isCMP1x())
            cmpField = new JDBCCMP1xFieldBridge(manager, cmpFieldMetaData);
        else
            cmpField = new JDBCCMP2xFieldBridge(manager, cmpFieldMetaData);
        return cmpField;
    }

    private void loadSelectors(JDBCEntityMetaData metadata) {
        // Don't know if this is the best way to do this.  Another way would be
        // to delegate selectors to the JDBCFindEntitiesCommand, but this is
        // easier now.
        selectorsByMethod = new HashMap(metadata.getQueries().size());
        Iterator definedFinders = manager.getMetaData().getQueries().iterator();
        while (definedFinders.hasNext()) {
            JDBCQueryMetaData q = (JDBCQueryMetaData) definedFinders.next();
            if (q.getMethod().getName().startsWith("ejbSelect"))
                selectorsByMethod.put(q.getMethod(), new JDBCSelectorBridge(manager, q));
        }
        selectorsByMethod = Collections.unmodifiableMap(selectorsByMethod);
    }

    private void addCMPField(JDBCCMPFieldBridge field) {
        JDBCCMPFieldBridge[] tmpCMPFields = cmpFields;
        cmpFields = new JDBCCMPFieldBridge[cmpFields.length + 1];
        System.arraycopy(tmpCMPFields, 0, cmpFields, 0, tmpCMPFields.length);
        cmpFields[tmpCMPFields.length] = field;
    }

    public class EntityState {
        private static final byte REMOVED = 1;
        private static final byte SCHEDULED_FOR_CASCADE_DELETE = 2;
        private static final byte SCHEDULED_FOR_BATCH_CASCADE_DELETE = 4;
        private static final byte IS_BEING_REMOVED = 8;

        /**
         * indicates whether ejbCreate method was executed
         */
        private boolean ejbCreateDone = false;
        /**
         * indicates whether ejbPostCreate method was executed
         */
        private boolean ejbPostCreateDone = false;

        private byte entityFlags;

        /**
         * array of field flags
         */
        private final byte[] fieldFlags = new byte[tableFields.length];

        public EntityState() {
            for (int i = 0; i < tableFields.length; ++i) {
                fieldFlags[i] = tableFields[i].getDefaultFlags();
            }
        }

        public void setRemoved() {
            entityFlags |= REMOVED;
            entityFlags &= ~(SCHEDULED_FOR_CASCADE_DELETE | SCHEDULED_FOR_BATCH_CASCADE_DELETE | IS_BEING_REMOVED);
        }

        public boolean isRemoved() {
            return (entityFlags & REMOVED) > 0;
        }

        public void setIsBeingRemoved() {
            entityFlags |= IS_BEING_REMOVED;
        }

        public boolean isBeingRemoved() {
            return (entityFlags & IS_BEING_REMOVED) > 0;
        }

        public void scheduleForCascadeDelete() {
            entityFlags |= SCHEDULED_FOR_CASCADE_DELETE;
        }

        public boolean isScheduledForCascadeDelete() {
            return (entityFlags & SCHEDULED_FOR_CASCADE_DELETE) > 0;
        }

        public void scheduleForBatchCascadeDelete() {
            entityFlags |= SCHEDULED_FOR_BATCH_CASCADE_DELETE | SCHEDULED_FOR_CASCADE_DELETE;
        }

        public boolean isScheduledForBatchCascadeDelete() {
            return (entityFlags & SCHEDULED_FOR_BATCH_CASCADE_DELETE) > 0;
        }

        public void setCreated() {
            ejbCreateDone = true;
            ejbPostCreateDone = true;
        }

        public boolean isCreated() {
            return ejbCreateDone && ejbPostCreateDone;
        }

        /**
         * @param fieldIndex index of the field
         * @return true if the field is loaded
         */
        public boolean isLoaded(int fieldIndex) {
            return (fieldFlags[fieldIndex] & LOADED) > 0;
        }

        /**
         * Marks the field as loaded.
         *
         * @param fieldIndex index of the field.
         */
        public void setLoaded(int fieldIndex) {
            fieldFlags[fieldIndex] |= LOADED;
        }

        /**
         * Marks the field to be loaded.
         *
         * @param fieldIndex index of the field.
         */
        public void setLoadRequired(int fieldIndex) {
            fieldFlags[fieldIndex] |= LOAD_REQUIRED;
        }

        /**
         * Marks the field to be updated.
         *
         * @param fieldIndex index of the field.
         */
        public void setUpdateRequired(int fieldIndex) {
            fieldFlags[fieldIndex] |= DIRTY;
        }

        /**
         * The field will be checked for dirty state at commit.
         *
         * @param fieldIndex index of the field.
         */
        public void setCheckDirty(int fieldIndex) {
            fieldFlags[fieldIndex] |= CHECK_DIRTY;
        }

        /**
         * @param fieldIndex the index of the field that should be checked for dirty state.
         * @return true if the field should be checked for dirty state.
         */
        public boolean isCheckDirty(int fieldIndex) {
            return (fieldFlags[fieldIndex] & CHECK_DIRTY) > 0;
        }

        /**
         * Marks the field as clean.
         *
         * @param fieldIndex nextIndex of the field.
         */
        public void setClean(int fieldIndex) {
            fieldFlags[fieldIndex] &= ~(CHECK_DIRTY | DIRTY | LOCKED);
        }

        /**
         * Resets field flags.
         *
         * @param fieldIndex nextIndex of the field.
         */
        public void resetFlags(int fieldIndex) {
            fieldFlags[fieldIndex] = tableFields[fieldIndex].getDefaultFlags();
        }

        public FieldIterator getDirtyIterator(CmpEntityBeanContext ctx) {
            return new MaskFieldIterator((byte) (DIRTY | ADD_TO_SET_ON_UPDATE));
        }

        public boolean hasLockedFields() {
            boolean result = false;
            for (int i = 0; i < fieldFlags.length; ++i) {
                if ((fieldFlags[i] & (LOCKED | ADD_TO_WHERE_ON_UPDATE)) > 0) {
                    result = true;
                    break;
                }
            }
            return result;
        }

        public FieldIterator getLockedIterator(CmpEntityBeanContext ctx) {
            return new MaskFieldIterator((byte) (LOCKED | ADD_TO_WHERE_ON_UPDATE));
        }

        public boolean lockValue(int fieldIndex) {
            boolean lock = false;
            byte fieldFlag = fieldFlags[fieldIndex];
            if ((fieldFlag & LOADED) > 0 && (fieldFlag & LOCKED) == 0) {
                fieldFlags[fieldIndex] |= LOCKED;
                lock = true;
            }
            return lock;
        }

        public FieldIterator getLoadIterator(CmpEntityBeanContext ctx) {
            return new MaskFieldIterator(LOAD_REQUIRED);
        }

        // Inner

        private class MaskFieldIterator implements FieldIterator {
            private final byte flagMask;
            private int nextIndex = 0;
            private int curIndex = -1;

            public MaskFieldIterator(byte flagMask) {
                this.flagMask = flagMask;
            }

            public boolean hasNext() {
                while (nextIndex < fieldFlags.length) {
                    if ((fieldFlags[nextIndex] & flagMask) > 0) {
                        return true;
                    }

                    ++nextIndex;
                }

                return false;
            }

            public JDBCCMPFieldBridge next() {
                if (!hasNext())
                    throw new NoSuchElementException();
                curIndex = nextIndex;
                return tableFields[nextIndex++];
            }

            public void remove() {
                fieldFlags[curIndex] &= ~flagMask;
            }

            public void removeAll() {
                int inversedMask = ~flagMask;
                for (int i = 0; i < fieldFlags.length; ++i)
                    fieldFlags[i] &= inversedMask;
            }

            public void reset() {
                nextIndex = 0;
                curIndex = -1;
            }
        }
    }

    public static final FieldIterator EMPTY_FIELD_ITERATOR = new FieldIterator() {
        public boolean hasNext() {
            return false;
        }

        public JDBCCMPFieldBridge next() {
            throw new NoSuchElementException();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public void removeAll() {
            throw new UnsupportedOperationException();
        }

        public void reset() {
        }
    };

    public static interface FieldIterator {
        /**
         * @return true if there are more fields to iterate through.
         */
        boolean hasNext();

        /**
         * @return the next field.
         */
        JDBCCMPFieldBridge next();

        /**
         * Removes the current field from the iterator (not from the underlying array or another source)
         */
        void remove();

        /**
         * Removes all the fields from the iterator (not from the underlying array or another source).
         */
        void removeAll();

        /**
         * Resets the current position to the first field.
         */
        void reset();
    }
}
