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
public class RepeatableDataSourceDefinitionTestCase {

    @Deployment
    public static Archive<?> deploy() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class,"testds.war");
        war.addClasses(RepeatedAnnotationDataSourceBean.class, EmbeddedDataSource.class);
        war.addAsManifestResource(new StringAsset("Dependencies: com.h2database.h2\n"),"MANIFEST.MF");
        return war;

    }

    @ArquillianResource
    private InitialContext ctx;

    @BeforeEach
    public void createTables() throws NamingException, SQLException {
        RepeatedAnnotationDataSourceBean bean = lookup();
        bean.createTable();
    }

    @Test
    public void testDataSourceDefinition() throws NamingException, SQLException {
        RepeatedAnnotationDataSourceBean bean = lookup();
        DataSource ds = bean.getDataSource();
        Connection c = ds.getConnection();
        ResultSet result = c.createStatement().executeQuery("select 1");
        Assertions.assertTrue(result.next());
        c.close();
    }

    @Test
    public void testTransactionEnlistment() throws NamingException, SQLException {
        RepeatedAnnotationDataSourceBean bean = lookup();
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
        RepeatedAnnotationDataSourceBean bean = lookup();
        bean.insert2();
        DataSource ds = bean.getDataSource();
        Connection c = ds.getConnection();
        ResultSet result = c.createStatement().executeQuery("select id from coffee where id=2;");
        Assertions.assertTrue(result.next());
        c.close();
    }

    @Test
    public void testResourceInjectionWithSameName() throws NamingException {
        RepeatedAnnotationDataSourceBean bean = lookup();
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
        RepeatedAnnotationDataSourceBean bean = lookup();
        Assertions.assertEquals(bean.getDataSource5().getConnection().nativeSQL("dse"),"dse");
    }

    private RepeatedAnnotationDataSourceBean lookup() throws NamingException {
        return (RepeatedAnnotationDataSourceBean) ctx.lookup("java:module/" + RepeatedAnnotationDataSourceBean.class.getSimpleName());
    }
}
