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

import java.sql.Date;
import org.jboss.as.cmp.jdbc.metadata.parser.ParsedAudit;
import org.jboss.as.cmp.jdbc.metadata.parser.ParsedCmpField;

/**
 * Audit field meta data
 *
 * @author <a href="mailto:Adrian.Brock@HappeningTimes.com">Adrian Brock</a>
 * @version $Revision: 81030 $
 */
public final class JDBCAuditMetaData {
    // Constants ---------------------------------------

    // Attributes --------------------------------------

    /**
     * The created by principal field
     */
    private final JDBCCMPFieldMetaData createdPrincipalField;

    /**
     * The created by time field
     */
    private final JDBCCMPFieldMetaData createdTimeField;

    /**
     * The last update by principal field
     */
    private final JDBCCMPFieldMetaData updatedPrincipalField;

    /**
     * The last update time time field
     */
    private final JDBCCMPFieldMetaData updatedTimeField;

    public JDBCAuditMetaData(JDBCEntityMetaData entityMetaData, ParsedAudit audit) {
        final ParsedCmpField createdBy = audit.getCreatedBy();
        if (createdBy != null) {
            if (entityMetaData.getCMPFieldByName(createdBy.getFieldName()) != null) {
                createdPrincipalField = entityMetaData.getCMPFieldByName(createdBy.getFieldName());
            } else {
                createdPrincipalField = new JDBCCMPFieldMetaData(entityMetaData, createdBy.getFieldName(), String.class, createdBy.getColumnName(),
                        createdBy.getJdbcType() != null ? createdBy.getJdbcType() : Integer.MAX_VALUE,
                        createdBy.getSqlType() != null ? createdBy.getSqlType() : null);
            }
        } else {
            createdPrincipalField = null;
        }
        final ParsedCmpField createdTime = audit.getCreatedTime();
        if (createdTime != null) {
            if (entityMetaData.getCMPFieldByName(createdTime.getFieldName()) != null) {
                this.createdTimeField = entityMetaData.getCMPFieldByName(createdTime.getFieldName());
            } else {
                this.createdTimeField = new JDBCCMPFieldMetaData(entityMetaData, createdTime.getFieldName(), Date.class, createdTime.getColumnName(),
                        createdTime.getJdbcType() != null ? createdTime.getJdbcType() : Integer.MAX_VALUE,
                        createdTime.getSqlType() != null ? createdTime.getSqlType() : null);
            }
        } else  {
            createdTimeField = null;
        }
        final ParsedCmpField updatedBy = audit.getUpdatedBy();
        if (updatedBy != null) {
            if (entityMetaData.getCMPFieldByName(updatedBy.getFieldName()) != null) {
                this.updatedPrincipalField = entityMetaData.getCMPFieldByName(updatedBy.getFieldName());
            } else {
                this.updatedPrincipalField = new JDBCCMPFieldMetaData(entityMetaData, updatedBy.getFieldName(), String.class, updatedBy.getColumnName(),
                        updatedBy.getJdbcType() != null ? updatedBy.getJdbcType() : Integer.MAX_VALUE,
                        updatedBy.getSqlType() != null ? updatedBy.getSqlType() : null);
            }
        } else {
            updatedPrincipalField = null;
        }
        final ParsedCmpField updatedTime = audit.getUpdatedTime();
        if (updatedTime != null) {
            if (entityMetaData.getCMPFieldByName(updatedTime.getFieldName()) != null) {
                this.updatedTimeField = entityMetaData.getCMPFieldByName(updatedTime.getFieldName());
            } else {
                this.updatedTimeField = new JDBCCMPFieldMetaData(entityMetaData, updatedTime.getFieldName(), String.class, updatedTime.getColumnName(),
                        updatedTime.getJdbcType() != null ? updatedTime.getJdbcType() : Integer.MAX_VALUE,
                        updatedTime.getSqlType() != null ? updatedTime.getSqlType() : null);
            }
        } else {
            updatedTimeField = null;
        }
    }

    // Public ------------------------------------------

    public JDBCCMPFieldMetaData getCreatedPrincipalField() {
        return createdPrincipalField;
    }

    public JDBCCMPFieldMetaData getCreatedTimeField() {
        return createdTimeField;
    }

    public JDBCCMPFieldMetaData getUpdatedPrincipalField() {
        return updatedPrincipalField;
    }

    public JDBCCMPFieldMetaData getUpdatedTimeField() {
        return updatedTimeField;
    }
}
