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

import java.lang.reflect.Field;
import javax.ejb.EJBException;
import org.jboss.as.cmp.jdbc.CMPFieldStateFactory;
import org.jboss.as.cmp.jdbc.JDBCContext;
import org.jboss.as.cmp.jdbc.JDBCStoreManager;
import org.jboss.as.cmp.jdbc.JDBCType;
import org.jboss.as.cmp.jdbc.metadata.JDBCCMPFieldMetaData;

import org.jboss.as.cmp.context.CmpEntityBeanContext;

/**
 * JDBCCMP2xFieldBridge is a concrete implementation of JDBCCMPFieldBridge for
 * CMP version 2.x. Instance data is stored in the entity persistence context.
 * Whenever a field is changed it is compared to the current value and sets
 * a dirty flag if the value has changed.
 * <p/>
 * Life-cycle:
 * Tied to the EntityBridge.
 * <p/>
 * Multiplicity:
 * One for each entity bean cmp field.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="mailto:alex@jboss.org">Alex Loubyansky</a>
 * @version $Revision: 81030 $
 */
public class JDBCCMP2xFieldBridge extends JDBCAbstractCMPFieldBridge {
    /**
     * column name (used only at deployment time to check whether fields mapped to the same column)
     */
    private final String columnName;

    /**
     * CMP field this foreign key field is mapped to
     */
    private final JDBCCMP2xFieldBridge cmpFieldIAmMappedTo;

    /**
     * this is used for foreign key fields mapped to CMP fields (check ChainLink)
     */
    private ChainLink cmrChainLink;

    // Constructors

    public JDBCCMP2xFieldBridge(JDBCStoreManager manager,
                                JDBCCMPFieldMetaData metadata) {
        super(manager, metadata);
        cmpFieldIAmMappedTo = null;
        columnName = metadata.getColumnName();
    }

    public JDBCCMP2xFieldBridge(JDBCStoreManager manager,
                                JDBCCMPFieldMetaData metadata,
                                CMPFieldStateFactory stateFactory,
                                boolean checkDirtyAfterGet) {
        this(manager, metadata);
        this.stateFactory = stateFactory;
        this.checkDirtyAfterGet = checkDirtyAfterGet;
    }

    public JDBCCMP2xFieldBridge(JDBCCMP2xFieldBridge cmpField,
                                CMPFieldStateFactory stateFactory,
                                boolean checkDirtyAfterGet) {
        this(
                (JDBCStoreManager) cmpField.getManager(),
                cmpField.getFieldName(),
                cmpField.getFieldType(),
                cmpField.getJDBCType(),
                cmpField.isReadOnly(), // should always be false?
                cmpField.getReadTimeOut(),
                cmpField.getPrimaryKeyClass(),
                cmpField.getPrimaryKeyField(),
                cmpField,
                null, // it should not be a foreign key
                cmpField.getColumnName()
        );
        this.stateFactory = stateFactory;
        this.checkDirtyAfterGet = checkDirtyAfterGet;
    }

    /**
     * This constructor creates a foreign key field.
     */
    public JDBCCMP2xFieldBridge(JDBCStoreManager manager,
                                JDBCCMPFieldMetaData metadata,
                                JDBCType jdbcType) {
        super(manager, metadata, jdbcType);
        cmpFieldIAmMappedTo = null;
        columnName = metadata.getColumnName();
    }

    /**
     * This constructor is used to create a foreign key field instance that is
     * a part of primary key field. See JDBCCMRFieldBridge.
     */
    public JDBCCMP2xFieldBridge(JDBCStoreManager manager,
                                String fieldName,
                                Class fieldType,
                                JDBCType jdbcType,
                                boolean readOnly,
                                long readTimeOut,
                                Class primaryKeyClass,
                                Field primaryKeyField,
                                JDBCCMP2xFieldBridge cmpFieldIAmMappedTo,
                                JDBCCMRFieldBridge myCMRField,
                                String columnName) {
        super(
                manager,
                fieldName,
                fieldType,
                jdbcType,
                readOnly,
                readTimeOut,
                primaryKeyClass,
                primaryKeyField,
                cmpFieldIAmMappedTo.getFieldIndex(),
                cmpFieldIAmMappedTo.getTableIndex(),
                cmpFieldIAmMappedTo.checkDirtyAfterGet,
                cmpFieldIAmMappedTo.stateFactory
        );
        this.cmpFieldIAmMappedTo = cmpFieldIAmMappedTo;
        if (myCMRField != null) {
            cmrChainLink = new CMRChainLink(myCMRField);
            cmpFieldIAmMappedTo.addCMRChainLink(cmrChainLink);
        }
        this.columnName = columnName;
    }

