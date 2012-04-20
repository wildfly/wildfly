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

import javax.annotation.Resource;
import javax.annotation.sql.DataSourceDefinition;
import javax.ejb.Stateless;
import javax.sql.DataSource;

import java.sql.SQLException;

/**
 * @author Stuart Douglas
 */
@DataSourceDefinition(
        name = "java:comp/ds",
        user = "sa",
        password = "sa",
        className = "org.h2.jdbcx.JdbcDataSource",
        url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
)
@Stateless
public class DataSourceBean {

    @Resource(lookup = "java:comp/ds", name="java:app/DataSource")
    private DataSource dataSource;

    /**
     * This should be injected with the same datasource as above, as they both have the same name
     */
    @Resource(name="java:comp/ds")
    private DataSource dataSource2;

    @Resource(lookup = "java:app/DataSource")
    private DataSource dataSource3;

    @Resource(name="org.jboss.as.test.integration.ee.datasourcedefinition.DataSourceBean/dataSource3")
    private DataSource dataSource4;


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
}
