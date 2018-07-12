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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that @DataSourceDefinition works, and that the datasource is automatically enlisted in the transaction
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class DataSourceDefinitionTestCase {

    @Deployment
    public static Archive<?> deploy() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class,"testds.war");
        war.addClasses(DataSourceBean.class, EmbeddedDataSource.class);
        war.addAsManifestResource(new StringAsset("Dependencies: com.h2database.h2\n"),"MANIFEST.MF");
        return war;

    }

    @ArquillianResource
    private InitialContext ctx;

    @Before
    public void createTables() throws NamingException, SQLException {
        DataSourceBean bean = (DataSourceBean)ctx.lookup("java:module/" + DataSourceBean.class.getSimpleName());
        bean.createTable();
    }

    @Test
    public void testDataSourceDefinition() throws NamingException, SQLException {
        DataSourceBean bean = (DataSourceBean)ctx.lookup("java:module/" + DataSourceBean.class.getSimpleName());
        DataSource ds = bean.getDataSource();
        Connection c = ds.getConnection();
        ResultSet result = c.createStatement().executeQuery("select 1");
        Assert.assertTrue(result.next());
        c.close();
    }

    @Test
    public void testTransactionEnlistment() throws NamingException, SQLException {
        DataSourceBean bean = (DataSourceBean)ctx.lookup("java:module/" + DataSourceBean.class.getSimpleName());
        try {
            bean.insert1RolledBack();
            Assert.fail("expect exception");
        } catch (RuntimeException expected) {
        }
        DataSource ds = bean.getDataSource();
        Connection c = ds.getConnection();
        ResultSet result = c.createStatement().executeQuery("select id from coffee where id=1;");
        Assert.assertFalse(result.next());
        c.close();
    }

    @Test
    public void testTransactionEnlistment2() throws NamingException, SQLException {
        DataSourceBean bean = (DataSourceBean)ctx.lookup("java:module/" + DataSourceBean.class.getSimpleName());
        bean.insert2();
        DataSource ds = bean.getDataSource();
        Connection c = ds.getConnection();
        ResultSet result = c.createStatement().executeQuery("select id from coffee where id=2;");
        Assert.assertTrue(result.next());
        c.close();
    }

    @Test
    public void testResourceInjectionWithSameName() throws NamingException {
        DataSourceBean bean = (DataSourceBean)ctx.lookup("java:module/" + DataSourceBean.class.getSimpleName());
        Assert.assertNotNull(bean.getDataSource2());
        Assert.assertNotNull(bean.getDataSource3());
        Assert.assertNotNull(bean.getDataSource4());
    }

    /**
     * Tests an embedded datasource resource def.
     * @throws NamingException
     * @throws SQLException
     */
    @Test
    public void testEmbeddedDatasource() throws NamingException, SQLException {
        DataSourceBean bean = (DataSourceBean)ctx.lookup("java:module/" + DataSourceBean.class.getSimpleName());
        Assert.assertEquals(bean.getDataSource5().getConnection().nativeSQL("dse"),"dse");
    }
}