    // Public

    public JDBCCMP2xFieldBridge getCmpFieldIAmMappedTo() {
        return cmpFieldIAmMappedTo;
    }

    public ChainLink getCmrChainLink() {
        return cmrChainLink;
    }

    public boolean isFKFieldMappedToCMPField() {
        return cmpFieldIAmMappedTo != null && this.cmrChainLink != null;
    }

    public String getColumnName() {
        return columnName;
    }

    // JDBCFieldBridge implementation

    public Object getInstanceValue(CmpEntityBeanContext ctx) {
        FieldState fieldState = getLoadedState(ctx);
        return fieldState.getValue();
    }

    public void setInstanceValue(CmpEntityBeanContext ctx, Object value) {
        FieldState fieldState = getFieldState(ctx);
        // update current value
        if (cmpFieldIAmMappedTo != null && cmpFieldIAmMappedTo.isPrimaryKeyMember()) {
            // if this field shares the column with the primary key field and new value
            // changes the primary key then we are in an illegal state.
            if (value != null) {
                if (fieldState.isLoaded() && fieldState.isValueChanged(value)) {
                    throw new IllegalStateException(
                            "New value [" + value + "] of a foreign key field "
                                    + getFieldName()
                                    + " changed the value of a primary key field "
                                    + cmpFieldIAmMappedTo.getFieldName()
                                    + "[" + fieldState.value + "]"
                    );
                } else {
                    fieldState.setValue(value);
                }
            }
        } else {
            if (cmrChainLink != null
                    && JDBCEntityBridge.isEjbCreateDone(ctx)
                    && fieldState.isLoaded()
                    && fieldState.isValueChanged(value)) {
                cmrChainLink.execute(ctx, fieldState, value);
            }

            fieldState.setValue(value);
        }

        // we are loading the field right now so it isLoaded
        fieldState.setLoaded();
    }

    public void lockInstanceValue(CmpEntityBeanContext ctx) {
        getFieldState(ctx).lockValue();
    }

    public boolean isLoaded(CmpEntityBeanContext ctx) {
        return getFieldState(ctx).isLoaded();
    }

    /**
     * Has the value of this field changes since the last time clean was called.
     */
    public boolean isDirty(CmpEntityBeanContext ctx) {
        return !primaryKeyMember
                && !readOnly
                && getFieldState(ctx).isDirty();
    }

    /**
     * Mark this field as clean. Saves the current state in context, so it
     * can be compared when isDirty is called.
     */
    public void setClean(CmpEntityBeanContext ctx) {
        FieldState fieldState = getFieldState(ctx);
        fieldState.setClean();

        // update last read time
        if (readOnly && readTimeOut != -1)
            fieldState.lastRead = System.currentTimeMillis();
    }

    public void resetPersistenceContext(CmpEntityBeanContext ctx) {
        if (isReadTimedOut(ctx)) {
            JDBCContext jdbcCtx = (JDBCContext)ctx.getPersistenceContext();
            FieldState fieldState = (FieldState) jdbcCtx.getFieldState(jdbcContextIndex);
            if (fieldState != null)
                fieldState.reset();
        }
    }

    public boolean isReadTimedOut(CmpEntityBeanContext ctx) {
        // if we are read/write then we are always timed out
        if (!readOnly)
            return true;

        // if read-time-out is -1 then we never time out.
        if (readTimeOut == -1)
            return false;

        long readInterval = System.currentTimeMillis() - getFieldState(ctx).lastRead;
        return readInterval >= readTimeOut;
    }

    public Object getLockedValue(CmpEntityBeanContext ctx) {
        return getLoadedState(ctx).getLockedValue();
    }

