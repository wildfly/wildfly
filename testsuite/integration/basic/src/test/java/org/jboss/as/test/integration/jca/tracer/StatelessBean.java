/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2016, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.jca.tracer;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.sql.DataSource;

import org.jboss.logging.Logger;

/**
 * @author Ondra Chaloupka <ochaloup@redhat.com>
 */
@Stateless
public class StatelessBean implements StatelessBeanRemote {
    private static final Logger log = Logger.getLogger(StatelessBean.class);

    private static final String table = "tracer_table";
    private static final String column = "id";

    @Resource
    private DataSource ds;

    public void insertToDB() {
        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", table, column, 1);
        executeUpdate(sql);
        log.infof("sql '%s' executed", sql);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void createTable() {
        String sql = String.format("CREATE TABLE IF NOT EXISTS %s (%s int)", table, column);
        executeUpdate(sql);
        log.infof("sql '%s' executed", sql);
    }

    private void executeUpdate(String sql) {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            Statement statement = conn.createStatement();
            statement.executeUpdate(sql);
        } catch (Exception e) {
            throw new RuntimeException("Can't run sql command '" + sql + "'", e);
        } finally {
            if(conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    // ignore
                }
            }
        }
    }
}
