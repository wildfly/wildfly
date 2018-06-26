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

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.ServerSnapshot;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Data source statistics testCase
 *
 * @author <a href="mailto:vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DataSourcePoolStatisticsTestCase extends JcaStatisticsBase {

    static int dsCount = 0;
    static int xaDsCount = 0;


    private ModelNode getDsAddress(int count, boolean xa) {
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, "datasources");
        address.add((xa ? "xa-" : "") + "data-source", getJndi(count, xa));
        address.protect();
        return address;
    }

    private ModelNode createDataSource(boolean xa, int minPoolSize, int maxPoolSize, boolean prefill) throws Exception {
        ModelNode address;
        String jndiName;
        if (xa) {
            xaDsCount++;
            jndiName = getJndi(xaDsCount, xa);
            address = getDsAddress(xaDsCount, xa);
        } else {
            dsCount++;
            jndiName = getJndi(dsCount, xa);
            address = getDsAddress(dsCount, xa);
        }

        ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).set(address);
        operation.get("jndi-name").set(jndiName);
        operation.get("driver-name").set("h2");
        operation.get("enabled").set("false");
        if (!xa) { operation.get("connection-url").set("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"); }
        operation.get("min-pool-size").set(minPoolSize);
        operation.get("max-pool-size").set(maxPoolSize);
        operation.get("pool-prefill").set(prefill);
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

    private AutoCloseable snapshot;

    @Before
    public void snapshot() {
        snapshot = ServerSnapshot.takeSnapshot(getManagementClient());
    }

    @After
    public void closeDataSources() throws Exception {
        snapshot.close();
        snapshot = null;
        dsCount = 0;
        xaDsCount = 0;
    }

    public String getJndi(int count, boolean xa) {
        return "java:/datasources/" + (xa ? "Xa" : "") + "Ds" + String.valueOf(count);
    }

    @Test
    public void testOneDs() throws Exception {
        ModelNode ds1 = createDataSource(false, 0, 20, false);

        testStatistics(ds1);
        testStatisticsDouble(ds1);
    }

    @Test
    public void testTwoDs() throws Exception {
        ModelNode ds1 = createDataSource(false, 0, 10, false);
        ModelNode ds2 = createDataSource(false, 0, 6, true);

        testStatistics(ds1);
        testStatistics(ds2);
        testStatisticsDouble(ds1);
        testStatisticsDouble(ds2);
        testInterference(ds1, ds2);
        testInterference(ds2, ds1);
    }

    @Test
    public void testOneXaDs() throws Exception {
        ModelNode ds1 = createDataSource(true, 0, 10, true);

        testStatistics(ds1);
        testStatisticsDouble(ds1);
    }

    @Test
    public void testTwoXaDs() throws Exception {
        ModelNode ds1 = createDataSource(true, 0, 1, false);
        ModelNode ds2 = createDataSource(true, 0, 1, true);

        testStatistics(ds1);
        testStatistics(ds2);
        testStatisticsDouble(ds1);
        testStatisticsDouble(ds2);
        testInterference(ds1, ds2);
        testInterference(ds2, ds1);
    }

    @Test
    public void testXaPlusDs() throws Exception {
        ModelNode ds1 = createDataSource(false, 0, 3, false);
        ModelNode ds2 = createDataSource(true, 0, 4, true);

        testStatistics(ds1);
        testStatistics(ds2);
        testStatisticsDouble(ds1);
        testStatisticsDouble(ds2);
        testInterference(ds1, ds2);
        testInterference(ds2, ds1);
    }

    @Override
    public ModelNode translateFromConnectionToStatistics(ModelNode connectionNode) {
        ModelNode statNode = connectionNode.clone();
        statNode.add("statistics", "pool");
        return statNode;
    }
}
