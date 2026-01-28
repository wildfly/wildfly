/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.group;

import java.io.IOException;
import java.time.Duration;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

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
import static org.jboss.as.controller.operations.common.Util.getReadAttributeOperation;

@RunWith(Arquillian.class)
public class JGroupsChannelRuntimeOperationsTestCase extends AbstractClusteringTestCase {
    private static final long TIMEOUT_SECONDS = TimeoutUtil.adjust(10);
    private static final String MODULE_NAME = JGroupsChannelRuntimeOperationsTestCase.class.getSimpleName();
    private static Logger log = Logger.getLogger(JGroupsChannelRuntimeOperationsTestCase.class);

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static WebArchive createDeployment1() {
        return ShrinkWrap.create(WebArchive.class, MODULE_NAME + ".war")
                .setWebXML(JGroupsChannelRuntimeOperationsTestCase.class.getPackage(), "web.xml");
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static WebArchive createDeployment2() {
        return ShrinkWrap.create(WebArchive.class, MODULE_NAME + ".war")
                .setWebXML(JGroupsChannelRuntimeOperationsTestCase.class.getPackage(), "web.xml");
    }

    @ArquillianResource
    @TargetsContainer(NODE_1)
    ManagementClient clientNode1;

    @ArquillianResource
    @TargetsContainer(NODE_2)
    ManagementClient clientNode2;

    @Test
    public void test() throws IOException {
        // normal operation
        assertThat(getChannelView(clientNode1), allOf(containsString(NODE_1), containsString(NODE_2)));
        assertThat(getChannelView(clientNode2), allOf(containsString(NODE_1), containsString(NODE_2)));

        // we check it reflects the new state so the result is not cached. when a node is removed
        stop(NODE_2);
        await().atMost(Duration.ofSeconds(TIMEOUT_SECONDS)).untilAsserted(() -> {
            assertThat(getChannelView(clientNode1), allOf(containsString(NODE_1), not(containsString(NODE_2))));
        });

        // we check it reflects the new state so the result is not cached when a new node is added
        start(NODE_2);
        await().atMost(Duration.ofSeconds(TIMEOUT_SECONDS)).untilAsserted(() -> {
            assertThat(getChannelView(clientNode1), allOf(containsString(NODE_1), containsString(NODE_2)));
        });
    }

    private String getChannelView(ManagementClient client) throws IOException {
        PathAddress channelAddress = pathAddress(pathElement("subsystem", "jgroups"), pathElement("channel", "ee"));
        ModelNode operation = getReadAttributeOperation(channelAddress, "view");
        operation.get("include-defaults").set(true);

        ModelNode outcome = client.getControllerClient().execute(operation);
        log.debugf("outcome %s node %s", outcome.toString(), client);
        Assert.assertEquals(SUCCESS, outcome.get(OUTCOME).asString());
        return outcome.get(RESULT).asString();
    }
}
