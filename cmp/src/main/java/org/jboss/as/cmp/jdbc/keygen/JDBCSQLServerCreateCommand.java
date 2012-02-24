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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import static org.jboss.as.cmp.CmpMessages.MESSAGES;
import org.jboss.as.cmp.context.CmpEntityBeanContext;
import org.jboss.as.cmp.jdbc.JDBCIdentityColumnCreateCommand;
import org.jboss.as.cmp.jdbc.JDBCUtil;
import org.jboss.as.cmp.jdbc.metadata.JDBCEntityCommandMetaData;

/**
 * Create command for Microsoft SQL Server that uses the value from an IDENTITY
 * columns. By default uses "SELECT SCOPE_IDENTITY()" to reduce the impact of
 * triggers; can be overridden with "pk-sql" attribute e.g. for V7.
 *
 * @author <a href="mailto:jeremy@boynes.com">Jeremy Boynes</a>
 * @version $Revision: 81030 $
 */
public class JDBCSQLServerCreateCommand extends JDBCIdentityColumnCreateCommand {
    protected void initEntityCommand(JDBCEntityCommandMetaData entityCommand) {
        super.initEntityCommand(entityCommand);
        pkSQL = entityCommand.getAttribute("pk-sql");
        if (pkSQL == null) {
            pkSQL = "SELECT SCOPE_IDENTITY()";
        }
    }

    protected void initInsertSQL() {
        super.initInsertSQL();
        insertSQL = insertSQL + "; " + pkSQL;
    }

    protected int executeInsert(int index, PreparedStatement ps, CmpEntityBeanContext ctx) throws SQLException {
        ps.execute();
        ResultSet rs = null;
        try {
            int rows = ps.getUpdateCount();
            if (rows != 1) {
                throw MESSAGES.expectedSingleRowButReceivedMore(rows);
            }
            if (ps.getMoreResults() == false) {
                throw MESSAGES.expectedResultSetReceivedUpdateCount();
            }

            rs = ps.getResultSet();
            if (!rs.next()) {
                throw MESSAGES.getGeneratedKeysEmptyResultSet();
            }
            pkField.loadInstanceResults(rs, 1, ctx);
            return rows;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw MESSAGES.errorExtractingGeneratedKey(e);
        } finally {
            JDBCUtil.safeClose(rs);
        }
    }
}
