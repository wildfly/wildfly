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

import java.util.Date;
import static org.jboss.as.cmp.CmpMessages.MESSAGES;
import org.jboss.as.cmp.jdbc.metadata.parser.ParsedCmpField;
import org.jboss.as.cmp.jdbc.metadata.parser.ParsedOptimisticLocking;

/**
 * Optimistic locking metadata
 *
 * @author <a href="mailto:aloubyansky@hotmail.com">Alex Loubyansky</a>
 * @version $Revision: 81030 $
 */
public final class JDBCOptimisticLockingMetaData {

    // Constants ---------------------------------------
    public enum LockingStrategy {
        FIELD_GROUP_STRATEGY, MODIFIED_STRATEGY, READ_STRATEGY, VERSION_COLUMN_STRATEGY, TIMESTAMP_COLUMN_STRATEGY, KEYGENERATOR_COLUMN_STRATEGY
    }

    // Attributes --------------------------------------
    /**
     * locking strategy
     */
    private final LockingStrategy lockingStrategy;

    /**
     * group name for field group strategy
     */
    private final String groupName;

    /**
     * locking field for version- or timestamp-column strategy
     */
    private final JDBCCMPFieldMetaData lockingField;

    /**
     * key generator factory
     */
    private final String keyGeneratorFactory;

    public JDBCOptimisticLockingMetaData(JDBCEntityMetaData entity, ParsedOptimisticLocking optimisticLocking) {
        lockingStrategy = optimisticLocking.getLockingStrategy();

        switch (lockingStrategy) {
            case FIELD_GROUP_STRATEGY: {
                groupName = optimisticLocking.getGroupName();
                lockingField = null;
                keyGeneratorFactory = null;
                break;
            }
            case MODIFIED_STRATEGY: {
                groupName = null;
                lockingField = null;
                keyGeneratorFactory = null;
                break;
            }
            case READ_STRATEGY: {
                groupName = null;
                lockingField = null;
                keyGeneratorFactory = null;
                break;
            }
            case VERSION_COLUMN_STRATEGY: {
                if (optimisticLocking.getLockingField().getFieldType() != null)
                    throw MESSAGES.fieldTypeNotAllowedForColumn("version", Long.class.getName());
                lockingField = constructLockingField(entity, optimisticLocking.getLockingField());
                groupName = null;
                keyGeneratorFactory = null;
                break;
            }
            case TIMESTAMP_COLUMN_STRATEGY: {
                if (optimisticLocking.getLockingField().getFieldType() != null)
                    throw MESSAGES.fieldTypeNotAllowedForColumn("timestamp", Date.class.getName());
                lockingField = constructLockingField(entity, optimisticLocking.getLockingField());
                groupName = null;
                keyGeneratorFactory = null;
                break;
            }
            case KEYGENERATOR_COLUMN_STRATEGY: {
                lockingField = constructLockingField(entity, optimisticLocking.getLockingField());
                groupName = null;
                keyGeneratorFactory = optimisticLocking.getKeyGeneratorFactory();
                break;
            }
            default: {
                throw MESSAGES.unknownLockingStrategy(entity.getName(), entity.getName());
            }
        }
    }

    private JDBCCMPFieldMetaData constructLockingField(JDBCEntityMetaData entity, ParsedCmpField lockingField) {
        // field name
        String fieldName = lockingField != null ? lockingField.getFieldName() : null;
        if (fieldName == null || fieldName.trim().length() < 1)
            fieldName = (lockingStrategy == LockingStrategy.VERSION_COLUMN_STRATEGY ? "version_lock" :
                    (lockingStrategy == LockingStrategy.TIMESTAMP_COLUMN_STRATEGY ? "timestamp_lock" : "generated_lock"));

        // column name
        String columnName = lockingField != null ? lockingField.getColumnName() : null;
        if (columnName == null || columnName.trim().length() < 1)
            columnName = (lockingStrategy == LockingStrategy.VERSION_COLUMN_STRATEGY ? "version_lock" :
                    (lockingStrategy == LockingStrategy.TIMESTAMP_COLUMN_STRATEGY ? "timestamp_lock" : "generated_lock"));

        // field type
        Class<?> fieldType = null;
        if (lockingStrategy == LockingStrategy.VERSION_COLUMN_STRATEGY)
            fieldType = java.lang.Long.class;
        else if (lockingStrategy == LockingStrategy.TIMESTAMP_COLUMN_STRATEGY)
            fieldType = java.util.Date.class;
        if (lockingField != null && lockingField.getFieldType() != null) {
            fieldType = lockingField.getFieldType();
        }

        // JDBC/SQL Type
        int jdbcType;
        String sqlType;
        if (lockingField != null && lockingField.getJdbcType() != null) {
            jdbcType = lockingField.getJdbcType();
            sqlType = lockingField.getSqlType();
        } else {
            jdbcType = Integer.MIN_VALUE;
            sqlType = null;
        }

        return new JDBCCMPFieldMetaData(entity, fieldName, fieldType, columnName, jdbcType, sqlType);
    }

    // Public ------------------------------------------
    public LockingStrategy getLockingStrategy() {
        return lockingStrategy;
    }

    public String getGroupName() {
        return groupName;
    }

    public JDBCCMPFieldMetaData getLockingField() {
        return lockingField;
    }

    public String getKeyGeneratorFactory() {
        return keyGeneratorFactory;
    }
}
