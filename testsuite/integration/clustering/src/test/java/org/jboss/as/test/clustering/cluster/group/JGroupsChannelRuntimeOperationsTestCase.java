/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.group;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Duration;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ArquillianExtension.class)
public class JGroupsChannelRuntimeOperationsTestCase extends AbstractClusteringTestCase {
    private static final String MODULE_NAME = JGroupsChannelRuntimeOperationsTestCase.class.getSimpleName();
    private static final Logger log = Logger.getLogger(JGroupsChannelRuntimeOperationsTestCase.class);

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static WebArchive createDeployment1() {
        return ShrinkWrap.create(WebArchive.class, MODULE_NAME + ".war")
                .setWebXML(JGroupsChannelRuntimeOperationsTestCase.class.getClassLoader().getResource("org/jboss/as/test/clustering/cluster/web/web.xml"));
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static WebArchive createDeployment2() {
        return ShrinkWrap.create(WebArchive.class, MODULE_NAME + ".war")
                .setWebXML(JGroupsChannelRuntimeOperationsTestCase.class.getClassLoader().getResource("org/jboss/as/test/clustering/cluster/web/web.xml"));
    }

    @ArquillianResource
    @TargetsContainer(NODE_1)
    ManagementClient clientNode1;

    @ArquillianResource
    @TargetsContainer(NODE_2)
    ManagementClient clientNode2;

    @Test
    public void test() throws IOException {
        // Verify all runtime attributes in a single composite operation
        ChannelAttributes attrs1 = readAllChannelAttributes(clientNode1);
        ChannelAttributes attrs2 = readAllChannelAttributes(clientNode2);

        // Verify view contains both nodes
        assertThat(attrs1.view, allOf(containsString(NODE_1), containsString(NODE_2)));
        assertThat(attrs2.view, allOf(containsString(NODE_1), containsString(NODE_2)));

        // Verify address is not undefined
        assertThat("Address should not be undefined", attrs1.address, not(containsString("undefined")));
        assertThat("Address should not be undefined", attrs2.address, not(containsString("undefined")));
        log.debugf("Node 1 address: %s, Node 2 address: %s", attrs1.address, attrs2.address);

        // Verify state is CONNECTED
        assertEquals("CONNECTED", attrs1.state, "Channel state should be CONNECTED");
        assertEquals("CONNECTED", attrs2.state, "Channel state should be CONNECTED");
        log.debugf("Node 1 state: %s, Node 2 state: %s", attrs1.state, attrs2.state);

        // Verify counters are non-negative (may be 0 initially, but validates the metric path works)
        assertTrue(attrs1.receivedMessages >= 0, "received-messages should be non-negative");
        assertTrue(attrs1.sentMessages >= 0, "sent-messages should be non-negative");
        assertTrue(attrs1.receivedBytes >= 0, "received-bytes should be non-negative");
        assertTrue(attrs1.sentBytes >= 0, "sent-bytes should be non-negative");
        log.debugf("Node 1 counters - received-messages: %d, sent-messages: %d, received-bytes: %d, sent-bytes: %d",
                attrs1.receivedMessages, attrs1.sentMessages, attrs1.receivedBytes, attrs1.sentBytes);

        // Verify runtime attributes reflect membership changes (not cached)
        stop(NODE_2);
        await().atMost(Duration.ofSeconds(GRACE_TIME_TO_MEMBERSHIP_CHANGE)).untilAsserted(() -> {
            ChannelAttributes attrs = readAllChannelAttributes(clientNode1);
            assertThat(attrs.view, allOf(containsString(NODE_1), not(containsString(NODE_2))));
        });

        // Verify runtime attributes update when node rejoins
        start(NODE_2);
        await().atMost(Duration.ofSeconds(GRACE_TIME_TO_MEMBERSHIP_CHANGE)).untilAsserted(() -> {
            ChannelAttributes attrs = readAllChannelAttributes(clientNode1);
            assertThat(attrs.view, allOf(containsString(NODE_1), containsString(NODE_2)));
            assertEquals("CONNECTED", attrs.state, "Channel should still be CONNECTED after node rejoins");
        });
    }

    /**
     * Reads all channel runtime attributes using a single read-resource operation.
     */
    private ChannelAttributes readAllChannelAttributes(ManagementClient client) throws IOException {
        PathAddress channelAddress = pathAddress(pathElement("subsystem", "jgroups"), pathElement("channel", "ee"));

        // Use single read-resource operation with include-runtime=true to get all attributes
        ModelNode operation = new ModelNode();
        operation.get("address").set(channelAddress.toModelNode());
        operation.get("operation").set("read-resource");
        operation.get("include-runtime").set(true);

        ModelNode outcome = client.getControllerClient().execute(operation);
        log.debugf("read-resource result: %s", outcome.toString());
        assertEquals(SUCCESS, outcome.get(OUTCOME).asString(), "read-resource operation failed: " + outcome);

        // Extract runtime attributes from result - all attributes are keyed by name
        ModelNode result = outcome.get(RESULT);

        return new ChannelAttributes(
                result.get("view").asString(),
                result.get("address").asString(),
                result.get("state").asString(),
                result.get("received-messages").asLong(),
                result.get("sent-messages").asLong(),
                result.get("received-bytes").asLong(),
                result.get("sent-bytes").asLong()
        );
    }

    /**
     * Immutable container for all channel runtime attributes.
     */
    private record ChannelAttributes(
            String view,
            String address,
            String state,
            long receivedMessages,
            long sentMessages,
            long receivedBytes,
            long sentBytes
    ) {}
}
