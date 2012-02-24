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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.jboss.as.cmp.CmpMessages;
import org.jboss.as.cmp.context.CmpEntityBeanContext;
import org.jboss.as.cmp.jdbc.JDBCIdentityColumnCreateCommand;
import org.jboss.as.cmp.jdbc.JDBCStoreManager;
import org.jboss.as.cmp.jdbc.JDBCUtil;
import org.jboss.as.cmp.jdbc.SQLUtil;
import org.jboss.as.cmp.jdbc.metadata.JDBCEntityCommandMetaData;

/**
 * Create command for use with Oracle that uses a sequence in conjunction with
 * a RETURNING clause to generate keys in a single statement
 *
 * @author <a href="mailto:jeremy@boynes.com">Jeremy Boynes</a>
 * @version $Revision: 81030 $
 */
public class JDBCOracleCreateCommand extends JDBCIdentityColumnCreateCommand {
    private String sequence;
    private int jdbcType;

    public void init(JDBCStoreManager manager) {
        super.init(manager);
    }

    protected void initEntityCommand(JDBCEntityCommandMetaData entityCommand) {
        super.initEntityCommand(entityCommand);
        sequence = entityCommand.getAttribute("sequence");
        if (sequence == null) {
            throw CmpMessages.MESSAGES.sequenceMustBeSpecified();
        }
    }

    protected void initInsertSQL() {
        jdbcType = pkField.getJDBCType().getJDBCTypes()[0];

        StringBuffer sql = new StringBuffer();
        sql.append("{call INSERT INTO ").append(entity.getQualifiedTableName());
        sql.append(" (");
        SQLUtil.getColumnNamesClause(pkField, sql)
                .append(", ");

        SQLUtil.getColumnNamesClause(insertFields, sql);

        sql.append(")");
        sql.append(" VALUES (");
        sql.append(sequence + ".NEXTVAL, ");
        SQLUtil.getValuesClause(insertFields, sql);
        sql.append(")");
        sql.append(" RETURNING ");
        SQLUtil.getColumnNamesClause(pkField, sql)
                .append(" INTO ? }");
        insertSQL = sql.toString();
        if (debug) {
            log.debug("Insert Entity SQL: " + insertSQL);
        }
    }

    protected PreparedStatement prepareStatement(Connection c, String sql, CmpEntityBeanContext ctx) throws SQLException {
        return c.prepareCall(sql);
    }

    protected int executeInsert(int paramIndex, PreparedStatement ps, CmpEntityBeanContext ctx) throws SQLException {
        CallableStatement cs = (CallableStatement) ps;
        cs.registerOutParameter(paramIndex, jdbcType);
        cs.execute();
        Object pk = JDBCUtil.getParameter(log, cs, paramIndex, jdbcType, pkField.getFieldType());
        pkField.setInstanceValue(ctx, pk);
        return 1;
    }
}
