/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jboss.as.test.integration.domain.management.cli;

import org.jboss.as.test.integration.domain.suites.CLITestSuite;
import java.util.Map;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
public class DataSourceTestCase extends AbstractCliTestBase {

    private static String[] profileNames;

    private static final String[][] DS_PROPS = new String[][] {
        {"idle-timeout-minutes", "5"}
    };

    @BeforeClass
    public static void before() throws Exception {
        AbstractCliTestBase.initCLI();
    }

    @AfterClass
    public static void after() throws Exception {
        AbstractCliTestBase.closeCLI();
    }

    @Before
    public void init() {
         profileNames = CLITestSuite.serverProfiles.keySet().toArray(new String[] {});
    }

    @Test
    public void testDataSource() throws Exception {
        testAddDataSource();
        testModifyDataSource();
        testRemoveDataSource();
    }

    @Test
    public void testXaDataSource() throws Exception {
        testAddXaDataSource();
        testModifyXaDataSource();
        testRemoveXaDataSource();
    }

    private void testAddDataSource() throws Exception {

        // add data source
        cli.sendLine("data-source add --profile=" + profileNames[0] + " --jndi-name=java:jboss/datasources/TestDS --name=java:jboss/datasources/TestDS --driver-name=h2 --connection-url=jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");

        // check the data source is listed
        cli.sendLine("cd /profile=" + profileNames[0] + "/subsystem=datasources/data-source");
        cli.sendLine("ls");
        String ls = cli.readAllUnformated(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
        assertTrue("Datasource not found: " + ls, ls.contains("java:jboss/datasources/TestDS"));

        // check that it is available through JNDI
        // TODO implement when @ArquillianResource InitialContext is done

    }

    private void testRemoveDataSource() throws Exception {

        // remove data source
        cli.sendLine("data-source remove --profile=" + profileNames[0] + " --name=java:jboss/datasources/TestDS");

        //check the data source is not listed
        cli.sendLine("cd /profile=" + profileNames[0] + "/subsystem=datasources/data-source");
        cli.sendLine("ls");
        String ls = cli.readAllUnformated(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
        assertFalse(ls.contains("java:jboss/datasources/TestDS"));

    }

    private void testModifyDataSource() throws Exception {
        StringBuilder cmd = new StringBuilder("data-source --profile=" + profileNames[0] + " --name=java:jboss/datasources/TestDS");
        for (String[] props : DS_PROPS) {
            cmd.append(" --");
            cmd.append(props[0]);
            cmd.append("=");
            cmd.append(props[1]);
        }
        cli.sendLine(cmd.toString());

        // check that datasource was modified
        cli.sendLine("/profile=" + profileNames[0] + "/subsystem=datasources/data-source=java\\:jboss\\/datasources\\/TestDS:read-resource(recursive=true)");
        CLIOpResult result = cli.readAllAsOpResult(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
        assertTrue(result.isIsOutcomeSuccess());
        assertTrue(result.getResult() instanceof Map);
        Map dsProps = (Map) result.getResult();
        for (String[] props : DS_PROPS) assertTrue(dsProps.get(props[0]).equals(props[1]));

    }

    private void testAddXaDataSource() throws Exception {

        // add data source
        cli.sendLine("xa-data-source add --profile=" + profileNames[0] +
                " --jndi-name=java:jboss/datasources/TestXADS --name=java:jboss/datasources/TestXADS --driver-name=h2");

        //check the data source is listed
        cli.sendLine("cd /profile=" + profileNames[0] + "/subsystem=datasources/xa-data-source");
        cli.sendLine("ls");
        String ls = cli.readAllUnformated(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
        assertTrue(ls.contains("java:jboss/datasources/TestXADS"));

    }

    private void testModifyXaDataSource() throws Exception {
        StringBuilder cmd = new StringBuilder("xa-data-source --profile=" + profileNames[0] + " --name=java:jboss/datasources/TestXADS");
        for (String[] props : DS_PROPS) {
            cmd.append(" --");
            cmd.append(props[0]);
            cmd.append("=");
            cmd.append(props[1]);
        }
        cli.sendLine(cmd.toString());

        // check that datasource was modified
        cli.sendLine("/profile=" + profileNames[0] + "/subsystem=datasources/xa-data-source=java\\:jboss\\/datasources\\/TestXADS:read-resource(recursive=true)");
        CLIOpResult result = cli.readAllAsOpResult(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
        assertTrue(result.isIsOutcomeSuccess());
        assertTrue(result.getResult() instanceof Map);
        Map dsProps = (Map) result.getResult();
        for (String[] props : DS_PROPS) assertTrue(dsProps.get(props[0]).equals(props[1]));

    }

    private void testRemoveXaDataSource() throws Exception {

        // remove data source
        cli.sendLine("xa-data-source remove  --profile=" + profileNames[0] + " --name=java:jboss/datasources/TestXADS");

        //check the data source is not listed
        cli.sendLine("cd /profile=" + profileNames[0] + "/subsystem=datasources/xa-data-source");
        cli.sendLine("ls");
        String ls = cli.readAllUnformated(WAIT_LINETIMEOUT, WAIT_LINETIMEOUT);
        assertFalse(ls.contains("java:jboss/datasources/TestXADS"));

    }

}
