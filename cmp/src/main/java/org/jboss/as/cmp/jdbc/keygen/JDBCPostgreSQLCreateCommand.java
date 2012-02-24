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
package org.jboss.as.cmp.jdbc.keygen;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.jboss.as.cmp.CmpMessages;
import org.jboss.as.cmp.context.CmpEntityBeanContext;
import org.jboss.as.cmp.jdbc.JDBCIdentityColumnCreateCommand;
import org.jboss.as.cmp.jdbc.JDBCStoreManager;
import org.jboss.as.cmp.jdbc.JDBCUtil;
import org.jboss.as.cmp.jdbc.SQLUtil;
import org.jboss.as.cmp.jdbc.metadata.JDBCEntityCommandMetaData;

/**
 * Create command for PostgreSQL that fetches the currval of the sequence
 * associated with a SERIAL column in this table.
 *
 * @author <a href="mailto:jeremy@boynes.com">Jeremy Boynes</a>
 * @version $Revision: 81030 $
 */
public class JDBCPostgreSQLCreateCommand extends JDBCIdentityColumnCreateCommand {
    private String sequence;
    private String sequenceSQL;

    public void init(JDBCStoreManager manager) {
        super.init(manager);
    }

    protected void initEntityCommand(JDBCEntityCommandMetaData entityCommand) {
        super.initEntityCommand(entityCommand);
        sequence = entityCommand.getAttribute("sequence");
        if (sequence == null) {
            sequence = entity.getQualifiedTableName()
                    + '_' + SQLUtil.getColumnNamesClause(pkField, new StringBuffer(20))
                    + "_seq";
        }
        sequenceSQL = "SELECT currval('" + sequence + "')";
        if (debug) {
            log.debug("SEQUENCE SQL is :" + sequenceSQL);
        }
    }

    protected int executeInsert(int index, PreparedStatement ps, CmpEntityBeanContext ctx) throws SQLException {
        int rows = ps.executeUpdate();

        Statement s = null;
        ResultSet rs = null;
        try {
            if (trace) {
                log.trace("Executing SQL :" + sequenceSQL);
            }
            Connection c = ps.getConnection();
            s = c.createStatement();
            rs = s.executeQuery(sequenceSQL);
            if (!rs.next()) {
                throw CmpMessages.MESSAGES.sequenceSqlReturnedEmptyResultSet();
            }
            pkField.loadInstanceResults(rs, 1, ctx);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // throw EJBException to force a rollback as the row has been inserted
            throw CmpMessages.MESSAGES.errorExtractingGeneratedKey(e);
        } finally {
            JDBCUtil.safeClose(rs);
            JDBCUtil.safeClose(s);
        }

        return rows;
    }
}