    public void updateState(CmpEntityBeanContext ctx, Object value) {
        getFieldState(ctx).updateState(value);
    }

    protected void setDirtyAfterGet(CmpEntityBeanContext ctx) {
        getFieldState(ctx).setCheckDirty();
    }

    // Private

    private FieldState getLoadedState(CmpEntityBeanContext ctx) {
        FieldState fieldState = getFieldState(ctx);
        if (!fieldState.isLoaded()) {
            manager.loadField(this, ctx);
            if (!fieldState.isLoaded())
                throw new EJBException("Could not load field value: " + getFieldName());
        }
        return fieldState;
    }

    private void addCMRChainLink(ChainLink nextCMRChainLink) {
        if (cmrChainLink == null) {
            cmrChainLink = new DummyChainLink();
        }
        cmrChainLink.setNextLink(nextCMRChainLink);
    }

    private FieldState getFieldState(CmpEntityBeanContext ctx) {
        JDBCContext jdbcCtx = (JDBCContext)ctx.getPersistenceContext();
        FieldState fieldState = (FieldState) jdbcCtx.getFieldState(jdbcContextIndex);
        if (fieldState == null) {
            fieldState = new FieldState(jdbcCtx);
            jdbcCtx.setFieldState(jdbcContextIndex, fieldState);
        }
        return fieldState;
    }

    // Inner

    private class FieldState {
        /**
         * entity's state this field state belongs to
         */
        private JDBCEntityBridge.EntityState entityState;
        /**
         * current field value
         */
        private Object value;
        /**
         * previous field state. NOTE: it might not be the same as previous field value
         */
        private Object state;
        /**
         * locked field value
         */
        private Object lockedValue;
        /**
         * last time the field was read
         */
        private long lastRead = -1;

        public FieldState(JDBCContext jdbcCtx) {
            this.entityState = jdbcCtx.getEntityState();
        }

        /**
         * Reads current field value.
         *
         * @return current field value.
         */
        public Object getValue() {
            //if(checkDirtyAfterGet)
            //   setCheckDirty();
            return value;
        }

        /**
         * Sets new field value and sets the flag that setter was called on the field
         *
         * @param newValue new field value.
         */
        public void setValue(Object newValue) {
            this.value = newValue;
            setCheckDirty();
        }

        private void setCheckDirty() {
            entityState.setCheckDirty(tableIndex);
        }

        /**
         * @return true if the field is loaded.
         */
        public boolean isLoaded() {
            return entityState.isLoaded(tableIndex);
        }

        /**
         * Marks the field as loaded.
         */
        public void setLoaded() {
            entityState.setLoaded(tableIndex);
        }

        /**
         * @return true if the field is dirty.
         */
        public boolean isDirty() {
            return isLoaded() && !stateFactory.isStateValid(state, value);
        }

        /**
         * Compares current value to a new value. Note, it does not compare
         * field states, just values.
         *
         * @param newValue new field value
         * @return true if field values are not equal.
         */
        public boolean isValueChanged(Object newValue) {
            return value == null ? newValue != null : !value.equals(newValue);
        }

        /**
         * Resets masks and updates the state.
         */
        public void setClean() {
            entityState.setClean(tableIndex);
            updateState(value);
        }

        /**
         * Updates the state to some specific value that might be different from the current
         * field's value. This trick is needed for foreign key fields because they can be
         * changed while not being loaded. When the owning CMR field is loaded this method is
         * called with the loaded from the database value. Thus, we have correct state and locked value.
         *
         * @param value the value loaded from the database.
         */
        private void updateState(Object value) {
            state = stateFactory.getFieldState(value);
            lockedValue = value;
        }

        /**
         * Resets everything.
         */
        public void reset() {
            value = null;
            state = null;
            lastRead = -1;
            entityState.resetFlags(tableIndex);
        }

        public void lockValue() {
            if (entityState.lockValue(tableIndex)) {
                //log.debug("locking> " + fieldName + "=" + value);
                lockedValue = value;
            }
        }

        public Object getLockedValue() {
            return lockedValue;
        }
    }

