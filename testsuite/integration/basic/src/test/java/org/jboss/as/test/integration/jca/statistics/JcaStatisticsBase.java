/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.statistics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.jboss.as.test.integration.jca.JcaMgmtBase;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * Base class for Jakarta Connectors statistics tests
 *
 * @author <a href="mailto:vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
public abstract class JcaStatisticsBase extends JcaMgmtBase {

    public static final Logger logger = Logger.getLogger(JcaStatisticsBase.class);


    /**
     * Default test for pool statistics: flush pool, then test connection
     *
     * @param connectionNode - where to test
     * @param statName       - name of statistics parameter
     * @throws Exception
     */
    protected void testStatistics(ModelNode connectionNode) throws Exception {
        ModelNode statisticsNode = translateFromConnectionToStatistics(connectionNode);
        writeAttribute(statisticsNode, "statistics-enabled", "true");

        executeOnNode(connectionNode, "flush-all-connection-in-pool");
        assertStatisticsShouldBeSet(statisticsNode, false);

        executeOnNode(connectionNode, "test-connection-in-pool");
        assertStatisticsShouldBeSet(statisticsNode, true);
    }

    /**
     * Test for pool statistics: flush pool, then test connection twice
     *
     * @param connectionNode - where to test
     * @param statName       - name of statistics parameter
     * @throws Exception
     */
    protected void testStatisticsDouble(ModelNode connectionNode) throws Exception {
        ModelNode statisticsNode = translateFromConnectionToStatistics(connectionNode);
        executeOnNode(connectionNode, "flush-all-connection-in-pool");
        executeOnNode(connectionNode, "test-connection-in-pool");
        executeOnNode(connectionNode, "test-connection-in-pool");
        assertStatisticsShouldBeSet(statisticsNode, true);
    }


    /**
     * Tests, if changing statistics in node1 don't change statistics in other node2
     *
     * @param node1
     * @param node2
     * @throws Exception
     */
    protected void testInterference(ModelNode node1, ModelNode node2) throws Exception {
        ModelNode statisticsNode = translateFromConnectionToStatistics(node2);
        executeOnNode(node1, "flush-all-connection-in-pool");
        executeOnNode(node2, "flush-all-connection-in-pool");
        resetStatistics(statisticsNode);
        executeOnNode(node1, "test-connection-in-pool");
        assertStatisticsShouldBeSet(statisticsNode, false);
    }

    /**
     * Resets stat counters and enables statistics on given node
     *
     * @param statisticsNode
     * @throws Exception
     */
    protected void resetStatistics(ModelNode statisticsNode) throws Exception {
        // statistics only reset when the value of "statistics-enabled" actually changes, so switch to false then true
        writeAttribute(statisticsNode, "statistics-enabled", "false");
        writeAttribute(statisticsNode, "statistics-enabled", "true");
    }

    /**
     * Checks if statistics properties set correctly
     *
     * @param node,on which to test
     * @param yes     - should be properties set or not
     * @throws Exception
     */
    protected void assertStatisticsShouldBeSet(ModelNode node, boolean yes) throws Exception {
        int avail = getStatisticsAttribute("AvailableCount", node);
        int active = getStatisticsAttribute("ActiveCount", node);
        int maxUsed = getStatisticsAttribute("MaxUsedCount", node);
        logger.trace("Node:" + node.toString() + "\n" + "Available:" + avail + "\n" + "Active:" + active + "\n" + "MaxUsed:"
                + maxUsed + "\n");
        assertTrue(avail > 0);
        if (yes) {
            assertTrue("active==" + active, active > 0);
            assertTrue("maxused==" + maxUsed, maxUsed > 0);
        } else {
            assertEquals(0, active);
            assertEquals(0, maxUsed);
        }
    }

    /**
     * Subclass should implement this method. It translates address of connection node to the address of statistics node for
     * chosen subsystem
     *
     * @param connection Node
     * @return statistics Node
     */
    public abstract ModelNode translateFromConnectionToStatistics(ModelNode connectionNode);
}
