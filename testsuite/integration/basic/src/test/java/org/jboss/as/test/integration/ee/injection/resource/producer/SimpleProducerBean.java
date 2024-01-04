/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.resource.producer;

import java.sql.Connection;
import java.sql.SQLException;

import jakarta.annotation.ManagedBean;
import jakarta.annotation.Resource;
import jakarta.enterprise.inject.Produces;
import javax.sql.DataSource;

/**
 * @author Thomas.Diesler@jboss.com
 * @since 09-Jul-2012
 */
@ManagedBean
public class SimpleProducerBean {

    @Resource(lookup = "java:jboss/datasources/ExampleDS")
    DataSource dataSource;

    @Produces
    public String getDriverName() {
        try {
            Connection con = dataSource.getConnection();
            try {
                return con.getMetaData().getDriverName();
            } finally {
                con.close();
            }
        } catch (SQLException ex) {
            return ex.toString();
        }
    }
}
