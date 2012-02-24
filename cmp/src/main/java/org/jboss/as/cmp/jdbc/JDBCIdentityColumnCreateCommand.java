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

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import static org.jboss.as.cmp.CmpMessages.MESSAGES;
import org.jboss.as.cmp.jdbc.bridge.JDBCCMPFieldBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCFieldBridge;
import org.jboss.as.cmp.context.CmpEntityBeanContext;

/**
 * Base class for create commands where the PK value is generated as side
 * effect of performing the insert operation. This is typically associated
 * with database platforms that use identity columns
 *
 * @author <a href="mailto:jeremy@boynes.com">Jeremy Boynes</a>
 */
public abstract class JDBCIdentityColumnCreateCommand extends JDBCAbstractCreateCommand {
    protected JDBCCMPFieldBridge pkField;
    protected String pkSQL;

    protected boolean isInsertField(JDBCFieldBridge field) {
        // do not include PK fields in the insert
        return super.isInsertField(field) && !field.isPrimaryKeyMember();
    }

    protected void initGeneratedFields() {
        super.initGeneratedFields();
        pkField = getGeneratedPKField();
    }

    protected int executeInsert(int paramIndex, PreparedStatement ps, CmpEntityBeanContext ctx) throws SQLException {
        int rows = ps.executeUpdate();
        Connection c;
        Statement s = null;
        ResultSet rs = null;
        try {
            c = ps.getConnection();
            s = c.createStatement();
            rs = s.executeQuery(pkSQL);
            if (!rs.next()) {
                throw MESSAGES.resultSetEmpty();
            }
            pkField.loadInstanceResults(rs, 1, ctx);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // throw EJBException to force a rollback as the row has been inserted
            throw MESSAGES.errorExtractingGeneratedKey(e);
        } finally {
            JDBCUtil.safeClose(rs);
            JDBCUtil.safeClose(s);
        }
        return rows;
    }

    /**
     * Helper for subclasses that use reflection to avoid driver dependencies.
     *
     * @param t an Exception raised by a reflected call
     * @return SQLException extracted from the Throwable
     */
    protected SQLException processException(Throwable t) {
        if (t instanceof InvocationTargetException) {
            t = ((InvocationTargetException) t).getTargetException();
        }
        if (t instanceof SQLException) {
            return (SQLException) t;
        }
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new IllegalStateException(t);
    }
}
