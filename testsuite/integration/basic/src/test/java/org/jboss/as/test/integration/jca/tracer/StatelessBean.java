/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.tracer;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
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
        log.debugf("sql '%s' executed", sql);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void createTable() {
        String sql = String.format("CREATE TABLE IF NOT EXISTS %s (%s int)", table, column);
        executeUpdate(sql);
        log.debugf("sql '%s' executed", sql);
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
