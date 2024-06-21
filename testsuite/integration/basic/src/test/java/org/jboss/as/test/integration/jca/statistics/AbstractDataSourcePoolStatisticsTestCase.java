/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jca.statistics;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.sql.DataSource;

import jakarta.annotation.Resource;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;

public abstract class AbstractDataSourcePoolStatisticsTestCase {

    private Set<Connection> connections = new CopyOnWriteArraySet<>();

    @ArquillianResource
    protected ManagementClient managementClient;

    @Resource(lookup = "java:jboss/datasources/ExampleDS")
    private DataSource dataSource;

    // /subsystem=datasources/data-source=ExampleDS/statistics=pool:read-attribute(name=AvailableCount)
    protected int readStatisticsAttribute(String attributeName) throws Exception {
        PathAddress statisticsPathAddress = PathAddress.parseCLIStyleAddress("/subsystem=datasources/data-source=ExampleDS/statistics=pool");
        ModelNode readAttributeOperation = Util.getReadAttributeOperation(statisticsPathAddress, attributeName);

        ModelNode result = managementClient.getControllerClient().execute(readAttributeOperation);
        assertThat("Failed to read statistics: " + result, Operations.isSuccessfulOutcome(result), is(true));
        return Operations.readResult(result).asInt();
    }

    // /subsystem=datasources/data-source=ExampleDS/statistics=pool/clear-statistics
    protected void clearStatistics() throws Exception {
        PathAddress statisticsPathAddress = PathAddress.parseCLIStyleAddress("/subsystem=datasources/data-source=ExampleDS/statistics=pool/");
        ModelNode operation = Util.createOperation("clear-statistics", statisticsPathAddress);
        ModelNode result = managementClient.getControllerClient().execute(operation);
        assertThat("Failed to configure connection pool: " + result, Operations.isSuccessfulOutcome(result), is(true));
    }

    protected void allocateConnection() throws SQLException {
        connections.add(dataSource.getConnection());
    }

    protected void clearConnections() {
        connections.forEach(connection -> {
            try {
                connection.close();
            } catch (SQLException sqle) {
                throw new RuntimeException(sqle);
            }
        });
    }
}
