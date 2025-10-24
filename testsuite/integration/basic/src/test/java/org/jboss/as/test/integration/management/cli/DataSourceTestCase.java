/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.management.cli;

import java.net.URL;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import org.junit.Assert;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.JndiServlet;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DataSourceTestCase extends AbstractCliTestBase {

    @ArquillianResource URL url;

    private static final String[][] DS_PROPS = new String[][] {
        {"idle-timeout-minutes", "5"}
    };

    @Deployment
    public static Archive<?> getDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "DataSourceTestCase.war");
        war.addClass(DataSourceTestCase.class);
        war.addClass(JndiServlet.class);
        return war;
    }

    @BeforeClass
    public static void before() throws Exception {
        AbstractCliTestBase.initCLI(20000);
    }

    @AfterClass
    public static void after() throws Exception {
        AbstractCliTestBase.closeCLI();
    }

    @Test
    public void testDataSource() throws Exception {
        testAddDataSource();
        testModifyDataSource();
        testRemoveDataSource();
    }

    @Test
    public void testDataSourceConnectionProperties() throws Exception {
        testAddDataSourceConnectionProperties();
        testModifyDataSource();
        testRemoveDataSource();
    }

    @Test
    public void testXaDataSource() throws Exception {
        testAddXaDataSource();
        testModifyXaDataSource();
        testRemoveXaDataSource();
    }

    /**
     * https://issues.jboss.org/browse/WFLY-12161
     */
    @Test
    public void testSharedPreparedStatementsEpxr() throws Exception {
        cli.sendLine("/system-property=prop:add(value=true)");
        testAddDataSourceWithSharedPreparedStatementsEpxr();
        testRemoveDataSource();
        cli.sendLine("/system-property=prop:remove()");
    }

    /**
     * https://issues.jboss.org/browse/WFLY-12161
     */
    @Test
    public void testXaSharedPreparedStatementsEpxr() throws Exception {
        cli.sendLine("/system-property=prop:add(value=true)");
        testAddXaDataSourceWithSharedPreparedStatementsEpxr();
        testRemoveXaDataSource();
        cli.sendLine("/system-property=prop:remove()");
    }

    private void testAddDataSource() throws Exception {

        // add data source
        cli.sendLine("data-source add --name=TestDS --jndi-name=java:jboss/datasources/TestDS --driver-name=h2 --connection-url=jdbc:h2:mem:test;DB_CLOSE_DELAY=-1 --enabled=true");

        // check the data source is listed
        cli.sendLine("cd /subsystem=datasources/data-source");
        cli.sendLine("ls");
        String ls = cli.readOutput();
        assertTrue(ls.contains("TestDS"));

        // check that it is available through JNDI
        String jndiClass = JndiServlet.lookup(url.toString(), "java:jboss/datasources/TestDS");
        Assert.assertTrue(javax.sql.DataSource.class.isAssignableFrom(Class.forName(jndiClass)));

    }

    private void testAddDataSourceConnectionProperties() throws Exception {

        // add data source
        cli.sendLine("data-source add --name=TestDS --jndi-name=java:jboss/datasources/TestDS --driver-name=h2 --datasource-class=org.h2.jdbcx.JdbcDataSource --connection-properties={\"url\"=>\"jdbc:h2:mem:test;DB_CLOSE_DELAY=-1\"}");

        // check the data source is listed
        cli.sendLine("cd /subsystem=datasources/data-source");
        cli.sendLine("ls");
        String ls = cli.readOutput();
        assertTrue(ls.contains("TestDS"));

        // check that it is available through JNDI
        String jndiClass = JndiServlet.lookup(url.toString(), "java:jboss/datasources/TestDS");
        Assert.assertTrue(javax.sql.DataSource.class.isAssignableFrom(Class.forName(jndiClass)));

    }

    private void testRemoveDataSource() throws Exception {

        // remove data source
        cli.sendLine("data-source remove --name=TestDS");
//        cli.sendLine("/subsystem=datasources/data-source=TestDS:remove()");
        cli.sendLine("reload");

        //check the data source is not listed
        cli.sendLine("cd /subsystem=datasources/data-source");
        cli.sendLine("ls");
        String ls = cli.readOutput();
        assertFalse(ls.contains("java:jboss/datasources/TestDS"));

        // check that it is not available through JNDI
        String jndiClass = JndiServlet.lookup(url.toString(), "java:jboss/datasources/TestDS");
        Assert.assertEquals(JndiServlet.NOT_FOUND, jndiClass);

    }

    private void testModifyDataSource() throws Exception {
        StringBuilder cmd = new StringBuilder("data-source --name=TestDS");
        for (String[] props : DS_PROPS) {
            cmd.append(" --");
            cmd.append(props[0]);
            cmd.append("=");
            cmd.append(props[1]);
        }
        cli.sendLine(cmd.toString());

        // check that datasource was modified
        cli.sendLine("/subsystem=datasources/data-source=TestDS:read-resource(recursive=true)");
        CLIOpResult result = cli.readAllAsOpResult();
        assertTrue(result.getResult().toString(), result.isIsOutcomeSuccess());
        assertTrue(result.getResult() instanceof Map);
        Map dsProps = (Map) result.getResult();
        for (String[] props : DS_PROPS) assertTrue(dsProps.get(props[0]).equals(props[1]));

    }

    private void testAddXaDataSource() throws Exception {

        // add data source
        cli.sendLine("xa-data-source add --name=TestXADS --jndi-name=java:jboss/datasources/TestXADS --driver-name=h2 --xa-datasource-properties={\"url\"=>\"jdbc:h2:mem:test\"}");

        //check the data source is listed
        cli.sendLine("cd /subsystem=datasources/xa-data-source");
        cli.sendLine("ls");
        String ls = cli.readOutput();
        assertTrue(ls.contains("TestXADS"));

        // check that it is available through JNDI
        String jndiClass = JndiServlet.lookup(url.toString(), "java:jboss/datasources/TestXADS");
        Assert.assertTrue(javax.sql.DataSource.class.isAssignableFrom(Class.forName(jndiClass)));


    }

    private void testRemoveXaDataSource() throws Exception {

        // remove data source
        cli.sendLine("xa-data-source remove --name=TestXADS");
        cli.sendLine("reload");

        //check the data source is not listed
        cli.sendLine("cd /subsystem=datasources/xa-data-source");
        cli.sendLine("ls");
        String ls = cli.readOutput();
        Assert.assertNull(ls);

        // check that it is no more available through JNDI
        String jndiClass = JndiServlet.lookup(url.toString(), "java:jboss/datasources/TestXADS");
        Assert.assertEquals(JndiServlet.NOT_FOUND, jndiClass);

    }

    private void testModifyXaDataSource() throws Exception {
        StringBuilder cmd = new StringBuilder("xa-data-source --name=TestXADS");
        for (String[] props : DS_PROPS) {
            cmd.append(" --");
            cmd.append(props[0]);
            cmd.append("=");
            cmd.append(props[1]);
        }
        cli.sendLine(cmd.toString());

        // check that datasource was modified
        cli.sendLine("/subsystem=datasources/xa-data-source=TestXADS:read-resource(recursive=true)");
        CLIOpResult result = cli.readAllAsOpResult();
        assertTrue(result.isIsOutcomeSuccess());
        assertTrue(result.getResult() instanceof Map);
        Map<String,Object> dsProps = (Map<String, Object>) result.getResult();
        for (String[] props : DS_PROPS) assertTrue(dsProps.get(props[0]).equals(props[1]));

    }

    private void testAddDataSourceWithSharedPreparedStatementsEpxr() throws Exception {

        // add data source
        cli.sendLine("data-source add --name=TestDS --jndi-name=java:jboss/datasources/TestDS --driver-name=h2" +
                " --datasource-class=org.h2.jdbcx.JdbcDataSource --connection-properties={\"url\"=>\"jdbc:h2:mem:test;DB_CLOSE_DELAY=-1\"}");

        // check the data source is listed
        cli.sendLine("cd /subsystem=datasources/data-source");
        cli.sendLine("ls");
        String ls = cli.readOutput();
        assertTrue(ls.contains("TestDS"));

    }

    private void testAddXaDataSourceWithSharedPreparedStatementsEpxr() throws Exception {

        // add data source
        cli.sendLine("xa-data-source add --name=TestXADS --jndi-name=java:jboss/datasources/TestXADS --driver-name=h2" +
                " --xa-datasource-properties={\"url\"=>\"jdbc:h2:mem:test\"} --share-prepared-statements=${prop}");

        //check the data source is listed
        cli.sendLine("cd /subsystem=datasources/xa-data-source");
        cli.sendLine("ls");
        String ls = cli.readOutput();
        assertTrue(ls.contains("TestXADS"));

    }

}
