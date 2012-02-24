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
 * Inserts relations into a relation table.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version $Revision: 81030 $
 */
public final class JDBCInsertRelationsCommand {
    private final Logger log;

    public JDBCInsertRelationsCommand(JDBCStoreManager manager) {
        this.log = Logger.getLogger(
                this.getClass().getName() +
                        "." +
                        manager.getMetaData().getName());
    }

    public void execute(RelationData relationData) {
        if (relationData.addedRelations.size() == 0) {
            return;
        }

        Connection con = null;
        PreparedStatement ps = null;

        JDBCCMRFieldBridge cmrField = relationData.getLeftCMRField();
        try {
            // get the sql
            String sql = getSQL(relationData);
            boolean debug = log.isDebugEnabled();
            if (debug)
                log.debug("Executing SQL: " + sql);

            // get the connection
            DataSource dataSource = cmrField.getDataSource();
            con = dataSource.getConnection();

            // get a prepared statement
            ps = con.prepareStatement(sql);

            Iterator pairs = relationData.addedRelations.iterator();
            while (pairs.hasNext()) {
                RelationPair pair = (RelationPair) pairs.next();

                // set the parameters
                setParameters(ps, relationData, pair);

                int rowsAffected = ps.executeUpdate();
            }
        } catch (Exception e) {
            throw CmpMessages.MESSAGES.couldNotInsertRelations(cmrField.getQualifiedTableName(), e);
        } finally {
            JDBCUtil.safeClose(ps);
            JDBCUtil.safeClose(con);
        }
    }

    protected static String getSQL(RelationData relationData) {
        JDBCCMRFieldBridge left = relationData.getLeftCMRField();
        JDBCCMRFieldBridge right = relationData.getRightCMRField();

        StringBuffer sql = new StringBuffer(200);
        sql.append(SQLUtil.INSERT_INTO).append(left.getQualifiedTableName());

        sql.append('(');
        SQLUtil.getColumnNamesClause(left.getTableKeyFields(), sql);
        sql.append(SQLUtil.COMMA);
        SQLUtil.getColumnNamesClause(right.getTableKeyFields(), sql);
        sql.append(')');

        sql.append(SQLUtil.VALUES).append('(');
        SQLUtil.getValuesClause(left.getTableKeyFields(), sql);
        sql.append(SQLUtil.COMMA);
        SQLUtil.getValuesClause(right.getTableKeyFields(), sql);
        sql.append(')');
        return sql.toString();
    }

    protected static void setParameters(PreparedStatement ps,
                                        RelationData relationData,
                                        RelationPair pair) {
        int index = 1;

        // left keys
        Object leftId = pair.getLeftId();
        JDBCCMPFieldBridge[] leftFields = (JDBCCMPFieldBridge[]) relationData.getLeftCMRField().getTableKeyFields();
        for (int i = 0; i < leftFields.length; ++i)
            index = leftFields[i].setPrimaryKeyParameters(ps, index, leftId);

        // right keys
        Object rightId = pair.getRightId();
        JDBCCMPFieldBridge[] rightFields = (JDBCCMPFieldBridge[]) relationData.getRightCMRField().getTableKeyFields();
        for (int i = 0; i < rightFields.length; ++i)
            index = rightFields[i].setPrimaryKeyParameters(ps, index, rightId);
    }
}
