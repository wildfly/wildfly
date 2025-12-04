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
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

@RunWith(Arquillian.class)
public class GroupDMRTestCase extends AbstractClusteringTestCase {
    private static final String MODULE_NAME = GroupListenerTestCase.class.getSimpleName();
    private static Logger log = Logger.getLogger(GroupDMRTestCase.class);

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static WebArchive createDeployment1() {
        return ShrinkWrap.create(WebArchive.class, MODULE_NAME + ".war").setWebXML(GroupDMRTestCase.class.getPackage(),
                "web.xml");
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static WebArchive createDeployment2() {
        return ShrinkWrap.create(WebArchive.class, MODULE_NAME + ".war").setWebXML(GroupDMRTestCase.class.getPackage(),
                "web.xml");
    }

    @ArquillianResource
    @TargetsContainer(NODE_1)
    ManagementClient clientNode1;

    @ArquillianResource
    @TargetsContainer(NODE_2)
    ManagementClient clientNode2;

    @Test
    public void test() throws IOException {
        ModelControllerClient modelControllerClientNode1 = clientNode1.getControllerClient();
        ModelControllerClient modelControllerClientNode2 = clientNode2.getControllerClient();

        ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
        ModelNode address = operation.get(OP_ADDR);
        address.add("subsystem", "jgroups");
        address.add("channel", "ee");
        operation.get("name").set("view");
        operation.get("include-defaults").set(true);

        // normal operation
        ModelNode outcomeNode1 = modelControllerClientNode1.execute(operation);
        log.debugf("outcome node 1 %s", outcomeNode1.toString());
        Assert.assertEquals(SUCCESS, outcomeNode1.get(OUTCOME).asString());
        String result1 = outcomeNode1.get(RESULT).asString();
        assertThat(result1, allOf(containsString(NODE_1), containsString(NODE_2)));

        ModelNode outcomeNode2 = modelControllerClientNode2.execute(operation);
        log.debugf("outcome node 2 %s", outcomeNode2.toString());
        Assert.assertEquals(SUCCESS, outcomeNode2.get(OUTCOME).asString());
        String result2 = outcomeNode2.get(RESULT).asString();
        assertThat(result2, allOf(containsString(NODE_1), containsString(NODE_2)));

        stop(NODE_2);

        // we check it reflects the new state so the result is not cached.
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            ModelNode outcome = modelControllerClientNode1.execute(operation);
            log.debugf("outcome node 1 %s", outcome.toString());
            Assert.assertEquals(SUCCESS, outcome.get(OUTCOME).asString());
            String result = outcome.get(RESULT).asString();
            assertThat(result, allOf(containsString(NODE_1), not(containsString(NODE_2))));
        });

        start(NODE_2);

        // we check it reflects the new state so the result is not cached when a new node is added
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            ModelNode outcome = modelControllerClientNode1.execute(operation);
            log.debugf("outcome node 1 %s", outcome.toString());
            Assert.assertEquals(SUCCESS, outcome.get(OUTCOME).asString());
            String result = outcome.get(RESULT).asString();
            assertThat(result, allOf(containsString(NODE_1), containsString(NODE_2)));
        });
    }

}
