/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

/**
 * Tests that @DataSourceDefinition works, and that the datasource is automatically enlisted in the transaction
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class DataSourceDefinitionTestCase {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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

    /**
     * Test if NullPointerException ir raised when connection is closed and then method connection.isWrapperFor(...) is called
     * See https://issues.jboss.org/browse/JBJCA-1389
     */
    @Test
    public void testCloseConnectionWrapperFor() throws NamingException, SQLException {
        expectedException.expect(SQLException.class);
        expectedException.expectMessage("IJ031041");

        DataSourceBean bean = (DataSourceBean)ctx.lookup("java:module/" + DataSourceBean.class.getSimpleName());
        DataSource ds = bean.getDataSource();
        Connection c = ds.getConnection();
        c.close();
        try {
            c.isWrapperFor(ResultSet.class);
        } catch (NullPointerException e) {
            Assert.fail("Wrong exception is raised. The exception should be Connection is not associated with a managed connection: ... See JBJCA-1389.");
        }
    }
}