    /**
     * Represents a link in the chain. The execute method will doExecute each link
     * in the chain except for the link (originator) execute() was called on.
     */
    private abstract static class ChainLink {
        private ChainLink nextLink;

        public ChainLink() {
            nextLink = this;
        }

        public void setNextLink(ChainLink nextLink) {
            nextLink.nextLink = this.nextLink;
            this.nextLink = nextLink;
        }

        public ChainLink getNextLink() {
            return nextLink;
        }

        public void execute(CmpEntityBeanContext ctx,
                            FieldState fieldState,
                            Object newValue) {
            nextLink.doExecute(this, ctx, fieldState, newValue);
        }

        protected abstract void doExecute(ChainLink originator,
                                          CmpEntityBeanContext ctx,
                                          FieldState fieldState,
                                          Object newValue);
    }

    /**
     * This chain link contains a CMR field a foreign key of which is mapped to a CMP field.
     */
    private static class CMRChainLink
            extends ChainLink {
        private final JDBCCMRFieldBridge cmrField;

        public CMRChainLink(JDBCCMRFieldBridge cmrField) {
            this.cmrField = cmrField;
        }

        /**
         * Going down the chain current related id is calculated and stored in oldRelatedId.
         * When the next link is originator, the flow is going backward:
         * - field state is updated with new value;
         * - new related id is calculated;
         * - old relationship is destroyed (if there is one);
         * - new relationship is established (if it is valid).
         *
         * @param originator ChainLink that started execution.
         * @param ctx        EnterpriseEntityContext of the entity.
         * @param fieldState field's state.
         * @param newValue   new field value.
         */
        public void doExecute(ChainLink originator,
                              CmpEntityBeanContext ctx,
                              FieldState fieldState,
                              Object newValue) {
            // get old related id
            Object oldRelatedId = cmrField.getRelatedIdFromContext(ctx);

            // invoke down the cmrChain
            if (originator != getNextLink()) {
                getNextLink().doExecute(originator, ctx, fieldState, newValue);
            }

            // update field state
            fieldState.setValue(newValue);

            // get new related id
            Object newRelatedId = cmrField.getRelatedIdFromContext(ctx);

            // destroy old relationship
            if (oldRelatedId != null)
                destroyRelations(oldRelatedId, ctx);

            // establish new relationship
            if (newRelatedId != null)
                createRelations(newRelatedId, ctx);
        }

        private void createRelations(Object newRelatedId, CmpEntityBeanContext ctx) {
            try {
                if (cmrField.isForeignKeyValid(newRelatedId)) {
                    cmrField.createRelationLinks(ctx, newRelatedId, false);
                } else {
                    // set foreign key to a new value
                    cmrField.setForeignKey(ctx, newRelatedId);
                    // put calculated relatedId to the waiting list
                    if (ctx.getPrimaryKey() != null) {
                        JDBCCMRFieldBridge relatedCMRField = (JDBCCMRFieldBridge) cmrField.getRelatedCMRField();
                        relatedCMRField.addRelatedPKWaitingForMyPK(newRelatedId, ctx.getPrimaryKey());
                    }
                }
            } catch (Exception e) {
                // no such object
            }
        }

        private void destroyRelations(Object oldRelatedId, CmpEntityBeanContext ctx) {
            JDBCCMRFieldBridge relatedCMRField = (JDBCCMRFieldBridge) cmrField.getRelatedCMRField();
            relatedCMRField.removeRelatedPKWaitingForMyPK(oldRelatedId, ctx.getPrimaryKey());
            try {
                if (cmrField.isForeignKeyValid(oldRelatedId)) {
                    cmrField.destroyRelationLinks(ctx, oldRelatedId, true, false);
                }
            } catch (Exception e) {
                // no such object
            }
        }
    }

    private static class DummyChainLink
            extends ChainLink {
        public void doExecute(ChainLink originator,
                              CmpEntityBeanContext ctx,
                              FieldState fieldState,
                              Object newValue) {
            // invoke down the cmrChain
            if (originator != getNextLink()) {
                getNextLink().doExecute(originator, ctx, fieldState, newValue);
            }
            // update field state
            fieldState.setValue(newValue);
        }
    }
}
