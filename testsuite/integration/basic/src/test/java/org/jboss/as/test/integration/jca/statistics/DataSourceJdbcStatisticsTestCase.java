/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.jca.statistics;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.annotation.Resource;
import javax.sql.DataSource;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.jca.JcaMgmtBase;
import org.jboss.as.test.integration.jca.JcaMgmtServerSetupTask;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * JBQA-6456 Test jdbc statistics of data sources
 *
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 */

@RunWith(Arquillian.class)
@ServerSetup(DataSourceJdbcStatisticsTestCase.TestCaseSetup.class)
public class DataSourceJdbcStatisticsTestCase {

    static final String jndiDs = "java:jboss/datasources/StatDS";
    static final String jndiXaDs = "java:jboss/datasources/StatXaDS";

    static class TestCaseSetup extends JcaMgmtServerSetupTask {

        ModelNode dsAddress;
        ModelNode dsXaAddress;

        @Override
        public void doSetup(final ManagementClient managementClient) throws Exception {
            try {
                dsAddress = createDataSource(false, jndiDs);
                dsXaAddress = createDataSource(true, jndiXaDs);
                StringBuffer sb = cleanStats(dsAddress).append(cleanStats(dsXaAddress));
                if (sb.length() > 0) { fail(sb.toString()); }
            } catch (Throwable e) {
                removeDss();
                throw new Exception(e);
            }
        }

        public void removeDss() {
            try {
                remove(dsAddress);
                reload();
            } catch (Throwable e) {

            }
            try {
                remove(dsXaAddress);
                reload();
            } catch (Throwable e) {

            }
        }

        /**
         * Creates data source and return its node address
         *
         * @param xa       - should be data source XA?
         * @param jndiName of data source
         * @return ModelNode - address of data source node
         * @throws Exception
         */
        private ModelNode createDataSource(boolean xa, String jndiName) throws Exception {
            ModelNode address = new ModelNode();
            address.add(SUBSYSTEM, "datasources");
            address.add((xa ? "xa-" : "") + "data-source", jndiName);
            address.protect();

            ModelNode operation = new ModelNode();
            operation.get(OP).set(ADD);
            operation.get(OP_ADDR).set(address);
            operation.get("jndi-name").set(jndiName);
            operation.get("driver-name").set("h2");
            operation.get("enabled").set("false");
            if (!xa) { operation.get("connection-url").set("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"); }
            operation.get("prepared-statements-cache-size").set(3);
            operation.get("user-name").set("sa");
            operation.get("password").set("sa");

            executeOperation(operation);

            if (xa) {
                final ModelNode xaDatasourcePropertiesAddress = address.clone();
                xaDatasourcePropertiesAddress.add("xa-datasource-properties", "URL");
                xaDatasourcePropertiesAddress.protect();
                final ModelNode xaDatasourcePropertyOperation = new ModelNode();
                xaDatasourcePropertyOperation.get(OP).set("add");
                xaDatasourcePropertyOperation.get(OP_ADDR).set(xaDatasourcePropertiesAddress);
                xaDatasourcePropertyOperation.get("value").set("jdbc:h2:mem:test");

                executeOperation(xaDatasourcePropertyOperation);
            }

            operation = new ModelNode();
            operation.get(OP).set("write-attribute");
            operation.get("name").set("enabled");
            operation.get("value").set(true);
            operation.get(OP_ADDR).set(address);

            executeOperation(operation);
            reload();
            return address;
        }

        /**
         * Cleans jdbc statistics of data source node
         *
         * @param ModelNode address of data source node
         * @return StringBuffer, contains error message, if operation fails
         * @throws Exception
         */
        public StringBuffer cleanStats(ModelNode address) throws Exception {
            ModelNode statAddress = getStatAddr(address);
            executeOnNode(address, "flush-all-connection-in-pool");
            executeOnNode(statAddress, "clear-statistics");
            return assertStatisticsSet(false, statAddress);
        }

        /**
         * Checks, if some parameters are set on data source statistics node
         *
         * @param yes           - should be parameters set?
         * @param statisticNode - address, where to check
         * @return StringBuffer, contains error message, if operation fails
         * @throws Exception
         */
        public StringBuffer assertStatisticsSet(boolean yes, ModelNode statisticNode) throws Exception {
            StringBuffer sb = new StringBuffer();
            String[] params = {"PreparedStatementCacheAccessCount", // The number of times that the statement cache was
                    // accessed
                    "PreparedStatementCacheAddCount", // The number of statements added to the statement cache
                    "PreparedStatementCacheCurrentSize", // The number of prepared and callable statements currently cached in
                    // the statement cache
                    "PreparedStatementCacheDeleteCount", // The number of statements discarded from the cache
                    "PreparedStatementCacheHitCount" // The number of times that statements from the cache were used

            };
            for (String param : params) {
                if ((getStatisticsAttribute(param, statisticNode) == 0) == yes) { sb.append("\nAttribute " + param + " is " + (yes ? "not " : "") + "set"); }
            }

            if (sb.length() > 0) { sb.insert(1, "Address:" + statisticNode.toString()); }
            return sb;
        }

        /**
         * Creates address of statistics jdbc node from address of data source node
         *
         * @param address of data source node
         * @return address of jdbc statistics node
         */
        public ModelNode getStatAddr(ModelNode address) {
            return address.clone().add("statistics", "jdbc");
        }
    }

    @Resource(mappedName = jndiDs)
    DataSource ds;

    @Resource(mappedName = jndiXaDs)
    DataSource xaDs;

    /**
     * Define the deployment
     *
     * @return The deployment archive
     */
    @Deployment
    public static JavaArchive createDeployment() throws Exception {
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "stat.jar");
        ja.addClasses(DataSourceJdbcStatisticsTestCase.class, MgmtOperationException.class, XMLElementReader.class,
                XMLElementWriter.class, AbstractMgmtTestBase.class, JcaMgmtServerSetupTask.class, JcaMgmtBase.class,
                ContainerResourceMgmtTestBase.class)
                .addAsManifestResource(
                        new StringAsset(
                                "Dependencies: org.jboss.as.controller-client,org.jboss.dmr,org.jboss.as.cli,org.jboss.as.connector \n"),
                        "MANIFEST.MF");
        return ja;
    }

    /**
     * Helps to test statistics of data source, tries to get prepared statement from cache
     *
     * @param d - DataSource to test
     * @throws Exception
     */
    public void statisticsTest(DataSource d) throws Exception {

        Connection c = d.getConnection();
        for (int i = 1; i <= 5; i++) {
            PreparedStatement s = c.prepareStatement("select " + i);
            s.execute();
            s.close();
        }
        for (int i = 5; i > 0; i--) {
            PreparedStatement s = c.prepareStatement("select " + i);
            s.execute();
            s.close();
        }

        c.close();
    }

    @Test
    public void testDs() throws Exception {
        statisticsTest(ds);
    }

    @Test
    public void testXaDs() throws Exception {
        statisticsTest(xaDs);
    }
}
