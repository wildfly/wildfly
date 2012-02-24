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

import org.jboss.as.cmp.CmpMessages;
import static org.jboss.as.cmp.CmpMessages.MESSAGES;
import org.jboss.as.cmp.context.CmpEntityBeanContext;
import org.jboss.as.cmp.jdbc.JDBCContext;
import org.jboss.as.cmp.jdbc.JDBCStoreManager;
import org.jboss.as.cmp.jdbc.metadata.JDBCCMPFieldMetaData;


/**
 * JDBCCMP1xFieldBridge is a concrete implementation of JDBCCMPFieldBridge for
 * CMP version 1.x. Getting and setting of instance fields set the
 * corresponding field in bean instance.  Dirty checking is performed by
 * storing the current value in the entity persistence context when ever
 * setClean is called, and comparing current value to the original value.
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
public class JDBCCMP1xFieldBridge extends JDBCAbstractCMPFieldBridge {
    private Field field;

    public JDBCCMP1xFieldBridge(JDBCStoreManager manager,
                                JDBCCMPFieldMetaData metadata) {
        super(manager, metadata);

        try {
            field = manager.getMetaData().getEntityClass().getField(getFieldName());
        } catch (NoSuchFieldException e) {
            // Non recoverable internal exception
            throw CmpMessages.MESSAGES.fieldNotFound(getFieldName(), manager.getMetaData().getName());
        }
    }

    public Object getInstanceValue(CmpEntityBeanContext ctx) {
        FieldState fieldState = getFieldState(ctx);
        if (!fieldState.isLoaded()) {
            throw MESSAGES.cmpFieldNotLoaded(fieldName);
        }

        try {
            return field.get(ctx.getInstance());
        } catch (Exception e) {
            // Non recoverable internal exception
            throw CmpMessages.MESSAGES.errorGettingInstanceField(getFieldName(), e);
        }
    }

    public void setInstanceValue(CmpEntityBeanContext ctx, Object value) {
        try {
            field.set(ctx.getInstance(), value);
            FieldState fieldState = getFieldState(ctx);
            fieldState.setLoaded();
            fieldState.setCheckDirty();
        } catch (Exception e) {
            // Non recoverable internal exception
            throw CmpMessages.MESSAGES.errorSettingInstanceField(getFieldName(), e);
        }
    }

    public Object getLockedValue(CmpEntityBeanContext ctx) {
        throw MESSAGES.optimisticLockingNotSupported();
    }

    public void lockInstanceValue(CmpEntityBeanContext ctx) {
        // not supported
    }

    public boolean isLoaded(CmpEntityBeanContext ctx) {
        return getFieldState(ctx).isLoaded();
    }

    /**
     * Has the value of this field changes since the last time clean was called.
     */
    public boolean isDirty(CmpEntityBeanContext ctx) {
        // read only and primary key fields are never dirty
        if (isReadOnly() || isPrimaryKeyMember()) {
            return false;
        }

        // has the value changes since setClean
        return isLoaded(ctx) && !stateFactory.isStateValid(getInstanceValue(ctx), getFieldState(ctx).originalValue);
    }

    /**
     * Mark this field as clean.
     * Saves the current state in context, so it can be compared when
     * isDirty is called.
     */
    public void setClean(CmpEntityBeanContext ctx) {
        FieldState fieldState = getFieldState(ctx);
        fieldState.originalValue = getInstanceValue(ctx);

        // update last read time
        if (isReadOnly()) {
            fieldState.lastRead = System.currentTimeMillis();
        }
    }

    public boolean isReadTimedOut(CmpEntityBeanContext ctx) {
        // if we are read/write then we are always timed out
        if (!isReadOnly()) {
            return true;
        }

        // if read-time-out is -1 then we never time out.
        if (getReadTimeOut() == -1) {
            return false;
        }

        long readInterval = System.currentTimeMillis() -
                getFieldState(ctx).lastRead;
        return readInterval >= getReadTimeOut();
    }

    public void resetPersistenceContext(CmpEntityBeanContext ctx) {
        if (isReadTimedOut(ctx)) {
            JDBCContext jdbcCtx = (JDBCContext)ctx.getPersistenceContext();
            FieldState fieldState = (FieldState) jdbcCtx.getFieldState(jdbcContextIndex);
            if (fieldState != null)
                fieldState.reset();
        }
    }

    protected void setDirtyAfterGet(CmpEntityBeanContext ctx) {
        getFieldState(ctx).setCheckDirty();
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

    private class FieldState {
        private Object originalValue;
        private long lastRead = -1;
        private JDBCEntityBridge.EntityState entityState;

        public FieldState(JDBCContext jdbcContext) {
            this.entityState = jdbcContext.getEntityState();
        }

        public boolean isLoaded() {
            return entityState.isLoaded(tableIndex);
        }

        public void setLoaded() {
            entityState.setLoaded(tableIndex);
        }

        public void setCheckDirty() {
            entityState.setCheckDirty(tableIndex);
        }

        public void reset() {
            originalValue = null;
            lastRead = -1;
            entityState.resetFlags(tableIndex);
            log.debug("reset field state");
        }
    }
}
