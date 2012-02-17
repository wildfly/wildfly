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
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.ejb.CreateException;
import javax.ejb.DuplicateKeyException;
import org.jboss.as.cmp.context.CmpEntityBeanContext;

/**
 * Base class for create commands that actually insert the primary key value.
 * If an exception processor is not supplied, this command will perform an
 * additional query to determine if a DuplicateKeyException should be thrown.
 *
 * @author <a href="mailto:jeremy@boynes.com">Jeremy Boynes</a>
 */
public abstract class JDBCInsertPKCreateCommand extends JDBCAbstractCreateCommand {
    protected String existsSQL;

    public void init(JDBCStoreManager manager) {
        super.init(manager);

        // if no exception processor is defined, we will perform a existence
        // check before trying the insert to report duplicate key
        if (exceptionProcessor == null) {
            initExistsSQL();
        }
    }

    protected void initExistsSQL() {
        StringBuffer sql = new StringBuffer(300);
        sql.append(SQLUtil.SELECT).append("COUNT(*)").append(SQLUtil.FROM)
                .append(entity.getQualifiedTableName())
                .append(SQLUtil.WHERE);
        SQLUtil.getWhereClause(entity.getPrimaryKeyFields(), sql);
        existsSQL = sql.toString();
        if (debug) {
            log.debug("Entity Exists SQL: " + existsSQL);
        }
    }

    protected void beforeInsert(CmpEntityBeanContext ctx) throws CreateException {
        // are we checking existence by query?
        if (existsSQL != null) {
            Connection c = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                if (debug)
                    log.debug("Executing SQL: " + existsSQL);

                c = entity.getDataSource().getConnection();
                ps = c.prepareStatement(existsSQL);

                // bind PK
                // @todo add a method to EntityBridge that binds pk fields directly
                Object pk = entity.extractPrimaryKeyFromInstance(ctx);
                entity.setPrimaryKeyParameters(ps, 1, pk);

                rs = ps.executeQuery();
                if (!rs.next()) {
                    throw new CreateException("Error checking if entity with primary pk " + pk + "exists: SQL returned no rows");
                }
                if (rs.getInt(1) > 0) {
                    throw new DuplicateKeyException("Entity with primary key " + pk + " already exists");
                }
            } catch (SQLException e) {
                log.error("Error checking if entity exists", e);
                throw new CreateException("Error checking if entity exists:" + e);
            } finally {
                JDBCUtil.safeClose(rs);
                JDBCUtil.safeClose(ps);
                JDBCUtil.safeClose(c);
            }
        }
    }
}
