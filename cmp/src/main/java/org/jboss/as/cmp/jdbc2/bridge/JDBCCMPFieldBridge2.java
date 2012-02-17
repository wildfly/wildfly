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

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.ejb.EJBException;
import org.jboss.as.cmp.bridge.CMPFieldBridge;
import org.jboss.as.cmp.context.CmpEntityBeanContext;
import org.jboss.as.cmp.jdbc.JDBCEntityPersistenceStore;
import org.jboss.as.cmp.jdbc.JDBCResultSetReader;
import org.jboss.as.cmp.jdbc.JDBCType;
import org.jboss.as.cmp.jdbc.metadata.JDBCCMPFieldMetaData;
import org.jboss.as.cmp.jdbc2.JDBCStoreManager2;
import org.jboss.as.cmp.jdbc2.PersistentContext;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version <tt>$Revision: 81030 $</tt>
 */
public class JDBCCMPFieldBridge2 implements CMPFieldBridge {
    private final JDBCEntityBridge2 entity;
    private final int rowIndex;
    private final JDBCType jdbcType;
    private final Class pkClass;
    private final Field pkField;
    private final boolean isPrimaryKeyMember;
    private final String fieldName;
    private final Class fieldType;
    private final String columnName;

    private final JDBCCMPFieldBridge2 cmpFieldIAmMappedTo;

    private final Logger log;

    private int versionIndex = -1;

    public JDBCCMPFieldBridge2(JDBCStoreManager2 manager,
                               JDBCEntityBridge2 entity,
                               JDBCCMPFieldMetaData metadata,
                               int rowIndex) {
        this.rowIndex = rowIndex;
        this.entity = entity;
        jdbcType = manager.getJDBCTypeFactory().getJDBCType(metadata);
        pkClass = metadata.getEntity().getPrimaryKeyClass();
        pkField = metadata.getPrimaryKeyField();
        isPrimaryKeyMember = metadata.isPrimaryKeyMember();
        fieldName = metadata.getFieldName();
        fieldType = metadata.getFieldType();
        cmpFieldIAmMappedTo = null;
        columnName = metadata.getColumnName();

        log = Logger.getLogger(this.getClass().getName() + "." + entity.getEntityName() + "#" + getFieldName());
    }

    public JDBCCMPFieldBridge2(JDBCCMPFieldBridge2 cmpField, JDBCCMPFieldBridge2 relatedPKField) {
        entity = cmpField.entity;
        rowIndex = cmpField.rowIndex;
        jdbcType = cmpField.jdbcType;
        columnName = cmpField.columnName;

        fieldName = relatedPKField.fieldName;
        fieldType = relatedPKField.fieldType;
        pkClass = relatedPKField.pkClass;
        pkField = relatedPKField.pkField;

        isPrimaryKeyMember = false;

        cmpFieldIAmMappedTo = cmpField;

        log = Logger.getLogger(this.getClass().getName() + "." + entity.getEntityName() + "#" + getFieldName());
    }

    // Public

    public void initVersion() {
        versionIndex = entity.getTable().addVersionField();
    }

    public int getVersionIndex() {
        return versionIndex;
    }

    public String getColumnName() {
        return columnName;
    }

    public Object setPrimaryKeyValue(Object primaryKey, Object value)
            throws IllegalArgumentException {
        try {
            if (pkField != null) {
                // if we are trying to set a null value into a null pk, we are already done.
                if (value == null && primaryKey == null) {
                    return null;
                }

                // if we don't have a pk object yet create one
                if (primaryKey == null) {
                    primaryKey = pkClass.newInstance();
                }

                // Set this field's value into the primary key object.
                pkField.set(primaryKey, value);
                return primaryKey;
            } else {
                // This field is the primary key, so no extraction is necessary.
                return value;
            }
        } catch (Exception e) {
            // Non recoverable internal exception
            throw new EJBException("Internal error setting instance field " + getFieldName(), e);
        }
    }

    public void setValueInternal(CmpEntityBeanContext ctx, Object value, boolean makeDirty) {
        PersistentContext pctx = (PersistentContext) ctx.getPersistenceContext();

        // todo this is weird
        if (cmpFieldIAmMappedTo != null && cmpFieldIAmMappedTo.isPrimaryKeyMember) {
            Object curValue = pctx.getFieldValue(rowIndex);
            if (value != null && !value.equals(curValue)) {
                throw new IllegalStateException(
                        "Attempt to modify a primary key field through a foreign key field mapped to it: "
                                +
                                entity.getEntityName()
                                + "."
                                + cmpFieldIAmMappedTo.getFieldName()
                                +
                                " -> "
                                + entity.getQualifiedTableName()
                                + "."
                                + cmpFieldIAmMappedTo.getColumnName() +
                                ", current value=" + curValue + ", new value=" + value
                );
            }

            makeDirty = false;
        } else {
            pctx.setFieldValue(rowIndex, value);
        }

        if (makeDirty) {
            pctx.setDirty();
        }
    }

