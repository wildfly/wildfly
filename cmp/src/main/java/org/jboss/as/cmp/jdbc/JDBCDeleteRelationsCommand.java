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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Iterator;
import javax.ejb.EJBException;
import javax.sql.DataSource;
import org.jboss.as.cmp.CmpMessages;
import org.jboss.as.cmp.jdbc.bridge.JDBCCMPFieldBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCCMRFieldBridge;
import org.jboss.logging.Logger;

/**
 * Deletes relations from a relation table.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="mailto:alex@jboss.org">Alex Loubyansky</a>
 * @version $Revision: 81030 $
 */
public final class JDBCDeleteRelationsCommand {
    private final Logger log;
    private int maxKeysInDelete;
    private String maxKeysStatement;

    public JDBCDeleteRelationsCommand(JDBCStoreManager manager) {
        // Create the Log
        log = Logger.getLogger(this.getClass().getName() +
                "." +
                manager.getMetaData().getName()
        );

        maxKeysInDelete = manager.getJDBCTypeFactory().getTypeMapping().getMaxKeysInDelete();
    }

    //
    // This command needs to be changed to chunk delete commands, because
    // some database have a limit on the number of parameters in a statement.
    //
    public void execute(RelationData relationData) {
        if (relationData.removedRelations.size() == 0) {
            return;
        }

        Iterator pairs = relationData.removedRelations.iterator();
        int i = 0;
        while (i < relationData.removedRelations.size()) {
            String sql = getSQL(relationData, relationData.removedRelations.size() - i);

            Connection con = null;
            PreparedStatement ps = null;
            JDBCCMRFieldBridge cmrField = relationData.getLeftCMRField();
            try {
                // create the statement
                if (log.isDebugEnabled()) {
                    log.debug("Executing SQL: " + sql);
                }

                // get the connection
                DataSource dataSource = cmrField.getDataSource();
                con = dataSource.getConnection();
                ps = con.prepareStatement(sql);

                // set the parameters
                setParameters(ps, relationData, pairs);

                // execute statement
                int rowsAffected = ps.executeUpdate();
                if (log.isDebugEnabled()) {
                    log.debug("Rows affected = " + rowsAffected);
                }

                i += (maxKeysInDelete > 0 ? maxKeysInDelete : relationData.removedRelations.size());
            } catch (Exception e) {
                throw CmpMessages.MESSAGES.couldNotDeleteRelations(cmrField.getQualifiedTableName(), e);
            } finally {
                JDBCUtil.safeClose(ps);
                JDBCUtil.safeClose(con);
            }
        }
    }

    private String getSQL(RelationData relationData, int keys) {
        if (maxKeysInDelete > 0 && keys >= maxKeysInDelete) {
            if (maxKeysStatement == null) {
                maxKeysStatement = createSQL(relationData, maxKeysInDelete);
            }
            return maxKeysStatement;
        }
        return createSQL(relationData, keys);
    }

    private static String createSQL(RelationData relationData, int keysInDelete) {
        JDBCCMRFieldBridge left = relationData.getLeftCMRField();
        JDBCCMRFieldBridge right = relationData.getRightCMRField();

        StringBuffer sql = new StringBuffer(300);
        sql.append(SQLUtil.DELETE_FROM)
                .append(left.getQualifiedTableName())
                .append(SQLUtil.WHERE);

        StringBuffer whereClause = new StringBuffer(20);
        whereClause.append('(');
        // left keys
        SQLUtil.getWhereClause(left.getTableKeyFields(), whereClause)
                .append(SQLUtil.AND);
        // right keys
        SQLUtil.getWhereClause(right.getTableKeyFields(), whereClause)
                .append(')');
        String whereClauseStr = whereClause.toString();
        sql.append(whereClauseStr);
        for (int i = 1; i < keysInDelete; ++i) {
            sql.append(SQLUtil.OR).append(whereClauseStr);
        }

        return sql.toString();
    }

    private void setParameters(PreparedStatement ps, RelationData relationData, Iterator pairs)
            throws Exception {
        int index = 1;
        JDBCCMPFieldBridge[] leftFields = (JDBCCMPFieldBridge[]) relationData.getLeftCMRField().getTableKeyFields();
        JDBCCMPFieldBridge[] rightFields = (JDBCCMPFieldBridge[]) relationData.getRightCMRField().getTableKeyFields();
        int keyIndex = 0;
        while (pairs.hasNext()) {
            RelationPair pair = (RelationPair) pairs.next();

            // left keys
            Object leftId = pair.getLeftId();
            for (int i = 0; i < leftFields.length; ++i) {
                index = leftFields[i].setPrimaryKeyParameters(ps, index, leftId);
            }

            // right keys
            Object rightId = pair.getRightId();
            for (int i = 0; i < rightFields.length; ++i) {
                index = rightFields[i].setPrimaryKeyParameters(ps, index, rightId);
            }

            if (maxKeysInDelete > 0) {
                ++keyIndex;
                if (keyIndex == maxKeysInDelete) {
                    break;
                }
            }
        }
    }
}
