/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jpa.resourcelocal;

import jakarta.annotation.Resource;
import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.ejb.Stateless;
import javax.sql.DataSource;

/**
 * @author Stuart Douglas
 */
@DataSourceDefinition(
        name = "java:app/DataSource",
        user = "sa",
        password = "sa",
        className = "org.h2.jdbcx.JdbcDataSource",
        url = "jdbc:h2:mem:test",
        transactional = false
)
@Stateless
public class DataSourceBean {

    @Resource(lookup = "java:app/DataSource")
    private DataSource dataSource;

    public DataSource getDataSource() {
        return dataSource;
    }
}