    public int setArgumentParameters(PreparedStatement ps, int parameterIndex, Object arg) {
        try {
            int[] jdbcTypes = jdbcType.getJDBCTypes();
            for (int i = 0; i < jdbcTypes.length; i++) {
                Object columnValue = jdbcType.getColumnValue(i, arg);
                jdbcType.getParameterSetter()[i].set(ps, parameterIndex++, jdbcTypes[i], columnValue, log);
                //JDBCUtil.setParameter(log, ps, parameterIndex++, jdbcTypes[i], columnValue);
            }
            return parameterIndex;
        } catch (SQLException e) {
            // Non recoverable internal exception
            throw new EJBException("Internal error setting parameters for field " + getFieldName(), e);
        }
    }

    public Object loadArgumentResults(ResultSet rs, int parameterIndex)
            throws IllegalArgumentException {
        try {
            // update the value from the result set
            Class[] javaTypes = jdbcType.getJavaTypes();
            if (javaTypes.length > 1) {
                throw new IllegalStateException("Complex types are not supported yet.");
            }

            JDBCResultSetReader[] rsReaders = jdbcType.getResultSetReaders();

            Object columnValue = null;
            for (int i = 0; i < javaTypes.length; i++) {
                columnValue = rsReaders[i].get(rs, parameterIndex++, javaTypes[i], log);
                columnValue = jdbcType.setColumnValue(i, null, columnValue);
            }

            // return the updated parameterIndex
            return columnValue;
        } catch (SQLException e) {
            // Non recoverable internal exception
            throw new EJBException("Internal error getting results for field member " + getFieldName(), e);
        }
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public JDBCEntityPersistenceStore getManager() {
        return entity.getManager();
    }

    // JDBCFieldBridge implementation

    public void initInstance(CmpEntityBeanContext ctx) {
        Object value;
        Class fieldType = getFieldType();
        if (fieldType == boolean.class) {
            value = Boolean.FALSE;
        } else if (fieldType == byte.class) {
            value = new Byte((byte) 0);
        } else if (fieldType == int.class) {
            value = new Integer(0);
        } else if (fieldType == long.class) {
            value = new Long(0L);
        } else if (fieldType == short.class) {
            value = new Short((short) 0);
        } else if (fieldType == char.class) {
            value = new Character('\u0000');
        } else if (fieldType == double.class) {
            value = new Double(0d);
        } else if (fieldType == float.class) {
            value = new Float(0f);
        } else {
            value = null;
        }
        setValueInternal(ctx, value, false);
    }

    public void resetPersistenceContext(CmpEntityBeanContext ctx) {
        throw new UnsupportedOperationException();
    }

    public int setInstanceParameters(PreparedStatement ps, int parameterIndex, CmpEntityBeanContext ctx) {
        throw new UnsupportedOperationException();
    }

    public Object getInstanceValue(CmpEntityBeanContext ctx) {
        throw new UnsupportedOperationException();
    }

    public void setInstanceValue(CmpEntityBeanContext ctx, Object value) {
        throw new UnsupportedOperationException();
    }

    public int loadInstanceResults(ResultSet rs, int parameterIndex, CmpEntityBeanContext ctx) {
        throw new UnsupportedOperationException();
    }

    public int loadArgumentResults(ResultSet rs, int parameterIndex, Object[] argumentRef) {
        throw new UnsupportedOperationException();
    }

    public boolean isDirty(CmpEntityBeanContext ctx) {
        throw new UnsupportedOperationException();
    }

    public void setClean(CmpEntityBeanContext ctx) {
        throw new UnsupportedOperationException();
    }

    public boolean isCMPField() {
        return true;
    }

    public boolean isPrimaryKeyMember() {
        return isPrimaryKeyMember;
    }

    public boolean isReadOnly() {
        throw new UnsupportedOperationException();
    }

    public boolean isReadTimedOut(CmpEntityBeanContext ctx) {
        throw new UnsupportedOperationException();
    }

    public boolean isLoaded(CmpEntityBeanContext ctx) {
        throw new UnsupportedOperationException();
    }

    public JDBCType getJDBCType() {
        return jdbcType;
    }

    public Object getPrimaryKeyValue(Object primaryKey)
            throws IllegalArgumentException {
        try {
            if (pkField != null) {
                if (primaryKey == null) {
                    return null;
                }

                return pkField.get(primaryKey);
            } else {
                return primaryKey;
            }
        } catch (Exception e) {
            throw new EJBException("Internal error getting primary key field member " + getFieldName(), e);
        }
    }

    // CMPFieldBridge implementation

    public String getFieldName() {
        return fieldName;
    }

    public Object getValue(CmpEntityBeanContext ctx) {
        PersistentContext pctx = (PersistentContext) ctx.getPersistenceContext();
        return pctx.getFieldValue(rowIndex);
    }

    public void setValue(CmpEntityBeanContext ctx, Object value) {
        setValueInternal(ctx, value, true);
    }

    public Class getFieldType() {
        return fieldType;
    }
}
