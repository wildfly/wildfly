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
package org.jboss.as.cmp.jdbc2;

import javax.ejb.FinderException;
import static org.jboss.as.cmp.CmpMessages.MESSAGES;
import org.jboss.as.cmp.jdbc.JDBCEntityPersistenceStore;
import org.jboss.as.cmp.jdbc.JDBCQueryCommand;
import org.jboss.as.cmp.jdbc.JDBCTypeFactory;
import org.jboss.as.cmp.jdbc.QueryParameter;
import org.jboss.as.cmp.jdbc.metadata.JDBCFunctionMappingMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCTypeMappingMetaData;
import org.jboss.as.cmp.jdbc2.bridge.JDBCCMPFieldBridge2;
import org.jboss.as.cmp.jdbc2.bridge.JDBCEntityBridge2;
import org.jboss.as.cmp.jdbc2.schema.Schema;
import org.jboss.logging.Logger;


/**
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version <tt>$Revision: 81030 $</tt>
 */
public class FindByPrimaryKeyCommand
        extends AbstractQueryCommand {
    public FindByPrimaryKeyCommand(JDBCEntityBridge2 entity) {
        this.entity = entity;

        JDBCCMPFieldBridge2[] fields = (JDBCCMPFieldBridge2[]) entity.getTableFields();
        String selectColumns = fields[0].getColumnName();
        for (int i = 1; i < fields.length; ++i) {
            selectColumns += ", " + fields[i].getColumnName();
        }

        JDBCCMPFieldBridge2[] pkFields = (JDBCCMPFieldBridge2[]) entity.getPrimaryKeyFields();
        String whereColumns = pkFields[0].getColumnName() + "=?";
        for (int i = 1; i < pkFields.length; ++i) {
            whereColumns += " and " + pkFields[i].getColumnName() + "=?";
        }

        if (entity.getMetaData().hasRowLocking()) {
            JDBCEntityPersistenceStore manager = entity.getManager();
            JDBCTypeFactory typeFactory = manager.getJDBCTypeFactory();
            JDBCTypeMappingMetaData typeMapping = typeFactory.getTypeMapping();
            JDBCFunctionMappingMetaData rowLockingTemplate = typeMapping.getRowLockingTemplate();

            if (rowLockingTemplate == null) {
                throw MESSAGES.noRowLockingTemplateForMapping(typeMapping.getName());
            }

            sql = rowLockingTemplate.getFunctionSql(
                    new Object[]{selectColumns, entity.getQualifiedTableName(), whereColumns, null}, new StringBuffer()
            ).toString();
        } else {
            sql = "select ";
            sql += selectColumns;
            sql += " from " + entity.getQualifiedTableName() + " where ";
            sql += whereColumns;
        }

        log = Logger.getLogger(getClass().getName() + "." + entity.getEntityName() + "#findByPrimaryKey");

        log.debug("sql: " + sql);

        setParameters(QueryParameter.createPrimaryKeyParameters(0, entity));
        setEntityReader(entity, false);
    }

    public Object fetchOne(Schema schema, Object[] args, JDBCQueryCommand.EntityProxyFactory factory) throws FinderException {
        Object pk = args[0];
        if (pk == null) {
            throw MESSAGES.nullArgumentForFindByPrimaryKey();
        }

        Object instance;
        boolean cached = entity.getTable().hasRow(pk);
        if (!cached) {
            instance = super.executeFetchOne(args, factory);
            if (instance == null) {
                throw MESSAGES.instanceNotFound(entity.getEntityName(), pk);
            }
        } else {
            instance = pk;
        }

        return instance;
    }
}
