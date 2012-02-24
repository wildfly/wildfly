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
import org.jboss.as.cmp.CmpMessages;
import org.jboss.as.cmp.context.CmpEntityBeanContext;
import org.jboss.as.cmp.jdbc.JDBCIdentityColumnCreateCommand;
import org.jboss.as.cmp.jdbc.JDBCUtil;

/**
 * Create method that uses the identity_val_local() function in DB2 to get
 * get the ID of the last inserted row, and populate it into the EJB
 * object being created.
 *
 * @author <a href="mailto:dwintschel@esports.com">Daniel Wintschel</a>
 * @version $Revision: 81030 $
 */
public class JDBCDB2IdentityValLocalCreateCommand extends JDBCIdentityColumnCreateCommand {
    private static final String SQL = "values (identity_val_local())";

    protected int executeInsert(int paramIndex, PreparedStatement ps, CmpEntityBeanContext ctx) throws SQLException {
        int rows = ps.executeUpdate();
        ResultSet results = null;
        try {
            Connection conn = ps.getConnection();
            results = conn.prepareStatement(SQL).executeQuery();
            if (!results.next()) {
                throw CmpMessages.MESSAGES.identityValLocalReturnedEmptyResultsSet();
            }
            pkField.loadInstanceResults(results, 1, ctx);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // throw EJBException to force a rollback as the row has been inserted
            throw CmpMessages.MESSAGES.errorExtractingIdentityValLocal(e);
        } finally {
            JDBCUtil.safeClose(results);
        }
        return rows;
    }
}
