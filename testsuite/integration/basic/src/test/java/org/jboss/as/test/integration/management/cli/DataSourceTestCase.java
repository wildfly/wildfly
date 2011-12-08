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
package org.jboss.as.test.integration.management.cli;

import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * TODO add/remove/modify-data-source commands are deprecated and shouldn't be used
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DataSourceTestCase extends AbstractCliTestBase {

    private static final String[][] DS_PROPS = new String[][] {
        {"idle-timeout-minutes", "5"}
    };

    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "dummy.jar");
        ja.addClass(DataSourceTestCase.class);
        return ja;
    }

    @Test
    public void testDataSource() throws Exception {
        testAddDataSource();
        testModifyDataSource();
        testRemoveDataSource();
    }

    @Test
    public void testXaDataSource() throws Exception {
        /*
        testAddXaDataSource();
        testModifyXaDataSource();
        testRemoveXaDataSource();
         */
    }

    private void testAddDataSource() throws Exception {

        // add data source
        cli.sendLine("add-data-source --jndi-name=java:jboss/datasources/TestDS --driver-name=h2 --pool-name=TestDS --connection-url=jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");

        // check the data source is listed
        cli.sendLine("cd /subsystem=datasources/data-source");
        cli.sendLine("ls");
        String ls = cli.readAllUnformated(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
        assertTrue(ls.contains("java:jboss/datasources/TestDS"));

        // check that it is available through JNDI
        // TODO implement when @ArquillianResource InitialContext is done

    }

    private void testRemoveDataSource() throws Exception {

        // remove data source
        cli.sendLine("remove-data-source --name=java:jboss/datasources/TestDS");

        //check the data source is not listed
        cli.sendLine("cd /subsystem=datasources/data-source");
        cli.sendLine("ls");
        String ls = cli.readAllUnformated(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
        assertFalse(ls.contains("java:jboss/datasources/TestDS"));

    }

    private void testModifyDataSource() throws Exception {
        StringBuilder cmd = new StringBuilder("modify-data-source --jndi-name=java:jboss/datasources/TestDS");
        for (String[] props : DS_PROPS) {
            cmd.append(" --");
            cmd.append(props[0]);
            cmd.append("=");
            cmd.append(props[1]);
        }
        cli.sendLine(cmd.toString());

        // check that datasource was modified
        cli.sendLine("/subsystem=datasources/data-source=java\\:jboss\\/datasources\\/TestDS:read-resource(recursive=true)");       
        CLIOpResult result = cli.readAllAsOpResult(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
        assertTrue(result.isIsOutcomeSuccess());
        assertTrue(result.getResult() instanceof Map);
        Map dsProps = (Map) result.getResult();
        for (String[] props : DS_PROPS) assertTrue(dsProps.get(props[0]).equals(props[1]));

    }

    private void testAddXaDataSource() throws Exception {

        // add data source
        cli.sendLine("add-data-source --jndi-name=java:jboss/datasources/TestDS --driver-name=h2 --pool-name=TestDS --connection-url=jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");

        //check the data source is listed
        cli.sendLine("cd /subsystem=datasources/data-source");
        cli.sendLine("ls");
        String ls = cli.readAllUnformated(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
        assertTrue(ls.contains("java:jboss/datasources/TestDS"));

    }

    private void testRemoveXaDataSource() throws Exception {

        // remove data source
        cli.sendLine("remove-data-source --name=java:jboss/datasources/TestDS");

        //check the data source is not listed
        cli.sendLine("cd /subsystem=datasources/data-source");
        cli.sendLine("ls");
        String ls = cli.readAllUnformated(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
        assertFalse(ls.contains("java:jboss/datasources/TestDS"));

    }

    private void testModifyXaDataSource() throws Exception {
        StringBuilder cmd = new StringBuilder("modify-data-source --jndi-name=java:jboss/datasources/TestDS");
        for (String[] props : DS_PROPS) {
            cmd.append(" --");
            cmd.append(props[0]);
            cmd.append("=");
            cmd.append(props[1]);
        }
        cli.sendLine(cmd.toString());

        // check that datasource was modified
        cli.sendLine("/subsystem=datasources/data-source=java\\:jboss\\/datasources\\/TestDS:read-resource(recursive=true)");       
        CLIOpResult result = cli.readAllAsOpResult(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
        assertTrue(result.isIsOutcomeSuccess());
        assertTrue(result.getResult() instanceof Map);
        Map dsProps = (Map) result.getResult();
        for (String[] props : DS_PROPS) assertTrue(dsProps.get(props[0]).equals(props[1]));

    }

}
