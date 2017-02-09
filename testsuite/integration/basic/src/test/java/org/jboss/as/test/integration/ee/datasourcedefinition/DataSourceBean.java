/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.ee.datasourcedefinition;

import java.sql.SQLException;
import javax.annotation.Resource;
import javax.annotation.sql.DataSourceDefinition;
import javax.annotation.sql.DataSourceDefinitions;
import javax.ejb.Stateless;
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
