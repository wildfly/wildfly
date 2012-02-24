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
import static org.jboss.as.cmp.CmpMessages.MESSAGES;
import org.jboss.as.cmp.context.CmpEntityBeanContext;
import org.jboss.as.cmp.jdbc.JDBCIdentityColumnCreateCommand;
import org.jboss.as.cmp.jdbc.JDBCUtil;


/**
 * Create method that uses the JDBC 3.0 getGeneratedKeys method to obtain
 * the value from the identity column.
 *
 * @author <a href="mailto:jeremy@boynes.com">Jeremy Boynes</a>
 * @version $Revision: 81030 $
 */
public class JDBC30GeneratedKeysCreateCommand extends JDBCIdentityColumnCreateCommand {
    protected PreparedStatement prepareStatement(Connection c, String sql, CmpEntityBeanContext ctx) throws SQLException {
        try {
            return c.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
        } catch (Exception e) {
            throw processException(e);
        }
    }

    protected int executeInsert(int paramIndex, PreparedStatement ps, CmpEntityBeanContext ctx) throws SQLException {
        int rows = ps.executeUpdate();
        ResultSet rs = null;
        try {
            rs = ps.getGeneratedKeys();
            if (!rs.next()) {
                // throw EJBException to force a rollback as the row has been inserted
                throw MESSAGES.getGeneratedKeysEmptyResultSet();
            }
            pkField.loadInstanceResults(rs, 1, ctx);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // throw EJBException to force a rollback as the row has been inserted
            throw MESSAGES.errorExtractingGeneratedKey(e);
        } finally {
            JDBCUtil.safeClose(rs);
        }
        return rows;
    }
}
