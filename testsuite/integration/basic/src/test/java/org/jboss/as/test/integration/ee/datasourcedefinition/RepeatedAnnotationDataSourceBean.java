/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.integration.ee.datasourcedefinition;

import java.sql.SQLException;
import javax.annotation.Resource;
import javax.annotation.sql.DataSourceDefinition;
import javax.ejb.Stateless;
import javax.sql.DataSource;

/**
 * @author Stuart Douglas
 */
@DataSourceDefinition(
        name = "java:comp/ds",
        user = "sa",
        password = "sa",
        className = "org.h2.jdbcx.JdbcDataSource",
        url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
)
@DataSourceDefinition(
        name = "java:comp/dse",
        className = "org.jboss.as.test.integration.ee.datasourcedefinition.EmbeddedDataSource",
        url = "jdbc:embedded:/some/url"
)
@Stateless
public class RepeatedAnnotationDataSourceBean {

    @Resource(lookup = "java:comp/ds", name = "java:app/DataSource")
    private DataSource dataSource;

    /**
     * This should be injected with the same datasource as above, as they both have the same name
     */
    @Resource(lookup = "java:comp/ds")
    private DataSource dataSource2;

    @Resource(lookup = "java:app/DataSource")
    private DataSource dataSource3;

    @Resource(lookup = "org.jboss.as.test.integration.ee.datasourcedefinition.RepeatedAnnotationDataSourceBean/dataSource3")
    private DataSource dataSource4;

    @Resource(lookup = "java:comp/dse")
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
