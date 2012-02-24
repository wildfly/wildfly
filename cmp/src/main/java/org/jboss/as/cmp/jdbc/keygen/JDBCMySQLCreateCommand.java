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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.ejb.EJBException;
import org.jboss.as.cmp.CmpMessages;
import static org.jboss.as.cmp.CmpMessages.MESSAGES;
import org.jboss.as.cmp.context.CmpEntityBeanContext;
import org.jboss.as.cmp.jdbc.JDBCIdentityColumnCreateCommand;
import org.jboss.as.cmp.jdbc.JDBCStoreManager;
import org.jboss.as.cmp.jdbc.JDBCUtil;
import org.jboss.as.cmp.jdbc.metadata.JDBCEntityCommandMetaData;

/**
 * Create command for MySQL that uses the driver's getGeneratedKeys method
 * to retrieve AUTO_INCREMENT values.
 *
 * @author <a href="mailto:jeremy@boynes.com">Jeremy Boynes</a>
 * @author Scott.Stark@jboss.org
 * @version $Revision: 81030 $
 */
public class JDBCMySQLCreateCommand extends JDBCIdentityColumnCreateCommand {
    private String className;
    private String methodName;
    private Method method;
    private Method getUnderlyingStatement;

    public void init(JDBCStoreManager manager) {
        super.init(manager);
        ClassLoader loader = GetTCLAction.getContextClassLoader();
        try {
            Class psClass = loader.loadClass(className);
            method = psClass.getMethod(methodName);
        } catch (ClassNotFoundException e) {
            throw MESSAGES.failedToLoadDriverClass(className, e);
        } catch (NoSuchMethodException e) {
            throw MESSAGES.driverDoesNotHaveMethod(className, methodName);
        }

        try {
            Class wrapperClass = loader.loadClass("org.jboss.resource.adapter.jdbc.StatementAccess");
            getUnderlyingStatement = wrapperClass.getMethod("getUnderlyingStatement");
        } catch (ClassNotFoundException e) {
            throw MESSAGES.couldNotLoadStatementAccess(e);
        } catch (NoSuchMethodException e) {
            throw MESSAGES.getUnderlyingStatementNotFound(e);
        }
    }

    protected void initEntityCommand(JDBCEntityCommandMetaData entityCommand) {
        super.initEntityCommand(entityCommand);
        className = entityCommand.getAttribute("class-name");
        if (className == null) {
            className = "com.mysql.jdbc.PreparedStatement";
        }
        methodName = entityCommand.getAttribute("method");
        if (methodName == null) {
            methodName = "getGeneratedKeys";
        }
    }

    protected int executeInsert(int paramIndex, PreparedStatement ps, CmpEntityBeanContext ctx) throws SQLException {
        int rows = ps.executeUpdate();

        // remove any JCA wrappers
        Statement stmt = ps;
        do {
            try {
                Object[] args = {};
                stmt = (Statement) getUnderlyingStatement.invoke(stmt, args);
            } catch (IllegalAccessException e) {
                SQLException ex = new SQLException("Failed to invoke getUnderlyingStatement");
                ex.initCause(e);
                throw ex;
            } catch (InvocationTargetException e) {
                SQLException ex = new SQLException("Failed to invoke getUnderlyingStatement");
                ex.initCause(e);
                throw ex;
            }
        } while (stmt != null && method.getDeclaringClass().isInstance(stmt) == false);

        ResultSet rs = null;
        try {
            rs = (ResultSet) method.invoke(stmt);
            if (!rs.next()) {
                throw MESSAGES.getGeneratedKeysEmptyResultSet();
            }
            pkField.loadInstanceResults(rs, 1, ctx);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw MESSAGES.errorExtractingGeneratedKey(e);
        } finally {
            JDBCUtil.safeClose(rs);
        }
        return rows;
    }
}
