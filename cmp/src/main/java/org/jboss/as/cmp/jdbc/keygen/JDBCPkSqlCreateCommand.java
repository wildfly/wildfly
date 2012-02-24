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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.ejb.CreateException;
import javax.sql.DataSource;
import org.jboss.as.cmp.CmpMessages;
import org.jboss.as.cmp.context.CmpEntityBeanContext;
import org.jboss.as.cmp.jdbc.JDBCInsertPKCreateCommand;
import org.jboss.as.cmp.jdbc.JDBCStoreManager;
import org.jboss.as.cmp.jdbc.JDBCUtil;
import org.jboss.as.cmp.jdbc.bridge.JDBCCMPFieldBridge;
import org.jboss.as.cmp.jdbc.metadata.JDBCEntityCommandMetaData;

/**
 * Create command that uses an SQL statement to generate the primary key.
 * Typically used with databases that support sequences.
 *
 * @author <a href="mailto:loubyansky@hotmail.com">Alex Loubyansky</a>
 * @version $Revision: 81030 $
 */
public class JDBCPkSqlCreateCommand extends JDBCInsertPKCreateCommand {
    protected String pkSQL;
    protected JDBCCMPFieldBridge pkField;

    public void init(JDBCStoreManager manager) {
        super.init(manager);
        pkField = getGeneratedPKField();
    }

    protected void initEntityCommand(JDBCEntityCommandMetaData entityCommand) {
        super.initEntityCommand(entityCommand);

        pkSQL = entityCommand.getAttribute("pk-sql");
        if (pkSQL == null) {
            throw CmpMessages.MESSAGES.pkSqlMustBeSet(entity.getEntityName());
        }
        if (debug) {
            log.debug("Generate PK sql is: " + pkSQL);
        }
    }

    protected void generateFields(CmpEntityBeanContext ctx) throws CreateException {
        super.generateFields(ctx);

        Connection con = null;
        Statement s = null;
        ResultSet rs = null;
        try {
            if (debug) {
                log.debug("Executing SQL: " + pkSQL);
            }

            DataSource dataSource = entity.getDataSource();
            con = dataSource.getConnection();
            s = con.createStatement();

            rs = s.executeQuery(pkSQL);
            if (!rs.next()) {
                throw CmpMessages.MESSAGES.errorFetchingNextPk();
            }
            pkField.loadInstanceResults(rs, 1, ctx);
        } catch (SQLException e) {
            throw CmpMessages.MESSAGES.errorFetchingPkValue(e);
        } finally {
            JDBCUtil.safeClose(rs);
            JDBCUtil.safeClose(s);
            JDBCUtil.safeClose(con);
        }
    }
}
