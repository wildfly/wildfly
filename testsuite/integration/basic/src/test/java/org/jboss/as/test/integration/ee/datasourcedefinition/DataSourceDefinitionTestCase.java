/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.datasourcedefinition;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that @DataSourceDefinition works, and that the datasource is automatically enlisted in the transaction
 *
 * @author Stuart Douglas
 */
@ExtendWith(ArquillianExtension.class)
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

    @BeforeEach
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
        Assertions.assertTrue(result.next());
        c.close();
    }

    @Test
    public void testTransactionEnlistment() throws NamingException, SQLException {
        DataSourceBean bean = (DataSourceBean)ctx.lookup("java:module/" + DataSourceBean.class.getSimpleName());
        try {
            bean.insert1RolledBack();
            Assertions.fail("expect exception");
        } catch (RuntimeException expected) {
        }
        DataSource ds = bean.getDataSource();
        Connection c = ds.getConnection();
        ResultSet result = c.createStatement().executeQuery("select id from coffee where id=1;");
        Assertions.assertFalse(result.next());
        c.close();
    }

    @Test
    public void testTransactionEnlistment2() throws NamingException, SQLException {
        DataSourceBean bean = (DataSourceBean)ctx.lookup("java:module/" + DataSourceBean.class.getSimpleName());
        bean.insert2();
        DataSource ds = bean.getDataSource();
        Connection c = ds.getConnection();
        ResultSet result = c.createStatement().executeQuery("select id from coffee where id=2;");
        Assertions.assertTrue(result.next());
        c.close();
    }

    @Test
    public void testResourceInjectionWithSameName() throws NamingException {
        DataSourceBean bean = (DataSourceBean)ctx.lookup("java:module/" + DataSourceBean.class.getSimpleName());
        Assertions.assertNotNull(bean.getDataSource2());
        Assertions.assertNotNull(bean.getDataSource3());
        Assertions.assertNotNull(bean.getDataSource4());
    }

    /**
     * Tests an embedded datasource resource def.
     * @throws NamingException
     * @throws SQLException
     */
    @Test
    public void testEmbeddedDatasource() throws NamingException, SQLException {
        DataSourceBean bean = (DataSourceBean)ctx.lookup("java:module/" + DataSourceBean.class.getSimpleName());
        Assertions.assertEquals(bean.getDataSource5().getConnection().nativeSQL("dse"),"dse");
    }

    /**
     * Test if NullPointerException ir raised when connection is closed and then method connection.isWrapperFor(...) is called
     * See https://issues.jboss.org/browse/JBJCA-1389
     */
    @Test
    public void testCloseConnectionWrapperFor() {
        Throwable exception = assertThrows(SQLException.class, () -> {

            DataSourceBean bean = (DataSourceBean) ctx.lookup("java:module/" + DataSourceBean.class.getSimpleName());
            DataSource ds = bean.getDataSource();
            Connection c = ds.getConnection();
            c.close();
            try {
                c.isWrapperFor(ResultSet.class);
            } catch (NullPointerException e) {
                Assertions.fail("Wrong exception is raised. The exception should be Connection is not associated with a managed connection: ... See JBJCA-1389.");
            }
        });
        assertThat(exception.getMessage(), containsString("IJ031041"));
    }
}
