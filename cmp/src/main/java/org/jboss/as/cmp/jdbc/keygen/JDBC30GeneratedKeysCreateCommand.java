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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.ejb.EJBException;
import org.jboss.as.cmp.context.CmpEntityBeanContext;
import org.jboss.as.cmp.jdbc.JDBCIdentityColumnCreateCommand;
import org.jboss.as.cmp.jdbc.JDBCStoreManager;
import org.jboss.as.cmp.jdbc.JDBCUtil;


/**
 * Create method that uses the JDBC 3.0 getGeneratedKeys method to obtain
 * the value from the identity column.
 *
 * @author <a href="mailto:jeremy@boynes.com">Jeremy Boynes</a>
 * @version $Revision: 81030 $
 */
public class JDBC30GeneratedKeysCreateCommand extends JDBCIdentityColumnCreateCommand {
    private static final Method CONNECTION_PREPARE;
    private static final Integer GENERATE_KEYS;
    private static final Method GET_GENERATED_KEYS;

    static {
        Method prepare, getGeneratedKeys;
        Integer generateKeys;
        try {
            prepare = Connection.class.getMethod("prepareStatement", new Class[]{String.class, int.class});
            getGeneratedKeys = PreparedStatement.class.getMethod("getGeneratedKeys", null);
            Field f = PreparedStatement.class.getField("RETURN_GENERATED_KEYS");
            generateKeys = (Integer) f.get(PreparedStatement.class);
        } catch (Exception e) {
            prepare = null;
            getGeneratedKeys = null;
            generateKeys = null;
        }
        CONNECTION_PREPARE = prepare;
        GET_GENERATED_KEYS = getGeneratedKeys;
        GENERATE_KEYS = generateKeys;
    }

    public void init(JDBCStoreManager manager) {
        if (CONNECTION_PREPARE == null) {
            throw new RuntimeException("Create command requires JDBC 3.0 (JDK1.4+)");
        }
        super.init(manager);
    }

    protected PreparedStatement prepareStatement(Connection c, String sql, CmpEntityBeanContext ctx) throws SQLException {
        try {
            return (PreparedStatement) CONNECTION_PREPARE.invoke(c, new Object[]{sql, GENERATE_KEYS});
        } catch (Exception e) {
            throw processException(e);
        }
    }

    protected int executeInsert(int paramIndex, PreparedStatement ps, CmpEntityBeanContext ctx) throws SQLException {
        int rows = ps.executeUpdate();
        ResultSet rs = null;
        try {
            rs = (ResultSet) GET_GENERATED_KEYS.invoke(ps, null);
            if (!rs.next()) {
                // throw EJBException to force a rollback as the row has been inserted
                throw new EJBException("getGeneratedKeys returned an empty ResultSet");
            }
            pkField.loadInstanceResults(rs, 1, ctx);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // throw EJBException to force a rollback as the row has been inserted
            throw new EJBException("Error extracting generated keys", e);
        } finally {
            JDBCUtil.safeClose(rs);
        }
        return rows;
    }
}
