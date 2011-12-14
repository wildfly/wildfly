/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.cmp.jdbc;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.ejb.FinderException;
import org.jboss.as.cmp.context.CmpEntityBeanContext;
import org.jboss.as.cmp.jdbc.bridge.JDBCAbstractCMPFieldBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCEntityBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCFieldBridge;
import org.jboss.as.cmp.jdbc.metadata.JDBCFunctionMappingMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCQueryMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCReadAheadMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCTypeMappingMetaData;

/**
 * JDBCBeanExistsCommand is a JDBC query that checks if an id exists
 * in the database.  This is used by the create and findByPrimaryKey
 * code.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="mailto:marc.fleury@telkel.com">Marc Fleury</a>
 * @author <a href="mailto:justin@j-m-f.demon.co.uk">Justin Forder</a>
 * @author <a href="mailto:alex@jboss.org">Alex Loubyansky</a>
 * @version $Revision: 81030 $
 */
public final class JDBCFindByPrimaryKeyQuery extends JDBCAbstractQueryCommand {
    private JDBCStoreManager manager;
    private boolean rowLocking;

    public JDBCFindByPrimaryKeyQuery(JDBCStoreManager manager, JDBCQueryMetaData q) {
        super(manager, q);
        this.manager = manager;
        rowLocking = manager.getMetaData().hasRowLocking();

        JDBCEntityBridge entity = (JDBCEntityBridge) manager.getEntityBridge();

        JDBCTypeMappingMetaData typeMapping = this.manager.getJDBCTypeFactory().getTypeMapping();
        AliasManager aliasManager = new AliasManager(
                typeMapping.getAliasHeaderPrefix(),
                typeMapping.getAliasHeaderSuffix(),
                typeMapping.getAliasMaxLength()
        );

        String alias = aliasManager.getAlias(entity.getEntityName());

        StringBuffer select = new StringBuffer(200);
        SQLUtil.getColumnNamesClause(entity.getPrimaryKeyFields(), alias, select);

        StringBuffer from = new StringBuffer();
        from.append(entity.getQualifiedTableName())
                .append(' ')
                .append(alias);

        // set the preload fields
        JDBCReadAheadMetaData readAhead = q.getReadAhead();
        if (readAhead.isOnFind()) {
            setEagerLoadGroup(readAhead.getEagerLoadGroup());
            if (getEagerLoadMask() != null) {
                SQLUtil.appendColumnNamesClause(entity.getTableFields(), getEagerLoadMask(), alias, select);

                List<LeftJoinCMRNode> onFindCMRList = JDBCAbstractQueryCommand.getLeftJoinCMRNodes(
                        entity, entity.getQualifiedTableName(), readAhead.getLeftJoins(), null);

                if (!onFindCMRList.isEmpty()) {
                    setOnFindCMRList(onFindCMRList);
                    JDBCAbstractQueryCommand.leftJoinCMRNodes(alias, onFindCMRList, aliasManager, from);
                    JDBCAbstractQueryCommand.appendLeftJoinCMRColumnNames(onFindCMRList, aliasManager, select);
                }
            }
        }

        StringBuffer where = new StringBuffer();
        SQLUtil.getWhereClause(entity.getPrimaryKeyFields(), alias, where);

        // generate the sql
        StringBuffer sql = new StringBuffer(300);
        if (rowLocking && readAhead.isOnFind() && getEagerLoadMask() != null) {
            JDBCFunctionMappingMetaData rowLockingTemplate = typeMapping.getRowLockingTemplate();
            rowLockingTemplate.getFunctionSql(
                    new Object[]{
                            select,
                            from,
                            where.length() == 0 ? null : where,
                            null // order by
                    },
                    sql
            );
        } else {
            sql.append(SQLUtil.SELECT)
                    .append(select)
                    .append(SQLUtil.FROM)
                    .append(from)
                    .append(SQLUtil.WHERE)
                    .append(where);
        }

        setSQL(sql.toString());
        setParameterList(QueryParameter.createPrimaryKeyParameters(0, entity));
    }

    public Collection execute(Method finderMethod, Object[] args, CmpEntityBeanContext ctx, EntityProxyFactory factory) throws FinderException {
        // Check in readahead cache.
        if (manager.getReadAheadCache().getPreloadDataMap(args[0], false) != null) {
            // copy pk [JBAS-1361]
            Object pk = null;
            JDBCFieldBridge[] pkFields = manager.getEntityBridge().getPrimaryKeyFields();
            for (int i = 0; i < pkFields.length; ++i) {
                JDBCAbstractCMPFieldBridge pkField = ((JDBCAbstractCMPFieldBridge) pkFields[i]);
                Object fieldValue = pkField.getPrimaryKeyValue(args[0]);
                pk = pkField.setPrimaryKeyValue(pk, fieldValue);
            }

            return Collections.singletonList(pk != null ? factory.getEntityObject(pk) : null);
        }
        return super.execute(finderMethod, args, ctx, factory);
    }
}
