/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.datasourcedefinition;

import java.sql.SQLException;
import jakarta.annotation.Resource;
import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.annotation.sql.DataSourceDefinitions;
import jakarta.ejb.Stateless;
import javax.sql.DataSource;

/**
 * @author Stuart Douglas
 */
@DataSourceDefinitions({
        @DataSourceDefinition(
                name = "java:comp/ds",
                user = "sa",
                password = "sa",
                className = "org.h2.jdbcx.JdbcDataSource",
                url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
        ),
        @DataSourceDefinition(
                name = "java:comp/dse",
                className = "org.jboss.as.test.integration.ee.datasourcedefinition.EmbeddedDataSource",
                url = "jdbc:embedded:/some/url"
        )
}
)
@Stateless
public class DataSourceBean {

    @Resource(lookup = "java:comp/ds", name="java:app/DataSource")
    private DataSource dataSource;

    /**
     * This should be injected with the same datasource as above, as they both have the same name
     */
    @Resource(lookup="java:comp/ds")
    private DataSource dataSource2;

    @Resource(lookup = "java:app/DataSource")
    private DataSource dataSource3;

    @Resource(lookup="org.jboss.as.test.integration.ee.datasourcedefinition.DataSourceBean/dataSource3")
    private DataSource dataSource4;

    @Resource(lookup="java:comp/dse")
    private DataSource dataSource5;

    public void createTable() throws SQLException {
        dataSource.getConnection().createStatement().execute("create table if not exists coffee(id int not null);");
    }

    public void insert1RolledBack() throws SQLException {
        dataSource.getConnection().createStatement().execute("insert into coffee values (1)");
        throw new RuntimeException("roll back");
    }

    public void insert2() throws SQLException {
        dataSource.getConnection().createStatement().execute("insert into coffee values (2)");
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public DataSource getDataSource2() {
        return dataSource2;
    }

    public DataSource getDataSource3() {
        return dataSource3;
    }

    public DataSource getDataSource4() {
        return dataSource4;
    }

    public DataSource getDataSource5() {
        return dataSource5;
    }
}
